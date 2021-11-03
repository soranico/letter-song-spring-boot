package com.kano.tomcat;

import com.kano.tomcat.config.KanoConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Import(KanoConfig.class)
@SpringBootApplication
public class TomcatApp {

	public static void main(String[] args) {
		SpringApplication.run(TomcatApp.class,args);
	}
}
