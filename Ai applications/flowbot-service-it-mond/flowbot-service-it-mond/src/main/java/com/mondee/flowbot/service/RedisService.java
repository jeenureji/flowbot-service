package com.mondee.flowbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveValue(String key, Object value, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e){
            throw e;
        }
    }

    public void deleteValue(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw e;
        }
    }
}
