package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

@Data
public class KnowledgeDocumentUpdateRequest {

    private Long hotelId;

    private Long agentId;

    private String title;

    private String docType;

    private String collectionName;

    private Integer enabled;
}
