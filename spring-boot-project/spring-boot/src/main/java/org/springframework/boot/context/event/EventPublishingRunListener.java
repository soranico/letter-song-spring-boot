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

package org.springframework.boot.context.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ErrorHandler;

/**
 * {@link SpringApplicationRunListener} to publish {@link SpringApplicationEvent}s.
 * <p>
 * Uses an internal {@link ApplicationEventMulticaster} for the events that are fired
 * before the context is actually refreshed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @author Brian Clozel
 * @since 1.0.0
 */
public class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {

	private final SpringApplication application;

	private final String[] args;

	private final SimpleApplicationEventMulticaster initialMulticaster;

	public EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
		/**
		 *
		 * @see org.springframework.boot.env.EnvironmentPostProcessorApplicationListener
		 * @see org.springframework.boot.context.config.AnsiOutputApplicationListener
		 * @see org.springframework.boot.context.logging.LoggingApplicationListener
		 * @see org.springframework.boot.autoconfigure.BackgroundPreinitializer
		 * @see org.springframework.boot.context.config.DelegatingApplicationListener
		 * @see org.springframework.boot.builder.ParentContextCloserApplicationListener
		 * @see org.springframework.boot.ClearCachesApplicationListener
		 * @see org.springframework.boot.context.FileEncodingApplicationListener
		 */
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void starting(ConfigurableBootstrapContext bootstrapContext) {
		/**
		 * @see SimpleApplicationEventMulticaster#multicastEvent(ApplicationEvent)
		 */
		this.initialMulticaster
				.multicastEvent(new ApplicationStartingEvent(bootstrapContext, this.application, this.args));
	}

	@Override
	public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
			ConfigurableEnvironment environment) {
		this.initialMulticaster.multicastEvent(
				new ApplicationEnvironmentPreparedEvent(bootstrapContext, this.application, this.args, environment));
	}

	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {
		/**
		 * 事件
		 * @see ApplicationContextInitializedEvent
		 *
		 * @see org.springframework.boot.autoconfigure.BackgroundPreinitializer#onApplicationEvent(SpringApplicationEvent)
		 *
		 * @see BackgroundPreinitalializer
		 * @see org.springframework.boot.context.config.DelegatingApplicationListener
		 */
		this.initialMulticaster
				.multicastEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
	}

	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		for (ApplicationListener<?> listener : this.application.getListeners()) {
			/**
			 * 设置回调接口的参数
			 */
			if (listener instanceof ApplicationContextAware) {
				((ApplicationContextAware) listener).setApplicationContext(context);
			}
			context.addApplicationListener(listener);
		}
		/**
		 * @see SimpleApplicationEventMulticaster#multicastEvent(ApplicationEvent)
		 * 
		 * @see org.springframework.boot.env.EnvironmentPostProcessorApplicationListener#onApplicationEvent(ApplicationEvent)
		 *
		 * @see org.springframework.boot.context.logging.LoggingApplicationListener#onApplicationEvent(ApplicationEvent)
		 *
		 * @see org.springframework.boot.autoconfigure.BackgroundPreinitializer#onApplicationEvent(SpringApplicationEvent)
		 *
		 * 这个会调用配置文件里面的监听
		 * context.listener.classes
		 * @see org.springframework.boot.context.config.DelegatingApplicationListener#onApplicationEvent(ApplicationEvent)
		 */
		this.initialMulticaster.multicastEvent(new ApplicationPreparedEvent(this.application, this.args, context));
	}

	@Override
	public void started(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context));
		AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
	}

	@Override
	public void running(ConfigurableApplicationContext context) {
		/**
		 * 判断容器环境准备完成事件开启的后台任务是否执行完成
		 * @see org.springframework.boot.autoconfigure.BackgroundPreinitializer#onApplicationEvent(org.springframework.boot.context.event.SpringApplicationEvent)
		 */
		context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context));
		AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
	}

	@Override
	public void failed(ConfigurableApplicationContext context, Throwable exception) {
		ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
		if (context != null && context.isActive()) {
			// Listeners have been registered to the application context so we should
			// use it at this point if we can
			context.publishEvent(event);
		}
		else {
			// An inactive context may not have a multicaster so we use our multicaster to
			// call all of the context's listeners instead
			if (context instanceof AbstractApplicationContext) {
				for (ApplicationListener<?> listener : ((AbstractApplicationContext) context)
						.getApplicationListeners()) {
					this.initialMulticaster.addApplicationListener(listener);
				}
			}
			this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
			this.initialMulticaster.multicastEvent(event);
		}
	}

	private static class LoggingErrorHandler implements ErrorHandler {

		private static final Log logger = LogFactory.getLog(EventPublishingRunListener.class);

		@Override
		public void handleError(Throwable throwable) {
			logger.warn("Error calling ApplicationEventListener", throwable);
		}

	}

}
