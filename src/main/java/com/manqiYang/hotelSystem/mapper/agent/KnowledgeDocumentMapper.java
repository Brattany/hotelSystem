package com.manqiYang.hotelSystem.mapper.agent;

import com.manqiYang.hotelSystem.dto.agent.KnowledgeDocumentQueryRequest;
import com.manqiYang.hotelSystem.entity.agent.KnowledgeDocument;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import com.manqiYang.hotelSystem.vo.agent.KnowledgeDocumentVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument, Long> {

    KnowledgeDocumentVO selectVoById(Long documentId);

    List<KnowledgeDocumentVO> selectList(KnowledgeDocumentQueryRequest request);
}
