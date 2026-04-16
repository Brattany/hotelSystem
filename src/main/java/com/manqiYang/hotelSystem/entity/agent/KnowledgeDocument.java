package com.manqiYang.hotelSystem.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocument {

    private Long documentId;

    private Long hotelId;

    private Long agentId;

    private String title;

    private String docType;

    private String fileName;

    private String originalName;

    private String filePath;

    private String fileUrl;

    private String sourceName;

    private String collectionName;

    private String fileHash;

    private Long fileSize;

    private Integer chunkCount;

    private String syncStatus;

    private String syncMessage;

    private Integer enabled;

    private LocalDateTime lastSyncTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
