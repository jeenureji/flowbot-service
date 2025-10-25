package com.mondee.flowbot.repository;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.couchbase.client.java.document.json.JsonObject;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is to access data from the data store
 *
 */
@Service
public class DataAccessor {
	
	@Value("${messenger.couchDbURL}")
	String couchDbURL;		
	
	@Value("${messenger.couchDbBucket}")
	String bucketName;
	
	@Value("${userAgent.couchDbBucket}")
	String userAgentBucket;
	
	@Value("${userAgent.idleTimeout:600}")
	int userAgentTimeout;
	
	@Value("${jello.couchDbBucket}")
	String jelloBucket;
	
	public static final Object PAYLOAD = "payload";
	
	
	private static final Logger logger = LoggerFactory.getLogger(DataAccessor.class);
	
		
	public String getTemplateData(String templateKey) {
		return getTemplateData(templateKey, bucketName, null, null);
	}

	public String getTemplateData(String templateKey, String langauge, String defaultLanguage) {
		return getTemplateData(templateKey, bucketName, langauge, defaultLanguage);
	}
	
	public String getTemplateData(String templateKey, String dbName, String language, String defaultLanguage) {
		
		String templateData = null;
		Datasource ds=Datasource.getInstance();
		
		logger.debug("[JELLO] DataAccessor: ###### couchDbURL : {} ###### bucketName : {}", couchDbURL, dbName);
		ds.init(couchDbURL);
		Optional<String> templateJson = ds.readJson(templateKey,dbName);
		try {
			JSONParser parser = new JSONParser();
			if(templateJson.isPresent()) {
				JSONObject obj = (JSONObject) parser.parse(templateJson.get());
				if(StringUtils.isNotBlank(language) && obj.containsKey(PAYLOAD + "_" + language))
					templateData = (String)obj.get(PAYLOAD + "_" + language);
//				else if(StringUtils.isNotBlank(defaultLanguage) && obj.containsKey(JelloConstants.PAYLOAD + "_" + defaultLanguage))
//					templateData = (String)obj.get(JelloConstants.PAYLOAD + "_" + defaultLanguage);
					//TODO Default language support needs to be added as part of next release
				else
					templateData = (String)obj.get(PAYLOAD);
			}else {
				logger.warn("[JELLO] DataAccessor: (CB) unable to fetch data for : {}", templateKey);
			}
		} catch (ParseException e) {
			logger.error("[JELLO] DataAccessor: Unable to parse template data retrieved from data store", e);
		}
		//logger.debug("DataAccessor: Template Data retrieved from data store : {}", templateData);
		return templateData;
	}
	
	public String getMerchantStoryBoard(String merchantId, String channel) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		Optional<String> merchantStoryBoard = ds.readJson(merchantId + ":" + channel + ":story_board",bucketName);
		//logger.debug("DataAccessor: Merchant StoryBoard retrieved from data store : {}", merchantStoryBoard.get());
		return merchantStoryBoard.get();
	}
	
	public String getBotTemplate(String botTemplateId) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		Optional<String> botTemplate = ds.readJson(botTemplateId,bucketName);
		return (null == botTemplate) ? null : botTemplate.get();
	}
	
	public boolean saveDialogContext(String key, String dialogContext, int expiry) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		//logger.debug("{}: DialogContext to be saved - {}", key, dialogContext);
		return ds.writeJson(key, dialogContext, 0, expiry, bucketName);
	}
	
	public String getDialogContext(String key) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		Optional<String> dialogContext = ds.readJson(key,bucketName);
		if(dialogContext.isPresent()) {
			//logger.debug("{}: DialogContext retrieved from data store : {}", key, dialogContext.get());
			return dialogContext.get();
		} else {
			logger.debug("[JELLO] {}: DialogContext not available in data store ", key);
		}
		return null;
	}
	
	public boolean deleteDialogContext(String key) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		logger.debug("[JELLO] Delete Dialog Context - {}", key);
		return ds.deleteDocument(key, bucketName);
	}
	
	public boolean saveCsIdContextKeyMapping(String csId, String contextKey) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		return ds.writeStringDocument(csId, contextKey, 0, 86400, bucketName);//expires in 1 day
	}
	
	public String getCsIdContextKeyMapping(String csId) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		return ds.readStringDocument(csId, bucketName);
	}
	
	public boolean saveVoiceDeviceInfo(String key, String deviceInfo, String bucket) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		return ds.writeJson(key, deviceInfo, 0, 600, bucket);//10 minutes expiry
	}
	
	public String getOAuthToken(String userName) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		return ds.readStringDocument("oauth-token:" + userName, jelloBucket);
	}
	
	public boolean saveOAuthToken(String userName, String accessToken, int expiry) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		return ds.writeStringDocument("oauth-token:" + userName, accessToken, 0, expiry, jelloBucket);
	}

	
	/**
	 * Inserts the document to CB 
	 * @param bucket
	 * @param bucketKey
	 * @param payload
	 * @param expiry
	 * @return
	 */
	public boolean insertDocument(String bucket, String bucketKey, String payload, int expiry) {
		Datasource ds = Datasource.getInstance();
		ds.init(couchDbURL);
		return ds.writeJson(bucketKey, payload, 0, expiry, bucket);
	}
	
	/**
	 * Fetches the document for the given key from the bucket
	 * @param bucket
	 * @param bucketKey
	 * @return JSONObject
	 */
	public JSONObject getDocument(String bucket, String bucketKey) {
		JSONObject result = null;
		try {
			Datasource ds = Datasource.getInstance();
			ds.init(couchDbURL);
			JSONParser parser = new JSONParser();
			Optional<String> jsonData = ds.readJson(bucketKey, bucket);
			result = (jsonData.isPresent()) ? ((JSONObject) parser.parse(jsonData.get())) : null;
		} catch (Exception ex) {
			logger.error("Exception occurred while fetching the data for the key:"+bucketKey+" from the CB bucket:"+bucket+",Error:"+ex.getStackTrace());
		}
		return result;
	}
	
	public JSONObject getMerchantAttributesNew(String merchantId) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		String key = "merchant-attributes:" + merchantId;
		Object js = ds.readJsonDocument(key, jelloBucket);
	    if (js != null) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return (JSONObject) mapper.readValue(js.toString(), JSONObject.class);
			} catch (Exception e) {
				logger.error("[JELLO] - getMerchantAttributesFromCB error:" + e.getMessage(), e);
			}
		}
		return null;
	}
	
	public boolean saveMerchantAttributesNew(String merchantId, JSONObject merchantAttributes) {
		Datasource ds=Datasource.getInstance();
		ds.init(couchDbURL);
		String key = "merchant-attributes:" + merchantId;
		logger.debug("[JELLO] {}: MerchantAttributes to be saved - {}", key, merchantAttributes);
		return ds.writeJsonNew(key, merchantAttributes, 0, 1800, jelloBucket); //All mongodb updates to also update couchbase copy
	}

	public boolean deleteDocument(String bucket, String bucketKey) {
		Datasource ds = Datasource.getInstance();
		ds.init(couchDbURL);
		return ds.deleteDocument(bucketKey, bucket);
	}
	
}
