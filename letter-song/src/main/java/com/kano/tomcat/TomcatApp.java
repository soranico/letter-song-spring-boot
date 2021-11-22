package com.kano.tomcat;

import com.kano.tomcat.config.KanoConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(KanoConfig.class)
@SpringBootApplication
public class TomcatApp {
	/**
	 * @see org.springframework.boot.autoconfigure.AutoConfigureAfter
	 * @see org.springframework.boot.autoconfigure.AutoConfigureBefore
	 * @see org.springframework.boot.autoconfigure.AutoConfigureOrder
	 * 只对META-INF/spring.factories文件配置的会生效
	 * @see AutoConfigurationImportSelector.AutoConfigurationGroup#selectImports()
	 */
	public static void main(String[] args) {
		SpringApplication.run(TomcatApp.class,args);
	}
}
