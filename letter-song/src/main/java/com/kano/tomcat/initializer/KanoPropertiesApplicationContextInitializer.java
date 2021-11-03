package com.kano.tomcat.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 配置文件加载
 * context.initializer.classes = 类名
 */
public class KanoPropertiesApplicationContextInitializer implements ApplicationContextInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(KanoPropertiesApplicationContextInitializer.class);

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		LOGGER.info("read");
	}
}
