package com.manqiYang.hotelSystem.service.agent;

import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentQueryRequest;
import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentSyncRequest;
import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentUpdateRequest;
import com.manqiYang.hotelSystem.vo.agent.KnowledgeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocumentService {

    KnowledgeDocumentVO uploadDocument(
            MultipartFile file,
            String title,
            Long hotelId,
            Long agentId,
            String docType,
            String collectionName,
            Boolean replaceBySource
    );

    List<KnowledgeDocumentVO> listDocuments(KnowledgeDocumentQueryRequest request);

    KnowledgeDocumentVO getDocument(Long documentId);

    KnowledgeDocumentVO updateDocument(Long documentId, KnowledgeDocumentUpdateRequest request);

    boolean deleteDocument(Long documentId);

    KnowledgeDocumentVO syncDocument(Long documentId, KnowledgeDocumentSyncRequest request);
}
