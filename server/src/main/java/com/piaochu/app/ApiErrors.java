package com.piaochu.app;
import static com.piaochu.app.AuthService.fail;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> response(ResponseStatusException e) {
        return error(e.getStatusCode().value(),Objects.requireNonNullElse(e.getReason(),"请求失败"));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        return error(400,e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+"："+x.getDefaultMessage()).orElse("参数错误"));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> unreadable(Exception e) { return error(400,"请求 JSON 格式不正确"); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(Exception e) { return error(409,"数据已变化或操作重复，请刷新后核实"); }
    @ExceptionHandler(OptimisticLockingFailureException.class) ResponseEntity<?> version(Exception e) { return error(409,"订单状态已变化，请刷新后重试"); }
    @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e) {
        LoggerFactory.getLogger(ApiErrors.class).error("接口内部错误，类型 {}",e.getClass().getSimpleName());return error(500,"服务暂时不可用，请根据请求编号排查");
    }
    private ResponseEntity<?> error(int status,String message) { return ResponseEntity.status(status).body(Map.of("message",message,"requestId",Objects.requireNonNullElse(MDC.get("requestId"),"unknown"))); }
}
