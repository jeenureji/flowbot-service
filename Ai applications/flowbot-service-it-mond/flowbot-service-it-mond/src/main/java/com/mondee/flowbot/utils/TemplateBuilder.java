package com.mondee.flowbot.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.mondee.flowbot.model.FlowBotRequest;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mondee.flowbot.repository.DataAccessor;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;

/**
 * This class generates the template with the provided template key and the data to fill the template
 *
 */
@Service
public class TemplateBuilder {
	
	@Autowired
	DataAccessor dataAccessor;
	
	private static final Logger logger = LoggerFactory.getLogger(TemplateBuilder.class);
	
	private Configuration cfg = null;
	
    private void init(String templateDirectory) throws Exception {
    	
    	if(cfg !=null) {
    		return;
    	}
        /* ------------------------------------------------------------------------ */
        /* You should do this ONLY ONCE in the whole application life-cycle:        */

        /* Create and adjust the configuration singleton */
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDirectoryForTemplateLoading(new File(templateDirectory));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

		try {
		  cfg.setSharedVariable("JIF",  new ObjectMapper());
		} catch (TemplateModelException e) {
	    	logger.info("[JELLO] TemplateBuilder:JIF: {} : error: {}", templateDirectory, e.getMessage());
		}
		 
        /* ------------------------------------------------------------------------ */
        /* You usually do these for MULTIPLE TIMES in the application life-cycle:   */

    }
    
    private void init() throws Exception {

    	if(cfg !=null) {
    		return;
    	}
    	/* ------------------------------------------------------------------------ */
    	/* You should do this ONLY ONCE in the whole application life-cycle:        */

    	/* Create and adjust the configuration singleton */
    	cfg = new Configuration(Configuration.VERSION_2_3_31);
    	cfg.setDefaultEncoding("UTF-8");
    	cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		try {
		  cfg.setSharedVariable("JIF",  new ObjectMapper());
		} catch (TemplateModelException e) {
	    	logger.info("[JELLO] TemplateBuilder:JIF: error: {}", e.getMessage());
		}

    	/* ------------------------------------------------------------------------ */
    	/* You usually do these for MULTIPLE TIMES in the application life-cycle:   */

    }
    
    public String generateTemplate(String templateFolder,String templateName,HashMap<String, JSONObject> actiontoDataMap)  {
        Template temp;
        String outData = null;

		try {
	    		init(templateFolder);
	    		logger.info("[JELLO] TemplateBuilder: templateName : {}", templateName);
			temp = cfg.getTemplate(templateName);
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	        /* Merge data-model with template */
	        Writer out = new OutputStreamWriter(outStream);
	        temp.process(actiontoDataMap, out);
	        outData = outStream.toString();
	       // logger.info("[JELLO] TemplateBuilder: outData : {}", outData);
	        // Note: Depending on what `out` is, you may need to call `out.close()`.
	        // This is usually the case for file output, but not for servlet output.
		} catch (Exception e) {
			logger.warn("[JELLO] TemplateBuilder: Unable to generate template", e);
		}
		return outData;
    }
    
    public String generateTemplate(String templateKey,HashMap<String, Object> dataMap, String dbName) throws Exception {
        Template temp;
        String outData = null;

        try {
        	init();
        	logger.info("[JELLO] TemplateBuilder: templateKey : {}", templateKey);
        	String templateData = dataAccessor.getTemplateData(templateKey, dbName, null, null);
        	temp = new Template(templateKey, new StringReader(templateData), cfg);
        	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        	/* Merge data-model with template */
        	Writer out = new OutputStreamWriter(outStream);
        	temp.process(dataMap, out);
        	outData = outStream.toString();
        //	logger.info("[JELLO] TemplateBuilder: outData : {}", outData);
        	// Note: Depending on what `out` is, you may need to call `out.close()`.
        	// This is usually the case for file output, but not for servlet output.
        } catch (IOException | TemplateException e) {
        	logger.warn("[JELLO] TemplateBuilder: Unable to generate template", e);
        }
        return outData;
    }
	public static String templateError(String str) {
		int start = (str != null) ? str.indexOf("Failed at") : 0;
		int end = (str != null)	 ? str.indexOf("\n----", start) : 0;
		if (start > 0) {
			if (end > start) str = str.substring(start, end);
			else str = str.substring(start);
		}
		return str != null ? str.replaceAll("\r\n|\r|\n", " ") : str;
	}
	public String substituteTemplate(String templateContent,Map<String, Object> actiontoDataMap, String templateName) {
        Template temp;
        String outData = null;

        try {
	        	init();
	        	temp = new Template(templateName, new StringReader(templateContent), cfg);
				temp.setLogTemplateExceptions(false);
	        	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	        	/* Merge data-model with template */
	        	Writer out = new OutputStreamWriter(outStream);
	        	temp.process(actiontoDataMap, out);
	        	outData = outStream.toString();
	        //	logger.info("[JELLO] TemplateBuilder: outData : {}", outData);
	        	// Note: Depending on what `out` is, you may need to call `out.close()`.
	        	// This is usually the case for file output, but not for servlet output.
        } catch (IOException | TemplateException e) {
			logger.warn("[JELLO] TemplateBuilder: substitute fail 1: {}", templateError(e.getMessage()));
		} catch (Exception e) {
			logger.error("[JELLO] TemplateBuilder: substitute fail 2: {}", e.toString());
		}
        return outData;
    }
    
    public String substituteTemplate(String templateContent, HashMap<String, JSONObject> actionToDataMap,
									 String templateName, FlowBotRequest request) {
        Template temp;
        String outData = null;

        try {
	        	init();
	        	temp = new Template(templateName, new StringReader(templateContent), cfg);
				temp.setLogTemplateExceptions(false);
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	        	/* Merge data-model with template */
	        	Writer out = new OutputStreamWriter(outStream);
	        	temp.process(actionToDataMap, out);
	        	outData = outStream.toString();
				logger.info("*****check out the original  out data***** :{}", outData);
			if( (request.getTags() != null && !request.getTags().isEmpty()) &&
					(templateContent.contains("promptId") || templateContent.contains("promptTitle")) ) {
				org.json.JSONObject outDataObject = new org.json.JSONObject(outData);
				org.json.JSONObject requestValue = outDataObject.optJSONObject("value");
				org.json.JSONObject requestParam = (org.json.JSONObject) requestValue.optJSONArray("request_params").get(0);
				if(requestParam != null && !requestParam.isEmpty()) {
					logger.info("*****check the requestParam***** :{}", requestParam);
					String getTempParam = requestParam.optString("templatePayload");
					if ((getTempParam != null && !getTempParam.isEmpty()) &&
							(request.getTags() != null && !request.getTags().isEmpty()) ) {
						org.json.JSONObject tagObj = new org.json.JSONObject(request.getTags());
						String tags = "\"tags\": " + tagObj;
						int insertIndex = getTempParam.indexOf("{")+1;
						StringBuilder sb = new StringBuilder(getTempParam);
						sb.insert(insertIndex, "\n" + tags + ",");
						requestParam.putOpt("templatePayload", sb.toString());

					}else {
						org.json.JSONObject tagObj = new org.json.JSONObject(request.getTags());
						if(tagObj != null && !tagObj.isEmpty()) {
							requestParam.putOpt("tags", tagObj);
						}
					}
					outData = outDataObject.toString();
					logger.info("*****checkout the overridden out data ***** :{}", outData);
				}
			}
			//	logger.info("[JELLO] TemplateBuilder: outData : {}", outData);
	        	// Note: Depending on what `out` is, you may need to call `out.close()`.
	        	// This is usually the case for file output, but not for servlet output.
        } catch (IOException | TemplateException e) {
			logger.warn("[JELLO] TemplateBuilder: substitute fail 3: {}: {} : {}", templateName, templateError(e.getMessage()), templateContent.replaceAll("\r\n|\r|\n", " "));
			logger.error("[JELLO] TemplateBuilder: substitute fail 3: {}:", templateName, e);
        } catch (Exception e) {
			logger.error("[JELLO] TemplateBuilder: substitute fail 4: {}", e.toString());
		}
        return outData;
    }
	public static String utilFTLSub(String templateName, String templateContent, HashMap<String, JSONObject> context) {
		TemplateBuilder builder = new TemplateBuilder();
		String respStr = null;
		FlowBotRequest flowBotRequest = new FlowBotRequest();
		try {
			respStr = builder.substituteTemplate(templateContent, context, templateName, flowBotRequest);
		} catch (Exception e) {
			logger.error("utilFTLSub:"+e.getMessage());
		}
		return respStr;
	}
}
