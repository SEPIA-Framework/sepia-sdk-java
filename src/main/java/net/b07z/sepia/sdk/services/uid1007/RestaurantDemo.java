package net.b07z.sepia.sdk.services.uid1007;

import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.CustomParameter;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Demo for a restaurant reservation service.
 * 
 * @author Florian Quirin
 *
 */
public class RestaurantDemo implements ServiceInterface{
	
	//Command name
	private static final String CMD_NAME = "restaurant_reservation";	//Name tag of your service (will be combined with userId to be unique)
	
	//Answers and questions used
	//NOTE: Since we only support English in this service and it's a demo we use <direct> sentences instead of links to the database.
	//		This is okay for development but has some disadvantages like there will be no variation on questions after a repeat ("sorry?" -> "sorry once more plz?" ...).
	private static final String successAnswer = "<direct>Ok, I've reserved a table at <1> for <2> people on the name <3>. Thank you and have a great meal.";
	private static final String okAnswer = "<direct>Sorry I could not reserve a table. Please try again later.";
	private static final String failAnswer = "error_0a";
	private static final String askTimeAndDate = "<direct>When would you like to visit Luigis?";
	private static final String askNumberOfPersons = "<direct>How many people will come?";
	private static final String askReservationName = "<direct>On what name should I reserve the table?";
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Ich würde gerne einen Tisch bei Luigis reservieren.");
		//OTHER
		}else{
			samples.add("I'd like to reserve a table at Luigis for tomorrow at 6 p.m. for 2 persons on the name SEPIA.");
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
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentence("I'd like to reserve a table.", EN);
		/* we will only support English in this demo ...
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentence("Ich würde gerne einen Tisch reservieren.", DE);
		*/
		
		//Regular expression triggers:
		info.setCustomTriggerRegX(".*\\b((reserve|book) a table)\\b.*", EN);
		//info.setCustomTriggerRegXscoreBoost(2);		//boost service to make it hard to overrule
		
		//Parameters:
		
		//This service has a number of required parameters (without them we can't complete the reservation).
		//Required parameters will be asked automatically by SEPIA using the defined question.
		
		//The first 2 are already available by default:
		Parameter p1 = new Parameter(PARAMETERS.TIME)
				.setRequired(true)
				.setQuestion(askTimeAndDate); 				//NOTE: if you are looking for TIME and NUMBER use this parameter first to exclude dates in NUMBER
		Parameter p2 = new Parameter(PARAMETERS.NUMBER)
				.setRequired(true)
				.setQuestion(askNumberOfPersons);
		
		//The 3rd is a custom parameter made for this service:
		Parameter p3 = new Parameter(new ReservationName())
				.setRequired(true)
				.setQuestion(askReservationName);
		
		info.addParameter(p1).addParameter(p2).addParameter(p3);
		
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
				
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
	
	//----------------- custom parameters -------------------
	
	/**
	 * Parameter handler that tries to extract a reservation name.
	 */
	public static class ReservationName extends CustomParameter {

		@Override
		public String extract(String input) {
			String extracted = "";
			
			//English
			if (this.language.equals(LANGUAGES.EN)){
				extracted = NluTools.stringFindFirst(input, "(name .*)");
				extracted = extracted.replaceAll("\\b(at|for)\\b.*", "").trim();
				extracted = extracted.replaceAll("\\b(name|is|a|the)\\b", "").trim();
			
			//Other languages
			}else{
				Debugger.println("Custom parameter 'ReservationName' does not support language: " + this.language, 1);
			}
			return extracted;
		}

		@Override
		public String build(String input) {
			//build result with entry for field "VALUE"
			JSONObject itemResultJSON = JSON.make(InterviewData.VALUE, input);

			this.buildSuccess = true;
			return itemResultJSON.toJSONString();
		}		
	}

}
