package com.mondee.flowbot.service;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mondee.flowbot.model.BotExecutionStats;
import com.mondee.flowbot.model.CallbackVO;
import com.mondee.flowbot.model.FlowBotContext;
import com.mondee.flowbot.model.FlowBotExecution;
import com.mondee.flowbot.model.FlowBotRequest;
import com.mondee.flowbot.repository.DataAccessor;
import com.mondee.flowbot.utils.FlowBotConstants;
import com.mondee.flowbot.utils.FlowBotUtils;
import com.mondee.flowbot.utils.FlowbotException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FlowBotHelper {
	
	@Autowired
	FlowBotUtils utils;
	
	@Value("${orchestrator.urlString}")
	String orchestratorURL;
	
	@Value("${orchestrator.payload.bucket}")
	String orchestraPayloadBucket;
	
	@Value("${orchestrator.payload.ttl:1800}")
	String orchestraPayloadTTL;
	
//	@Autowired
//	DataAccessor dataAccessor;

	
	/**
	 * Invokes the Orchestrator service for execute of each node
	 * 
	 * @param templateContent
	 * @param context
	 * @return JSONObject
	 * @throws FlowbotException 
	 */
	@SuppressWarnings("unchecked")
	public JSONObject invokeOrchestratorService(JSONObject templateContent, FlowBotContext context,
			String nodeTemplateName, String correlationId) throws FlowbotException {
		log.info("Invoking the orchestrator service for node :{}, correlationId:{}", nodeTemplateName, correlationId);
		JSONObject orchestratorPayload = new JSONObject();
		JSONObject response = null;
		StopWatch watch = new StopWatch();
		String errorMess = null;
		try {
			watch.start();
			orchestratorPayload.put("payload", templateContent);
//			orchestratorPayload.put("bucketName", orchestraPayloadBucket);
//			orchestratorPayload.put("engagementId", context.getEngagementId()+"");
//			orchestratorPayload.put("sessionId", correlationId);
			// set appInput from DialogContext into the orchestrator payload
			if (context.getAppInput() != null) {
//				JSONObject appInput = new JSONObject();
//				appInput.put(FlowBotConstants.APP_INPUT, (context.getAppInput() != null) ? context.getAppInput() : new JSONObject());
//				dataAccessor.insertDocument(orchestraPayloadBucket, correlationId, new ObjectMapper().writeValueAsString(appInput), Integer.parseInt(orchestraPayloadTTL));
				orchestratorPayload.put(FlowBotConstants.APP_INPUT, context.getAppInput());
			}

			// Send to jello orchestrator
			RestTemplate restTemplate = new RestTemplate();
			String orchestrationUrl = orchestratorURL + FlowBotConstants.ORCHESTRATOR_PATH;
			log.info("Request payload for nodeTemplate:{},correlationId:{}, request:{}", nodeTemplateName, correlationId,
					orchestratorPayload.toJSONString());
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> entity = new HttpEntity<String>(orchestratorPayload.toJSONString(), headers);
			ResponseEntity<String> responseEntity = restTemplate.postForEntity(orchestrationUrl, entity, String.class);

			log.info("Response received for nodeTemplate:{}, correlationId:{}, response:{}", nodeTemplateName,
					correlationId, utils.convertObjectToJSONString(responseEntity.getBody()));
			response = validateOrchestratorResponse(responseEntity, context.getBotTemplateName(), nodeTemplateName, correlationId);
		} catch (Exception ex) {
			if (ex instanceof FlowbotException) {
				errorMess = ex.getMessage();
				throw (FlowbotException)ex;
			}
			errorMess = "Exception while invoking the orchectrator service for nodetemplateName: "
					+ nodeTemplateName + ", Error:" + ex.getMessage();
			log.error(errorMess,ex);
//			log.error("Exception:"+ExceptionUtils.getStackTrace(ex));
			throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMess);
		} finally {
			watch.stop();
			if(null != response && null != response.get("status") && !(boolean)response.get("status")) {
				errorMess = (String)response.get("errorMessage");
			}
			utils.updateBotStatsToContext(context, errorMess, watch.getTime(), FlowBotConstants.NODE_EXECUTION_STATS,
					nodeTemplateName, (null != orchestratorPayload) ? orchestratorPayload.toJSONString() : null,
					(null != response) ? response.toJSONString() : null, (null != templateContent ? templateContent.get("type")+"" : null));
			log.info("Time taken to execute the nodeTemplate:{}, correlationId:{} is {}", nodeTemplateName, correlationId,
					watch.getTime());
		}
		log.debug("Response received for orchestrator service for node :{}, correlationId:{}", nodeTemplateName,
				correlationId);
		return response;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject validateOrchestratorResponse(ResponseEntity<String> responseEntity, String botName,
			String nodeTemplateName, String correlationId) throws FlowbotException {
		log.debug("validateOrchestratorResponse nodeTemplate:{}, correlationId:{} ", nodeTemplateName, correlationId);
		JSONObject response = null;
		try {
		String errorMess = null;
		if (responseEntity != null && responseEntity.getStatusCode().equals(HttpStatus.OK)) {
			response = utils.getJSONObject(responseEntity.getBody());
//			JSONObject respPayload = dataAccessor.getDocument(orchestraPayloadBucket, correlationId);
//			response.put(FlowBotConstants.APP_INPUT, (null == respPayload) ? null : (JSONObject)respPayload.get(FlowBotConstants.APP_INPUT));
			JSONObject respPayload = (null == response.get(FlowBotConstants.APP_INPUT)) ? null : (JSONObject) response.get(FlowBotConstants.APP_INPUT);
			if (null != respPayload && null != respPayload.get(FlowBotConstants.JSON_RESULT) && (respPayload.get(FlowBotConstants.JSON_RESULT) instanceof JSONObject)) {
				JSONObject jsonResult = (JSONObject) respPayload.get(FlowBotConstants.JSON_RESULT);
				if (null != jsonResult && null != jsonResult.get("success") && !(boolean) jsonResult.get("success")) {
					log.warn("The response from the Service is not 200,So returning the response json to caller");
					response.put("status", false);
					response.put("errorMessage",utils.convertObjectToJSONString(jsonResult));
				}
			}

		} else {
			errorMess = "The response from the Service is not 200, Received the empty/null response for the nodeTemplate:"
					+ nodeTemplateName + ", correlationId:" + correlationId;
		}
		if (null != errorMess && !errorMess.isEmpty()) {
			log.error(errorMess);
			throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMess);
		}
		
		}catch(Exception ex) {
			log.error("Exception:"+ExceptionUtils.getStackTrace(ex));
			throw ex;
		}
		
		log.debug("End validateOrchestratorResponse nodeTemplate:{}, correlationId:{} ", nodeTemplateName, correlationId);
		return response;
	}
	
	/**
	 * This method will be executed after the flowbot execution is completed to insert the record in raw visitor for the reporting purpose. 
	 * @param correlationId
	 * @param context
	 * @param botstatsMap
	 */
	@Async
	public void postProcess(String correlationId, FlowBotContext context, FlowBotRequest request) {
		log.info("Executing the post process for the Bot :{}, correlationId:{}", context.getBotTemplateName(),
				correlationId);
		try {

			FlowBotExecution execution = new FlowBotExecution();
			if (null != context.getFlowbotStats() && !context.getFlowbotStats().isEmpty()) {
				List<BotExecutionStats> botStats = context.getFlowbotStats().get(FlowBotConstants.BOT_EXECUTION_STATS);
				persistsExecutionStats(context, request, execution, botStats);
				if(null != request.getCallbackUrl() && !request.getCallbackUrl().isEmpty()) {
					CallbackVO callback = new CallbackVO(correlationId, context.getEngagementId() + "",
							context.getMerchantId(), context.getBotTemplateName(), utils.convertObjectToJSONString(request),
							utils.convertObjectToJSONString(context.getAppInput().get(FlowBotConstants.JSON_RESULT)),
							execution.getStatus(), execution.getDescription());
//					log.info("Response******************"+context.getAppInput().get(FlowBotConstants.JSON_RESULT));
//					log.info("Response******************"+utils.convertObjectToJSONString(context.getAppInput().get(FlowBotConstants.JSON_RESULT)));
					triggerCallbackURL(request, callback);
				}
			} else {
				log.error("Flow bot stats not present in the context");
			}

		} catch (Exception ex) {
//			log.error("Exception is:::" + ExceptionUtils.getStackTrace(ex));
			log.error("Exception occurred during post process stage of flowbot:{}, correlationId:{}, error:{}",
					context.getBotTemplateName(), correlationId, ex.getMessage());
		}
	}



	private void persistsExecutionStats(FlowBotContext context, FlowBotRequest request,FlowBotExecution execution, List<BotExecutionStats> botStats) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if (null != botStats && !botStats.isEmpty()) {
				execution.setBotName(context.getBotTemplateName());
				execution.setBotRequest(utils.convertObjectToJSONString(request));
				execution.setCorrelationId(request.getCorrelationId());
				execution.setMerchantId(context.getMerchantId());
				BotExecutionStats botExecStats = botStats.get(0);
				execution.setDescription(botExecStats.getMessage());
				execution.setEngagementId(context.getEngagementId() + "");
				execution.setBotExecutionTime(botExecStats.getExecutionTime());
				execution.setStatus(botExecStats.getStatus());
				execution.setNodeStats(context.getFlowbotStats().get(FlowBotConstants.NODE_EXECUTION_STATS));
				utils.createFlotbotExecutionInChimes(mapper.writeValueAsString(execution),
						utils.getOAuthTokenDirect("gcpuser", "password"), request.getCorrelationId());
			} else {
				log.error("Not able to build the BotExecutionStats due to bot stats not present in the context");
			}
		}catch(Exception ex) {
			log.error("Exception occurred while persisting the Execution stats for correlationId:"+request.getCorrelationId());
		}
	}



	/**
	 * Trigers the callback URL
	 * @param request
	 * @param callback
	 * @throws FlowbotException
	 */
	private void triggerCallbackURL(FlowBotRequest request, CallbackVO callback) throws FlowbotException{
		try {
			RestTemplate restTemplate = new RestTemplate();
			String callbackPayload = utils.convertObjectToJSONString(callback);
			log.info("triggerCallbackURL for correlationId:{}, request :{}",request.getCorrelationId(),callbackPayload);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> entity = new HttpEntity<String>(callbackPayload, headers);
			restTemplate.postForEntity(request.getCallbackUrl(), entity, String.class);
		}catch(Exception ex) {
			String errorMess = "Exception occurred while invoking the callbackURL:"+request.getCallbackUrl()+", correlationId:"+request.getCorrelationId()+", error:"+ex.getMessage();
			log.error(errorMess,ex);
//			log.error("Exception while triggering callback URL:"+ExceptionUtils.getStackTrace(ex));
		}
	}
	
}
