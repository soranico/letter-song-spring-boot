package com.kano.tomcat.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@ConditionalOnBean(value = Kano.class)
@Component
public class KanoConditionOnBean {

}
