package net.b07z.sepia.sdk.services.uid1007;

import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interpreters.NormalizerLight;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.CustomParameter;
import net.b07z.sepia.server.assist.services.ServiceAccessManager;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.assist.users.ACCOUNT;
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
	//		This is okay for development but has some disadvantages like there will be no variation on questions after a repeat 
	//		("sorry?" -> "sorry once more plz?" ...).
	private static final String successAnswer = "<direct>Ok, I've reserved a table for the <1> for <2> people on the name <3>. "
												+ "Thank you and have a great meal.";
	private static final String okAnswer = "<direct>Sorry I could not reserve a table. Please try again later.";
	private static final String failAnswer = "error_0a";
	private static final String askTimeAndDate = "<direct>When would you like to visit Luigis?";
	private static final String askTimeAndDateMoreSpecific = "<direct>Sorry can you tell me the date and time of your visit again? I think I missed one of them.";
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
		info.setCustomTriggerRegX(".*\\b((reserve|book)\\b.* table)\\b.*", EN);
		info.setCustomTriggerRegXscoreBoost(5);		//boost service a bit to increase priority over similar ones
		
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
		
		//Add answer parameters that are used to replace <1>, <2>, ... in your answers.
		//The name is arbitrary but you need to use the same one in getResult(...) later for api.resultInfoPut(...)
		info.addAnswerParameters("time", "number", "name"); 	//<1>=time, <2>=number, ...
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, getInfo(nluResult.language));
		
		//get required parameters:
		
		//-date and time
		Parameter dateTimeParameter = nluResult.getRequiredParameter(PARAMETERS.TIME);
		String day = (String) dateTimeParameter.getDataFieldOrDefault(InterviewData.DATE_DAY);
		String time = (String) dateTimeParameter.getDataFieldOrDefault(InterviewData.DATE_TIME);
		
		//in this scenario we can get a time AND date or JUST ONE of them - (check Alarm service for more elegant handling)
		if (day.isEmpty() || time.isEmpty()){
			//abort with question
			api.setIncompleteAndAsk(PARAMETERS.TIME, askTimeAndDateMoreSpecific);
			ServiceResult result = api.buildResult();
			return result;
		}
		
		//some cosmetics (Note: this is language dependent and should be handled with more care in a real service! ;-))
		String timeDate = day + " " + time;
		if (api.language.equals(LANGUAGES.EN)){
			timeDate = DateTimeConverters.getSpeakableDate(day, "yyyy.MM.dd", api.language)
				+ " at " + DateTimeConverters.convertDateFormat(time, "HH:mm:ss", "h:mm a");
		}
		
		//-number
		Parameter numberParameter = nluResult.getRequiredParameter(PARAMETERS.NUMBER);
		String number = numberParameter.getValueAsString();
		
		//-name - NOTE: custom parameter has different naming
		Parameter nameParameter = nluResult.getRequiredParameter(ReservationName.class.getName());
		String name = nameParameter.getValueAsString();
		
		//get optional parameters
		//NONE
		
		//Set answer parameters as defined in getInfo():
		api.resultInfoPut("time", timeDate);
		api.resultInfoPut("number", number);
		api.resultInfoPut("name", name);
		
		//This service basically cannot fail ... ;-)
		// ... here you would call your reservation method/API/program ...
				
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
				extracted = NluTools.stringFindFirst(input, "(my name|my$|name .*)");
				extracted = extracted.replaceAll("\\b(at|for)\\b.*", "").trim();
				extracted = extracted.replaceAll("\\b(name|is|a|the)\\b", "").trim();
				
				//check some specials and access account if allowed
				if (extracted.equals("my")){
					//access account
					ServiceAccessManager sam = new ServiceAccessManager("demoKey");
					if (sam.isAllowedToAccess(ACCOUNT.USER_NAME_LAST)){
						extracted = nluInput.user.getName(sam);
					}else{
						//refuse and ask again - a real service should handle this with a more specific follow-up question
						extracted = "<user_data_unresolved>"; 
					}
				}
			
			//Other languages
			}else{
				Debugger.println("Custom parameter 'ReservationName' does not support language: " + this.language, 1);
			}
			
			//Reconstruct original text format (before normalization) - This is just a cosmetic change
			if (!extracted.isEmpty()){
				Normalizer normalizer = new NormalizerLight();
				extracted = normalizer.reconstructPhrase(nluInput.textRaw, extracted);
			}
			return extracted;
		}
		
		@Override
		public String responseTweaker(String input){
			if (language.equals(LANGUAGES.EN)){
				return input.replaceAll(".*(for |on |the |name )", "").trim();
			}else{
				Debugger.println("Custom parameter 'ReservationName' does not support language: " + this.language, 1);
				return input;
			}
		}

		@Override
		public String build(String input){
			//any errors?
			if (input.equals("<user_data_unresolved>")){
				this.buildSuccess = false;
				return ""; 		//TODO: this probably should become something like 'Interview.ERROR_USER_DATA_ACCESS' in the future;
			}else{
				//build result with entry for field "VALUE"
				JSONObject itemResultJSON = JSON.make(InterviewData.VALUE, input);
				this.buildSuccess = true;
				return itemResultJSON.toJSONString();
			}
		}		
	}

}
