package com.piaochu.app;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.piaochu.app.AuthService.fail;
/** 单实例的保底限流。只读取实际连接IP，不信任任意客户端伪造的X-Forwarded-For。多实例应在入口使用共享限流。 */
@Component
class RequestLimits {
    private final Clock clock;private final Map<String,Window> windows=new HashMap<>();
    RequestLimits(Clock clock){this.clock=clock;}
    private static class Window {long minute;int count;Window(long m){minute=m;}}
    synchronized void check(String ip,boolean auth) {
        long minute=clock.instant().getEpochSecond()/60;String key=(auth?"auth:":"api:")+ip;
        if(windows.size()>10000){windows.entrySet().removeIf(e->e.getValue().minute<minute);if(windows.size()>10000)throw fail(429,"请求过多，请稍后再试");}
        Window w=windows.computeIfAbsent(key,k->new Window(minute));
        if(w.minute!=minute){w.minute=minute;w.count=0;}
        if(++w.count>(auth?30:600))throw fail(429,"请求过多，请一分钟后重试");
    }
}
