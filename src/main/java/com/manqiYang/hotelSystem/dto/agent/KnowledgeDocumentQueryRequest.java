package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

@Data
public class KnowledgeDocumentQueryRequest {

    private Long hotelId;

    private Long agentId;

    private String title;

    private String docType;

    private String syncStatus;

    private Integer enabled;

    private String collectionName;
}
