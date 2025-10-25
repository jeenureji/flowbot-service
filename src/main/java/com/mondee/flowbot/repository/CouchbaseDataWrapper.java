package com.mondee.flowbot.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.datastructures.collections.CouchbaseMap;
import com.couchbase.client.java.datastructures.collections.CouchbaseQueue;
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import com.google.gson.Gson;
import com.mondee.flowbot.utils.PoolInstance;

public class CouchbaseDataWrapper {

	private static List<String> SEED_IPS = null;
	private static Cluster CLUSTER = null;
	private static Map<String, Bucket> BUCKETMAP = Collections.synchronizedMap(new LinkedHashMap<String, Bucket>());
	CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder().connectTimeout(100000) // 10000ms = 10s, default is
																							// 5s
			.build();
	private static final Logger logger = LoggerFactory.getLogger(CouchbaseDataWrapper.class);
	
	public CouchbaseDataWrapper(String nodes) {

		try {
			System.setProperty("viewmode", "Production"); // before the connection to Couchbase

			SEED_IPS = Arrays.asList(nodes.split(","));
			CLUSTER = CouchbaseCluster.create(env, SEED_IPS);
		} catch (Exception e) {
			logger.error("[JELLO] NO CONNECTIVITY TO COUCHBASE.. " + SEED_IPS);
		}
	}

	public void shutdown() {
		CLUSTER.disconnect();
	}

	private Bucket getBucket(String bucket) {
		if (!isValidConnection()) {
			return null;
		} else {
			if (BUCKETMAP.containsKey(bucket)) {
				return BUCKETMAP.get(bucket);
			} else {
				Bucket newbucket = CLUSTER.openBucket(bucket);
				BUCKETMAP.put(bucket, newbucket);
				return newbucket;
			}
		}
	}

	private boolean isValidConnection() {
		try {
			if (CLUSTER == null) {
				logger.info("[JELLO] RECONNECT COUCHBASE : " + SEED_IPS);
				CLUSTER = CouchbaseCluster.create(env, SEED_IPS);
				return true;
			} else {
				return true;
			}
		} catch (Exception e) {
			logger.error("[JELLO] NO CONNECTIVITY TO COUCHBASE " + SEED_IPS);
			return false;
		}
	}
	
	public List<Object> queryView(String bucket,String designdoc,String viewName) {

		ViewResult result = getBucket(bucket).query(ViewQuery.from(designdoc,viewName));
		List<Object> documents=new ArrayList<>();
		for (ViewRow row : result) {
			documents.add(row.value());
		}
		return documents;
	}

	public Object readJsonDocument(String key, String bucket) {
		try {
			try {
				if (bucket.equals("conversation")) {
					StringDocument stringDocument = getBucket(bucket).get(key, StringDocument.class);
					assert stringDocument != null;
					return stringDocument.content();
				}
				JsonDocument document = getBucket(bucket).get(key);
				assert document != null;
				return document.content();
			}catch (Exception ex){
				StringDocument stringDocument = getBucket(bucket).get(key, StringDocument.class);
				assert stringDocument != null;
				return stringDocument.content();
			}
		}catch (Exception e){
			logger.error("exception while fetching couch base doc:{}", e.getMessage(), e);
			return e.getMessage();
		}
	}

	public JsonDocument writeJsonNew(String key, Object value, int timeout, int expiry, String bucket) {
		try {
			JsonDocument doc = JsonDocument.create(key, expiry, JsonObject.from((Map<String, ?>) value));
			logger.info("[JELLO] COUCHBASE writeJsonNew exp:" + expiry + " :" + bucket + " :" + doc.id());
			return getBucket(bucket).upsert(doc);
		} catch (CouchbaseException cbe) {
			logger.error("[JELLO] COUCHBASE writeJson err:"+cbe.getMessage() + " : " + bucket + " :" + key);
			return null;
		}
	}

	public JsonDocument writeJson(String key, Object value, int timeout, int expiry, String bucket) {
		try {
			JsonDocument doc = JsonDocument.create(key, expiry, JsonObject.fromJson((String) value));

			logger.info("[JELLO] COUCHBASE writeJson exp:" + expiry +  " :" + bucket + " :" + doc.id());
			return getBucket(bucket).upsert(doc);
		} catch (CouchbaseException cbe) {
			logger.error("[JELLO] COUCHBASE writeJson :"+cbe.getMessage());
			return null;
		}
	}

	public JsonDocument write(String key, Object value, int timeout, String bucket) {
		try {
			JsonObject content = JsonObject.create().put(key, (String) value);
			JsonDocument inserted = getBucket(bucket).upsert(JsonDocument.create("helloworld", content));

			return inserted;
		} catch (CouchbaseException cbe) {
			logger.error("[JELLO] COUCHBASE write :"+cbe.getMessage());
			return null;
		}
	}

	public void append(String key, String value, String bucket) {
		long start = System.currentTimeMillis();
		try {
			StringDocument doc = StringDocument.create(key, (String) value);
			if (!getBucket(bucket).exists(key)) {
				getBucket(bucket).upsert(doc);
			} else {
				getBucket(bucket).append(doc);
			}
			logger.info("[JELLO] COUCHBASE - append (" + key + ")");

		} catch (Exception e) {
			long now = System.currentTimeMillis();
			logger.error("[JELLO] COUCHBASE append " + (now - start) + "ms (" + key + "): "+e.getMessage(), e);
		}

	}

	public void append(String key, String value, int timeout, TimeUnit unit, String bucket) {
		long start = System.currentTimeMillis();
		try {
			StringDocument doc = StringDocument.create(key, (String) value);
			if (!getBucket(bucket).exists(key)) {
				getBucket(bucket).upsert(doc);
			} else {
				getBucket(bucket).append(doc);
			}
			logger.info("[JELLO] COUCHBASE - append (" + key + ")");

		} catch (Exception e) {
			long now = System.currentTimeMillis();
			logger.error("[JELLO] COUCHBASE append 2 " + (now - start) + "ms (" + key + "): " + e.getMessage(), e);
		}

	}


	public PoolInstance poolStatus(String key, String k, PoolInstance p,String bucket) {
		Gson gson = new Gson();
		Map<String, Object> usedpool = new CouchbaseMap<Object>(key, getBucket(bucket));
		if(p == null) {
			return gson.fromJson((String) usedpool.get(k), PoolInstance.class);
		}else {
			usedpool.put(k,gson.toJson(p));
			return p;
		}
		
	}
	/**
	 * 
	 * @param key - 
	 * @param val
	 * @param bPop - true - retrive from Queue , false - push to Queue
	 * @param bucket
	 * @return
	 */
	public Object handleQueue(String key,Object val,boolean bPop,String bucket) {
		try{
			Queue commonpool = new CouchbaseQueue(key, getBucket(bucket));
			if(bPop) {
				return commonpool.poll();
			}else {
				commonpool.add(val);
				return val;
			}
		}catch(CouchbaseException cbe){
			return null;
		}
	}

	public StringDocument writeStringDocument(String key, String value, int timeout, int expiry, String bucket) {
		try {
			StringDocument doc = StringDocument.create(key, expiry, value);
			StringDocument inserted = getBucket(bucket).upsert(doc);
			return inserted;
		} catch (CouchbaseException cbe) {
			logger.error("[JELLO] COUCHBASE writeStringDocument :"+cbe.getMessage());
			return null;
		}
	}

	public Object readStringDocument(String key, String bucket) {
		return getBucket(bucket).get(key, StringDocument.class).content();
	}

	public Object readDocument(String key, String bucket) {
		return getBucket(bucket).get(key);
	}

	private JsonObject merge(JsonObject obj1, JsonObject obj2) {
		for (String key : obj2.getNames()) {
			obj1.put(key, obj2.get(key));
		}
		return obj1;
	}

	public JsonDocument updateJson(String key, Object value, int timeout, int expiry, String bucket) {
		if (!isValidConnection()) {
			return null;
		}
		try {
			JsonDocument doc = null;
			Bucket bkt = getBucket(bucket);
			if (bkt == null) {
				return null;
			}
			if ((doc = bkt.get(key)) == null) {
				doc = writeJson(key, value, timeout, expiry, bucket);
			} else {
				JsonObject jobj = merge(doc.content(), JsonObject.fromJson((String) value));
				doc = JsonDocument.create(key, expiry, jobj);
			}
			logger.info("[JELLO] COUCHBASE - updateJson (" + doc.id() + ")");
			return bkt.upsert(doc);
		} catch (CouchbaseException cbe) {
			logger.error("[JELLO] COUCHBASE updateJson :"+cbe.getMessage());
			return null;
		}
	}

	public JsonArrayDocument updateJsonArray(String key, List<String> items, int timeout, String bucket) {
		if (!isValidConnection()) {
			return null;
		}
		try {

			JsonArrayDocument doc = null;
			if ((doc = getBucket(bucket).get(key, JsonArrayDocument.class)) == null) {
				// doc=JsonArrayDocument.create(key, JsonArray.from(items));
				doc = JsonArrayDocument.create(key, JsonArray.empty());
				doc.content().add(items);
			} else {
				doc.content().add(items);
			}
			logger.info("[JELLO] COUCHBASE - updateJsonArray (" + doc.id() + ")");
			return getBucket(bucket).upsert(doc);
		} catch (CouchbaseException cbe) {
			logger.error("[JELLO] COUCHBASE updateJsonArray :"+cbe.getMessage());
			return null;
		}
	}

	public JsonArray readJsonArray(String key, String bucket) {
		return getBucket(bucket).get(key, JsonArrayDocument.class).content();
	}

	public JsonArrayDocument writeJsonArray(String key, List<String> items, int timeout, String bucket) {
		if (!isValidConnection()) {
			return null;
		}
		try {

			JsonArrayDocument doc = JsonArrayDocument.create(key, JsonArray.empty());
			items.forEach(item -> doc.content().add(item));
			// doc.content().add(items);

			//System.out.println("Json Document :" + doc.id());
			return getBucket(bucket).upsert(doc);
		} catch (CouchbaseException cbe) {
			logger.error("[JELLO] COUCHBASE writeJsonArray :"+cbe.getMessage());
			return null;
		}
	}

	public JsonDocument deleteDocument(String key, String bucket) {
		//System.out.println("Deleting document from couchbase :" + key);
		try {
			return getBucket(bucket).remove(key);
		} catch (com.couchbase.client.java.error.DocumentDoesNotExistException e) {
			logger.info("[JELLO] COUCHBASE deleteDocument:" + bucket + ":" +  key + " : " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Write custom object to couchbasse
	 */
	public Object writeCustomDocument(String key, Object value, int timeout, String bucket){
		BinaryDocument doc=BinaryDocument.create(key, timeout, Unpooled.copiedBuffer(SerializationUtils.serialize(value)));

		return getBucket(bucket).upsert(doc);
	}
	
	/**
	 * read custom object to couchbasse
	 */
	public Object readCustomDocument(String key,String bucket) {
		ByteBuf buf= getBucket(bucket).get(key,BinaryDocument.class).content();
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return SerializationUtils.deserialize(data);
	}
	
	public N1qlQueryResult getAllRecordsFromBucket(String bucket, String query) {
		N1qlQueryResult result = null;
		try {
			result  = getBucket(bucket).query(N1qlQuery.simple(query));
		} catch (Exception e) {
			logger.error("[JELLO] COUCHBASE append :"+e.getMessage());
		}
		return result;
	}
}
