package net.b07z.sepia.sdk.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.connect.UploadService;
import net.b07z.sepia.sdk.services.uid1007.HelloWorld;
import net.b07z.sepia.sdk.services.uid1007.SandboxTest;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Language;

public class TestViaUpload {
	
	private static final Logger log = LoggerFactory.getLogger(TestViaUpload.class);

	/**
	 * Run transfer to server and make test-call to API.
	 * @param args
	 */
    public static void main(String[] args) {

        //Define class, language and sentence (optional)
        ServiceInterface service = new HelloWorld();
        Language language = Language.DE;
        String testSentence = null; 	//we use the ones included in the service
        
        //Upload (and test)
        log.info("");
        log.info("Testing service 1:");
        log.info("");
        //UploadService.transferClassFile(service, language, testSentence);
        UploadService.transferJavaFile(service, language, testSentence);
        
        //2nd test
        log.info("");
        log.info("Testing service 2:");
        log.info("");
        service = new SandboxTest();
        //UploadService.transferClassFile(service, language, testSentence);
        UploadService.transferJavaFile(service, language, testSentence);
    }
}
