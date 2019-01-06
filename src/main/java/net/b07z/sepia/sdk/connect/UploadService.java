package net.b07z.sepia.sdk.connect;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import net.b07z.sepia.sdk.main.SdkConfig;
import net.b07z.sepia.sdk.tools.HttpTools;
import net.b07z.sepia.server.assist.endpoints.SdkEndpoint;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.Is;
import org.apache.http.ParseException;
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
	 * Transfer service as source-code string to server.
	 * @param simpleClassName - Simple name of class (package will be determined by user-ID during compilation)
	 * @param sourceCode - Service to transfer as source-code string (if null try to load code from file)
	 * @return
	 */
	public static boolean transferCode(String simpleClassName, String sourceCode){
        //Load credentials and API URLs from configuration file
        SdkConfig.loadSettings(SdkConfig.configFile);

        //UPLOAD SERVICE
        try {
        	//Try to get source code?
            if (sourceCode == null){
            	sourceCode = getCustomServiceSourceCode(simpleClassName, SdkConfig.userId, "\n");
            }
            //Upload
        	uploadSourceCodeToServiceEndpoint(simpleClassName, sourceCode);
        	
        }catch (Exception e){
        	log.error("------------------ TERMINATED ------------------");
            e.printStackTrace();
            throw new RuntimeException("Upload failed! Error: " + e.getMessage(), e);
        }
        log.info("------------------------------------------------");
		return true;
	}
	/**
	 * Read service Java file and return source-code as String with custom line-break character.
	 * @param simpleClassName - simple name of class to load
	 * @param userId - user ID used for package path
	 * @param lineBreakChar - e.g. "\n" or System.lineSeparator()
	 * @return
	 * @throws IOException
	 */
	public static String getCustomServiceSourceCode(String simpleClassName, String userId, String lineBreakChar) throws IOException{
		File serviceFile = new File("src/main/java/" + SdkConfig.servicesPath + userId + "/" + simpleClassName + ".java");
		return String.join(lineBreakChar, FilesAndStreams.readFileAsList(serviceFile.getPath()));
	}
	/**
	 * Transfer service as java-file to server.
	 * @param service - Service to transfer. Has to be inside your services.[userId] package.
	 * @return
	 */
	public static boolean transferJavaFile(ServiceInterface service){
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
        log.info("------------------------------------------------");
		return true;
	}
	/**
	 * Transfer service as class-file to server.<br>
	 * NOTE: This only works when the service has no inner classes. If it does use {@link #transferJavaFile}.
	 * @param service - Service to transfer. Has to be inside your services.[userId] package.
	 * @return
	 */
	public static boolean transferClassFile(ServiceInterface service){
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
        log.info("------------------------------------------------");
		return true;
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
	 * @param simpleClassName - Simple name of class (package will be determined by user-ID during compilation)
	 * @param sourceCode - Service to transfer as source-code string
	 * @return
	 * @throws IOException 
	 * @throws ParseException
	 * @throws RuntimeException
	 */
	public static void uploadSourceCodeToServiceEndpoint(String simpleClassName, String sourceCode) throws ParseException, IOException{
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
        parameters.put(SdkEndpoint.UPLOAD_CODE_CLASS_NAME, simpleClassName);
        
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
}
