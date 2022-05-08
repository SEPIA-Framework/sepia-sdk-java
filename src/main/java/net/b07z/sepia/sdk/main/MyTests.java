package net.b07z.sepia.sdk.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.connect.AssistApi;
import net.b07z.sepia.sdk.connect.UploadService;
import net.b07z.sepia.sdk.services.uid1007.DynamicQuestionAnswering;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Language;

/**
 * Playground to write your own tests.
 */
public class MyTests {

	private static final Logger log = LoggerFactory.getLogger(MyTests.class);
			
	public static void main(String[] args) {
		
		//Define class, language and sentence (optional)
        ServiceInterface service = new DynamicQuestionAnswering(); //new RoboCleaner(); 	//new CoronaDataEcdc(); 
        	//new MqttDemo(); //new PythonBridgeDemo(); //new RestaurantDemo(); new WorkoutHelperDemo();
        Language language = Language.EN;
        String testSentence = null; //"reserve a table for 2"; 	//we use the ones included in the service
        
        //Upload
        log.info("");
        log.info("Testing service '" + service.getClass().getName() + "': ");
        log.info("");
        
        //load file
        //UploadService.transferJavaFile(service);
        
        //load source code
        String serviceClass = service.getClass().getSimpleName();
        UploadService.transferCode(serviceClass, null);
        
        //Test
        AssistApi.callAnswerEndpoint(service, language, testSentence);
	}

}
