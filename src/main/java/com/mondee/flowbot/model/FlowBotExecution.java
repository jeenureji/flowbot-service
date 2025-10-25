package com.mondee.flowbot.model;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowBotExecution {

	private String correlationId;
	private String engagementId;
	private String merchantId;
	private String botName;
	private String botRequest;
	private long botExecutionTime;
	private String status;
	private String description;
	private Map<String, Object> additionalAttr = new HashMap<String, Object>();
	private Date expiryDate;
	
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy HH:mm:ss", timezone = "UTC")
    private Instant createdDate;
    private List<BotExecutionStats> nodeStats;
	private String sessionId;
	private String processedIntent;
	private String payload;
	private String userQuery;
	private String requestPayload;
	private String responsePayload;

}
