package net.b07z.sepia.sdk.services.uid1007;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.assist.smarthome.SmartHomeHub;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Custom service demo to control a robotic vacuum cleaner via smart-home HUB interface.
 * 
 * @author Florian Quirin
 *
 */
public class RoboCleaner implements ServiceInterface{
	
	//Command name
	private static final String CMD_NAME = "robo_cleaner";
	
	//Robotic vacuum cleaner info - NOTE: you need to create this device in your SEPIA Control-HUB (or adjust values)
	private static final SmartDevice.Types DEVICE_TYPE = SmartDevice.Types.device;
	private static final String DEVICE_NAME = "Robo Cleaner";		//NOTE: has to be unique! (since Robo can be in any room)
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Schicke Robo ins Wohnzimmer.");
			samples.add("Es ist staubig in der Garage!");
			
		//OTHER
		}else{
			samples.add("Send Robo to the living-room.");
			samples.add("It is dusty in the garage!");
		}
		return samples;
	}
	
	@Override
	public ServiceAnswers getAnswersPool(String language) {
		ServiceAnswers answerPool = new ServiceAnswers(language);
		
		//Build German answers
		if (language.equals(LANGUAGES.DE)){
			answerPool
				.addAnswer(successAnswer, 	0, "Ok, Robo guckt gleich mal <1> vorbei.") 		//...be aware of German articles :-p
				.addAnswer(successAnswer, 	0, "Ok, Robo hat den Befehl bekommen <1> zu saugen.")
				.addAnswer(askRoom, 	0, "Wo soll ich Robo hinschicken?")
				.addAnswer(okAnswer, 		0, "Die Anfrage ist angekommen aber ich kann sie nicht bearbeiten.")
			;
			return answerPool;
		
		//Or default to English
		}else{
			answerPool	
				.addAnswer(successAnswer, 	0, "Ok, Robo will clean <1>")
				.addAnswer(successAnswer, 	0, "Ok, I told Robo to clean <1>")
				.addAnswer(askRoom, 	0, "Where should I send Robo?")
				.addAnswer(okAnswer, 		0, "Message received but I could not fulfill your request.")
			;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers start with a specific prefix, the system answers don't.
	private static final String successAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_success_0a";
	private static final String askRoom = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_ask_room_0a";
	private static final String okAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_still_ok_0a";
	private static final String failAnswer = "error_0a";
	private static final String notAllowed = "smartdevice_0d";
	private static final String notWithAdmin = "default_not_with_admin_0a";
	private static final String noDeviceMatchesFound = "smartdevice_0f";
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.otherAPI, Content.data, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release
		//info.makePublic();
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		String DE = Language.DE.toValue();
		String EN = Language.EN.toValue();
		
		//Direct-match trigger sentences in different languages:
		info.addCustomTriggerSentence("Activate vacuum cleaner!", EN)
			.addCustomTriggerSentenceWithRawParameters("Send Robo Cleaner home", EN, JSON.make(PARAMETERS.ROOM, "office"))
			.addCustomTriggerSentence("It is dirty!", EN);
		
		info.addCustomTriggerSentence("Staubsauger aktivieren!", DE)
			.addCustomTriggerSentenceWithRawParameters("Robo Cleaner nach Hause schicken", DE, JSON.make(PARAMETERS.ROOM, "Büro"))
			.addCustomTriggerSentence("Es ist dreckig!", DE);
				
		//Regular expression triggers
		//NOTE: use 'normalized' text here, e.g. lower-case and no special characters ?, !, ., ', etc. ... for ä, ö, ü, ß, ... use ae, oe, ue, ss, ...
		info.setCustomTriggerRegX(".*\\b("
				+ "(send|activate) (robo|robotic) (vacuum( |-|)|)(cleaner)|"
				+ "(it is|its) (dirty|dusty) in"
			+ ")\\b.*", EN
		);
		info.setCustomTriggerRegX(".*\\b("
				+ "(sende|schicke|aktiviere) (robo|(staubsauger|saug(er|))( |-|)roboter)|"
				+ "es ist (dreckig|staubig) (im|in)"
			+ ")\\b.*", DE
		);
		info.setCustomTriggerRegXscoreBoost(2);		//boost service to increase priority over similar ones
		
		//Parameters:
		//Required
		Parameter p1 = new Parameter(PARAMETERS.ROOM)
			.setQuestion(askRoom)
			.setRequired(true);
		info.addParameter(p1);
		
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module with api.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(failAnswer)
			.addOkayAnswer(okAnswer)
			.addCustomAnswer("notAllowed", notAllowed)
			.addCustomAnswer("notWithAdmin", notWithAdmin)
			.addCustomAnswer("noDeviceMatchesFound", noDeviceMatchesFound)
		;
		info.addAnswerParameters("room"); 	//variables used inside answers: <1>, <2>, ...
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//SECURITY:
		
		//check user role 'smarthomeguest' for this skill (because it controls devices in the server's network)
		if (nluResult.input.user.hasRole(Role.superuser)){
			//allow or not?
			
			//add button that links to help
			service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
			service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-docs/wiki/Create-and-Edit-Users");
			service.putActionInfo("title", "Info: Create Users");
			
			service.setStatusOkay();
			service.setCustomAnswer(notWithAdmin);			//"soft"-fail with "not allowed" answer
			return service.buildResult();
			
		}else if (!nluResult.input.user.hasRole(Role.smarthomeguest) && !nluResult.input.user.hasRole(Role.smarthomeadmin)){
			//add button that links to help
			service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
			service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-docs/wiki/Smart-Home-Controls");
			service.putActionInfo("title", "Info: Smart Home Setup");
			
			service.setStatusOkay();
			service.setCustomAnswer(notAllowed);			//"soft"-fail with "not allowed" answer
			return service.buildResult();
		}
		
		//Check if we know a Smart Home HUB (e.g. openHAB, FHEM, internal, etc.)
		SmartHomeHub smartHomeHUB = SmartHomeHub.getHubFromSeverConfig();
		if (smartHomeHUB == null){
			service.setStatusOkay(); 				//"soft"-fail (no error just missing info)
			return service.buildResult();
		}
		
		//ACTIONS:
		
		//get required parameters
		Parameter room = nluResult.getRequiredParameter(PARAMETERS.ROOM);
		String roomType = room.getValueAsString();
		String roomTypeLocal = JSON.getStringOrDefault(room.getData(), InterviewData.VALUE_LOCAL, roomType);
		int roomIndex = JSON.getIntegerOrDefault(room.getData(), InterviewData.ITEM_INDEX, Integer.MIN_VALUE);
		
		//store room for answer (answer wildcard <1>)
		if (roomIndex != Integer.MIN_VALUE){
			service.resultInfoPut("room", roomTypeLocal + " " + roomIndex);
		}else{
			service.resultInfoPut("room", roomTypeLocal);
		}
		
		//get optional parameters
		//NONE
		
		//find robotic cleaner device:
		
		SmartHomeDevice roboCleaner = null;
		//get all devices of all rooms
		Map<String, Object> filters = new HashMap<>();
		filters.put(SmartHomeDevice.FILTER_TYPE, DEVICE_TYPE.name());
		filters.put("limit", -1);
		List<SmartHomeDevice> matchingDevices = smartHomeHUB.getFilteredDevicesList(filters);
		//find Robo by name
		if (matchingDevices != null && !matchingDevices.isEmpty()){
			List<SmartHomeDevice> roboCleanerDevices = SmartHomeDevice.findDevicesWithMatchingTagIgnoreCase(matchingDevices, DEVICE_NAME);
			if (roboCleanerDevices != null && roboCleanerDevices.size() == 1){
				//Robo has to be unique!
				roboCleaner = roboCleanerDevices.get(0);
			}
		}
		//abort if no Robo was found
		if (roboCleaner == null){
			service.setStatusOkay(); 				//"soft"-fail (no error just missing info)
			service.setCustomAnswer(noDeviceMatchesFound);
			return service.buildResult();
		}
		
		//send request to device (NOTE: make sure that the Device/SmartHomeHub can process your state value) 
		
		boolean setSuccess = smartHomeHUB.setDeviceState(
			roboCleaner, 
			roomType,
			SmartHomeDevice.StateType.text_raw.name()
		);
		if (!setSuccess){
			//fail answer
			service.setStatusFail();				//"hard"-fail (probably HUB connection error)
			return service.buildResult();
		}
		
		//all good
		service.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = service.buildResult();
		return result;
	}

}
