package com.mondee.flowbot.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.mondee.flowbot.model.BotExecutionStats;
import com.mondee.flowbot.model.BotTemplate;
import com.mondee.flowbot.model.DialogContext;
import com.mondee.flowbot.model.Engagement;
import com.mondee.flowbot.model.FlowBotContext;
import com.mondee.flowbot.model.FlowBotRequest;
import com.mondee.flowbot.model.FlowBotResponse;
import com.mondee.flowbot.model.Intents;
import com.mondee.flowbot.model.JelloResponse;
import com.mondee.flowbot.repository.DataAccessor;
import com.mondee.flowbot.utils.FlowBotConstants;
import com.mondee.flowbot.utils.FlowBotUtils;
import com.mondee.flowbot.utils.FlowbotException;
import com.mondee.flowbot.utils.IntentUtil;
import com.mondee.flowbot.utils.TemplateBuilder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FlowBotService {

	@Autowired
	DataAccessor dataAccessor;

	@Autowired
	TemplateBuilder templateBuilder;

	@Autowired
	FlowBotUtils utils;

	@Autowired
	FlowBotHelper flowbotHelper;


	/**
	 * Triggers the Flowbot in Async way
	 * @param request
	 * @param engagement
	 * @throws FlowbotException
	 */
	@Async
	public void triggerAsyncFlowBot(FlowBotRequest request, Engagement engagement) throws FlowbotException {
		String engagementId = null != engagement ?  engagement.getEngagementID()+"" : null;
		log.info("Triggering the flowbot Asynchronously for the merchantId:{}, engagementId:{}",request.getMerchantId(), engagementId);
		try {
			triggerFlowBot(request, engagement);
		}catch(Exception ex) {
				log.error("Exception occurred running the flowbot Asynchronously for the merchantId:{}, engagementId:{}",request.getMerchantId(), engagementId);
		}
		
	}
	
	/**
	 * Triggers the Flowbot in sync way
	 * @param request
	 * @param engagement
	 * @throws FlowbotException
	 */
	public FlowBotResponse triggerSyncFlowBot(FlowBotRequest request, Engagement engagement) throws FlowbotException {
		String engagementId = null != engagement ?  engagement.getEngagementID()+"" : null;
		log.info("Triggering the flowbot synchronously for the merchantId:{}, engagementId:{}", request.getMerchantId(),
				engagementId);
		FlowBotResponse response = new FlowBotResponse(HttpStatus.OK.value(),
				"FlowBot execution completed successfully");
		try {
			FlowBotContext context = triggerFlowBot(request, engagement);
			
			if(null != context && null != context.getAppInput()) {
//				JSONObject result = (JSONObject)context.getAppInput().get(FlowBotConstants.JSON_RESULT);
				response.setCorelationId(request.getCorrelationId());
				response.setEngagementId(engagementId);
				response.setBotName(request.getBotName());
				response.setResponsePayload(context.getAppInput().get(FlowBotConstants.JSON_RESULT));
				String errMessage = getResponseDescription(context);
				if(null != errMessage) {
					response.setResponseDesc(errMessage);
				}
			}else {
				throw new FlowbotException("Invalid response for merchantId:"+request.getMerchantId()+", engagementId: "+engagementId+", correlationId:"+ request.getCorrelationId());
			}
		} catch (Exception ex) {
			if (ex instanceof FlowbotException) {
				throw ex;
			}
			String errorMess = "Exception occurred while executing the flow bot synchronously for the merchantId:{}"
					+ request.getMerchantId() + ", engagementId :" + engagementId + ", Error:"
					+ ex.getMessage();
			log.error(errorMess,ex);
//			log.error("Exception is:::" + ExceptionUtils.getStackTrace(ex));
			throw new FlowbotException(errorMess);
		}
		log.info("Completed flowbot execution synchronously for the merchantId:{}, engagementId:{}",
				request.getMerchantId(), engagementId);
		return response;
	}
	
	
	/**
	 * This method will check whether any of the nodes were failed and then it will frame the error message
	 * @param context
	 * @return errorMessage
	 */
	private String getResponseDescription(FlowBotContext context) {
//		log.info("Checking the Error in flowbotContext"+utils.convertObjectToJSONString(context));
		
		String message = null;
		if(null != context.getFlowbotStats() && null != context.getFlowbotStats().get(FlowBotConstants.NODE_EXECUTION_STATS) && !context.getFlowbotStats().get(FlowBotConstants.NODE_EXECUTION_STATS).isEmpty()) {
			List<BotExecutionStats>  executionStats= context.getFlowbotStats().get(FlowBotConstants.NODE_EXECUTION_STATS);
			Optional<BotExecutionStats>  exeStats = executionStats.stream().filter(stats -> stats.getStatus().equalsIgnoreCase(FlowBotConstants.FAILED)).findFirst();
			message =  (exeStats.isPresent()) ? "Error while executing node:"+exeStats.get().getName()+", error:"+exeStats.get().getMessage() :null;
			log.info("Checking the Error in flowbotContext"+message);
		}
		return message;
	}

	/**
	 * This Method will perform below operation Creates the Context just to hold the
	 * request and response flowing through the Orchestra service Fetch the
	 * storyboard which is nothing but the Bot Template Get the start Intent from
	 * where the bot should invoke else it will consider the order and get the start
	 * node. Prepare the request and Invoke the Orchestrator service Loop through
	 * each node in the botTemplate and invoke the orchestra service to execute each
	 * node.
	 * @param request
	 * @param engagement
	 * @throws FlowbotException
	 */
	public FlowBotContext triggerFlowBot(FlowBotRequest request, Engagement engagement) throws FlowbotException {
		JelloResponse response = new JelloResponse();
		StopWatch watch = new StopWatch();
		FlowBotContext context = new FlowBotContext();
		String errorMess = null;
		try {
			watch.start();
			log.info("Triggered the flowbot for , correlationId:{}, StartTime: {}", request.getCorrelationId(),
					watch.getTime());
			// This method gets the prerequisite for execution of flowbots
			preProcess(request, context, engagement);
			// Execute the flowbot by invoking the orchestrator-service
			process(request, response, context);
			
			log.info("FlowBot execution completed for the  bot template:{}, correlationId:{}, EndTime: {}",
					context.getBotTemplateName(), request.getCorrelationId(), watch.getTime());
		} catch (Exception ex) {
			if (ex instanceof FlowbotException) {
				errorMess = ex.getMessage();
				throw (FlowbotException)ex;
			} else {
				errorMess = "Exception occurred while executing the flow bot: " + context.getBotTemplateName()
						+ ", correlationId :" + request.getCorrelationId() + ", Error:" + ex.getMessage();
				log.error(errorMess);
				throw new FlowbotException(errorMess);
			}
		} finally {
			watch.stop();
			if(null == errorMess) {
				errorMess = getResponseDescription(context);
			}
			utils.updateBotStatsToContext(context,errorMess,watch.getTime(), FlowBotConstants.BOT_EXECUTION_STATS, request.getCorrelationId(),null,null,null);
			// Execute the steps after flowbot execution is completed
//			log.info("JSON RESULT******************"+utils.convertObjectToJSONString(context));
			flowbotHelper.postProcess(request.getCorrelationId(), context, request);
		}
		return context;
	}
	
	

	/**
	 * This method will perform below operations 1) Creates the visitor object in
	 * couchBase DB with correlationId has visitorId 2) Get the flowbot template
	 * from couchBase DB for the given botName. 3) Identify the next Intent
	 * 
	 * @param request
	 * @param context
	 */
	private void preProcess(FlowBotRequest request, FlowBotContext context, Engagement engagement) throws FlowbotException{
		log.info("Executing the preprocess stage for correlationId:{}", request.getCorrelationId());
		try {
			context.setEventDataMap(new HashMap<String, JSONObject>());
			context.setEngagementId(null != engagement ? engagement.getEngagementID() : -1L);
			context.setBotTemplateName((null != engagement && null != engagement.getDefaultBotTemplateName()) ? engagement.getDefaultBotTemplateName(): request.getBotName());
			// fetch the story board list for the bot from Couchbase DB
			BotTemplate botTemplate = getBotTemplate(context.getBotTemplateName(), request.getCorrelationId());
			context.setBotTemplate(botTemplate);
			// getStartIntent for flow bot
			Intents intent = IntentUtil.getIntentOrStartIntent(request.getIntent(),
					botTemplate.getStoryBoardList());
			context.setNextState(intent);
			context.setMerchantId(request.getMerchantId());
		} catch (Exception ex) {
			if(ex instanceof FlowbotException) {
				throw ex;
			}
			String errorMess = "Exception occurred during preprocess for botTemplate:"+context.getBotTemplateName() +", Error :"+ex.getMessage();
			log.error(errorMess, ex);
//			log.error("Exception is:::" + ExceptionUtils.getStackTrace(ex));
			throw new FlowbotException(errorMess);
		}
		log.info("Completed the preprocess stage for the bot template:{}, correlationId:{}", context.getBotTemplateName(),
				request.getCorrelationId());
	}

	/**
	 * This method will iterate through each node in flowbot and invoke the orchestra-service for each node.
	 * @param request
	 * @param response
	 * @param context
	 * @throws FlowbotException
	 * @throws ParseException 
	 */
	private void process(FlowBotRequest request, JelloResponse response, FlowBotContext context) throws FlowbotException {
		log.debug("Executing the process stage for the bot template:{}, correlationId:{}", context.getBotTemplateName(),
				request.getCorrelationId());
		try {
			Intents nextIntent = context.getNextState();
			boolean firstNode=true;
			while (nextIntent != null) {
				String nodeTemplateName = nextIntent.getTemplate() != null ? nextIntent.getTemplate().get(0) : null;
				String nodeTemplate = dataAccessor.getTemplateData(nodeTemplateName);
				// populate Data Map
				HashMap<String, JSONObject> dataMap = populateDataMap(request, context);
//				log.debug("************DataMap************" + utils.convertObjectToJSONString(dataMap));
				String substitutedTemplate = templateBuilder.substituteTemplate(nodeTemplate, dataMap, nodeTemplateName, request);
				log.info("Template Before Substitution" + utils.convertObjectToJSONString(substitutedTemplate));
				JSONObject templateContent = utils.getJSONObject(substitutedTemplate);
				log.info("Template After Substitution" + utils.convertObjectToJSONString(templateContent));
//				log.info("**********userInput Before invoking**************"+context.getUserInput().toJSONString());
				//set the JSON Input for APPInput only for firstNode
				if(firstNode && null != request.getJsonInput()) {
					context.getAppInput().put(FlowBotConstants.JSON_RESULT, (JSONObject)new JSONParser().parse(request.getJsonInput()));
				}
				JSONObject orchestraResponse = flowbotHelper.invokeOrchestratorService(templateContent, context, nodeTemplateName,
						request.getCorrelationId());
				firstNode = false;
				if(null == orchestraResponse) {
					throw new FlowbotException("Invalid response from orchestator service for node:"+nodeTemplateName+", correlationId:"+request.getCorrelationId());
				}
				// removed this check because Error Node Not Triggering for API Errors in Flowbot
//				else if(null != orchestraResponse.get("status") && (orchestraResponse.get("status") instanceof Boolean) && !(boolean)orchestraResponse.get("status")){
//					nextIntent = null;
//					context.setAppInput((null == orchestraResponse.get(FlowBotConstants.APP_INPUT)) ? null
//							: (JSONObject) orchestraResponse.get(FlowBotConstants.APP_INPUT));
//					context.setResponsePayload(orchestraResponse);
//				}
				else {
					// continue the flowbot if the next node exists
					nextIntent = processOrchestratorResponse(orchestraResponse, context,
							context.getBotTemplate().getStoryBoardList(), templateContent, request.getCorrelationId());
//					log.info("**********userInput after invoking**************"+context.getUserInput().toJSONString());
				}
			}
		} catch (Exception ex) {
			String errorMess = "Exception occurred during process method the flow bot: " + context.getBotTemplateName()
					+ ", correlationId :" + request.getCorrelationId() + ", Error:" + ExceptionUtils.getStackTrace(ex);
			log.error(errorMess);
			throw new FlowbotException(errorMess);
		}
	
		
		log.debug("Completed the preprocess stage for the bot template:{}, correlationId:{}", context.getBotTemplateName(),
				request.getCorrelationId());
	}

	/**
	 * This method will get the engagement details for the provided engagementId
	 * @param request
	 * @param visitor
	 * @throws FlowbotException
	 */
	@SuppressWarnings("unchecked")
	public Engagement getEngagements(FlowBotRequest request) throws FlowbotException {
		log.info("Fetching the engagement details for merchantId: {}, engagementId: {}",request.getMerchantId(), request.getEngagementId());
		Engagement eng = new Engagement();
		try {
			// TODO Auto-generated method stub
			List<LinkedHashMap<String, Object>> eInfoList = utils.getEngagementDetails(Long.valueOf(request.getEngagementId()),
					utils.getOAuthTokenDirect("gcpuser", "password"));
			
			if (null == eInfoList || eInfoList.isEmpty() || null == eInfoList.get(0) || eInfoList.get(0).isEmpty()) {
				throw new FlowbotException("Engagement record does not exists for the engagementId: "+request.getEngagementId());
			}
			LinkedHashMap<String, Object> eInfo = eInfoList.get(0);
			// ToDo remove Hardcoding
			eng.setEngagementID(Long.valueOf(String.valueOf(eInfo.get("engagementId"))));
			eng.setEngagementName(String.valueOf(eInfo.get("name")));
			List<LinkedHashMap<String,Object>> botList = (List<LinkedHashMap<String,Object>>)eInfo.get("botList");
			if(null == botList) {
				throw new FlowbotException("Empty or Invalid bot info for the engagment :"+request.getEngagementId());
			}else {
				String botTemplateName = (String)botList.stream().filter(bot -> (boolean)bot.get("defaultBot")).findFirst().get().get("botTemplateName");
				eng.setDefaultBotTemplateName(botTemplateName);
			}
			eng.setChannel(FlowBotConstants.FLOW_BOT_CHANNEL);
		}catch(Exception ex) {
			if(ex instanceof FlowbotException) {
				throw ex;
			}
			String errorMess = "Exception occurred while fetching the engagement details for the provided engagementId: "+request.getEngagementId()+" , error:"+ex.getMessage();
			log.error(errorMess);
			throw new FlowbotException(errorMess);
		}
		return eng;
	}

	/**
	 * The method is not refractored its copied from MessageResponse class which was
	 * used for chatbot
	 * 
	 * @param response
	 * @param context
	 * @param storyBoard
	 * @param templateContent
	 * @return Intents
	 */
	@SuppressWarnings("unchecked")
	private Intents processOrchestratorResponse(JSONObject response, FlowBotContext context,
			ArrayList<Intents> storyBoard, JSONObject templateContent, String correlationId) throws FlowbotException {
		Intents nextIntent = null;
		try {
			// get next intent
			String nextIntentStr = (null == response.get("nextIntent")) ? null : (String) response.get("nextIntent");
			// payload
			JSONObject responsePayload = (null == response.get("payload")) ? null
					: (JSONObject) response.get("payload");
			// appInput
			JSONObject responseAppInput = (null == response.get(FlowBotConstants.APP_INPUT)) ? null
					: (JSONObject) response.get(FlowBotConstants.APP_INPUT);

			// if templateContent type is userInput ... get Intent and payLoadKey and set it
			// in dialog context.
			if (null != responsePayload) {
				setDataToContext(responsePayload, context, storyBoard);
			}

			if (templateContent != null) {
				String type = (String) templateContent.get("type");
				JSONObject val = (JSONObject) templateContent.get("value");
				updateJSONResultToContext(type, val, context, responseAppInput, correlationId);
				// update AttributeList to Context
				if (val != null && Arrays.asList(FlowBotConstants.ATTR_COMPONENTS).contains(type)
						&& responseAppInput != null && null != responseAppInput.get(FlowBotConstants.ATTR_LIST)
						&& null != ((JSONObject) responseAppInput.get(FlowBotConstants.ATTR_LIST))) {
					JSONObject attr = (JSONObject) responseAppInput.get(FlowBotConstants.ATTR_LIST);
					for (Object key : attr.keySet()) {
						String keyStr = (String) key;
						Object value = attr.get(keyStr);
						context.getUserInput().put(keyStr, value);
					}
				}
			}

			log.info("Before invoking get app input {} ", context);
			if (context.getAppInput() == null) {
				context.setAppInput(responseAppInput);
			} else {
				context.getAppInput().put(FlowBotConstants.JSON_RESULT,
						(Object) responseAppInput.get(FlowBotConstants.JSON_RESULT));
				context.getAppInput().put(FlowBotConstants.ATTR_LIST,
						(Object) responseAppInput.get(FlowBotConstants.ATTR_LIST));
			}
			nextIntent = IntentUtil.getIntents(new Intents(nextIntentStr), storyBoard);
		} catch (Exception ex) {
			String errorMess = "Exception occurred while processing the orchestrator service response for correlationId:"
					+ correlationId + ", Error:" + ex.getMessage();
			log.error(errorMess,ex);
//			log.error(ExceptionUtils.getStackTrace(ex));
			throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMess);
		}
		context.setNextState(nextIntent);
		return nextIntent;
	}


	/**
	 * This Method will update the response Json result to context
	 * @param type
	 * @param val
	 * @param context
	 * @param appInput
	 */
	@SuppressWarnings("unchecked")
	private void updateJSONResultToContext(String type, JSONObject val, DialogContext context, JSONObject appInput, String correlationId) throws FlowbotException{
		String apiName = null;
		try {
			if (val != null && "json".equalsIgnoreCase(type)) {
				List<JSONObject> reqParams = (List<JSONObject>) val.get(FlowBotConstants.REQUEST_PARAMS);
				JSONObject rp0 = (reqParams != null) ? (JSONObject) reqParams.get(0) : null;
				if (rp0 != null) {
					apiName = (String) rp0.get("apiName");
					if (apiName != null)
						apiName = apiName.trim().replaceAll("\\s+", " ").replaceAll(" ", "_");
				}
			}

			if (apiName != null) {
				Object jsr = (Object) appInput.get(FlowBotConstants.JSON_RESULT);
				if (jsr != null) {
					if (jsr instanceof JSONObject) {
						context.getEventDataMap().put(apiName, (JSONObject) jsr);
					} else if (jsr instanceof JSONArray) {
						JSONObject resp = new JSONObject();
						resp.put("list", (JSONArray) jsr);
						context.getEventDataMap().put(apiName, (JSONObject) resp);
					}
				}
			}
		} catch (Exception ex) {
			String exception = "Exception occurred while updating JSON Result to Context for correlationId:"+correlationId+", error:"+ex.getMessage();
			log.error(ExceptionUtils.getStackTrace(ex));
			throw new FlowbotException(exception);
		}
	}

	private void setDataToContext(JSONObject payLoad, DialogContext context, ArrayList<Intents> storyBoard) {
		// if templateContent type is userInput ... get Intent and payLoadKey and set it
		// in dialog context.
		String templateType = (String) payLoad.get("type");
		if (templateType.equalsIgnoreCase(FlowBotConstants.USER_INPUT)) {
			JSONObject value = (JSONObject) payLoad.get("value");
			String nextIntent = (String) value.get("next_intent");
			JSONArray attributes = (JSONArray) value.get("attributes");
			if (attributes != null) {
				JSONObject attributeObj = (JSONObject) attributes.get(0);
				String attributeName = (String) attributeObj.get("attribute_name");
				log.info("[JELLO] MessageResponder:{} setup-userInput-handling - attr:{} nxtIntent:{}", attributeName,
						nextIntent);
				context.setNextState(IntentUtil.getIntents(new Intents(nextIntent), storyBoard));
				context.setNextPayloadKey(attributeName);
				context.getEventDataMap().put("userInputPayLoad", payLoad);
			} else {
				log.info("[JELLO] MessageResponder:{} skip setup-userInput-handling - NO-attr nxtIntent:{}",
						nextIntent);
			}
		}
	}

	

	/**
	 * This method will populate the dataMap which containes the required attribute
	 * value which are required to be submitted in freemarker template
	 * 
	 * @param request
	 * @param context
	 * @return dataMap
	 * @throws FlowbotException
	 */
	@SuppressWarnings("unchecked")
	private HashMap<String, JSONObject> populateDataMap(FlowBotRequest request, DialogContext context)
			throws FlowbotException {
		HashMap<String, JSONObject> dataMap = new HashMap<String, JSONObject>();
		try {
			JSONObject payLoadJSON = null;
			if (request.getRequestParams() != null) {
				payLoadJSON = utils.getJSONObject(request.getRequestParams());
				if (payLoadJSON != null) {
					dataMap.put(FlowBotConstants.PAYLOAD, payLoadJSON);
					dataMap.put("userInput", (null == context.getUserInput() || context.getUserInput().isEmpty()) ? payLoadJSON : context.getUserInput());
					context.setUserInput((null == context.getUserInput() || context.getUserInput().isEmpty()) ? payLoadJSON : context.getUserInput());
				}
			}
			
			JSONObject appInput = context.getAppInput();
			if (appInput != null && appInput.get(FlowBotConstants.JSON_RESULT) != null) {
				Object appinputJsonResult = (Object) appInput.get(FlowBotConstants.JSON_RESULT);
				if (appinputJsonResult instanceof JSONObject) {
					dataMap.put(FlowBotConstants.JSON, (JSONObject)appinputJsonResult);
				} else if (appinputJsonResult instanceof JSONArray) {
					JSONObject jsonResult = new JSONObject();
					jsonResult.put("list", (JSONArray) appinputJsonResult);
					dataMap.put(FlowBotConstants.JSON, jsonResult);
				}
				
			}
			
			// Adding sessionId, merchant, engagement, access_token info
			JSONObject tokenInfo = utils.getAccessToken(request.getCorrelationId(), request.getMerchantId());
			dataMap.put("pgt", tokenInfo);
			
			if(null != request.getEngagementId()) {
				List<LinkedHashMap<String, Object>> eInfoList = utils.getEngagementDetails(Long.parseLong(request.getEngagementId()), tokenInfo.get("access_token").toString());
				if (null != eInfoList && !eInfoList.isEmpty() && null != eInfoList.get(0) && !eInfoList.get(0).isEmpty()) {
					LinkedHashMap<String, Object> eInfo = eInfoList.get(0);
					dataMap.put("engagementInfo", new JSONObject(eInfo));
				}
			}
			dataMap.put("merchant", utils.getMerchantAttributesFromCB(request.getMerchantId(), tokenInfo.get("access_token").toString()));
			
			//JSONObject merchant = new JSONObject();
			//merchant.put("id", request.getMerchantId());
			//dataMap.put("merchant", merchant);

		} catch (Exception ex) {
			log.error(
					"Exception occurred while populating the data map which is used for substituting template dynamic values, error:"
							+ ex.getMessage());
			throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
		}
		return dataMap;
	}

	/**
	 * This Method will fetch the BotTemplate/Storyboard for the given template name
	 * from couchBase DB
	 * 
	 * @param botTemplateName
	 * @return BotTemplate
	 * @throws FlowbotException
	 */
	private BotTemplate getBotTemplate(String botTemplateName, String correlationId) throws FlowbotException {
		if(null == botTemplateName || botTemplateName.isEmpty()) {
			throw new FlowbotException("Invalid or Empty botTemplateName for the correlationId:"+correlationId);
		}
		String botTemplateKey = botTemplateName.trim() + FlowBotConstants.WEB_STORY_BOARD;
		String botTemplateStr = dataAccessor.getBotTemplate(botTemplateKey);
		BotTemplate botTemplate = (null == botTemplateStr) ? null : utils.convertStringToObject(botTemplateStr, BotTemplate.class);
		if (null == botTemplate) {
			log.error("Bot template: " + botTemplateName + " does not exists, correlationId:"+correlationId);
			throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"Bot template: " + botTemplateName + " does not exists");
		} else if (botTemplate.getErrorState() == null) {
			// set default error state defined by Jello
			Intents errorState = new Intents();
			errorState.setTemplate(Arrays.asList(new String[] { FlowBotConstants.JELLO_TEMPLATE_ERROR_MESSAGE }));
			botTemplate.setErrorState(errorState);
		}
		return botTemplate;
	}

}
