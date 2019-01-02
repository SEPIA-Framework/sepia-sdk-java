package net.b07z.sepia.sdk.services.uid1007;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * "Hello World" custom service that just returns a "Hello" answer and a button that links to the SDK.
 * 
 * @author Florian Quirin
 *
 */
public class HelloWorld implements ServiceInterface{
	
	//Command name
	private static final String CMD_NAME = "hello_world";				//Name tag of your service (will be combined with userId to be unique)
	//Answers used:
	private static final String successAnswer = "chat_hello_0a"; 		//successful answer
	private static final String okAnswer = "default_not_possible_0a";	//service ran properly but can't generate desired result (e.g. a search term was not found)
	private static final String failAnswer = "error_0a";				//fallback if service or some part of it crashed
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Hallo Welt!");
			
		//OTHER
		}else{
			samples.add("Hello world!");
		}
		return samples;
	}
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release
		//info.makePublic();
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Direct-match trigger sentences in different languages:
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentence("Hallo Welt!", DE)
			.addCustomTriggerSentence("Hello world!", DE)
			.addCustomTriggerSentence("Teste meinen hallo Welt service.", DE);
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentence("Hello world!", EN)
			.addCustomTriggerSentence("Test my hello world service.", EN);
		
		//Parameters:
		//This service has no parameters
		
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(failAnswer)
			.addOkayAnswer(okAnswer);
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, getInfo(nluResult.language));
		
		//get required parameters
		//NONE
		//get optional parameters
		//NONE
		
		//This service basically cannot fail ... ;-)
		
		//Just for demo purposes we add a button-action with a link to the SDK
		api.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
		api.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-sdk-java");
		api.putActionInfo("title", "SDK info");
		
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}

}
