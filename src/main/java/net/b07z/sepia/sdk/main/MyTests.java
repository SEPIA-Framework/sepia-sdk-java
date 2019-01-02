package net.b07z.sepia.sdk.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.connect.UploadService;
import net.b07z.sepia.sdk.services.uid1007.HelloWorld;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Language;

public class MyTests {

	private static final Logger log = LoggerFactory.getLogger(MyTests.class);
			
	public static void main(String[] args) {
		
		//Define class, language and sentence (optional)
        ServiceInterface service = new HelloWorld();
        Language language = Language.DE;
        String testSentence = null; 	//we use the ones included in the service
        
        //Upload (and test)
        log.info("\n");
        log.info("Testing service 1:\n");
        UploadService.transfer(service, language, testSentence);
	}

}
