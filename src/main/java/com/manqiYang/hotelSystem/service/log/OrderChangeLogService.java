package com.manqiYang.hotelSystem.service.log;

import com.manqiYang.hotelSystem.entity.log.OrderChangeLog;

import java.util.List;

public interface OrderChangeLogService {
    boolean create(OrderChangeLog log);

    List<OrderChangeLog> getAll();

    OrderChangeLog getById(Long id);

    List<OrderChangeLog> getByCType(String changeType);

    List<OrderChangeLog> getByOType(String oType);

    boolean delete(Long id);
}
