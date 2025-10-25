package com.mondee.flowbot.utils;

import org.springframework.http.HttpStatus;

import com.mondee.flowbot.model.FlowBotResponse;

public class FlowbotException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private FlowBotResponse errorResponse;

    public FlowbotException(int code, String message) {
        super(message);
        if (errorResponse == null) {
            errorResponse = new FlowBotResponse();
        }
        errorResponse.setResponseCode(code);
        errorResponse.setResponseDesc(message);
    }
    
    public FlowbotException(String message) {
        super(message);
        if (errorResponse == null) {
            errorResponse = new FlowBotResponse();
        }
        errorResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setResponseDesc(message);
    }

    @Override
    public String getMessage() {
        if (errorResponse != null) {
            return errorResponse.getResponseDesc();
        }
        return super.getMessage();
    }

    public int getCode() {
        if (errorResponse != null) {
            return errorResponse.getResponseCode();
        }
        return 1000 ;
    }
}
