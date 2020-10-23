package net.b07z.sepia.sdk.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.services.uid1007.RestaurantDemo;
import net.b07z.sepia.sdk.substitutes.AnswerLoaderEmpty;
import net.b07z.sepia.server.assist.answers.AnswerLoaderFile;
import net.b07z.sepia.server.assist.answers.DefaultReplies;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluKeywordAnalyzer;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.AbstractInterview;
import net.b07z.sepia.server.assist.interviews.Interview;
import net.b07z.sepia.server.assist.interviews.InterviewInterface;
import net.b07z.sepia.server.assist.interviews.InterviewResult;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.parameters.ParameterConfig;
import net.b07z.sepia.server.assist.parameters.ParameterHandler;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.JSONWriter;

/**
 * Demonstrates how to test a service locally before using the upload function and offers methods to help with testing.
 * 
 * @author Florian Quirin
 *
 */
public class TestServiceLocally {
	
	private static final Logger log = LoggerFactory.getLogger(TestServiceLocally.class);

	/**
	 * Write your own tests here or use this as an example.
	 * @param args
	 */
	public static void main(String[] args) {
		
		//Test this service
		String text = null;		//'null' means 'load first from samples' 
		ServiceInterface service = new RestaurantDemo();
		String lang = LANGUAGES.EN;
		
		//Get a result using the regular-expressions defined inside custom service
		setup();
		NluResult result = testServiceWithRegExpTrigger(service, text, lang);
		
		//Print result
		log.info(JSONWriter.getPrettyString(result.getBestResultJSON()));
		
		//Build a parameter - result is added to given NluResult
		/*
		Interview interviewApi = new Interview(result);
		Parameter p1 = buildParameter(interviewApi, PARAMETERS.NUMBER);						//global name or ...
		Parameter p2 = buildParameter(interviewApi, PARAMETERS.TIME);
		Parameter p3 = buildParameter(interviewApi, new RestaurantDemo.ReservationName());	//... parameter handler
		
		//Print individual results
		log.info(JSONWriter.getPrettyString(p1.getData()));
		log.info(JSONWriter.getPrettyString(p2.getData()));
		log.info(JSONWriter.getPrettyString(p3.getData()));
		*/
		
		//Get service result from NLU result
		ServiceResult sr = getServiceResultFromNluResult(service, result);
		
		//Print result
		log.info(JSONWriter.getPrettyString(sr.getResultJSONObject()));
	}
	
	/**
	 * Setup some default components of SEPIA.
	 */
	public static void setup() {
		setup(false);
	}
	/**
	 * Setup some default components of SEPIA and include the answers-file loader.<br>
	 * NOTE: It will try to load answer files from "answers/" folder (may be useful if you want to include your custom files).
	 */
	public static void setup(boolean includeAnswersFromFile){
		//setup answers
		if (includeAnswersFromFile){
			Config.answersPath = "answers/";	//new folder to copy answer files to
			Config.setAnswerModule(new AnswerLoaderFile()); 	//choose txt-file answers-module
		}else{
			Config.setAnswerModule(new AnswerLoaderEmpty());
		}
		DefaultReplies.setupDefaults(); 	//setup default question mapping for parameters and stuff
		
		//setup commands and parameters
		InterviewServicesMap.load();		//services connected to interviews
		ParameterConfig.setup(); 			//connect parameter names to handlers and other stuff
		
		//reduce DB access for default test user (we are not running a DB in this test-mode)
		ConfigTestServer.reduceDatabaseAccess(ConfigTestServer.getFakeUserId(null));
	}
	
	/**
	 * Test a custom service that has a regular-expression trigger.
	 * @param service - service to test
	 * @param text - original input text (not normalized)
	 * @param languageCode - language code of text input (e.g. LANGUAGES.EN)
	 * @return
	 */
	public static NluResult testServiceWithRegExpTrigger(ServiceInterface service, String text, String languageCode) {
		if (text == null) {
			text = service.getSampleSentences(languageCode).first();
		}
		//Get normalized input
		String normText = normalize(text, languageCode);
		log.info("Input text: " + text);
		log.info("Norm. text: " + normText);
		
		//Get Fake input
		NluInput input = ConfigTestServer.getFakeInput(normText, languageCode);
		input.textRaw = text;

		//Use the regular-expression interpreter
		
		List<String> possibleCMDs = new ArrayList<>();			//make a list of possible interpretations of the text
		List<Map<String, String>> possibleParameters = new ArrayList<>();		//possible parameters
		List<Integer> possibleScore = new ArrayList<>();		//make scores to decide which one is correct command
		int index = -1;
		
		index = NluKeywordAnalyzer.abstractRegExAnalyzer(normText, input, service,
				possibleCMDs, possibleScore, possibleParameters, index);
		log.info("Possible commands (size): " + possibleCMDs.size());
		
		int bestScoreIndex = 0; 	//for the test we assume that index 0 is our goal
		log.info("Primary score: " + possibleScore.get(bestScoreIndex));
		
		NluResult result = new NluResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex);
		result.input = input; 		//TODO: is this enough for simple testing?
		
		return result;
	}
	
	/**
	 * Normalize text. Resembles server behavior.
	 */
	private static String normalize(String text, String languageCode) {
		Normalizer normalizer = Config.inputNormalizers.get(languageCode);
		String normText = normalizer.normalizeText(text);
		return normText;
	}

	/**
	 * Very basic method to execute parameter 'build' process.
	 * @param interview	- {@link Interview} object initialized with given {@NluResult}
	 * @param parameterName - name of a parameter, usually defined in {@link PARAMETERS}
	 */
	public static Parameter buildParameter(Interview interview, String parameterName){
		Parameter p = new Parameter(parameterName);
		return buildParameter(interview, p);
	}
	/**
	 * Very basic method to execute parameter 'build' process.
	 * @param interview	- {@link Interview} object initialized with given {@NluResult}
	 * @param parameterHandler - {@link ParameterHandler} e.g. your parameter from a custom service
	 */
	public static Parameter buildParameter(Interview interview, ParameterHandler parameterHandler){
		Parameter p = new Parameter(parameterHandler);
		return buildParameter(interview, p);
	}
	/**
	 * Very basic method to execute parameter 'build' process.
	 */
	private static Parameter buildParameter(Interview interview, Parameter p){
		interview.getParameterInput(p);
		interview.buildParameterOrComment(p, null);
		return interview.nluResult.getOptionalParameter(p.getName(), null);
	}
	
	/**
	 * Build a service result for a given {@link ServiceInterface} from previously created {@link NluResult}.<br>
	 * NOTE: Depending on how your answers-module is set up (default is empty) you will only get the answer-keys not the "real" answer. 
	 * @param service - your custom service, e.g. "new RestaurantDemo()"
	 * @param nluResult - previously created {@link NluResult}, e.g. via {@link #testServiceWithRegExpTrigger}
	 * @return
	 */
	public static ServiceResult getServiceResultFromNluResult(ServiceInterface service, NluResult nluResult){
		//ServiceResult answer;
		List<ServiceInterface> services = Arrays.asList(service);
		InterviewInterface interview = new AbstractInterview();
		interview.setCommand(nluResult.getCommand());
		interview.setServices(services);
		InterviewResult iResult = interview.getMissingParameters(nluResult);
		if (iResult.isComplete()){
			return interview.getServiceResults(iResult);
		}else{
			return iResult.getApiComment();
		}
	}
}
