package com.mondee.flowbot.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DialogContext {
	
	//Dialogue Context has the following information
	//Intent Map (Story line mapping) - Defined in couch DB - to be fetched and initialize dialogue context
	//fulfillment state - messageResponder will update and read back - vary for each customer
	//cart - Message responder will do - vary for each customer
	//response from AI. - Need to be set by MessageReceiver - vary for each customer request
	//eventDataMap <id-value map> - added by response builder based on action data - vary for each customer
	private HashMap<String,JSONObject> eventDataMap;
	private Intents currentState;
	private Intents nextState;
	private Map<String, Integer> intentExecutionCountMap = new HashMap<>();
	private String nextPayloadKey;
	private boolean doClearOnRecvStart = false;
	private boolean realAgent = false;
	private boolean eventEnabled = false;
	private boolean languageTranslationEnabled = false; //Flag to hold the decision for translation of the content as per merchant configuration
	private String pgAgentForwardIntent;
	private String merchantId;
	private long engagementId;
	private String channel;
	private String svcProvider;
	private String botTemplateName;
	private String aiId;
	private String aiContext;
	private String aiConvContext;
	private String aiAgentRespMessage;
	private boolean aiStream = false;
	private String conversationId;
	private String customerServiceMsgId;
	private boolean contextExpired = false;
	private JSONObject engagement;
	private String lastAccessedDate;
	private String lastVisitedPage;
	private Map<String, String> pageBotMap;
	private List<String> routeTo;
	private JSONObject aiAgent;
	private int errorCount = 0;
	private int executionCount = 0;
	private String appointmentId;
	private JSONObject userInput = new JSONObject();
	private JSONObject appInput = new JSONObject();
	private Map<String,Object> globalParams = new HashMap<>();
	private String extensionIntent;
	private BotTemplate botTemplate;
	
	private JSONObject userPersona = new JSONObject();
	
	/**
	 * This attribute will store all the extension variables at Intent Level
	 */
	private JSONObject extentionVariables = new JSONObject();

	/**
	 * @TODO :- needs to be removed
	 */
	private JSONObject extentionVarContext = new JSONObject();
	
	
	/**
	 * This Attribute will store the default model Params in the context, so that for every request no need to fetch from DB
	 */
	private LinkedHashMap<String,Object> defaultModelParams = new LinkedHashMap<String, Object>();


	/*public DialogContext() {

	}*/

	public void incrementIntentExecutionCount(String intentName) {
		if(intentExecutionCountMap.containsKey(intentName)) {
			intentExecutionCountMap.put(intentName, intentExecutionCountMap.get(intentName)+1);
		} else {
			intentExecutionCountMap.put(intentName, 1);
		}
	}

	public int getIntentExecutionCount(String intentName) {
		if(intentExecutionCountMap.containsKey(intentName)) {
			return intentExecutionCountMap.get(intentName);
		}
		return 0;
	}
	
}
