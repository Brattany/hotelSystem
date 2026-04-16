package com.manqiYang.hotelSystem.service.impl.agent;

import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentQueryRequest;
import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentSyncRequest;
import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentUpdateRequest;
import com.manqiYang.hotelSystem.entity.agent.KnowledgeDocument;
import com.manqiYang.hotelSystem.mapper.agent.KnowledgeDocumentMapper;
import com.manqiYang.hotelSystem.service.agent.KnowledgeDocumentService;
import com.manqiYang.hotelSystem.vo.agent.KnowledgeDocumentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private static final Set<String> ALLOWED_SUFFIXES = Set.of(".txt", ".md", ".pdf", ".docx");
    private static final String SYNC_STATUS_PENDING = "PENDING";
    private static final String SYNC_STATUS_SUCCESS = "SUCCESS";
    private static final String SYNC_STATUS_FAILED = "FAILED";

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Value("${app.upload.base-dir:front_uploads}")
    private String uploadBaseDir;

    @Value("${agent.knowledge-document.dir:knowledge}")
    private String knowledgeDocumentDir;

    @Value("${agent.rag.base-url:http://localhost:3366}")
    private String ragBaseUrl;

    @Value("${agent.rag.collection-name:hotel_knowledge}")
    private String defaultCollectionName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO uploadDocument(
            MultipartFile file,
            String title,
            Long hotelId,
            Long agentId,
            String docType,
            String collectionName,
            Boolean replaceBySource
    ) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文档不能为空");
        }

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        validateFileType(originalName);

        SavedFileInfo savedFileInfo = saveFile(file, originalName);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setHotelId(hotelId);
        document.setAgentId(agentId);
        document.setTitle(StringUtils.hasText(title) ? title.trim() : originalName);
        document.setDocType(StringUtils.hasText(docType) ? docType.trim() : "policy");
        document.setFileName(savedFileInfo.fileName());
        document.setOriginalName(originalName);
        document.setFilePath(savedFileInfo.filePath());
        document.setFileUrl(savedFileInfo.fileUrl());
        document.setSourceName(buildSourceName(hotelId, originalName));
        document.setCollectionName(resolveCollectionName(collectionName));
        document.setFileSize(file.getSize());
        document.setChunkCount(0);
        document.setSyncStatus(SYNC_STATUS_PENDING);
        document.setSyncMessage("文档已上传，等待同步");
        document.setEnabled(1);
        document.setLastSyncTime(null);

        if (!knowledgeDocumentMapper.insert(document)) {
            throw new RuntimeException("保存知识文档记录失败");
        }

        return syncDocumentInternal(
                document,
                replaceBySource != null ? replaceBySource : Boolean.TRUE,
                null,
                null
        );
    }

    @Override
    public List<KnowledgeDocumentVO> listDocuments(KnowledgeDocumentQueryRequest request) {
        return knowledgeDocumentMapper.selectList(request == null ? new KnowledgeDocumentQueryRequest() : request);
    }

    @Override
    public KnowledgeDocumentVO getDocument(Long documentId) {
        if (documentId == null) {
            throw new RuntimeException("documentId 不能为空");
        }
        KnowledgeDocumentVO documentVO = knowledgeDocumentMapper.selectVoById(documentId);
        if (documentVO == null) {
            throw new RuntimeException("知识文档不存在");
        }
        return documentVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO updateDocument(Long documentId, KnowledgeDocumentUpdateRequest request) {
        if (documentId == null) {
            throw new RuntimeException("documentId 不能为空");
        }
        if (request == null) {
            throw new RuntimeException("请求体不能为空");
        }
        KnowledgeDocument document = requireDocument(documentId);
        if (StringUtils.hasText(request.getTitle())) {
            document.setTitle(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getDocType())) {
            document.setDocType(request.getDocType().trim());
        }
        if (request.getHotelId() != null) {
            document.setHotelId(request.getHotelId());
            document.setSourceName(buildSourceName(request.getHotelId(), document.getOriginalName()));
        }
        if (request.getAgentId() != null) {
            document.setAgentId(request.getAgentId());
        }
        if (StringUtils.hasText(request.getCollectionName())) {
            document.setCollectionName(request.getCollectionName().trim());
        }
        if (request.getEnabled() != null) {
            document.setEnabled(request.getEnabled());
        }
        if (!knowledgeDocumentMapper.updateById(document)) {
            throw new RuntimeException("更新知识文档失败");
        }
        return getDocument(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long documentId) {
        KnowledgeDocument document = requireDocument(documentId);
        deleteRagSource(document);
        deleteLocalFile(document.getFilePath());
        return knowledgeDocumentMapper.deleteById(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO syncDocument(Long documentId, KnowledgeDocumentSyncRequest request) {
        KnowledgeDocument document = requireDocument(documentId);
        Boolean replaceBySource = request == null || request.getReplaceBySource() == null ? Boolean.TRUE : request.getReplaceBySource();
        Integer chunkSize = request == null ? null : request.getChunkSize();
        Integer chunkOverlap = request == null ? null : request.getChunkOverlap();
        if (request != null && StringUtils.hasText(request.getCollectionName())) {
            document.setCollectionName(request.getCollectionName().trim());
        }
        return syncDocumentInternal(document, replaceBySource, chunkSize, chunkOverlap);
    }

    private KnowledgeDocument requireDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new RuntimeException("知识文档不存在");
        }
        return document;
    }

    private KnowledgeDocumentVO syncDocumentInternal(
            KnowledgeDocument document,
            Boolean replaceBySource,
            Integer chunkSize,
            Integer chunkOverlap
    ) {
        document.setSyncStatus(SYNC_STATUS_PENDING);
        document.setSyncMessage("正在同步到知识库");
        document.setLastSyncTime(LocalDateTime.now());
        knowledgeDocumentMapper.updateById(document);

        try {
            Map<String, Object> syncData = uploadToRag(document, replaceBySource, chunkSize, chunkOverlap);
            document.setSyncStatus(SYNC_STATUS_SUCCESS);
            document.setSyncMessage("同步成功");
            document.setChunkCount(readInt(syncData.get("chunk_count"), 0));
            document.setFileHash(readString(syncData.get("file_hash"), document.getFileHash()));
            document.setCollectionName(readString(syncData.get("collection_name"), document.getCollectionName()));
            document.setLastSyncTime(LocalDateTime.now());
        } catch (Exception exception) {
            document.setSyncStatus(SYNC_STATUS_FAILED);
            document.setSyncMessage(limitMessage(exception.getMessage()));
            document.setLastSyncTime(LocalDateTime.now());
        }

        if (!knowledgeDocumentMapper.updateById(document)) {
            throw new RuntimeException("更新同步结果失败");
        }
        return getDocument(document.getDocumentId());
    }

    private SavedFileInfo saveFile(MultipartFile file, String originalName) {
        try {
            Path uploadRoot = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
            Path targetDir = uploadRoot.resolve(knowledgeDocumentDir).normalize();
            Files.createDirectories(targetDir);

            String extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetFile = targetDir.resolve(uniqueFileName).normalize();
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "/uploads/" + knowledgeDocumentDir + "/" + uniqueFileName;
            String accessUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(relativePath)
                    .toUriString();
            return new SavedFileInfo(uniqueFileName, targetFile.toString(), accessUrl);
        } catch (IOException exception) {
            throw new RuntimeException("保存知识文档文件失败: " + exception.getMessage());
        }
    }

    private void deleteLocalFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ignored) {
            // best effort
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> uploadToRag(
            KnowledgeDocument document,
            Boolean replaceBySource,
            Integer chunkSize,
            Integer chunkOverlap
    ) {
        String requestUrl = ragBaseUrl.replaceAll("/+$", "") + "/rag/upload";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(Paths.get(document.getFilePath())));
        body.add("collection_name", resolveCollectionName(document.getCollectionName()));
        body.add("doc_type", document.getDocType());
        body.add("replace_by_source", String.valueOf(replaceBySource == null || replaceBySource));
        if (document.getHotelId() != null) {
            body.add("hotel_id", String.valueOf(document.getHotelId()));
        }
        if (chunkSize != null && chunkSize > 0) {
            body.add("chunk_size", String.valueOf(chunkSize));
        }
        if (chunkOverlap != null && chunkOverlap >= 0) {
            body.add("chunk_overlap", String.valueOf(chunkOverlap));
        }

        ResponseEntity<Map> response = restTemplate.postForEntity(requestUrl, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("RAG 服务返回空响应");
        }

        Object codeValue = responseBody.get("code");
        if (codeValue instanceof Number && ((Number) codeValue).intValue() != 200) {
            throw new RuntimeException(readString(responseBody.get("message"), "RAG 同步失败"));
        }

        Object dataValue = responseBody.get("data");
        if (!(dataValue instanceof Map<?, ?> dataMap)) {
            throw new RuntimeException("RAG 服务返回数据格式不正确");
        }
        return (Map<String, Object>) dataMap;
    }

    private void deleteRagSource(KnowledgeDocument document) {
        if (!StringUtils.hasText(document.getSourceName())) {
            return;
        }
        try {
            String requestUrl = ragBaseUrl.replaceAll("/+$", "") + "/rag/delete_source";
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = Map.of(
                    "source", document.getSourceName(),
                    "collection_name", resolveCollectionName(document.getCollectionName())
            );
            restTemplate.postForEntity(requestUrl, new HttpEntity<>(payload, headers), Map.class);
        } catch (Exception ignored) {
            // best effort
        }
    }

    private String normalizeOriginalName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            throw new RuntimeException("原始文件名不能为空");
        }
        return Paths.get(originalName).getFileName().toString();
    }

    private void validateFileType(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new RuntimeException("知识文档必须包含文件后缀");
        }
        String suffix = originalName.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SUFFIXES.contains(suffix)) {
            throw new RuntimeException("知识文档仅支持 txt、md、pdf、docx");
        }
    }

    private String buildSourceName(Long hotelId, String originalName) {
        String safeName = originalName.replaceAll("\\s+", "_");
        return hotelId == null ? safeName : ("hotel_" + hotelId + "_" + safeName);
    }

    private String resolveCollectionName(String collectionName) {
        return StringUtils.hasText(collectionName) ? collectionName.trim() : defaultCollectionName;
    }

    private String readString(Object value, String fallback) {
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue;
        }
        return fallback;
    }

    private Integer readInt(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String limitMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "同步失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private record SavedFileInfo(String fileName, String filePath, String fileUrl) {
    }
}
