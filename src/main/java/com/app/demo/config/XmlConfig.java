package com.app.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@ImportResource("classpath:beans.xml")
public class XmlConfig {
	// @Bean
	// public ServerEndpointExporter serverEndpointExporter() {
	// return new ServerEndpointExporter();
	// }

}