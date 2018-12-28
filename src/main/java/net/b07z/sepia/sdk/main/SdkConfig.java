package net.b07z.sepia.sdk.main;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.server.core.tools.FilesAndStreams;

public class SdkConfig {
	
	private static final Logger log = LoggerFactory.getLogger(SdkConfig.class);
	
	//external configuration file, put this somewhere save if you add credentials
	public static final String config_file = "Settings/sdk.properties";
	
	public static String assistAPI = "http://localhost:20721/";	 	//URL to assistant API
	//static String teachAPI = "https://localhost:20722/";		//URL to teach API
	
	public static String userId;			//your user ID
	public static String password;			//your password
	
	//---------- helpers -----------
	
	/**
	 * Load server settings from properties file. 
	 */
	public static void load_settings(String configFile){
		if (configFile == null || configFile.isEmpty())	configFile = config_file;
		
		try{
			Properties settings = FilesAndStreams.loadSettings(configFile);
			
			//server
			assistAPI = settings.getProperty("assist_api_url");	
			//teachAPI = settings.getProperty("teach_api_url");
			
			//credentials
			userId = settings.getProperty("user_id");
			password = settings.getProperty("password");
			
			log.info("Loading settings from " + configFile + "... done.");
		}catch (Exception e){
			log.info("Loading settings from " + configFile + "... failed!");
		}
	}

}
