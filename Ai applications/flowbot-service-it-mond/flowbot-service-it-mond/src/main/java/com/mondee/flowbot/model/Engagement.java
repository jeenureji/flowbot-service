package com.mondee.flowbot.model;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class Engagement {
	
	@EqualsAndHashCode.Include
	long engagementID;
	@EqualsAndHashCode.Include
	String engagementName;
	String image;
	String channel;
	String svcProvider;
	String channel_user_id;
	String accessDate;
	String aiAgent; 
	String aiAgentId;
	List<String>  aiAgentImage;
	String agentWelcomeNote;
	String agentProactive;
	List<String> aiTextureImage;
	//Date lastModifiedTime;
	
//	List<AgentProactiveElements> agentProactiveElements;

	//AIAgent aiAgentInfo;
	
	String activeConversation;
	
//	List<Conversation> allConversations;
	
	String sourceId = null;
	Map<String,String> params = new HashMap<>();
	
	long totalConversations;
	String campaign;
	List<String> productNames;
	BigInteger productId;
	List<String> productCategories;
	String status;	
	
	String defaultBotTemplateName;
	
//	public void addConversation(Conversation conversation) {
//		if(allConversations == null) {
//			allConversations = new ArrayList<>();
//		}
//		allConversations.add(conversation);
//	}
	
}
