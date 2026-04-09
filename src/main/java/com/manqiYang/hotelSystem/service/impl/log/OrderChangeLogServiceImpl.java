package com.manqiYang.hotelSystem.service.impl.log;

import com.manqiYang.hotelSystem.entity.log.OrderChangeLog;
import com.manqiYang.hotelSystem.mapper.log.OrderChangeLogMapper;
import com.manqiYang.hotelSystem.service.log.OrderChangeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderChangeLogServiceImpl implements OrderChangeLogService {

    @Autowired
    public OrderChangeLogMapper oclMapper;

    @Override
    public boolean create(OrderChangeLog log){
        return oclMapper.insert(log);
    }

    @Override
    public List<OrderChangeLog> getAll(){
        return oclMapper.selectAll();
    }

    @Override
    public OrderChangeLog getById(Long id){
        return oclMapper.selectById(id);
    }

    @Override
    public List<OrderChangeLog> getByCType(String changeType){
        return oclMapper.selectByCType(changeType);
    }

    @Override
    public List<OrderChangeLog> getByOType(String oType){
        return oclMapper.selectByOperator(oType);
    }

    @Override
    public boolean delete(Long id){
        return oclMapper.deleteById(id);
    }
}
