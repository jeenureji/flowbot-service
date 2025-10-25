package com.mondee.flowbot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotExecutionStats {

	private String name;
	private String status;
	private String message;
	private long executionTime;
	private String request;
	private String response;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy HH:mm:ss", timezone = "UTC")
	private Date createdDate;
	private String type;

	public BotExecutionStats(String name, String status, String message, long executionTime, String request, String response, String type) {
		this.name = name;
		this.status = status;
		this.message = message;
		this.executionTime = executionTime;
		this.request = request;
		this.response = response;
		this.type = type;
	}


}
