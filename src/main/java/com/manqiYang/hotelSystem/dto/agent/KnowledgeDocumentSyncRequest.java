package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

@Data
public class KnowledgeDocumentSyncRequest {

    private String collectionName;

    private Boolean replaceBySource;

    private Integer chunkSize;

    private Integer chunkOverlap;
}
