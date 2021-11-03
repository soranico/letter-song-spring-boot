package com.kano.tomcat.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class KanoPropertiesApplicationListener implements ApplicationListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(KanoPropertiesApplicationListener.class);
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		LOGGER.info("linstener");
	}
}
