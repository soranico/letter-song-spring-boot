package com.kano.tomcat.bind.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "kano")
public class KanoProperties {
	private String name;





	public void setName(String name) {
		this.name = name;
	}

}
