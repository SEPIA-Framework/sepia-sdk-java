package net.b07z.sepia.sdk.services.uid1007;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.RuntimeInterface;
import net.b07z.sepia.server.core.tools.RuntimeInterface.RuntimeResult;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Service to test the security sandbox of custom services.
 * 
 * @author Florian Quirin
 *
 */
public class SandboxTest implements ServiceInterface{

	//Command name
	private static final String CMD_NAME = "sand_box_test";
	//Answers used:
	private static final String successAnswer = "<direct>Sandbox test was successful.";
	private static final String errorAnswer = "<direct>It seems the sandbox test crashed with an internal error.";
	private static final String okAnswer = "<direct>Sandbox test result unclear.";
	private static final String testFailedAnswer = "<direct>Sandbox test failed!";
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		samples.add("Sandbox test");
		return samples;
	}
	
	@Override
	public ServiceInfo getInfo(String language) {
		//type
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Triggers
		info.addCustomTriggerSentence("Sandbox test", Language.DE.toValue());
		info.addCustomTriggerSentence("Sandbox test", Language.EN.toValue());
		
		//Answers (these are the default answers, you can add a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(errorAnswer)
			.addOkayAnswer(okAnswer);
		info.addCustomAnswer("testFailedAnswer", testFailedAnswer);
		
		//------ GET_INFO SANDBOX TEST ----
		Debugger.println("--------- GET_INFO SANDBOX SECURITY TEST ---------", 3);
		testSystemExit();
		Debugger.println("--------------------------------------------------", 3);
		//----------
		
		return info;
	}
	@Override
	public ServiceResult getResult(NluResult NLU_result) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result, getInfo(""));
		
		//------ GET_RESULT SANDBOX TEST ----
		Debugger.println("--------- GET_RESULT SANDBOX SECURITY TEST ---------", 3);
		int testSuccesses = 0;
		int expectedSuccesses = 5;
		if (testFileAccess()) 	testSuccesses++;
		if (testHttpRequest())	testSuccesses++;
		if (testConfigAccess()) testSuccesses++;
		if (testSystemExit()) 	testSuccesses++;
		if (testRuntimeCommands())	testSuccesses++;
		Debugger.println("----------------------------------------------------", 3);
		
		boolean allGood = (testSuccesses == expectedSuccesses);
		//----------
		
		//debug in client - note: the 'Debugger' class (see below) will only write to server log, use this with SDK
		api.addToExtendedLog("Sandbox tested with result: " + testSuccesses + " of " + expectedSuccesses + " are good!");

		//all clear?
		if (allGood){
			api.setStatusSuccess();
		}else{
			api.setStatusOkay(); 		//'okay' in a sense that the service did not crash ;-)
			api.setCustomAnswer(testFailedAnswer);
		}
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
	
	//---- TESTS ----
	
	//Supposed to work:
	
	private boolean testHttpRequest(){
		try{
			String getRes = Connectors.simpleHtmlGet("https://api.github.com/");
			Debugger.println("Http GET worked as planned: " + getRes.substring(0, 5) + "...", 3);
			return true;
		}catch(Exception e){
			Debugger.println("Http GET failed with: " + e.getMessage(), 1);
			return false;
		}
	}
	
	//Supposed to fail:
	
	private boolean testSystemExit(){
		try{
			System.exit(-1);
			return false; 		//this will never happen ^^
		}catch(Exception e){
			Debugger.println("System.exit failed as planned with: " + e.getMessage(), 3);
			return true;
		}
	}
	
	private boolean testFileAccess(){
		try{
			List<File> files = FilesAndStreams.directoryToFileList("Xtensions/", new  ArrayList<File>(), true);
			Debugger.println("FILES SUCCESS: ", 3);
			Debugger.println("File access DID NOT fail as planned!", 1);
			for (File f : files){
				Debugger.println("Read file: " + f.getAbsolutePath(), 1);
			}
			return false;
		}catch(Exception e){
			Debugger.println("File access failed as planned with: " + e.getMessage(), 3);
			return true;
		}
	}
	
	private boolean testConfigAccess(){
		try{
			Debugger.println("Config access DID NOT FAIL, exposed info: " + Config.getUniversalUser(), 1);
			return false;
		}catch(Exception e){
			Debugger.println("Config access failed as planned with: " + e.getMessage(), 3);
			return true;
		}
	}
	
	private boolean testRuntimeCommands(){
		try{
			RuntimeResult rtr = RuntimeInterface.runCommand(new String[]{"echo", "Hello World!"}, 5000);
			if (rtr.getStatusCode() == 1 && rtr.getException().getClass().equals(java.security.AccessControlException.class)){
				Debugger.println("Runtime access failed as planned with: " + rtr.getException().getMessage(), 3);
				return true;
			}else{
				Debugger.println("Runtime access DID NOT FAIL! Runtime status-code: " + rtr.getStatusCode(), 1);
				Debugger.println("Runtime access DID NOT FAIL! Runtime output: " + rtr.getOutput(), 1);
				Debugger.println("Runtime access DID NOT FAIL! Runtime exception: " + rtr.getException(), 1);
				return false;
			}
		}catch(Exception e){
			Debugger.println("Runtime access failed as planned with: " + e.getMessage(), 3);
			return true;
		}
	}

}
