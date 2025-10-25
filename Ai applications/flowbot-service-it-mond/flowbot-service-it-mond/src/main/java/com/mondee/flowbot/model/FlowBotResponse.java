package com.mondee.flowbot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class FlowBotResponse {
	private String engagementId;
	private String botId;
	private String botName;
	private Object responsePayload;
	private int responseCode;
	private String responseDesc;
	private String corelationId;
	
    @JsonInclude(Include.NON_NULL)
    private Object data;

	public FlowBotResponse(int code, String message) {
		this.responseCode = code;
		this.responseDesc = message;
	}

	public FlowBotResponse(int code, String message, String corelationId, Object responsePayload) {
		this.responseCode = code;
		this.responseDesc = message;
		this.responsePayload = responsePayload;
		this.corelationId = corelationId;
	}

	public FlowBotResponse(int code, String message, Object payload, String corelationId, String engagementId) {
		this.responseCode = code;
		this.responseDesc = message;
		this.responsePayload = payload;
		this.corelationId = corelationId;
		this.engagementId = engagementId;
	}

	public FlowBotResponse(int code, String message, String corelationId) {
		this.responseCode = code;
		this.responseDesc = message;
		this.corelationId = corelationId;
	}

}
