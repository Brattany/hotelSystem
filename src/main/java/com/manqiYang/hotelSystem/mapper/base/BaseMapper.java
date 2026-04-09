package com.manqiYang.hotelSystem.mapper.base;

public interface BaseMapper<T, ID> {

    boolean insert(T entity);

    boolean updateById(T entity);

    boolean deleteById(ID id);

    T selectById(ID id);
}