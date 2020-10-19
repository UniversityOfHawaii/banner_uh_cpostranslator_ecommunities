package edu.hawaii.banner.cpos;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ContextClosedListener
		implements ApplicationListener<ContextClosedEvent> {
	private PropertyInitializedBean propertyInitializedBean;
	private static Logger logger = LogManager
			.getLogger(ContextRefreshedEvent.class);

	@Autowired
	public void setPropertyInitializedBean(
			PropertyInitializedBean propertyInitializedBean) {
		this.propertyInitializedBean = propertyInitializedBean;
	}

	// @Override
	public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
		System.out.println("Context Closed Event received, exiting gracefully");
		logger.debug("Context Closed Event received, exiting gracefully");
		propertyInitializedBean.setCtxClosed(true);
	}

}
