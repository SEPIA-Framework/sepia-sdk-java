package net.b07z.sepia.sdk.connect;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import net.b07z.sepia.sdk.main.SdkConfig;
import net.b07z.sepia.sdk.tools.HttpTools;
import net.b07z.sepia.server.assist.endpoints.SdkEndpoint;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.ContentBuilder;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

import org.apache.http.ParseException;
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
	 * Transfer service as code string to server and make test-call to API.
	 * @param canonicalClassName - Name of class after upload (as given in source-code: package+className)
	 * @param sourceCode - Service to transfer as source-code string
	 * @param language - ISO language code
	 * @param testSentence - sentence to test service
	 * @return
	 */
	public static boolean transferCode(String canonicalClassName, String sourceCode, Language language, String testSentence){
        //Load credentials and API URLs from configuration file
        SdkConfig.loadSettings(SdkConfig.configFile);

        //UPLOAD SERVICE
        try {
        	uploadSourceCodeToServiceEndpoint(canonicalClassName, sourceCode);
        }catch (Exception e){
        	log.error("------------------ TERMINATED ------------------");
            e.printStackTrace();
            throw new RuntimeException("Upload failed! Error: " + e.getMessage(), e);
        }

        //TEST CALL
        boolean testSuccess = testCall(language, testSentence);
        log.info("------------------------------------------------");
        log.info("*DONE*");
		return testSuccess;
	}
	/**
	 * Transfer service as java-file to server and make test-call to API.
	 * @param service - Service to transfer. Has to be inside your services.[userId] package.
	 * @param language - ISO language code
	 * @param testSentence - can be null if service has defined test sentences
	 * @return
	 */
	public static boolean transferJavaFile(ServiceInterface service, Language language, String testSentence){
		//Upload this service:
        String serviceClass = service.getClass().getSimpleName();
        
        //Load credentials and API URLs from configuration file
        SdkConfig.loadSettings(SdkConfig.configFile);

        //Get the .java file from your src folder (usually: target/...)
        log.info("------------------- GET JAVA FILE ---------------------");
        File serviceFile = new File("src/main/java/" + SdkConfig.servicesPath + SdkConfig.userId + "/" + serviceClass + ".java");
        if (!serviceFile.exists())
            throw new RuntimeException("Class-file not found: " + serviceFile.getAbsolutePath());
        else {
        	log.info("Loaded file: " + serviceFile.getAbsolutePath());
        }

        //UPLOAD SERVICE
        try {
        	uploadFileToServiceEndpoint(serviceFile);
        }catch (Exception e){
        	log.error("------------------ TERMINATED ------------------");
            e.printStackTrace();
            throw new RuntimeException("Upload failed! Error: " + e.getMessage(), e);
        }

        //TEST CALL
        boolean testSuccess = testCall(service, language, testSentence);
        log.info("------------------------------------------------");
        log.info("*DONE*");
		return testSuccess;
	}
	/**
	 * Transfer service as class-file to server and make test-call to API.<br>
	 * NOTE: This only works when the service has no inner classes. If it does use {@link #transferJavaFile}.
	 * @param service - Service to transfer. Has to be inside your services.[userId] package.
	 * @param language - ISO language code
	 * @param testSentence - can be null if service has defined test sentences
	 * @return
	 */
	public static boolean transferClassFile(ServiceInterface service, Language language, String testSentence){
		//Upload this service:
        String serviceClass = service.getClass().getSimpleName();

        //Load credentials and API URLs from configuration file
        SdkConfig.loadSettings(SdkConfig.configFile);

        //Get the .class file from your folder (usually: target/...)
        log.info("------------------- GET CLASS ---------------------");
        File serviceFile = new File("target/classes/" + SdkConfig.servicesPath + SdkConfig.userId + "/" + serviceClass + ".class");
        if (!serviceFile.exists())
            throw new RuntimeException("Class-file not found: " + serviceFile.getAbsolutePath());
        else {
        	log.info("Loaded file: " + serviceFile.getAbsolutePath());
        }

        //UPLOAD SERVICE
        try {
        	uploadFileToServiceEndpoint(serviceFile);

        }catch (Exception e){
        	log.error("------------------ TERMINATED ------------------");
            e.printStackTrace();
            throw new RuntimeException("Upload failed! Error: " + e.getMessage(), e);
        }

        //TEST CALL
        boolean testSuccess = testCall(service, language, testSentence);
        log.info("------------------------------------------------");
        log.info("*DONE*");
		return testSuccess;
	}
	
	/**
	 * Upload a service file to the server's upload endpoint.<br>
	 * NOTE: You probably want to load SDK settings in advance: {@link SdkConfig#loadSettings}.
	 * @return
	 * @throws IOException 
	 * @throws ParseException
	 * @throws RuntimeException
	 */
	public static void uploadFileToServiceEndpoint(File serviceFile) throws ParseException, IOException{
		//upload service
        if (Is.nullOrEmpty(SdkConfig.userId) || Is.nullOrEmpty(SdkConfig.password)){
            throw new RuntimeException("Please check your credentials in: " + SdkConfig.configFile);
        }
    	log.info("------------------- UPLOADING -------------------");
    	
        HashMap<String, String> parameters = new HashMap<>();
        //credentials
        parameters.put("GUUID", SdkConfig.userId);
        parameters.put("PWD", SdkConfig.password);
        
        String URL = SdkConfig.assistAPI + "upload-service";
        log.info("Uploading file to: " + URL);
        boolean success = HttpTools.httpPostFileAndDebug(URL, SdkEndpoint.UPLOAD_FILE_KEY, serviceFile, parameters);
        if (!success){
            throw new RuntimeException("HTTP POST failed! Error: see comments");
        }else{
        	//Wait for server-database refresh (storing custom sentences)
            waitForServerDatabaseRefresh();
        }
	}
	/**
	 * Upload service source-code to the server's upload endpoint.<br>
	 * NOTE: You probably want to load SDK settings in advance: {@link SdkConfig#loadSettings}.
	 * @param canonicalClassName - Name of class after upload (as given in source-code: package+className)
	 * @param sourceCode - Service to transfer as source-code string
	 * @return
	 * @throws IOException 
	 * @throws ParseException
	 * @throws RuntimeException
	 */
	public static void uploadSourceCodeToServiceEndpoint(String canonicalClassName, String sourceCode) throws ParseException, IOException{
		//upload service
        if (Is.nullOrEmpty(SdkConfig.userId) || Is.nullOrEmpty(SdkConfig.password)){
            throw new RuntimeException("Please check your credentials in: " + SdkConfig.configFile);
        }
    	log.info("------------------- UPLOADING -------------------");
        
    	HashMap<String, String> parameters = new HashMap<>();
    	//credentials
        parameters.put("GUUID", SdkConfig.userId);
        parameters.put("PWD", SdkConfig.password);
        //class meta info
        parameters.put(SdkEndpoint.UPLOAD_CODE_CLASS_NAME, canonicalClassName);
        
        String URL = SdkConfig.assistAPI + "upload-service";
        log.info("Uploading source-code to: " + URL);
        boolean success = HttpTools.httpPostTextAndDebug(URL, SdkEndpoint.UPLOAD_CODE_KEY, sourceCode, parameters);
        if (!success){
            throw new RuntimeException("HTTP POST failed! Error: see comments");
        }else{
        	//Wait for server-database refresh (storing custom sentences)
            waitForServerDatabaseRefresh();
        }
	}
	private static void waitForServerDatabaseRefresh(){
		log.info("------------- WAITING FOR DATABASE REFRESH --------------");
        //wait a bit for the database (in a nice way) - we might need to adjust this value but 1s usually is enough already
        for (int i = 1; i <= 3; i++){
        	log.info("*");
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e1){
            }
        }
	}
	
	/**
	 * Make a test call to server to test custom service answer.<br>
	 * NOTE: You probably want to load SDK settings in advance: {@link SdkConfig#loadSettings}.
	 * @param language - language code for testing
	 * @param testSentence - test sentence
	 * @return true/false if call was successful
	 */
	public static boolean testCall(Language language, String testSentence){
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
	 * Make a test call to server to test custom service answer.<br>
	 * NOTE: You probably want to load SDK settings in advance: {@link SdkConfig#loadSettings}.
	 * @param service - Service to test
	 * @param language - language code for testing
	 * @param testSentence - test sentence or null (to load sentence from service)
	 * @return true/false if call was successful
	 */
	public static boolean testCall(ServiceInterface service, Language language, String testSentence){
        //Get test sentence
        Set<String> testSentences = service.getSampleSentences(language.toValue());
        if (Is.notNullOrEmpty(testSentences)){
        	testSentence = testSentences.iterator().next();
        }
        //Call
        return testCall(language, testSentence);
	}
}
