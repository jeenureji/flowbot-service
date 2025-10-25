package com.mondee.flowbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheRequest {
    private String key;
    private Object value;
    private Integer ttlInSeconds;
    private String executionId;
    private String cacheType;
}
