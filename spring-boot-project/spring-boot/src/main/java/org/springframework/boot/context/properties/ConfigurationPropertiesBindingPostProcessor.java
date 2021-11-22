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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertySources;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class ConfigurationPropertiesBindingPostProcessor
		implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {

	/**
	 * The bean name that this post-processor is registered with.
	 */
	public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();

	private ApplicationContext applicationContext;

	private BeanDefinitionRegistry registry;

	private ConfigurationPropertiesBinder binder;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// We can't use constructor injection of the application context because
		// it causes eager factory bean initialization
		this.registry = (BeanDefinitionRegistry) this.applicationContext.getAutowireCapableBeanFactory();
		/**
		 *
		 * 在容器注册 BPP 时会被创建为bean
		 * @see AbstractApplicationContext#registerBeanPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
		 *
		 * @see ConfigurationPropertiesBinder
		 *
		 * 这个是从这个 ImportBeanDefinitionRegistrar
		 * @see EnableConfigurationPropertiesRegistrar#registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)
		 */
		this.binder = ConfigurationPropertiesBinder.get(this.applicationContext);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		/**
		 * 这里会将创建出来的对象封装为一个配置bean
		 * @see ConfigurationPropertiesBean#get(ApplicationContext, Object, String)
		 * 
		 * 因为需要进行参数的绑定
		 * @see ConfigurationPropertiesBindingPostProcessor#bind(ConfigurationPropertiesBean)
		 */
		bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
		return bean;
	}

	private void bind(ConfigurationPropertiesBean bean) {
		if (bean == null || hasBoundValueObject(bean.getName())) {
			return;
		}
		Assert.state(bean.getBindMethod() == BindMethod.JAVA_BEAN, "Cannot bind @ConfigurationProperties for bean '"
				+ bean.getName() + "'. Ensure that @ConstructorBinding has not been applied to regular bean");
		try {
			/**
			 * 进行配置参数绑定
			 * @see ConfigurationPropertiesBinder#bind(ConfigurationPropertiesBean)
			 */
			this.binder.bind(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

	private boolean hasBoundValueObject(String beanName) {
		return this.registry.containsBeanDefinition(beanName) && this.registry
				.getBeanDefinition(beanName) instanceof ConfigurationPropertiesValueObjectBeanDefinition;
	}

	/**
	 * Register a {@link ConfigurationPropertiesBindingPostProcessor} bean if one is not
	 * already registered.
	 * @param registry the bean definition registry
	 * @since 2.2.0
	 */
	public static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class,
							ConfigurationPropertiesBindingPostProcessor::new)
					.getBeanDefinition();
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
		/**
		 * 用于创建 ConfigurationPropertiesBinder
		 * @see ConfigurationPropertiesBinder.Factory
		 *
		 * 用于真正执行配置参数的绑定
		 * @see ConfigurationPropertiesBinder
		 */
		ConfigurationPropertiesBinder.register(registry);
	}

}
