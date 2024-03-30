package com.itzkz.usercenter.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itzkz.usercenter.model.domain.User;
import com.itzkz.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RecommendJob {

    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final List<Integer> testUserList = Arrays.asList(1, 3);


    @Scheduled(cron = "0 59 17 * * ?")
    public void recommendUser() {
        //设置锁 也就是key
        String lockKey = "xiaxiang:recommend:key";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 只有一个线程能获取到锁
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                // 实际业务逻辑
                for (Integer userId : testUserList) {
                    Page<User> userPage = userService.page(new Page<>(1, 8));
                    String redisKey = String.format("itzkz:recommend:%s", userId);
                    ValueOperations<String, Object> valueOperations =
                            redisTemplate.opsForValue();
                    // 写入缓存
                    try {
                        valueOperations.set(redisKey, userPage, 300000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("Failed to set key in Redis", e);
                    }
                }
            } else {
                // 获取锁失败
                log.warn("Failed to acquire lock for key: {}", lockKey);
            }
        } catch (InterruptedException e) {
            log.error("Thread interrupted while attempting to acquire lock", e);
        } finally {
            // 确保释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
