package net.b07z.sepia.sdk.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.connect.UploadService;
import net.b07z.sepia.sdk.services.uid1007.RestaurantDemo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Language;

public class MyTests {

	private static final Logger log = LoggerFactory.getLogger(MyTests.class);
			
	public static void main(String[] args) {
		
		//Define class, language and sentence (optional)
        ServiceInterface service = new RestaurantDemo();
        Language language = Language.EN;
        String testSentence = "reserve a table for 2"; 	//we use the ones included in the service
        
        //Upload (and test)
        log.info("");
        log.info("Testing service '" + service.getClass().getName() + "': ");
        log.info("");
        UploadService.transferJavaFile(service, language, testSentence);
	}

}
