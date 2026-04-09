package com.manqiYang.hotelSystem.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常（自定义异常）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {

        log.error("业务异常：{}", e.getMessage(), e);

        return Result.error(e.getMessage());
    }

    /**
     * 参数校验异常（如果你后面用 @Valid 会用到）
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {

        log.error("系统异常：{}", e.getMessage(), e);

        return Result.error("系统异常，请联系管理员");
    }
}
