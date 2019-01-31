package net.b07z.sepia.sdk.connect;

import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.main.SdkConfig;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.ContentBuilder;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

/**
 * Communicate with SEPIA Assist-API.
 * 
 * @author Florian Quirin
 *
 */
public class AssistApi {
	
	private static final Logger log = LoggerFactory.getLogger(AssistApi.class);
	
	/**
	 * Make a test call to server to test service answer.<br>
	 * NOTE: You probably want to load SDK settings in advance: {@link SdkConfig#loadSettings}.
	 * @param language - language code for testing
	 * @param testSentence - test sentence
	 * @return true/false if call was successful
	 */
	public static boolean callAnswerEndpoint(Language language, String testSentence){
		log.info("------------------- TEST CALL -------------------");
        
        //Get test sentence
        if (Is.nullOrEmpty(testSentence)){
        	throw new RuntimeException("Test aborted: No test sentences defined!");
        }

        //make test-call to assistAPI to trigger service and read "more.extendedLog" from result if used
        //set default parameters
        String text = testSentence;
        String lang = language.toValue();
        String client = "java_sdk_v1.0.0";
        long time = System.currentTimeMillis();
        String timeLocal = DateTime.getFormattedDate("yyyy.MM.dd_HH:mm:ss");
        JSONObject userLocation = JSON.make("city", "Berlin", "latitude", "52.5186", "longitude", "13.4046");
        String context = "default";
        //build request
        String url = SdkConfig.assistAPI + "answer";
        String formData = ContentBuilder.postForm(
        		"GUUID", SdkConfig.userId, 
        		"PWD", SdkConfig.password,
        		"client", client, 
        		"lang", lang,
        		"text", text,
        		"time", String.valueOf(time),	
        		"time_local", timeLocal,
        		"user_location", userLocation, 
        		"context", context
        );
        //call
        JSONObject result;
        try{
            //GET
            result = Connectors.httpFormPOST(url, formData);
            if (Connectors.httpSuccess(result)){
                //print
            	log.info(JSONWriter.getPrettyString(result));
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
	/**
	 * Make a test call to server to test service answer.<br>
	 * NOTE: You probably want to load SDK settings in advance: {@link SdkConfig#loadSettings}.
	 * @param service - Service to test
	 * @param language - language code for testing
	 * @param testSentence - test sentence or null (to load sentence from service)
	 * @return true/false if call was successful
	 */
	public static boolean callAnswerEndpoint(ServiceInterface service, Language language, String testSentence){
        //Get test sentence
        Set<String> testSentences = service.getSampleSentences(language.toValue());
        if (Is.notNullOrEmpty(testSentences)){
        	testSentence = testSentences.iterator().next();
        }
        //Call
        return callAnswerEndpoint(language, testSentence);
	}

}
