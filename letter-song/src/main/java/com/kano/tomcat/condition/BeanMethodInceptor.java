package com.kano.tomcat.condition;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Component
public class BeanMethodInceptor implements FactoryBean<MappedInterceptor> {

//	@Autowired
//	private Kano kano;

	@Override
	public MappedInterceptor getObject() throws Exception {
		MappedInterceptor mappedInterceptor = new MappedInterceptor(null,new MyInceptor(null));
		return mappedInterceptor;
	}

	@Override
	public Class<?> getObjectType() {
		return MappedInterceptor.class;
	}

	private static class MyInceptor implements HandlerInterceptor{
		MyInceptor(Kano kano){

		}
	}
}
