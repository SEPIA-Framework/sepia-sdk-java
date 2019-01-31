package net.b07z.sepia.sdk.main;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.Is;

public class SdkConfig {
	
	private static final Logger log = LoggerFactory.getLogger(SdkConfig.class);
	
	//external configuration file, put this somewhere save if you add credentials
	public static final String configFile = "Settings/sdk.properties";
	
	public static String assistAPI = "http://localhost:20721/";	 	//URL to assistant API
	//static String teachAPI = "https://localhost:20722/";		//URL to teach API
	
	public static String userId;			//your user ID
	public static String password;			//your password
	
	public static final String servicesPath = "net/b07z/sepia/sdk/services/";
	
	//---------- helpers -----------
	
	/**
	 * Load server settings from properties file. 
	 */
	public static void loadSettings(String customConfigFile){
		if (Is.nullOrEmpty(customConfigFile)){
			customConfigFile = configFile;
		}
		
		try{
			Properties settings = FilesAndStreams.loadSettings(customConfigFile);
			
			//server
			assistAPI = settings.getProperty("assist_api_url");	
			//teachAPI = settings.getProperty("teach_api_url");
			
			//credentials
			userId = settings.getProperty("user_id");
			password = settings.getProperty("password");
			
			log.info("Loading settings from " + customConfigFile + "... done.");
		}catch (Exception e){
			log.info("Loading settings from " + customConfigFile + "... failed!");
		}
	}

}
