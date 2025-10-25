package com.mondee.flowbot.controller;

import com.mondee.flowbot.model.CacheRequest;
import com.mondee.flowbot.model.CacheResponse;
import com.mondee.flowbot.utils.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ExternalCacheController {

    @Autowired
    CacheUtil cacheUtil;

    @RequestMapping(value = "/api/v1/cache/save", method = { RequestMethod.POST })
    public ResponseEntity<String> saveToCache(@RequestBody CacheRequest request) {
        log.info("saveToCache --> request{}", request);
        try {
            cacheUtil.validateRequest(request);
            cacheUtil.saveToCache(request);
        } catch (Exception e){
            log.error("Error saveToCache: ", e.getMessage());
            return new ResponseEntity<>("FAILURE "+e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.info("saveToCache <--");
        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
    }


    @RequestMapping(value = "/api/v1/cache/get", method = { RequestMethod.GET })
    public ResponseEntity<CacheResponse> retriveFromCache(@RequestParam("key") String key, @RequestParam(value = "executionId", required = false) String executionId) {
        log.info("retriveFromCache --> key {}, correlationId {}", key, executionId);
        try {
            String finaleKey=executionId==null?key:executionId+"_"+key;
            Object data = cacheUtil.retriveFromCache(finaleKey);
            CacheResponse response = new CacheResponse(finaleKey, data);
            log.info("retriveFromCache <--");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e){
            log.error("Error retriveFromCache: ", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }

    @RequestMapping(value = "/api/v1/cache/clear", method = { RequestMethod.DELETE })
    public ResponseEntity<String> deleteCache(@RequestParam("key") String key, @RequestParam(value = "executionId", required = false) String executionId) {
        log.info("deleteCache --> key {}, correlationId {}", key, executionId);
        try {
            cacheUtil.deleteCache(executionId==null?key:executionId+"_"+key);
            log.info("deleteCache <--");
            return ResponseEntity.status(HttpStatus.OK).body("SUCCESS");
        } catch (Exception e){
            log.error("Error deleteCache: ", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }
}
