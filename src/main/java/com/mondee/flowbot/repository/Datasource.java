package com.mondee.flowbot.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.mondee.flowbot.utils.PoolInstance;

public enum Datasource {
	
	INSTANCE;
	
	private static final Logger logger = LoggerFactory.getLogger(Datasource.class);

	//Map<String,CouchbaseDataWrapper> cbclientMap=new ConcurrentHashMap<String,CouchbaseDataWrapper>();
	CouchbaseDataWrapper cbClient=null;
	int retrycount=3;
	
	public static Datasource getInstance()
    {
        return INSTANCE;
    }
    
	/**
	 * Assuming only 1 bucket right now.
	 * @param cluster
	 * @param bucket
	 */
	public void init(String cluster) {
		if (cbClient == null) {
        		cbClient=new CouchbaseDataWrapper(cluster);
		}
    }
	
	public String readStringDocument(String key,String bucket){
		try {
			return (String) cbClient.readStringDocument(key,bucket);
		}catch(Exception e) {
			return null;
		}
	}
	
	public List<Object> queryView(String bucket,String designdoc,String viewName) {
		return cbClient.queryView(bucket,designdoc,viewName);
	}
	/**
	 * return JSON document in StringFormat
	 * @param key
	 * @return
	 */
	public Optional<String> readJson(String key,String bucket) {
		Object jdoc=readJsonDocument(key,bucket);
		if(jdoc!=null) {
		return Optional.ofNullable(jdoc.toString());
		}else {
			return Optional.empty();
		}
	}
	
	public N1qlQueryResult readAllDocuments(String bucket, String query) {
		N1qlQueryResult result = null;
		if(cbClient!=null){
			result = cbClient.getAllRecordsFromBucket(bucket, query);
			/*if(!CollectionUtils.isEmpty(allRows)) {
				return Optional.ofNullable(allRows)
						.orElseGet(Collections::emptyList)
						.stream().map(record -> record.toString())
						.collect(Collectors.toList());
			}*/
		}
		return result;
	}
	
	/**
	 * write json document
	 * @param key
	 * @param value
	 * @param timeout
	 * @return
	 */
	
	public boolean writeJson(String key,Object value,String bucket) {
		
		return writeJson(key,value,30,bucket);
	}
	
	public boolean writeJson(String key,Object value,int timeout,String bucket) {
		return writeJson(key,value,timeout,0,bucket);
	}


	public boolean writeJsonNew(String key,Object value,int timeout,int expiry,String bucket) {

		if(cbClient!=null){
			//System.out.println("Writing to Couchbase :"+cbClient+","+key+": value:"+value.toString());
			JsonDocument jdoc=cbClient.writeJsonNew(key, value, timeout, expiry, bucket);
			if(jdoc!=null) return true;
			else return false;
		}else{
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
			return false;
		}
	}

	public boolean writeJson(String key,Object value,int timeout,int expiry,String bucket) {
		
		if(cbClient!=null){
			//System.out.println("Writing to Couchbase :"+cbClient+","+key+": value:"+value.toString());
			JsonDocument jdoc=cbClient.writeJson(key, value, timeout, expiry, bucket);
			if(jdoc!=null) return true;
			else return false;
		}else{
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
			return false;
		}
	}
	
	public Object readJsonDocument(String key,String bucket){
		Object ret=null;
		int counter=0;
		do{
			Object jsd = cbClient.readJsonDocument(key,bucket);
			if(jsd != null)
			{
				ret = jsd;
			}
		}while(ret ==null && counter++ <retrycount);
		return ret;
	}
	

	public JsonDocument updateJson(String key,Object value,int timeout,String bucket) {
		return updateJson(key, value, timeout, 0, bucket);
	}
	
	public JsonDocument updateJson(String key,Object value,int timeout,int expiry,String bucket) {
		
		if(cbClient!=null){
			return cbClient.updateJson(key, value, timeout, expiry, bucket);
		}else{
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
			return null;
		}
	}
	
	public JsonDocument write(String key,Object value,int timeout,String bucket) {
		
		if(cbClient!=null){
			return cbClient.write(key, value, timeout,bucket);
		}else{
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
			return null;
		}
	}
	
	public boolean writeStringDocument(String key, String value, int timeout, int expiry, String bucket) {
		if(cbClient!=null){
			StringDocument sdoc=cbClient.writeStringDocument(key, value, timeout, expiry, bucket);
			if(sdoc!=null) return true;
			else return false;
		}else{
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
			return false;
		}
	}

	public void append(String key, String value, int timeout, String bucket) {
		cbClient.append(key, value, bucket);
	}

	public void append(String key, String value, int timeout, TimeUnit unit, String bucket) {
		cbClient.append(key, value, timeout, unit, bucket);
	}

	public void updateJsonArray(String key, String value, int timeout,String bucket) {
		if(cbClient!=null){
			//System.out.println("Writing to Couchbase :"+cbClient+","+key+": value:"+value.toString());
			cbClient.updateJsonArray(key,Arrays.asList(value.split("\\s*,\\s*")), timeout,bucket);
		}else{
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
		}
		
	}

	public JsonArray readJsonArrayDocument(String key,String bucket) {
		JsonArray ret=null;
		int counter=0;
		do{
			ret= cbClient.readJsonArray(key,bucket);
		}while(ret ==null && counter++ <retrycount);
		return ret;
	}
	
	public boolean writeJsonArray(String key, String value, int timeout,String bucket) {
		if(cbClient!=null){
			JsonArrayDocument jdoc = cbClient.writeJsonArray(key,Arrays.asList(value.split("\\s*,\\s*")), timeout,bucket);
			if(jdoc!=null) return true;
			else return false;
		}else{
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
			return false;
		}
		
	}
	
	public boolean deleteDocument(String key, String bucket) {
		if(cbClient != null) {
			logger.info("[JELLO] DELETE from Couchbase " + key);
			JsonDocument jdoc = cbClient.deleteDocument(key, bucket);
			if(jdoc != null) return true;
			else return false;
		} else {
			logger.warn("[JELLO] Not able to write to Couchbase " + key);
			return false;
		}
	}
	
	public Object poolStatus(String key, String k, PoolInstance p,String bucket) {
		if(cbClient!=null){
			return cbClient.poolStatus(key, k, p,bucket);
		}else {
			return null;
		}
	}
	
	public Object handleQueue(String key,Object ret,boolean bPop,String bucket) {
		if(cbClient!=null){
			return cbClient.handleQueue(key,ret,bPop,bucket);
		}else{
			return null;
		}
	}
	
	public boolean writeCustomDocument(String key, Object value, int timeout, String bucket) {
		if(cbClient!=null){
			logger.info("[JELLO] Writing to Couchbase :"+cbClient+","+key);
			Object jdoc = cbClient.writeCustomDocument(key, value, timeout, bucket);
			if(jdoc!=null) return true;
			else return false;
		}else{
			logger.info("[JELLO] Not able to write to DB");
			return false;
		}
		
	}
	
	public Object readCustomDocument(String key, String bucket) {
		if(cbClient!=null){
			Object jdoc = cbClient.readCustomDocument(key, bucket);
			return jdoc;
		}else{
			logger.info("[JELLO] Not able to read from DB");
			return null;
		}
		
	}

}
