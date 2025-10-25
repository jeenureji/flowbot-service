package com.mondee.flowbot.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mondee.flowbot.model.Engagement;
import com.mondee.flowbot.model.FlowBotRequest;
import com.mondee.flowbot.model.FlowBotResponse;
import com.mondee.flowbot.service.FlowBotService;
import com.mondee.flowbot.utils.FlowBotUtils;
import com.mondee.flowbot.utils.FlowbotException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class FlowbotServiceController {


    @CrossOrigin(origins = "*")
    @GetMapping(value = "/ping")
    public ResponseEntity<String> pingService() {
        return ResponseEntity.status(HttpStatus.OK).body("pong");
    }

    @Autowired
	FlowBotService flowBotService;
	
	@Autowired
	FlowBotUtils utils;
	
	@RequestMapping(value = "/api/v1/trigger", method = { RequestMethod.POST })
	public ResponseEntity<FlowBotResponse> triggerFlowBot(@RequestBody FlowBotRequest request) {
		log.info("Triggered the flowbot for the request:{}", utils.convertObjectToJSONString(request));
		FlowBotResponse response = new FlowBotResponse(HttpStatus.OK.value(), "FlowBot execution completed successfully");
		try {
			validateRequest(request);
			//the below code will be called when a webhook is trigged from ai-gateway service to get the botname.
			if(null == request.getBotName()) {
				log.info("Botname is null ");
				String botName = utils.getBotDetails(request.getMerchantId(), request.getBotId());
				if (botName != null) {
					log.info("Botname is {} ",botName);
				request.setBotName(botName);
				} else {
					log.error("Exception occurred getting the botname for botID {} ",request.getBotId());
					response = new FlowBotResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),"Exception occurred getting the botname ");
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
				}
			}
			// 
			Engagement engagement = null;
			if(null != request.getEngagementId() && !request.getEngagementId().isEmpty()) {
				engagement = flowBotService.getEngagements(request);
				if(null == engagement || null == engagement.getEngagementName() || engagement.getEngagementName().isEmpty()) {
					throw new FlowbotException(HttpStatus.BAD_REQUEST.value(), "Engagement/ Bot info does not exists for the given engagementId: "+request.getEngagementId());
				}
				//Checking whether the request has correlation Id if not setting one
				utils.setCorelationId(request,engagement.getDefaultBotTemplateName());
			} else {
				//Checking whether the request has correlation Id if not setting one
				utils.setCorelationId(request,"BOT-"+request.getBotName());
			}
			//if (sync=true and callbackUrl is present) or (sync is false) we will trigger Async
			if(null != request.getSync() && request.getSync() && (null == request.getCallbackUrl() || request.getCallbackUrl().isEmpty())) {
				response = flowBotService.triggerSyncFlowBot(request, engagement);
			} else {
				response.setResponseDesc((null != request.getCallbackUrl() && !request.getCallbackUrl().isEmpty() && null != request.getSync() && request.getSync())
						? "FlowBot triggered successfully in Async Mode, because the callBackUrl was provided in request"
						: "FlowBot execution triggered successfully");
				flowBotService.triggerAsyncFlowBot(request, engagement);
			}
		}catch(FlowbotException ex) {
			ex.printStackTrace();
			response = new FlowBotResponse(ex.getCode(), ex.getMessage(), request.getCorrelationId());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
		
		log.info("Flow bot triggered successfully for MerchantId:{},  engagementId:{}",request.getMerchantId(),request.getEngagementId());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	private void validateRequest(FlowBotRequest request) throws FlowbotException {
		String errorMessage = null;
		if (null == request) {
			errorMessage = "Invalid Request or empty request";
		} /*
			 * else if (null == request.getEngagementId() ||
			 * request.getEngagementId().isEmpty()) { errorMessage =
			 * "Invalid or empty flowbot"; }
			 */else if (null == request.getMerchantId() || request.getMerchantId().isEmpty()) {
			errorMessage = "Invalid or empty MerchantId";

		} else if (((null != request.getSync() && !request.getSync()) || null == request.getSync())
				&& (null == request.getCallbackUrl() || request.getCallbackUrl().isEmpty())) {
			errorMessage = "Empty CallBackURL for triggering flowbot Asynchronously";
		}
		if (null != errorMessage) {
			throw new FlowbotException(HttpStatus.BAD_REQUEST.value(), errorMessage);
		}

	}
	
	
	@RequestMapping(value = "/testcallback", method = { RequestMethod.POST })
	public ResponseEntity<String> testCallbackURL(@RequestBody String request) {
		try {
			log.info("Received callback:"+request);
		}catch(Exception ex) {
			
		}
		
		return ResponseEntity.status(HttpStatus.OK).body("Success");
	}
	
	
	@RequestMapping(value = "/testError", method = { RequestMethod.POST })
	public ResponseEntity<String> testError(@RequestBody String request) {
		try {
			log.info("testError",request);
		}catch(Exception ex) {
			
		}
		
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Success");
	}

	



}
