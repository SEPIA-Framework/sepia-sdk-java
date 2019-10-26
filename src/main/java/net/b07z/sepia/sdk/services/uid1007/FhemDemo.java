package net.b07z.sepia.sdk.services.uid1007;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.Action;
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
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Sdk;
import net.b07z.sepia.server.core.tools.URLBuilder;

/**
 * Demo integration of FHEM smart home control HUB.
 * 
 * @author Florian Quirin
 *
 */
public class FhemDemo implements ServiceInterface {
	
	//Command name of your service (will be combined with userId to be unique, e.g. 'uid1007.python_bridge')
	private static final String CMD_NAME = "fhem";
	
	//TODO: add in SEPIA v2.3.2
	/*
	@Override
	public ServiceRequirements getRequirements(){
		return new ServiceRequirements()
			.serverMinVersion("2.3.2")
			.apiAccess(ServiceRequirements.Apis.fhem)
		;
	}
	*/
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang) {
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("FHEM testen.");
		//OTHER
		}else{
			samples.add("Test FHEM.");
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
		
		//Build German answers
		if (language.equals(LANGUAGES.DE)){
			answerPool
				.addAnswer(successAnswer, 	0, "Test erfolgreich.")
				.addAnswer(okAnswer, 		0, "Die Anfrage ist angekommen aber ich kann sie nicht bearbeiten.")
				.addAnswer(customAnswer,	0, "Das Gerät mit dem Namen <1> wurde auf <2> gesetzt.")
			;
			return answerPool;
		
		//Or default to English
		}else{
			answerPool	
				.addAnswer(successAnswer, 	0, "Test successful.")
				.addAnswer(okAnswer, 		0, "Message received but I could not fulfill your request.")
				.addAnswer(customAnswer,	0, "The device with the name <1> has been set to <2>.")
			;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers start with a specific prefix, the system answers don't.
	private static final String successAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_success_0a";
	private static final String okAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_still_ok_0a";
	private static final String customAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_state_set_0a";
	private static final String failAnswer = "error_0a";
	private static final String notAllowed = "smartdevice_0d";
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.REST, Content.apiInterface, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release.
		//NOTE: only services uploaded by the assistant account (default ID: uid1005) can be public for all users.
		//info.makePublic();
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Direct-match trigger sentences in different languages
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentenceWithExtractedParameters("Test FHEM.", EN, JSON.make(
				PARAMETERS.SMART_DEVICE, 	"<" + SmartDevice.Types.device + ">;;" + "FHEM",	//TODO: replace with 'getExtractedValueFromType' in SEPIA-Home v2.3.2
				PARAMETERS.ACTION, 			"<" + Action.Type.show + ">"						// "		"		"
		));
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentenceWithExtractedParameters("FHEM testen.", DE, JSON.make(
				PARAMETERS.SMART_DEVICE, 	"<" + SmartDevice.Types.device + ">;;" + "FHEM",
				PARAMETERS.ACTION, 			"<" + Action.Type.show + ">"
		));
		
		//Regular expression triggers
		//NOTE: use 'normalized' text here, e.g. lower-case and no special characters ?, !, ., ', etc. ... for ä, ö, ü, ß, ... use ae, oe, ue, ss, ...
		info.setCustomTriggerRegX(".*\\b(fhem)\\b.*", EN);
		info.setCustomTriggerRegXscoreBoost(5);		//boost service to increase priority over similar ones
		
		//Parameters:
		
		//Required parameters will be asked automatically by SEPIA using the defined question.
		Parameter p1 = new Parameter(PARAMETERS.SMART_DEVICE)
				.setRequired(true)
				.setQuestion("smartdevice_1a")		//note: question defined by system
		;
		info.addParameter(p1);
		
		//Optional parameters will be set if found or ignored (no question, default value)
		Parameter p2 = new Parameter(PARAMETERS.ACTION, Action.Type.toggle); 	//toggle seems to be the most reasonable default action "lights" -> "light on"
		Parameter p3 = new Parameter(PARAMETERS.SMART_DEVICE_VALUE, "");
		Parameter p4 = new Parameter(PARAMETERS.ROOM, "");
		info.addParameter(p2).addParameter(p3).addParameter(p4);
				
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module 
		//with serviceBuilder.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(failAnswer)
			.addOkayAnswer(okAnswer)
			.addCustomAnswer("setState", customAnswer) 	//adding these answers here is optional and used just as info
			.addCustomAnswer("notAllowed", notAllowed)
		;
		
		//Add answer parameters that are used to replace <1>, <2>, ... in your answers.
		//The name is arbitrary but you need to use the same one in getResult(...) later for api.resultInfoPut(...)
		info.addAnswerParameters("device_name", "device_value"); 	//<1>=..., <2>=...
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()),
				getAnswersPool(nluResult.language));
		
		//check user role 'smarthomeguest' for this service (because it controls devices in the server's network)
		if (!nluResult.input.user.hasRole(Role.smarthomeguest)){
			service.setStatusOkay();
			service.setCustomAnswer(notAllowed);			//"soft"-fail with "not allowed" answer
			return service.buildResult();
		}
		
		//this services uses the 'SmartHomeHub' interface
		String smartHomeHubHost = "http://localhost:8083/fhem";
		Fhem smartHomeHUB = new Fhem(smartHomeHubHost);			//TODO: replace with 'SmartHomeHub' in SEPIA v2.3.2
				
		//get required parameters
		Parameter device = nluResult.getRequiredParameter(PARAMETERS.SMART_DEVICE);
		//get optional parameters
		Parameter action = nluResult.getOptionalParameter(PARAMETERS.ACTION, "");
		Parameter deviceValue = nluResult.getOptionalParameter(PARAMETERS.SMART_DEVICE_VALUE, "");
		Parameter room = nluResult.getOptionalParameter(PARAMETERS.ROOM, "");
		
		//----Evaluate---
		
		String deviceType = device.getValueAsString();	//shortcut for: JSON.getString(device.getData(), InterviewData.VALUE)
		String deviceName = JSON.getStringOrDefault(device.getData(), InterviewData.FOUND, "");
		String actionType = action.getValueAsString().replaceAll("^<|>$", "").trim();	//TODO: fix inconsistency in parameter format for device and room
		String roomType = room.getValueAsString().replaceAll("^<|>$", "").trim();		//... this is actually already without <..>
		
		//Device is FHEM server itself?
		if (typeIsEqual(deviceType, SmartDevice.Types.device) && deviceName.equals("FHEM")){
			if (typeIsEqual(actionType, Action.Type.show)){
				//check if SEPIA is registered - if not try to register
				if (!smartHomeHUB.registerSepiaFramework()){
					//FAIL
					service.setStatusFail(); 				//"hard"-fail (probably connection or token error)
					return service.buildResult();
				}
				//get devices
				Map<String, SmartHomeDevice> devices = smartHomeHUB.getDevices();
				if (devices == null){
					//FAIL
					service.setStatusFail(); 				//"hard"-fail (probably connection or token error)
					return service.buildResult();
				}else{
					//list devices
					for (SmartHomeDevice shd : devices.values()){
						service.addToExtendedLog("device: " + shd.getDeviceAsJson().toJSONString());		//DEBUG
					}
					//all good
					service.setStatusSuccess();
				}
			}else{
				//OK but no result
				service.setStatusOkay();
			}

		//OK but no result
		}else{
			service.setStatusOkay();
		}
		
		//---------------
		
		//Just for demo purposes we add a button-action with a link to the SDK
		service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
		service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-sdk-java");
		service.putActionInfo("title", "SDK info");
		
		//... and we also add a demo card that points to the SEPIA homepage
		Card card = new Card(Card.TYPE_SINGLE);
		JSONObject linkCard = card.addElement(
				ElementType.link, 
				JSON.make("title", "S.E.P.I.A." + ":", "desc", "Homepage"),
				null, null, "", 
				"https://sepia-framework.github.io/", 
				"https://sepia-framework.github.io/img/icon.png", 
				null, null
		);
		JSON.put(linkCard, "imageBackground", "#000");	//more options like CSS background
		service.addCard(card.getJSON());
		
		//build the API_Result
		ServiceResult result = service.buildResult();
		return result;
	}
	
	//------------ TOOLS ------------
	
	/**
	 * TODO: replace with Is.typeEqual in SEPIA-Home v2.3.2
	 */
	private static boolean typeIsEqual(String value, Enum<?> type){
		return value.equals(type.name());
	}
	
	//------------- FHEM ------------- //TODO: move to server
	
	public static class Fhem implements SmartHomeHub {
		
		private String host;
		private String csrfToken = "";
		public static final String NAME = "fhem";
		
		private static final String TAG_NAME = "sepia-name";		//TODO: replace with SmartHomeDevice.SEPIA_TAG_.. in SEPIA v2.3.2
		private static final String TAG_TYPE = "sepia-type";
		private static final String TAG_ROOM = "sepia-room";
		private static final String TAG_DATA = "sepia-data";
		private static final String TAG_MEM_STATE = "sepia-mem-state";
		
		/**
		 * Create new FHEM instance and automatically get CSRF token.
		 * @param host - e.g.: http://localhost:8083/fhem
		 */
		public Fhem(String host){
			if (Is.nullOrEmpty(host)){
				throw new RuntimeException("No host address found for FHEM integration!");
			}else{
				this.host = host;
				this.csrfToken = getCsrfToken(this.host);
			}
		}
		/**
		 * Create new FHEM instance with given CSRF token.
		 * @param host - e.g.: http://localhost:8083/fhem
		 * @param csrfToken - e.g.: csrf_12345...
		 */
		public Fhem(String host, String csrfToken){
			if (Is.nullOrEmpty(host)){
				throw new RuntimeException("No host address found for FHEM integration!");
			}else{
				this.host = host;
				this.csrfToken = csrfToken;
			}
		}
		
		//TODO: add override annotation in SEPIA v2.3.2+
		public boolean registerSepiaFramework(){
			//Find attributes first
			String foundAttributes = "";
			String getUrl = URLBuilder.getString(this.host, 
					"?cmd=", "jsonlist2 global",
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			try {
				//Call and check
				JSONObject resultGet = Connectors.httpGET(getUrl);
				if (Connectors.httpSuccess(resultGet) && JSON.getIntegerOrDefault(resultGet, "totalResultsReturned", 0) == 1){
					foundAttributes = (String) JSON.getJObject((JSONObject) JSON.getJArray(resultGet, "Results").get(0), "Attributes").get("userattr");
					if (Is.nullOrEmpty(foundAttributes)){
						Debugger.println("FHEM - registerSepiaFramework: Failed! No existing 'userattr' found in 'global' attributes. Please add manually.", 1);
						return false;
					}else{
						//System.out.println("foundAttributes: " + foundAttributes); 			//DEBUG
						//check if attributes are already there (if one is there all should be there ...)
						if (foundAttributes.matches(".*\\b" + TAG_NAME + "\\b.*")){
							return true;
						}
					}
				}else{
					Debugger.println("FHEM - registerSepiaFramework: Failed! Could not load global attributes. Msg.: " + resultGet.toJSONString(), 1);
					return false;
				}
				//Register FHEM means adding the SEPIA tags to global attributes
				String setUrl = URLBuilder.getString(this.host, 
						"?cmd=", "attr global userattr " + foundAttributes + " " + TAG_NAME + " " + TAG_TYPE + " " + TAG_ROOM + " " + TAG_DATA + " " + TAG_MEM_STATE,
						"&XHR=", "1",
						"&fwcsrf=", this.csrfToken
				);
				JSONObject resultSet = Connectors.httpGET(setUrl);
				if (Connectors.httpSuccess(resultSet)){
					//all good
					return true;
				}else{
					Debugger.println("FHEM - registerSepiaFramework: Failed! Could not set global attributes. Msg.: " + resultSet.toJSONString(), 1);
					return false;
				}
			}catch (Exception e){
				Debugger.println("FHEM - registerSepiaFramework: Failed! Error: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				return false;
			}
		}

		@Override
		public Map<String, SmartHomeDevice> getDevices(){
			String url = URLBuilder.getString(this.host, 
					"?cmd=", "jsonlist2",
					"&XHR=", "1",
					"&fwcsrf=", this.csrfToken
			);
			JSONObject result = Connectors.httpGET(url);
			if (Connectors.httpSuccess(result)){
				try {
					Map<String, SmartHomeDevice> devices = new HashMap<>();
					JSONArray devicesArray = JSON.getJArray(result, "Results");
					for (Object o : devicesArray){
						JSONObject hubDevice = (JSONObject) o;
						
						//Build unified object for SEPIA
						SmartHomeDevice shd = buildDeviceFromResponse(hubDevice);
						
						//devices
						if (shd != null){
							devices.put(shd.getName(), shd);
						}
					}
					return devices;
					
				}catch (Exception e){
					Debugger.println("FHEM - getDevices FAILED with msg.: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
					return null;
				}
			}else{
				Debugger.println("FHEM - getDevices FAILED with msg.: " + result.toJSONString(), 1);
				return null;
			}
		}

		@Override
		public SmartHomeDevice loadDeviceData(SmartHomeDevice arg0){
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean setDeviceState(SmartHomeDevice arg0, String arg1){
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean setDeviceStateMemory(SmartHomeDevice arg0, String arg1){
			// TODO Auto-generated method stub
			return false;
		}
		
		//------------- FHEM specific helper methods --------------
		
		/**
		 * Get CSRF token from FHEM server.
		 * @param smartHomeHubHost - host base URL
		 * @return token or null
		 */
		public static String getCsrfToken(String smartHomeHubHost){
			try{
				//HttpClientResult httpRes = Connectors.apacheHttpGET(smartHomeHubHost + "?XHR=1", null);
				//service.addToExtendedLog(httpRes.headers);		//NOTE: you may use this in SEPIA-Home v2.3.2+
				URLConnection conn = new URL(smartHomeHubHost + "?XHR=1").openConnection();
				//read header
				return conn.getHeaderField("X-FHEM-csrfToken").toString();
				
			}catch (Exception e){
				Debugger.println("FHEM - getCsrfToken FAILED with msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
				return null;
			}
		}
		
		//build device from JSON response
		private SmartHomeDevice buildDeviceFromResponse(JSONObject hubDevice){
			//Build unified object for SEPIA
			JSONObject internals = JSON.getJObject(hubDevice, "Internals");
			JSONObject attributes = JSON.getJObject(hubDevice, "Attributes");
			String name = null;
			String type = null;
			String room = null;
			String memoryState = "";
			if (attributes != null){
				//try to find self-defined SEPIA tags first
				name = JSON.getStringOrDefault(attributes, "sepia-name", null);				//TODO: replace with SmartHomeDevice.SEPIA_TAG_NAME in SEPIA v2.3.2+
				type = JSON.getStringOrDefault(attributes, "sepia-type", null);
				room = JSON.getStringOrDefault(attributes, "sepia-room", null);
				memoryState = JSON.getStringOrDefault(attributes, "sepia-mem-state", null);
			}
			//smart-guess if missing sepia-specific settings
			if (name == null && internals != null){
				name = JSON.getStringOrDefault(internals, "name", null);		//NOTE: has to be unique!
			}
			if (name == null){
				//we only accept devices with name
				return null;
			}
			if (type == null && internals != null){
				String fhemType = JSON.getString(internals, "type").toLowerCase(); 
				if (fhemType.matches("(.*\\s|^|,)(light.*|lamp.*)")){
					type = SmartDevice.Types.light.name();		//LIGHT
				}else if (fhemType.matches("(.*\\s|^|,)(heat.*|thermo.*)")){
					type = SmartDevice.Types.heater.name();		//HEATER
				}else{
					type = fhemType;		//take this if we don't have a specific type yet
				}
			}
			if (room == null && attributes != null){
				String fhemRoom = JSON.getString(attributes, "room").toLowerCase();
				room = fhemRoom;
			}
			//create common object
			String fhemObjName = JSON.getStringOrDefault(hubDevice, "Name", null);
			Object stateObj = JSON.getObject(hubDevice, new String[]{"Readings", "state"});
			Object linkObj = (fhemObjName != null)? (this.host + "?cmd." + fhemObjName) : null;
			JSONObject meta = JSON.make(
					"fhem-id", fhemObjName
			);
			//note: we need fhem-id for commands although it is basically already in 'link'
			SmartHomeDevice shd = new SmartHomeDevice(name, type, room, 
					(stateObj != null)? stateObj.toString() : null, memoryState, 
					(linkObj != null)? linkObj.toString() : null, meta);
			return shd;
		}
		
	}

}
