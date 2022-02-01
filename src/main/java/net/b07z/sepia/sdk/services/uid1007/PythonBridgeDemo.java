package net.b07z.sepia.sdk.services.uid1007;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.WebApiParameter;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Demonstration of how to use SEPIA Python-Bridge to enhance NLU and services.<br>
 * <br>
 * To use this service please make sure your Python-Bridge server is running:<br>
 * https://github.com/SEPIA-Framework/sepia-python-bridge 
 * 
 * @author Florian Quirin
 *
 */
public class PythonBridgeDemo implements ServiceInterface {
	
	//Command name of your service (will be combined with userId to be unique, e.g. 'uid1007.python_bridge')
	private static final String CMD_NAME = "python_bridge";
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang) {
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Python Brücke testen.");
		//OTHER
		}else{
			samples.add("Test Python bridge.");
		}
		return samples;
	}
	
	//Basic service setup:
	
	//Overriding the 'getAnswersPool' methods enables you to define custom answers with more complex features.
	//You can build a pool of answers that can have multiple versions of the same answer used for different 
	//situations like a repeated question (what was the time? -> sorry say again, what was the time? -> ...).

	@Override
	public ServiceAnswers getAnswersPool(String language) {
		ServiceAnswers answerPool = new ServiceAnswers(language);
		
		//Build English answers
		if (language.equals(LANGUAGES.EN)){
			answerPool
				//simple method to add answers	
				.addAnswer(successAnswer, 0, "Test successful.")
				//complete method to add answers
				.addAnswer(new Answer(
						Language.from(language), okAnswer, 
						"Message received but I could not fulfill your request.", 
						Answer.Character.neutral, 0, 5
				))
				
				.addAnswer(askCodeWord, 0, "What's your code word?")
				.addAnswer(askCodeWord, 1, "Wrong, try again please. What's your code word?")
				.addAnswer(askCodeWord, 2, "Still wrong. Do you know the mines of Moria? Speak, 'friend', and enter.")
				;
			return answerPool;
		
		//Other languages not yet supported
		}else{
			answerPool
				.addAnswer(successAnswer, 0, "Test erfolgreich.")
				.addAnswer(okAnswer, 0, "Die Anfrage ist angekommen aber ich kann sie nicht bearbeiten.")
				
				.addAnswer(askCodeWord, 0, "Wie lautet das Schlüsselwort?")
				.addAnswer(askCodeWord, 1, "Falsch, versuch es noch einmal bitte. Wie lautet das Schlüsselwort?")
				.addAnswer(askCodeWord, 2, "Immer noch falsch. Kennst du die Minen von Moria? Sprich, 'Freund', und tritt ein.")
				;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers need to start with a certain prefix
	private static final String failAnswer = "error_0a";
	private static final String successAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_success_0a";
	private static final String okAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_still_ok_0a";
	private static final String askCodeWord = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_ask_code_1a";
	

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release
		//info.makePublic();
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Direct-match trigger sentences in different languages
		//NOTE: we use SEPIA internal NLU for direct match here and Python for more complex stuff to see how both work in parallel
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentence("Test Python bridge.", EN);
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentence("Python Brücke testen.", DE);
		
		//Regular expression triggers
		//NOTE: we don't use those here because we want to do complex NLU via the Python bridge
		//info.setCustomTriggerRegX(".*\\b(python bridge)\\b.*", EN);
		//info.setCustomTriggerRegXscoreBoost(5);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//This service has a one required parameter, the code word.
		//Required parameters will be asked automatically by SEPIA using the defined question.
		Parameter p1 = new Parameter(new CodeWord())
				.setRequired(true)
				.setQuestion(askCodeWord);
		info.addParameter(p1);
		
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module 
		//with serviceBuilder.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(failAnswer)
			.addOkayAnswer(okAnswer)
			.addCustomAnswer("askCodeWord", askCodeWord); 	//optional, just for info
		
		//Add answer parameters that are used to replace <1>, <2>, ... in your answers.
		//The name is arbitrary but you need to use the same one in getResult(...) later for api.resultInfoPut(...)
		info.addAnswerParameters("code"); 	//<1>=code, <2>=...
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()),
				getAnswersPool(nluResult.language));
		
		//get required parameters:
		
		//-code
		Parameter codeParameter = nluResult.getRequiredParameter(CodeWord.class.getName());
		String code = codeParameter.getValueAsString();
		
		//get optional parameters:
		//NONE
		
		//Set answer parameters as defined in getInfo():
		api.resultInfoPut("code", code);
		
		// ... here you could put some code that runs after successful code word.
		// wrong code will automatically lead to rejection before reaching this part ...
				
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
	
	//----------------- custom parameters -------------------
	
	/**
	 * Parameter handler that tries to extract code word via Python bridge.
	 */
	public static class CodeWord extends WebApiParameter {

		@Override
		public String getApiUrl(){
			//Enter the URL to your SEPIA Python-Bridge here including parameter path
			String nluBridgeUrl = "http://localhost:20731/nlu/";
			String parameterPath = "get_parameter/code_word";
			return nluBridgeUrl + parameterPath;
		}		
	}

}
