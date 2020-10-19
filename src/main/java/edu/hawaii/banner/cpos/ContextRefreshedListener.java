package edu.hawaii.banner.cpos;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

//This class checks the existence of STAR connection details
//It's used during startup - to continue or halt startup
@Component
public class ContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent>{
	//  STAR connection details
	@Value("${star.user}")
  String starUser;
  @Value("${star.cred}")
  String starCred;
  @Value("${star.url}")
  String starUrl;
  
  private PropertyInitializedBean propertyInitializedBean;
  private static Logger logger = LogManager.getLogger(ContextRefreshedEvent.class);
  
    @Autowired
    public void setPropertyInitializedBean(PropertyInitializedBean propertyInitializedBean) {
        this.propertyInitializedBean = propertyInitializedBean;
    }

    //@Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
      System.out.println("Context Event Received, checking for existence of app processing script");
      logger.debug("Context Event Received, checking for existence of app processing script");
        System.out.println("starUser: " + starUser);
        //System.out.println("starCred: " + scriptDir);
        System.out.println("starUrl: " + starUrl);
        
        if (starUser != null && starCred != null && starUrl != null) {
    			System.out.println("STAR connection details exists");
      		propertyInitializedBean.setStarDetailsFound(true);
    		}
    		else {
    			System.out.println("STAR connection details does not exist");
      		propertyInitializedBean.setStarDetailsFound(false);
    		}
    }

}
