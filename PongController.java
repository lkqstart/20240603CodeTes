package org.example.pong.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.pong.RateLimiterUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
class PongController {

    @GetMapping("/pong")
    public Mono<ResponseEntity<String>> pong() {
        List<String> fileNames = Arrays.asList("global.lock");
        Boolean flag = false;
        for (String fileName : fileNames) {
            if (RateLimiterUtil.tryLock(fileName, 1L)) {
                flag = true;
                break;
            }
        }
        if (flag) {
            return Mono.just(ResponseEntity.ok("Pong Response World"));
        }

        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("429 Too Many Requests"));
    }


}



