package com.mondee.flowbot.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@Data
public class FlowBotContext extends DialogContext{

	Map<String,List<BotExecutionStats>> flowbotStats = new HashMap<String,List<BotExecutionStats>>();
	JSONObject responsePayload = new JSONObject();
	
}
