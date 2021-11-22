package com.kano.tomcat.config;

import com.kano.tomcat.bind.properties.KanoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@EnableConfigurationProperties(KanoProperties.class)
@ComponentScan({"com.kano.tomcat.condition","com.kano.tomcat.bind.properties"})
public class KanoConfig {
}
