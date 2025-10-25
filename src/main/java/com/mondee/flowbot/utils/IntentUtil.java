package com.mondee.flowbot.utils;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;

import com.mondee.flowbot.model.Intents;

public class IntentUtil {
	private static final int START_INTENT = 1;
	public static ArrayList<String> intentTypeList = new ArrayList<String>();

	public static void setIntentTypeList(ArrayList<String> intentTypeList) {
		intentTypeList.add("json"); intentTypeList.add("soap"); intentTypeList.add("decision"); intentTypeList.add("setUserData");
		intentTypeList.add("setpagedata"); intentTypeList.add("javaScript"); intentTypeList.add("triggerPath"); intentTypeList.add("task");
		intentTypeList.add("email"); intentTypeList.add("liveAgent"); intentTypeList.add("reset");
		IntentUtil.intentTypeList = intentTypeList;
	}

	public static ArrayList<String> getIntentTypeList() {
		return intentTypeList;
	}

	public static int compareIntents(Intents currentState,Intents futureIntent,ArrayList<Intents> storyBoard) {
		int diff = 0;
		String futureIntentName = futureIntent.getIntents();
		for(Intents intent : storyBoard) {
			String intentName = intent.getIntents();
			if(intentName.equalsIgnoreCase(futureIntentName)) {
				if(futureIntent.getOrder() > 1) {
					diff = futureIntent.getOrder() - currentState.getOrder();
					if(diff == 1) {
						
					} else if(diff > 1) {
						//is intent optional
					} else if(diff < 1) {
						//is looping allowed
					}
				}
			}
		}
		return diff;
	}

	public static Intents getIntents(Intents futureIntent,ArrayList<Intents> storyBoard) {
		String futureIntentName = futureIntent.getIntents();
		for(Intents intent : storyBoard) {
			String intentName = intent.getIntents();
			if(intentName.equalsIgnoreCase(futureIntentName)) {
				return intent;
			}
		}
		return null;
	}
	

	/**
	 * This method will return the Intent object for the provided IntentName else they will get the Intent whose order is 1
	 * @param intentName
	 * @param storyBoard
	 * @return Intents
	 */
	public static Intents getIntentOrStartIntent(String intentName,ArrayList<Intents> storyBoard) throws FlowbotException {
		Intents intent = null;
		if(null != intentName && !intentName.isEmpty()) {
			intent = storyBoard.stream().filter(template -> intentName.equalsIgnoreCase(template.getIntents())).findAny().orElse(null);
		} else {
			intent = storyBoard.stream().filter(template -> (START_INTENT == template.getOrder())).findFirst().orElse(null);
		}
		if(null == intent) {
			throw new FlowbotException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "The provided intent does not exists");
		}
		return intent;
	}
	
	
	public static int getIntentOrder(Intents futureIntent,ArrayList<Intents> storyBoard) {
		String futureIntentName = futureIntent.getIntents();
		for(Intents intent : storyBoard) {
			String intentName = intent.getIntents();
			if(intentName.equalsIgnoreCase(futureIntentName)) {
				return intent.getOrder();
			}
		}
		return -1;
	}
	
	public static boolean isLastIntentInOrder(Intents futureIntent,ArrayList<Intents> storyBoard) {
		if(getLastIntentOrder(storyBoard) == futureIntent.getOrder()) {
			return true;
		}
		return false;
	}
	
	private static int getLastIntentOrder(ArrayList<Intents> storyBoard) {
		int order = -1;
		for(Intents i : storyBoard) {
			if(i.getOrder() > order) {
				order = i.getOrder();
			}
		}
		return order;
	}

}
