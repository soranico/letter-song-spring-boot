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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.util.ReflectionUtils;

/**
 * A collection of {@link SpringApplicationRunListener}.
 *
 * @author Phillip Webb
 */
class SpringApplicationRunListeners {

	private final Log log;

	private final List<SpringApplicationRunListener> listeners;

	private final ApplicationStartup applicationStartup;

	SpringApplicationRunListeners(Log log, Collection<? extends SpringApplicationRunListener> listeners,
			ApplicationStartup applicationStartup) {
		this.log = log;
		this.listeners = new ArrayList<>(listeners);
		this.applicationStartup = applicationStartup;
	}

	void starting(ConfigurableBootstrapContext bootstrapContext, Class<?> mainApplicationClass) {
		/**
		 * 发布启动事件
		 * @see org.springframework.boot.context.event.ApplicationStartingEvent 事件类型
		 * @see SpringApplicationRunListeners#doWithListeners(java.lang.String, java.util.function.Consumer, java.util.function.Consumer)
		 *
		 * @see org.springframework.boot.context.event.EventPublishingRunListener#starting(ConfigurableBootstrapContext)
		 */
		doWithListeners("spring.boot.application.starting", (listener) -> listener.starting(bootstrapContext),
				(step) -> {
					if (mainApplicationClass != null) {
						step.tag("mainApplicationClass", mainApplicationClass.getName());
					}
				});
	}

	void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
		/**
		 * 事件
		 * @see org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
		 *
		 * 订阅者
		 * 从spring.factories加载 EnvironmentPostProcessor 并调用方法
		 * @see org.springframework.boot.env.EnvironmentPostProcessor#postProcessEnvironment(ConfigurableEnvironment, SpringApplication)
		 * @see org.springframework.boot.env.EnvironmentPostProcessorApplicationListener#onApplicationEvent(ApplicationEvent)
		 *
		 * @see org.springframework.boot.context.config.AnsiOutputApplicationListener#onApplicationEvent(ApplicationEnvironmentPreparedEvent)
		 *
		 * 进行日志的初始化配置
		 * @see org.springframework.boot.context.logging.LoggingApplicationListener#onApplicationEvent(ApplicationEvent)
		 *
		 * 后台线程进行一些类的初始化
		 * @see org.springframework.boot.autoconfigure.BackgroundPreinitializer#onApplicationEvent(SpringApplicationEvent)
		 *
		 * 加载 context.listener.classes 并调用其监听方法传播环境刷新事件
		 * @see org.springframework.boot.context.config.DelegatingApplicationListener#onApplicationEvent(ApplicationEvent)
		 *
		 * @see org.springframework.boot.context.FileEncodingApplicationListener#onApplicationEvent(ApplicationEnvironmentPreparedEvent)
		 */
		doWithListeners("spring.boot.application.environment-prepared",
				(listener) -> listener.environmentPrepared(bootstrapContext, environment));
	}

	void contextPrepared(ConfigurableApplicationContext context) {
		/**
		 * 发布容器准备事件，此时容器已经创建还没有刷新
		 * @see org.springframework.boot.context.event.ApplicationContextInitializedEvent
		 * @see org.springframework.boot.context.event.EventPublishingRunListener#contextPrepared(ConfigurableApplicationContext)
		 */
		doWithListeners("spring.boot.application.context-prepared", (listener) -> listener.contextPrepared(context));
	}

	void contextLoaded(ConfigurableApplicationContext context) {
		/**
		 * 发布容器准备完成事件
		 * @see org.springframework.boot.context.event.ApplicationPreparedEvent
		 * @see org.springframework.boot.context.event.EventPublishingRunListener#contextLoaded(ConfigurableApplicationContext) 容器准备完成事件
		 */
		doWithListeners("spring.boot.application.context-loaded", (listener) -> listener.contextLoaded(context));
	}

	void started(ConfigurableApplicationContext context) {
		doWithListeners("spring.boot.application.started", (listener) -> listener.started(context));
	}

	void running(ConfigurableApplicationContext context) {
		doWithListeners("spring.boot.application.running", (listener) -> listener.running(context));
	}

	void failed(ConfigurableApplicationContext context, Throwable exception) {
		doWithListeners("spring.boot.application.failed",
				(listener) -> callFailedListener(listener, context, exception), (step) -> {
					step.tag("exception", exception.getClass().toString());
					step.tag("message", exception.getMessage());
				});
	}

	private void callFailedListener(SpringApplicationRunListener listener, ConfigurableApplicationContext context,
			Throwable exception) {
		try {
			listener.failed(context, exception);
		}
		catch (Throwable ex) {
			if (exception == null) {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			if (this.log.isDebugEnabled()) {
				this.log.error("Error handling failed", ex);
			}
			else {
				String message = ex.getMessage();
				message = (message != null) ? message : "no error message";
				this.log.warn("Error handling failed (" + message + ")");
			}
		}
	}

	private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction) {
		doWithListeners(stepName, listenerAction, null);
	}

	private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction,
			Consumer<StartupStep> stepAction) {
		/**
		 * @see org.springframework.core.metrics.DefaultApplicationStartup#start(String)
		 */
		StartupStep step = this.applicationStartup.start(stepName);
		/**
		 * 执行具体的事件
		 */
		this.listeners.forEach(listenerAction);
		if (stepAction != null) {
			stepAction.accept(step);
		}
		step.end();
	}

}
