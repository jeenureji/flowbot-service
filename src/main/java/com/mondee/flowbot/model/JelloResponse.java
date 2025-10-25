package com.mondee.flowbot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class JelloResponse {

    private int responseCode;
    private String responseDesc;
    private String corelationId;

    @JsonInclude(Include.NON_NULL)
    private Object data;

    public JelloResponse() {

    }

    public JelloResponse(int code, String message) {
        this.responseCode = code;
        this.responseDesc = message;
    }

    public JelloResponse(int code, String message, Object data) {
        this.responseCode = code;
        this.responseDesc = message;
        this.data = data;
    }
    
    public JelloResponse(int code, String message, Object data,String corelationId) {
        this.responseCode = code;
        this.responseDesc = message;
        this.data = data;
        this.corelationId = corelationId;
    }
    
    public JelloResponse(int code, String message,String corelationId) {
        this.responseCode = code;
        this.responseDesc = message;
        this.corelationId = corelationId;
    }


    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[code=");
        builder.append(responseCode);
        builder.append(", message=");
        builder.append(responseDesc);
        builder.append(", data=");
        builder.append(data);
        builder.append("]");
        return builder.toString();
    }

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getResponseDesc() {
		return responseDesc;
	}

	public void setResponseDesc(String responseDesc) {
		this.responseDesc = responseDesc;
	}

	public String getCorelationId() {
		return corelationId;
	}

	public void setCorelationId(String corelationId) {
		this.corelationId = corelationId;
	}
}
