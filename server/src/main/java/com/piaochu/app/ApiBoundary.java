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
@Component
class ApiBoundary extends OncePerRequestFilter {
    private static final Logger LOG=LoggerFactory.getLogger(ApiBoundary.class);
    private final AuthService auth;private final ObjectMapper json;private final RequestLimits limits;
    ApiBoundary(AuthService auth,ObjectMapper json,RequestLimits limits) { this.auth=auth;this.json=json;this.limits=limits; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String candidate=req.getHeader("X-Request-Id");String id=candidate!=null&&candidate.matches("[a-zA-Z0-9-]{1,80}")?candidate:UUID.randomUUID().toString();
        res.setHeader("X-Request-Id",id);res.setHeader("Cache-Control","no-store");MDC.put("requestId",id);
        long started=System.nanoTime();
        try {
            String path=req.getRequestURI();
            if(req.getContentLengthLong()>131072)throw fail(413,"请求内容过大");
            if(path.startsWith("/api/"))limits.check(req.getRemoteAddr(),path.equals("/api/auth/login")||path.equals("/api/auth/register"));
            boolean publicApi=Set.of("/api/health","/api/catalog","/api/auth/login","/api/auth/register").contains(path);
            if(path.startsWith("/api/")&&!publicApi) {
                Contracts.Actor actor=auth.authenticate(req.getHeader("Authorization"));
                if(path.startsWith("/api/operator/")&&!actor.role().equals("OPERATOR")) throw fail(403,"需要运营账号");
                req.setAttribute("actor",actor);
            }
            chain.doFilter(req,res);
        } catch(ResponseStatusException error) {
            res.setStatus(error.getStatusCode().value());res.setContentType("application/json;charset=UTF-8");
            json.writeValue(res.getWriter(),Map.of("message",Objects.requireNonNullElse(error.getReason(),"请求失败"),"requestId",id));
        } finally {
            // 不记录 Authorization、请求体、手机号和地址；抓包只在调试环境主动开启。
            LOG.info("{} {} -> {} ({}ms)",req.getMethod(),req.getRequestURI(),res.getStatus(),(System.nanoTime()-started)/1_000_000);MDC.remove("requestId");
        }
    }
}
