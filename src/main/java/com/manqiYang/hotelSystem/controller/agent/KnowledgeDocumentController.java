package com.manqiYang.hotelSystem.controller.agent;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentQueryRequest;
import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentSyncRequest;
import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentUpdateRequest;
import com.manqiYang.hotelSystem.service.agent.KnowledgeDocumentService;
import com.manqiYang.hotelSystem.vo.agent.KnowledgeDocumentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/agent/knowledge-documents")
@CrossOrigin(origins = "*")
public class KnowledgeDocumentController {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @PostMapping("/upload")
    public Result<KnowledgeDocumentVO> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "hotelId", required = false) Long hotelId,
            @RequestParam(value = "agentId", required = false) Long agentId,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "collectionName", required = false) String collectionName,
            @RequestParam(value = "replaceBySource", required = false) Boolean replaceBySource
    ) {
        return execute(() -> knowledgeDocumentService.uploadDocument(
                file,
                title,
                hotelId,
                agentId,
                docType,
                collectionName,
                replaceBySource
        ));
    }

    @GetMapping
    public Result<List<KnowledgeDocumentVO>> list(KnowledgeDocumentQueryRequest request) {
        return execute(() -> knowledgeDocumentService.listDocuments(request));
    }

    @GetMapping("/{documentId}")
    public Result<KnowledgeDocumentVO> detail(@PathVariable Long documentId) {
        return execute(() -> knowledgeDocumentService.getDocument(documentId));
    }

    @PutMapping("/{documentId}")
    public Result<KnowledgeDocumentVO> update(
            @PathVariable Long documentId,
            @RequestBody KnowledgeDocumentUpdateRequest request
    ) {
        return execute(() -> knowledgeDocumentService.updateDocument(documentId, request));
    }

    @PostMapping("/{documentId}/sync")
    public Result<KnowledgeDocumentVO> sync(
            @PathVariable Long documentId,
            @RequestBody(required = false) KnowledgeDocumentSyncRequest request
    ) {
        return execute(() -> knowledgeDocumentService.syncDocument(documentId, request));
    }

    @DeleteMapping("/{documentId}")
    public Result<Boolean> delete(@PathVariable Long documentId) {
        return execute(() -> knowledgeDocumentService.deleteDocument(documentId));
    }

    private <T> Result<T> execute(Supplier<T> supplier) {
        try {
            return Result.success(supplier.get());
        } catch (RuntimeException exception) {
            return Result.error(exception.getMessage());
        }
    }
}
