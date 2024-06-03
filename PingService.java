package com.lukaiqi.distributed.lock.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.Duration;

@Service
@Slf4j
public class PingService {

    //使用@Scheduled注解，每秒尝试获取锁并发送请求。
    @Scheduled(fixedRate = 1000)
    public void FileLockSendPing() {
        try (FileChannel channel = new RandomAccessFile(new File("global.lock"), "rw").getChannel()) {
            FileLock lock = channel.tryLock();
            //确保在多个JVM实例中，每秒只有一个实例能发送请求。
            if (lock != null) {
                try {
                    //使用WebClient发送HTTP请求。
                    webClient.get()
                            .uri("/pong")
                            .retrieve()
                            .bodyToMono(String.class)
                            //输出对应结果
                            .subscribe(response -> log.info("Ping sent successfully: " + response),
                                    error -> log.error("Ping failed: " + error.getMessage()));
                } finally {
                    lock.release();
                }
            } else {
                log.error("Ping failed: Rate limited");
            }
        } catch (Exception e) {
            log.error("Ping failed: " + e.getMessage());
        }
        /*
        验证Ping服务日志
	    查看不同的结果：
		Ping sent successfully: World
		Ping failed: 429 Too Many Requests
		Ping failed: Rate limited
     */
    }

    private final WebClient webClient = WebClient.create("http://123.123.123.12:8081");

    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${redis.lock.key}")
    private String redisLockKey;

    //使用@Scheduled注解，每秒发送一次请求。
    @Scheduled(fixedRate = 1000)
    public void sendPing() {
        //使用Redis分布式锁(redis.setnx)实现全局速率限制，确保每秒只有一个实例发送请求
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(redisLockKey, "locked", Duration.ofSeconds(1));
        if (Boolean.TRUE.equals(lockAcquired)) {
            webClient.get().uri("/pong").retrieve().bodyToMono(String.class)
                    .doOnSuccess(response -> log.info("Ping sent successfully: " + response))
                    .doOnError(error -> log.error("Ping failed: " + error.getMessage()))
                    //释放锁
                    .doFinally(signal -> redisTemplate.delete(redisLockKey))
                    .subscribe();
        }
    }


}
