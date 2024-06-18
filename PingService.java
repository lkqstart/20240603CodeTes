package org.example.ping.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.ping.RateLimiterUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


@RestController
@Slf4j
public class PingController {

    private static final String PONG_SERVICE_URL = "http://localhost:8081/pong";


    @GetMapping("/ping")
    public ResponseEntity<String> ping(@RequestParam("fileNames") List<String> fileNames) {
        Boolean flag = false;
        //要操作的文件名列表
        for (String fileName : fileNames) {
            //尝试获取对应文件的锁.
            if (RateLimiterUtil.tryLock(fileName, 1L)) {
                flag = true;
                break;
            }
        }
        //文件的锁获取成功
        if (flag) {
            return sendPongRequest();
        }
            return sendPongRequest();
    }


    private ResponseEntity<String> sendPongRequest() {
        try {
            log.info("Ping Request Hello");
            HttpURLConnection connection = (HttpURLConnection) new URL(PONG_SERVICE_URL).openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
                //返回响应结果
                log.info(response.toString());
                return ResponseEntity.ok("200 Ping Request Hello" + " | " + response.toString());
            } else {
                log.warn("429 Too Many Requests");
                log.warn("430 Rate limited, Pong throttled it");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limited, Pong throttled it");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}

