package net.b07z.sepia.sdk.connect;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.main.SdkConfig;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

public class DeleteService {

	private static final Logger log = LoggerFactory.getLogger(DeleteService.class);
	
	/**
	 * Delete a custom service by removing the custom command mappings and triggers. 
	 * Optionally deletes service files by giving an array of class names (simple name).
	 * @param commandsToRemove
	 * @param serviceNames
	 * @return
	 */
	public static boolean deleteServicesConnectedToCommands(JSONArray commandsToRemove, JSONArray serviceNames){
		//Load credentials and API URLs from configuration file
        SdkConfig.loadSettings(SdkConfig.configFile);
        
        //Check credentials
        if (Is.nullOrEmpty(SdkConfig.userId) || Is.nullOrEmpty(SdkConfig.password)){
            throw new RuntimeException("Please check your credentials in: " + SdkConfig.configFile);
        }
        
        //Set delete endpoint and request body
        String URL = SdkConfig.assistAPI + "delete-service";
        log.info("Using endpoint: " + URL);
        
        JSONObject requestBody = JSON.make(
        		"commands", commandsToRemove, 
        		"services", serviceNames,
        		"GUUID", SdkConfig.userId,
        		"PWD", SdkConfig.password
        );
        
        //Call
        JSONObject response = Connectors.httpPOST(URL, requestBody.toJSONString(), null);
        log.info(JSONWriter.getPrettyString(response));
        
        if (Connectors.httpSuccess(response) && JSON.getString(response, "result").equals("success")){
        	return true;
        }else{
        	return false;
        }
	}
}
