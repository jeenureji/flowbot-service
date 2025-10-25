package com.mondee.flowbot.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BotTemplate {

	private ArrayList<Intents> storyBoardList;
	private Intents contextExpiredState;
	private Intents errorState;
	
	private double version;
	private double verison;
	
	public double getVerison() {
		return verison;
	}

	public void setVerison(double verison) {
		this.verison = verison;
	}

	public double getVersion() {
		return version;
	}

	public void setVersion(double version) {
		this.version = version;
	}

	/**
	 * @return the storyBoardList
	 */
	public ArrayList<Intents> getStoryBoardList() {
		return storyBoardList;
	}
	
	/**
	 * @param storyBoardList the storyBoardList to set
	 */
	public void setStoryBoardList(ArrayList<Intents> storyBoardList) {
		this.storyBoardList = storyBoardList;
	}
	
	/**
	 * @return the contextExpiredState
	 */
	public Intents getContextExpiredState() {
		return contextExpiredState;
	}
	
	/**
	 * @param contextExpiredState the contextExpiredState to set
	 */
	public void setContextExpiredState(Intents contextExpiredState) {
		this.contextExpiredState = contextExpiredState;
	}
	
	/**
	 * @return the errorState
	 */
	public Intents getErrorState() {
		return errorState;
	}
	
	/**
	 * @param errorState the errorState to set
	 */
	public void setErrorState(Intents errorState) {
		this.errorState = errorState;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BotTemplate [storyBoardList=" + storyBoardList + ", contextExpiredState=" + contextExpiredState
				+ ", errorState=" + errorState + "]";
	}
	
}
