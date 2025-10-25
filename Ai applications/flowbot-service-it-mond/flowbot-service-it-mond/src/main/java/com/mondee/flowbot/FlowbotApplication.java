package com.mondee.flowbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

//import springfox.documentation.swagger2.annotations.EnableSwagger2;

//@EnableSwagger2
@SpringBootApplication
@EnableAsync
public class FlowbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowbotApplication.class, args);
	}

}
