package edu.hawaii.banner.cpos;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CposApplication {
	private static final Logger logger = LogManager
			.getLogger(CposApplication.class);

	private static final String nameOS = "os.name";
	private static String sFilename = null;

	// This does not seem to work
	public void attachShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("\n\n *** Spring Boot API shutting down ***\n");
				System.out.println("*** Exiting Gracefully....... ***\n");
			}
		});
		System.out.println("\n\n *** Shut Down Hook Attached ***\n");
	}

	public static void main(String[] args) {
		CposApplication application = new CposApplication();
		application.attachShutDownHook();

		// check for existence of certain properties
		// see ContextRefreshedListener.java
		ConfigurableApplicationContext ctx = SpringApplication
				.run(CposApplication.class, args);
		PropertyInitializedBean bean = ctx.getBean(PropertyInitializedBean.class);
		System.out.println("required scripts/properties found? - " + bean.getStarDetailsFound());

		logger.debug("Starting main CPOS translator Spring Boot application...");
		logger.debug("Does STAR connection details exists? " + bean.getStarDetailsFound());

		if (!bean.getStarDetailsFound()) {
			System.err.println(
					"Application Aborting.  STAR connection details not found");
			logger.error(
					"Application Aborting.  STAR connection details not found");
			ctx.close();
		}

		// This does not work
		if (bean.getCtxClosed()) {
			System.err.println("Application Aborting gracefully.");
			logger.error("Application Aborting gracefully.");
		}

	}

}