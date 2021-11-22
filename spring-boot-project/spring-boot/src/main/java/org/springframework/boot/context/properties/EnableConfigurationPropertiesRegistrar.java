/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link EnableConfigurationProperties @EnableConfigurationProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class EnableConfigurationPropertiesRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME = Conventions
			.getQualifiedAttributeName(EnableConfigurationPropertiesRegistrar.class, "methodValidationExcludeFilter");

	/**
	 * 通过中注解Import进来的
	 * @see EnableConfigurationProperties
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		/**
		 * 注册处理配置参数绑定的BD
		 * @see ConfigurationPropertiesBindingPostProcessor 这个是BPP
		 * @see BoundConfigurationProperties
		 *
		 * 用于创建 ConfigurationPropertiesBinder
		 * @see ConfigurationPropertiesBinder.Factory
		 *
		 * 用于真正执行配置参数的绑定
		 * @see ConfigurationPropertiesBinder
		 *
		 */
		registerInfrastructureBeans(registry);

		/**
		 * 注册方法级别的注解排除
		 * @see MethodValidationExcludeFilter
		 */
		registerMethodValidationExcludeFilter(registry);

		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(registry);
		/**
		 * 这里获取的是
		 * @see EnableConfigurationProperties
		 * 里面配置的类
		 * 如果容器里面没有这个类那么会默认注册到容器
		 * 
		 * @see ConfigurationPropertiesBeanRegistrar#register(Class) 
		 */
		getTypes(metadata).forEach(beanRegistrar::register);
	}

	private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
		return metadata.getAnnotations().stream(EnableConfigurationProperties.class)
				.flatMap((annotation) -> Arrays.stream(annotation.getClassArray(MergedAnnotation.VALUE)))
				.filter((type) -> void.class != type).collect(Collectors.toSet());
	}

	static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
		/**
		 * 注册一个BPP
		 * 用于处理绑定配置参数
		 * @see ConfigurationPropertiesBindingPostProcessor
		 *
		 * 用于创建 ConfigurationPropertiesBinder
		 * @see ConfigurationPropertiesBinder.Factory
		 *
		 * 用于真正执行配置参数的绑定
		 * @see ConfigurationPropertiesBinder
		 */
		ConfigurationPropertiesBindingPostProcessor.register(registry);
		/**
		 * 用于记录绑定的配置参数
		 * @see BoundConfigurationProperties
		 */
		BoundConfigurationProperties.register(registry);
	}

	static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
					.genericBeanDefinition(MethodValidationExcludeFilter.class,
							() -> MethodValidationExcludeFilter.byAnnotation(ConfigurationProperties.class))
					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition();
			registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
		}
	}

}
