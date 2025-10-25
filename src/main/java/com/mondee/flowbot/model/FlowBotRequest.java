package com.mondee.flowbot.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowBotRequest {

	private String merchantId;
	private String correlationId;
	private String engagementId;
	private String requestParams;
	private String intent;
	private String callbackUrl;
	private Map<String, String> tags;
	private Map<String,Object> callbackUrlAttr;
	private Boolean sync;
	private String jsonInput;
	private String botId;
	private String botName;
	
}
