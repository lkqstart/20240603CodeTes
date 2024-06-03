package com.lukaiqi.distributed.lock.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class PongController {
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long lastResetTime = System.currentTimeMillis();

    @GetMapping("/pong")
    public ResponseEntity<String> getPong() {
        synchronized (this) {
            //判断是否超出
            if (System.currentTimeMillis() - lastResetTime > 1000) {
                requestCount.set(0);
                lastResetTime = System.currentTimeMillis();
            }
            //判断是否超限
            if (requestCount.incrementAndGet() > 1) {
                //超限请求返回HTTP 429状态码
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
        }
        //请求成功
        return ResponseEntity.ok("World");
    }

}
