package com.mondee.flowbot.model;

import lombok.Data;

@Data
public class OAuthTokenData {
	
	String accessToken;
	long expiresIn;
}
