package net.b07z.sepia.sdk.connect;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import net.b07z.sepia.sdk.main.SdkConfig;
import net.b07z.sepia.sdk.tools.HttpTools;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.ContentBuilder;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to automatically upload the class files to the assistant API and test them.
 *
 * @author Florian Quirin
 *
 */
public class UploadService {
	
	private static final Logger log = LoggerFactory.getLogger(UploadService.class);
	
	/**
	 * Transfer class-file to server and make test-call to API.
	 * @param service - Class to transfer
	 * @param language - ISO language code
	 * @param testSentence - can be null if service has defined sentences
	 * @return
	 */
	public static boolean transfer(ServiceInterface service, Language language, String testSentence){
		//Upload this service:
        String serviceClass = service.getClass().getSimpleName();

        //Get test sentence
        String lang = language.toValue();
        Set<String> testSentences = service.getSampleSentences(lang);
        if (Is.nullOrEmpty(testSentence) && testSentences.isEmpty()){
        	throw new RuntimeException("Upload aborted: No test sentences defined!");
        }else if (Is.nullOrEmpty(testSentence)){
            testSentence = testSentences.iterator().next();
        }
        
        //Load credentials and API URLs from configuration file
        SdkConfig.load_settings(SdkConfig.config_file);
        log.info("\n");
        log.info("------------------- GET CLASS ---------------------");

        //get the .class file from your folder (usually: target/...)
        File serviceFile = new File("target/classes/net/b07z/sepia/sdk/services/" + SdkConfig.userId + "/" + serviceClass + ".class");
        if (!serviceFile.exists())
            throw new RuntimeException("Class-file not found: " + serviceFile.getAbsolutePath());
        else {
        	log.info("Loaded file: " + serviceFile.getAbsolutePath());
        	log.info("\n");
        	log.info("-------------------- UPLOAD --------------------");
        }

        //upload service
        try {
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("GUUID", SdkConfig.userId);
            parameters.put("PWD", SdkConfig.password);
            String URL = SdkConfig.assistAPI + "upload-service";
            log.info("Uploading file to: " + URL);
            boolean success = HttpTools.httpFilePOST(URL, "uploaded_file", serviceFile, parameters);
            if (!success){
            	log.info("\n");
            	log.error("------------------ TERMINATED ------------------");
                throw new RuntimeException("Upload failed! Error: see comments");
            }

        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Upload failed! Error: " + e.getMessage());
        }
        log.info("\n");
        log.info("------------- WAITING FOR DATABASE REFRESH --------------");

        //wait a bit for the database (in a nice way) - we might need to adjust this value
        for (int i = 1; i <= 3; i++){
        	log.info("*");
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e1){
            }
        }

        log.info("\n");
        log.info("------------------- TEST CALL -------------------");

        //make test-call to assistAPI to trigger service and read "more.extendedLog" from result if used
        //set default parameters
        String text = testSentence;
        String client = "java_sdk_v1.0.0";
        long time = System.currentTimeMillis();
        String timeLocal = DateTime.getFormattedDate("yyyy.MM.dd_HH:mm:ss");
        JSONObject userLocation = JSON.make("city", "Berlin", "latitude", "52.5186", "longitude", "13.4046");
        String context = "default";
        //build request
        String url = SdkConfig.assistAPI + "answer";
        String formData = ContentBuilder.postForm(
        		"GUUID", SdkConfig.userId, "PWD", SdkConfig.password,
        		"client", client, "lang", lang,
        		"text", text,
        		"time", String.valueOf(time),	"time_local", timeLocal,
        		"user_location", userLocation, "context", context
        );
        //call
        JSONObject result;
        try{
            //GET
            result = Connectors.httpFormPOST(url, formData);
            if (Connectors.httpSuccess(result)){
                //print
            	log.info(JSONWriter.getPrettyString(result));
            	log.info("\n");
                log.info("------------------- MY LOG ---------------------");
                log.info("Test sentence: " + testSentence);
                log.info("Assistant answer: " + JSON.getString(result, "answer"));
                //extra look at extendedLog
                if (result.containsKey("more")){
	                JSONArray devLog = (JSONArray) JSON.getJObject(result, "more").get("extendedLog");
	                if (devLog != null){
	                    for (Object e : devLog){
	                    	log.info("extendedLog - " + e);
	                    }
	                }
                }
            //error
            }else{
            	log.error("ERROR during HTTP REST call - " + Connectors.httpError(result));
            	return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
		return true;
	}
}
