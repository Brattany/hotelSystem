package com.manqiYang.hotelSystem.mapper.log;

import com.manqiYang.hotelSystem.entity.log.OrderChangeLog;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderChangeLogMapper extends BaseMapper<OrderChangeLog, Long> {

    List<OrderChangeLog> selectAll();

    List<OrderChangeLog> selectByCType(String changeType);

    List<OrderChangeLog> selectByOperator(String operatorType);
}

