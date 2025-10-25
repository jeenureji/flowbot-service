package com.mondee.flowbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheResponse {
    private String key;
    private Object value;
}
