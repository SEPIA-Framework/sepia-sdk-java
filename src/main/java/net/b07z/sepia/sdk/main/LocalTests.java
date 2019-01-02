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

public class LocalTests {
	
	private static final Logger log = LoggerFactory.getLogger(LocalTests.class);

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
	
	private static void setup() {
		//setup answers
		//Config.setAnswerModule(new AnswerLoaderFile()); 	//choose txt-file answers-module
		//DefaultReplies.setupDefaults(); 	//setup default question mapping for parameters and stuff
		
		//setup commands and parameters
		InterviewServicesMap.load();		//services connected to interviews
		ParameterConfig.setup(); 			//connect parameter names to handlers and other stuff
	}
	
	private static String normalize(String text, String languageCode) {
		Normalizer normalizer = Config.inputNormalizers.get(languageCode);
		String normText = normalizer.normalizeText(text);
		return normText;
	}
	
	public static NluResult testServiceWithRegExpTrigger(ServiceInterface service, String text, String languageCode) {
		if (text == null) {
			text = service.getSampleSentences(languageCode).first();
		}
		
		//Get Fake input
		NluInput input = ConfigTestServer.getFakeInput(text, languageCode);
		log.info("Input Text: " + text);
		
		//Get normalized input
		String normText = normalize(text, languageCode);
		log.info("Norm. Text: " + normText);
		
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

}
