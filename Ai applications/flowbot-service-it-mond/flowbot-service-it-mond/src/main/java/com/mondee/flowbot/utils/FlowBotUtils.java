package com.mondee.flowbot.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mondee.flowbot.model.BotExecutionStats;
import com.mondee.flowbot.model.DialogContext;
import com.mondee.flowbot.model.FlowBotContext;
import com.mondee.flowbot.model.FlowBotRequest;
import com.mondee.flowbot.model.OAuthTokenData;
import com.mondee.flowbot.repository.DataAccessor;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FlowBotUtils {
	
	@Value("${pg-oauth2.token.expiry.time.diff}")
	String pgOauth2TokenExpiryDelta;
	
	
	@Value("${pgOauth2.urlString}")
	String pgOauth2Url;
	
	@Value("${chimes.urlString}")
	String chimesURL;
	
	private RestTemplate restTemplate = new RestTemplate();
	
	@Autowired
	DataAccessor dataAccessor;

	public void setCorelationId(FlowBotRequest request,String botTemplateName) {
		if(null == request.getCorrelationId() || request.getCorrelationId().isEmpty()) {
			request.setCorrelationId(request.getMerchantId().concat("-").concat(botTemplateName).concat("-").concat(""+System.currentTimeMillis()));
		}
	}
	
	public String convertObjectToJSONString(Object object)
    {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json;
        try {
            json = ow.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return convertObjectToJSONString(e);
        }
        return json;
    }
	
	public <T> T convertStringToObject(String jsonStr,Class<T> clazz){
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Convert JSON string to Object
	        return mapper.readValue(jsonStr, clazz);
		} catch (Exception e) {
			log.error("Exception occurred while converting the Json String to Object");
			return null;
		}
	}
	
	public void updateBotStatsToContext(FlowBotContext context, String errorMess, long time, String statsType,
			String name, String request, String response, String type) {
//		log.info("**************Context******before ******, Message:"+errorMess+", StatsType:"+statsType);
		try {
			
			if ((null != context.getFlowbotStats() && context.getFlowbotStats().isEmpty()) || (null != context.getFlowbotStats() && null == context.getFlowbotStats().get(statsType))) {
				context.getFlowbotStats().put(statsType, new ArrayList<BotExecutionStats>(Arrays.asList(new BotExecutionStats(name,
						(null == errorMess || errorMess.isEmpty()) ? FlowBotConstants.SUCCESS : FlowBotConstants.FAILED,
								(null == errorMess || errorMess.isEmpty()) ? "Successfully executed" : errorMess, time, request, response, type))));
			} else {
				context.getFlowbotStats().get(statsType).add(new BotExecutionStats(name,
						(null == errorMess || errorMess.isEmpty()) ? FlowBotConstants.SUCCESS : FlowBotConstants.FAILED,
								(null == errorMess || errorMess.isEmpty()) ? "Successfully executed" : errorMess, time, request, response, type));
			}
		}catch(Exception ex) {
			log.error("Exception ex:---"+ExceptionUtils.getStackTrace(ex));
		}
		
	}
	
    public JSONObject getJSONObject(String payload) {
    	JSONObject obj = null;
		try {
			if (payload != null && payload != "") {
				JSONParser parser = new JSONParser();
				obj = (JSONObject) parser.parse(payload);
			}
		} catch (Exception e) {
			log.error("Exception occurred while convertring the String to JSONObject , Error:{}",e.toString());
//			e.printStackTrace();
		}
		return obj;
    }
    
    public String getOAuthTokenDirect(String userName, String password){
    	log.info("* Access token not available in cache for the user - {}", userName);
    	log.info("**getOAUTH CACHEMISS:" + userName);
		String accessToken = "";
		OAuthTokenData oAuthTokenData = getOAuthTokenData(userName, password);
		if (oAuthTokenData != null && oAuthTokenData.getAccessToken() != null && oAuthTokenData.getExpiresIn() > 0) {
			//save OAuth access token into the cache
			accessToken = oAuthTokenData.getAccessToken();
			int ttl = (int) oAuthTokenData.getExpiresIn() - Integer.parseInt(pgOauth2TokenExpiryDelta);
			if (ttl > 0) {
				//save only if ttl is greater than 0
				dataAccessor.saveOAuthToken(userName, accessToken, ttl);
			}
		}
		return accessToken;
	}
    
	public String getOAuthToken(String userName, String password) {
		//get OAuth access token from cache
		String accessToken = "";
		try {
			accessToken = dataAccessor.getOAuthToken(userName);
			if (accessToken == null) {
				return getOAuthTokenDirect(userName, password);
			} else {
				log.info("**getOAUTH CACHEHIT:" + userName);
			}
		} catch (Exception e) {}
		return accessToken;
	}
	
	public OAuthTokenData getOAuthTokenData(String userName, String password) {
		////String nameofCurrMethod = new Throwable().getStackTrace()[0].getMethodName(); 
		OAuthTokenData oAuthTokenData = null;
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add("grant_type", "password");
		map.add("username", userName);
		map.add("password", password);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		try {
			String encoding = Base64.getEncoder().encodeToString("my-trusted-client:secret".getBytes("UTF-8"));
			headers.set("authorization", "Basic " + encoding);
			headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");

		} catch (UnsupportedEncodingException e1) {
			log.error(" Exception occurred while encoding the authorization credentials for user " + userName, e1);
			return oAuthTokenData;
		}

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

		String accessTokenUrl = pgOauth2Url + "/oauth/token";
		log.info("access token url {} ",pgOauth2Url);
		log.info("Calling auth server to get access token {} for user - {}, {}", accessTokenUrl, userName,request);
		try {
			ResponseEntity<String> response = restTemplate.postForEntity(accessTokenUrl, request , String.class);
			log.info("Response from auth server - {}", response.getBody());
			if(response.getStatusCode().equals(HttpStatus.OK)) {
				String responseString = response.getBody();

				JSONParser parser = new JSONParser();
				JSONObject obj = (JSONObject) parser.parse(responseString);
				String accessToken = (String)obj.get("access_token");
//				String refreshToken = (String)obj.get("refresh_token");
				long expiresIn = (long)obj.get("expires_in");
//				if(expiresIn < Long.parseLong(pqOauth2RefreshTokenExpiryIn)) {
//					//log.info("Calling auth server to get access token via refresh token");
//					//obj = getRefreshToken(refreshToken);
//					//accessToken = (String)obj.get("access_token");
//					//expiresIn = (long)obj.get("expires_in");
//				}
				oAuthTokenData = new OAuthTokenData();
				oAuthTokenData.setAccessToken(accessToken);
				oAuthTokenData.setExpiresIn(expiresIn);
			}
		} catch (Exception rce) {
			//rce.printStackTrace(); jello doesn't need this line below line will print the stacktrace
			log.error("Exception get authToken:" + accessTokenUrl + " : " + userName  + " : " + rce.getMessage(),  rce);
		}
		return oAuthTokenData;
	}
	
	/**
	 * This method will invoke the chimes service to create a record in flowbotExecution collection of MongoDB
	 * @param flowbotExecution
	 * @param accessToken
	 * @param correlationId
	 * @throws JelloException
	 */
	public void createFlotbotExecutionInChimes(String flowbotExecution, String accessToken,String correlationId) throws FlowbotException{
		String chimesFlowbotExecutionURL = chimesURL+"createFlowbotExec";
		log.info("Chimes flowbot execution URL "+chimesFlowbotExecutionURL);
		try {
			chimesFlowbotExecutionURL = chimesFlowbotExecutionURL+"?access_token="+accessToken;

			// need to integrate auth token later
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(chimesFlowbotExecutionURL);
			//logger.info(" Uristring  is {} and visitor record {} ",builder.toUriString(), visitor);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> entity = new HttpEntity<String>(flowbotExecution ,headers);
			log.info("Builder URL "+builder.toUriString() + " : "+entity);

			ResponseEntity<String> responseEntity =  restTemplate.postForEntity(builder.toUriString(), entity,String.class);
			
			if (responseEntity != null && !responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				String errorMess = "The response from the Service is not 200, Received the empty/null response for the createFlowbotExecution:, correlationId:" + correlationId;
				log.error(errorMess);
				throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMess);
			}

		}catch(Exception ex) {
			if(ex instanceof FlowbotException) {
				
			}
			String erroMess = "Exeception occurred while invoking the chimes service, error:"+ex.getMessage();
			log.error(erroMess,ex);
			throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(), erroMess);
		}
	}
	
	public List<LinkedHashMap<String,Object>> getEngagementDetails(long engagementID, String accessToken) {
		//String nameofCurrMethod = new Throwable().getStackTrace()[0].getMethodName(); 
		log.info("start getEngagementDetails");
		int loopCnt = 0;
		while(loopCnt++ < 2) {
			try {
				UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(chimesURL + "engagementList")
						.queryParam("access_token", accessToken).queryParam("engagementId", engagementID);

				JSONObject json = restTemplate.getForObject(builder.toUriString(), JSONObject.class);
				List<LinkedHashMap<String, Object>> eInfoList = (List<LinkedHashMap<String, Object>>) json.get("engagements");
				return eInfoList;
			} catch (Exception e) {
				accessToken = getOAuthTokenDirect("gcpuser", "password"); //todo get the merchantId
			}
		}
		log.info("end getEngagementDetails");
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getAccessToken(String tag, String merchantId) {
		JSONObject tokenInfo = null;
		String username = "gcpuser";
		String password = "password";
		if (merchantId != null && !merchantId.isEmpty()) {
			username = "jello" + merchantId + "usr";
			password =  "jello" + merchantId + "pd";
		}
		String newToken = getOAuthToken(username, password);
		tokenInfo = new JSONObject();
		tokenInfo.put("access_token", newToken);
			tokenInfo.put("merchantId",  merchantId);
			tokenInfo.put("username", username);
			tokenInfo.put("sessionId", tag);
			log.info("sessionId:{}, getAccessToken",tag);
			return tokenInfo;
	}
	
	public JSONObject getMerchantAttributesFromCB(String merchantId, String accessToken) {
		//String nameofCurrMethod = new Throwable().getStackTrace()[0].getMethodName();
		try {
			JSONObject obj = getMerchantAttributes(merchantId, accessToken);
			if (obj != null && obj.get("merchant") != null) {
				Object ob = obj.get("merchant");
				if (ob instanceof LinkedHashMap) {
					return new JSONObject((LinkedHashMap) ob);
				} else if (ob instanceof HashMap) {
					return new JSONObject((HashMap) ob);
				} else if (ob instanceof JSONObject) {
					return (JSONObject) ob;
				}
			}
		}
		catch (Exception e) {
			log.error("getMerchantAttributesFromCB : " + merchantId + " : " + e.getMessage() + ":" + e.getStackTrace()[0].getLineNumber(), e);
		}
		return null;
	}
	
	public JSONObject getMerchantAttributes(String merchantId, String accessToken) {
		//String nameofCurrMethod = new Throwable().getStackTrace()[0].getMethodName();
		log.info("start getMerchantAttributes");
		JSONObject merchantAttributes = null;
		try {
			merchantAttributes = dataAccessor.getMerchantAttributesNew(merchantId);
		} catch (Exception e) {}
		if(merchantAttributes == null) {
			merchantAttributes = getMerchantAttributesInt(merchantId, accessToken);
			if (merchantAttributes != null && merchantAttributes.get("merchant") != null) {
				try {
					dataAccessor.saveMerchantAttributesNew(merchantId, merchantAttributes);
				} catch (Exception e) {}
			}
		}
		log.info("end getMerchantAttributes");
		return merchantAttributes;
	}
	
	public JSONObject getMerchantAttributesInt(String merchantId, String accessToken) {
		//String nameofCurrMethod = new Throwable().getStackTrace()[0].getMethodName(); 
		log.info("start getMerchantAttributesInt");
		int loopCount = 0;
		while(loopCount++ < 2) {
			try {
				if (StringUtils.isEmpty(accessToken) && !StringUtils.isEmpty(merchantId)) {
					accessToken = getOAuthToken("jello" + merchantId + "usr", "jello" + merchantId + "pd");
				}
				UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(chimesURL + "getMerchantAttributes")
						.queryParam("access_token", accessToken).queryParam("merchantId", merchantId);
				JSONObject json = restTemplate.getForObject(builder.toUriString(), JSONObject.class);
				return json;
			} catch (Exception e) {
				accessToken = getOAuthTokenDirect("jello" + merchantId + "usr", "jello" + merchantId + "pd");
			}
		}
		log.info("[JELLO] failed to getMerchantAttributesInt json {} ", merchantId);
		return null;
	}
	
	
	
	/**
	 * This method will get the botname based on merchantId and BotId.
	 * @param merchantId
	 * @param botId
	 * @return
	 */
	public String getBotDetails(String merchantId, String botId) {
		log.info("inside getBotDetails for  merchantId:{}, botId:{}", merchantId, botId);
		String accessToken = getOAuthToken("jello" + merchantId + "usr", "jello" + merchantId + "pd");
		String botName = null;
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(chimesURL + "getMerchantBot")
				.queryParam("access_token", accessToken).queryParam("merchantId", merchantId).queryParam("botId", botId);

		ResponseEntity<String> response = restTemplate.getForEntity(builder.toUriString(), String.class);
		log.info("Response from Chimes for bot - {}", response.getBody());
		if(response.getStatusCode().equals(HttpStatus.OK)) {
			if (response.getBody() != null) {
				String responseString = response.getBody();

				JSONParser parser = new JSONParser();
				JSONObject jsonObject;
				try {
					jsonObject = (JSONObject) parser.parse(responseString);
					JSONArray myBots = (JSONArray) jsonObject.get("myBots");
					if (myBots != null) {
						JSONObject bot = (JSONObject) ((JSONArray) ((JSONObject) myBots.get(0)).get("bot")).get(0);
						botName = (String) bot.get("botName");
						log.info("botName:{}", botName);
					}
				} catch (ParseException ex) {
					log.error(
							"Exception occurred getting the botname {}",
							ex.toString());
				}
			}
		}
		
		return botName;
		

	}
	
}
