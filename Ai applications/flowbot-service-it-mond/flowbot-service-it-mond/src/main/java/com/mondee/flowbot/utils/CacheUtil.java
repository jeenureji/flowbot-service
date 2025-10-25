package com.mondee.flowbot.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mondee.flowbot.model.CacheRequest;
import com.mondee.flowbot.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheUtil {

    //@Autowired
    //DataAccessor dataAccessor;

    @Autowired
    RedisService redisService;

    @Value("${jello.couchDbBucket}")
    private String bucket;

    @Value("${flowbot.cache.default.ttl}")
    Integer defaultTTL;

    ObjectMapper mapper = new ObjectMapper();
    public void saveToCache(CacheRequest request) throws Exception {
        try {

            String key = CacheConstants.CACHE_CONSTANTS.STATIC.name().equalsIgnoreCase(request.getCacheType())?request.getKey():request.getExecutionId()+"_"+request.getKey();
            String value= mapper.writeValueAsString(request.getValue());
            Integer ttl = request.getTtlInSeconds()!=null?request.getTtlInSeconds():defaultTTL;
            //dataAccessor.insertDocument(bucket, key, value, ttl);
            redisService.saveValue(key, value, ttl);
        } catch (Exception e) {
            log.error("Error while saveToCache {}", e);
            throw new Exception("Error while saving cache", e);
        }

    }

    public Object retriveFromCache(String key) {
        //return dataAccessor.getDocument(bucket, key);
        return redisService.getValue(key);
    }

    public void deleteCache(String key) {
        //dataAccessor.deleteDocument(bucket, key);
        redisService.deleteValue(key);
    }

    public void validateRequest(CacheRequest cacheRequest) throws Exception {
        if (cacheRequest.getKey()==null || cacheRequest.getKey().isBlank()){
            throw new Exception("Key is mandatory in request");
        }
        if (cacheRequest.getCacheType()==null || cacheRequest.getCacheType().isBlank()){
            throw new Exception("CacheType is mandatory in request");
        }

        if (cacheRequest.getTtlInSeconds()<0){
            throw new Exception("TtlInSeconds should be greater than or equal to 0");
        }

        if (CacheConstants.CACHE_CONSTANTS.STATIC.name().equalsIgnoreCase(cacheRequest.getCacheType())){
            if (cacheRequest.getTtlInSeconds()>604800){ //1 Week
                throw new Exception("TtlInSeconds should be less than a week for STATIC");
            }
        } else if (CacheConstants.CACHE_CONSTANTS.DYNAMIC.name().equalsIgnoreCase(cacheRequest.getCacheType())){
            if (cacheRequest.getTtlInSeconds()>86400){ //1 Day
                throw new Exception("TtlInSeconds should be less than a day for DYNAMIC for STATIC");
            }
        } else {
            throw new Exception("Invalid CacheType");
        }
    }

}
