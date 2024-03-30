package com.itzkz.usercenter.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("spring.redis")
@Data
public class RedissonConfig {

    private String host;
    private int port;
    private int database;
    private String password;

    @Bean
    public RedissonClient redisson() {


        // 创建Redisson配置
        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format("redis://%s:%d", host, port))
                .setDatabase(database)
                .setPassword(password);
        // 创建Redisson客户端

        return Redisson.create(config);
    }


}
