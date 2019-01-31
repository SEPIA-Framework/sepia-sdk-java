package net.b07z.sepia.sdk.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.services.uid1007.RestaurantDemo;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluKeywordAnalyzer;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.parameters.ParameterConfig;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.tools.JSONWriter;

/**
 * Demonstrates how to test a service locally before using the upload function.
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
		
		//Run
		setup();
		NluResult result = testServiceWithRegExpTrigger(service, text, lang);
		
		//Print result
		log.info(JSONWriter.getPrettyString(result.getBestResultJSON()));
	}
	
	/**
	 * Setup some components of SEPIA.
	 */
	public static void setup() {
		//setup answers
		//Config.setAnswerModule(new AnswerLoaderFile()); 	//choose txt-file answers-module
		//DefaultReplies.setupDefaults(); 	//setup default question mapping for parameters and stuff
		
		//setup commands and parameters
		InterviewServicesMap.load();		//services connected to interviews
		ParameterConfig.setup(); 			//connect parameter names to handlers and other stuff
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

}
