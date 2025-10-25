package com.mondee.flowbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackVO {
	private String correlationId;
	private String engagementId;
	private String merchantId;
	private String botName;
	private String botRequest;
	private String botResponse;
	private String status;
	private String description;
	
}
