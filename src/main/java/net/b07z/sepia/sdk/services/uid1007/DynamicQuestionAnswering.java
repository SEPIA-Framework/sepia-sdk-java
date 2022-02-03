package net.b07z.sepia.sdk.services.uid1007;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.GenericEmptyParameter;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Demonstration of a dynamic service with 3 steps: trigger activation - question input - answer or fail.<br>
 * Questions will be generated dynamically and not with predefined parameters.
 * 
 * @author Florian Quirin
 *
 */
public class DynamicQuestionAnswering implements ServiceInterface {
	
	//Command name of your service (will be combined with userId to be unique, e.g. 'uid1007.question_answer')
	private static final String CMD_NAME = "question_answer";
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang) {
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Ich habe eine spezielle Frage.");
		//OTHER
		}else{
			samples.add("I have a special question.");
		}
		return samples;
	}
	
	//Basic service setup:
	
	//Overriding the 'getAnswersPool' methods enables you to define custom answers with more complex features..

	@Override
	public ServiceAnswers getAnswersPool(String language) {
		ServiceAnswers answerPool = new ServiceAnswers(language);
		
		//Build English answers
		if (language.equals(LANGUAGES.EN)){
			answerPool
				//the default answer
				.addAnswer(successAnswer, 0, "Here is what I found: <1>")
				//ask what the users wants to know				
				.addAnswer(askForInput, 0, "What do you want to know?")
				.addAnswer(askForInput, 1, "Say again please.")		//used on 2nd consecutive try...
				//found no answer
				.addAnswer(noAnswerFound, 0, "Sorry but it seems I have no answer to that yet.")
			;
			return answerPool;
		
		//Other languages not yet supported
		}else{
			answerPool
				//the default answer
				.addAnswer(successAnswer, 0, "Das habe ich gefunden: <1>")
				//ask what the users wants to know				
				.addAnswer(askForInput, 0, "Was würdest du gerne wissen?")
				.addAnswer(askForInput, 1, "Sag noch mal bitte.")
				//found no answer
				.addAnswer(noAnswerFound, 0, "Sorry, aber das kann ich wohl noch nicht beantworten.")
			;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers need to start with a certain prefix
	private static final String failAnswer = "error_0a";
	private static final String successAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_success_0a";
	private static final String noAnswerFound = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_no_answer_0a";
	private static final String askForInput = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_ask_input_0a";
	

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release
		//info.makePublic();
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Direct-match trigger sentences in different languages
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentence("I have a special question.", EN);
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentence("Ich habe eine spezielle Frage.", DE);
		
		//Add some regular expression triggers if you like ...
		//info.setCustomTriggerRegX(".*\\b(a special question)\\b.*", EN);
		//info.setCustomTriggerRegXscoreBoost(4);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//This service has no fixed parameter. The question is instead generated dynamically inside the result handler.
		//We still need to define the custom parameter as optional though to extract it properly later:
		Parameter p1 = new Parameter(new SpecialQuestion());
		info.addParameter(p1);
		
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module 
		//with serviceBuilder.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(failAnswer)
			.addOkayAnswer(noAnswerFound)
			.addCustomAnswer("askForInput", askForInput);
		
		//Add answer parameters that are used to replace <1>, <2>, ... in your answers.
		//The name is arbitrary but you need to use the same one in getResult(...) later for api.resultInfoPut(...)
		info.addAnswerParameters("answer"); 	//<1>=answer text
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()),
				getAnswersPool(nluResult.language));
		
		//we haven't defined anything specific but we will ask for our custom parameter and the result will be stored here in the 2nd stage of the dialog
		Parameter questionParameter = nluResult.getOptionalParameter(SpecialQuestion.class.getName(), "");
		String qNormalized = questionParameter.getValueAsString();
		String qRaw = (String) questionParameter.getDataFieldOrDefault(InterviewData.INPUT_RAW);	//raw input (before normalization)
		//String q = nluResult.getParameter(PARAMETERS.ANY_ANSWER);		//alternative way for "ad-hoc" parameters (not recommended)
		
		//in the 1st stage of the dialog our input will be empty because no parameter was defined, so we need to ask manually:
		if (Is.nullOrEmpty(qNormalized)){
			//ask and ...
			api.setIncompleteAndAsk(SpecialQuestion.class.getName(), askForInput);
			//api.setIncompleteAndAsk(PARAMETERS.ANY_ANSWER, askForInput);		//define a parameter "ad-hoc" (never registered in 'getInfo')
			//.. return
			ServiceResult result = api.buildResult();
			return result;
			
		}else{
			//in the 2nd stage this will be the user "answer"
			
			//we could check the parameter here and still reject it (e.g. asking again)
			//if (qNormalized.equalsIgnoreCase("...")){ ... }
			
			//generate answer:
			
			//TODO: Here you can add your scripts to answer the question. Write your own logic or maybe use a web API?
			String questionResponse = "";
			boolean answeredSuccessfully = false;
			//Example HTTP call e.g. to a web API, your PythonBridge or any other micro-service with HTTP interface:
			/*
			try{
				JSONObject response = Connectors.apacheHttpGETjson("http://localhost:20731/my-service/");
				if (response != null && response.containsKey("answer")){
					questionResponse = JSON.getString(response, "answer");
					answeredSuccessfully = true;
				}
			}catch (Exception e){
				e.printStackTrace();
			}
			*/
			//JUST FOR TESTING: return the question:
			questionResponse = qRaw;
			answeredSuccessfully = Is.notNullOrEmpty(questionResponse);		//implement your own checks depending on how you generate answers!
						
			//success or no answer?
			if (!answeredSuccessfully){
				//no error but also no answer
				api.setStatusOkay();
			}else{
				//Set answer parameters as defined in getInfo():
				api.resultInfoPut("answer", questionResponse);
				
				//all good
				api.setStatusSuccess();
			}
			
			//build the API_Result
			ServiceResult result = api.buildResult();
			return result;
		}
	}
	
	//----------------- custom parameters -------------------
	
	/**
	 * Parameter handler that is just a placeholder for our custom question.
	 * It will contain the full input, normalized and raw if used in a response.
	 */
	public static class SpecialQuestion extends GenericEmptyParameter {}
}
