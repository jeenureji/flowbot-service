package com.mondee.flowbot.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class holds the Intents and the actions mapping for the message
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Intents {
	
	public Intents() {
		
	}
	
	private String version;
	private String intent;
	private String action;
	private String activity;
	private String nextIntent;
	private String nextPayloadKey;
	private String nextStep;
	private String events;
	private String channel;
	private String channelHeader;
	private String aiAgentResponse;
	private String aiAgentConfidence;
	private int order;
	private int oorder;
	private String intentType;
	private List<String> template;
	private String acknowledgeTemplate;
	private String component;
	private String traits;
	private int maxExecutionCount = 0;
	private String userInput;
	private String description;
	
	public String getUserInput() {
		return userInput;
	}

	public void setUserInput(String userInput) {
		this.userInput = userInput;
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAction() {
		return action;
	}


	
	public void setAction(String action) {
		this.action = action;
	}

	public String getEvents() {
		return events;
	}

	public void setEvents(String events) {
		this.events = events;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getChannelHeader() {
		return channelHeader;
	}

	public void setChannelHeader(String channelHeader) {
		this.channelHeader = channelHeader;
	}

	public String getAiAgentResponse() {
		return aiAgentResponse;
	}

	public void setAiAgentResponse(String aiAgentResponse) {
		this.aiAgentResponse = aiAgentResponse;
	}
	
	public String getAiAgentConfidence() {
		return aiAgentConfidence;
	}

	public void setAiAgentConfidence(String aiAgentConfidence) {
		this.aiAgentConfidence = aiAgentConfidence;
	}

	public List<String> getTemplate() {
		return template;
	}

	public void setTemplate(List<String> template) {
		this.template = template;
	}
	
	/**
	 * @return the acknowledgeTemplate
	 */
	public String getAcknowledgeTemplate() {
		return acknowledgeTemplate;
	}

	/**
	 * @param acknowledgeTemplate the acknowledgeTemplate to set
	 */
	public void setAcknowledgeTemplate(String acknowledgeTemplate) {
		this.acknowledgeTemplate = acknowledgeTemplate;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getTraits() {
		return traits;
	}

	public void setTraits(String traits) {
		this.traits = traits;
	}
	
	/**
	 * @return the maxExecutionCount
	 */
	public int getMaxExecutionCount() {
		return maxExecutionCount;
	}

	/**
	 * @param maxExecutionCount the maxExecutionCount to set
	 */
	public void setMaxExecutionCount(int maxExecutionCount) {
		this.maxExecutionCount = maxExecutionCount;
	}


	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	

	public String getIntent() {
		return intent;
	}

	public void setIntent(String intent) {
		this.intent = intent;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
	public int getOorder() { return oorder; }
	public void setOorder(int order) { this.oorder = order; }

	public String getIntentType() {
		return intentType;
	}

	public void setIntentType(String intentType) {
		this.intentType = intentType;
	}

	public Intents(String context) {
		this.intent = context;
	}

	/**
	 * @return the intents
	 */
	public String getIntents() {
		return intent;
	}

	/**
	 * @param intents the intents to set
	 */
	public void setIntents(String intents) {
		this.intent = intents;
	}

	/**
	 * @return the nextIntent
	 */
	public String getNextIntent() {
		return nextIntent;
	}

	/**
	 * @param nextIntent the nextIntent to set
	 */
	public void setNextIntent(String nextIntent) {
		this.nextIntent = nextIntent;
	}
	
	/**
	 * @return the nextPayloadKey
	 */
	public String getNextPayloadKey() {
		return nextPayloadKey;
	}

	/**
	 * @param nextPayloadKey the nextPayloadKey to set
	 */
	public void setNextPayloadKey(String nextPayloadKey) {
		this.nextPayloadKey = nextPayloadKey;
	}
	
	/**
	 * @return the nextStep
	 */
	public String getNextStep() {
		return nextStep;
	}

	/**
	 * @param nextStep the nextStep to set
	 */
	public void setNextStep(String nextStep) {
		this.nextStep = nextStep;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Intents [version=" + version + ", intent=" + intent + ", action=" + action + ", nextIntent="
				+ nextIntent + ", nextPayloadKey=" + nextPayloadKey + ", nextStep=" + nextStep + ", events=" 
				+ events + ", channel=" + channel + ", channelHeader=" + channelHeader + ", aiAgentResponse=" 
				+ aiAgentResponse + ", order=" + order +"," + oorder +  ", intentType=" + intentType + ", template=" + template
				+ ", acknowledgeTemplate=" + acknowledgeTemplate + ", component=" + component + ", traits=" 
				+ traits + ", maxExecutionCount=" + maxExecutionCount + ", description=" + description
				+ ", aiAgentConfidence=" + aiAgentConfidence + "]";
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}	

}
