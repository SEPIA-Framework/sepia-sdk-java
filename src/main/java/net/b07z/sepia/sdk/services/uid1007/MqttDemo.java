package net.b07z.sepia.sdk.services.uid1007;

import java.util.TreeSet;

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
import net.b07z.sepia.server.assist.services.ServiceRequirements;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Sdk;
import net.b07z.sepia.websockets.mqtt.SepiaMqttClient;
import net.b07z.sepia.websockets.mqtt.SepiaMqttClientOptions;
import net.b07z.sepia.websockets.mqtt.SepiaMqttMessage;

/**
 * Demo integration of MQTT client.
 * 
 * @author Florian Quirin
 *
 */
public class MqttDemo implements ServiceInterface {
	
	//Command name of your service (will be combined with userId to be unique, e.g. 'uid1007.mqtt')
	private static final String CMD_NAME = "mqtt";
	
	//MQTT broker address
	//private String mqttBroker = "tcp://localhost:1883"; 		//Typical Mosquitto broker on same machine
	//private String mqttBroker = "ws://localhost:1883"; 		//Mosquitto broker on same machine configured for WebSockets
	private String mqttBroker = "ws://broker.hivemq.com:8000";		//public test broker
	private String mqttUserName = "";
	private String mqttPassword = "";
	private String sepiaMqttTopic = "sepia/mqtt-demo";		//SEPIA MQTT topic
	
	@Override
	public ServiceRequirements getRequirements(){
		return new ServiceRequirements()
			.serverMinVersion("2.4.0")
		;
	}
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang) {
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("MQTT testen.");
		//OTHER
		}else{
			samples.add("Test MQTT.");
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
				.addAnswer(successAnswer, 	0, "Nachricht wurde gesendet.")
				.addAnswer(okAnswer, 		0, "Die Anfrage ist angekommen aber ich kann sie nicht bearbeiten.")
			;
			return answerPool;
		
		//Or default to English
		}else{
			answerPool	
				.addAnswer(successAnswer, 	0, "Message has been broadcasted.")
				.addAnswer(okAnswer, 		0, "Message received but I could not fulfill your request.")
			;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers start with a specific prefix, the system answers don't.
	private static final String successAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_success_0a";
	private static final String okAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_still_ok_0a";
	private static final String failAnswer = "error_0a";
	private static final String notAllowed = "smartdevice_0d";
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.otherAPI, Content.data, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release.
		//NOTE: only services uploaded by the assistant account (default ID: uid1005) can be public for all users.
		//info.makePublic();
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Direct-match trigger sentences in different languages
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentenceWithExtractedParameters("Test MQTT.", EN, JSON.make(
				PARAMETERS.SMART_DEVICE, 	SmartDevice.getExtractedValueFromType(SmartDevice.Types.device, "MQTT Client"),
				PARAMETERS.ACTION, 			Action.getExtractedValueFromType(Action.Type.on)
		));
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentenceWithExtractedParameters("MQTT testen.", DE, JSON.make(
				PARAMETERS.SMART_DEVICE, 	SmartDevice.getExtractedValueFromType(SmartDevice.Types.device, "MQTT Client"),
				PARAMETERS.ACTION, 			Action.getExtractedValueFromType(Action.Type.on)
		));
		
		//Regular expression triggers
		//NOTE: use 'normalized' text here, e.g. lower-case and no special characters ?, !, ., ', etc. ... for ä, ö, ü, ß, ... use ae, oe, ue, ss, ...
		info.setCustomTriggerRegX("^(mqtt)\\b.*|.*\\b(mqtt)$", EN);
		info.setCustomTriggerRegX("^(mqtt)\\b.*|.*\\b(mqtt)$", DE);
		info.setCustomTriggerRegXscoreBoost(10);		//boost service to increase priority over similar ones
		
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
			//.addCustomAnswer("setState", deviceStateSet) 	//adding these answers here is optional and used just as info
			//.addCustomAnswer("showState", deviceStateShow)
			//.addCustomAnswer("askStateValue", askStateValue)
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
		
		//check user role 'smarthomeguest' for this service (because it can control devices in the server's network)
		if (!nluResult.input.user.hasRole(Role.smarthomeguest)){
			service.setStatusOkay();
			service.setCustomAnswer(notAllowed);			//"soft"-fail with "not allowed" answer
			return service.buildResult();
		}
				
		//get required parameters
		Parameter device = nluResult.getRequiredParameter(PARAMETERS.SMART_DEVICE);
		//get optional parameters
		Parameter action = nluResult.getOptionalParameter(PARAMETERS.ACTION, "");
		Parameter deviceValue = nluResult.getOptionalParameter(PARAMETERS.SMART_DEVICE_VALUE, "");
		Parameter room = nluResult.getOptionalParameter(PARAMETERS.ROOM, "");
		
		//----Evaluate---
		
		String deviceType = device.getValueAsString();	//shortcut for: JSON.getString(device.getData(), InterviewData.VALUE)
		//String deviceName = JSON.getStringOrDefault(device.getData(), InterviewData.FOUND, "");
		//String deviceTypeLocal = JSON.getStringOrDefault(device.getData(), InterviewData.VALUE_LOCAL, deviceType);
		int deviceNumber = JSON.getIntegerOrDefault(device.getData(), InterviewData.ITEM_INDEX, Integer.MIN_VALUE);
		
		String roomType = room.getValueAsString().replaceAll("^<|>$", "").trim();		//... this is actually already without <..>
		//String roomTypeLocal = JSON.getStringOrDefault(room.getData(), InterviewData.VALUE_LOCAL, roomType);
		int roomNumber = JSON.getIntegerOrDefault(room.getData(), InterviewData.ITEM_INDEX, Integer.MIN_VALUE);
		
		String actionType = action.getValueAsString().replaceAll("^<|>$", "").trim();	//TODO: fix inconsistency in parameter format for device and room
				
		String targetSetValue = deviceValue.getValueAsString();
		String targetValueType = JSON.getStringOrDefault(deviceValue.getData(), 
				InterviewData.SMART_DEVICE_VALUE_TYPE, SmartHomeDevice.StateType.number_plain.name());
		
		//SHOW
		if (Is.typeEqual(actionType, Action.Type.show)){
			//TODO: we cannot show, because we can't read MQTT data in this example (only broadcast).
			service.setStatusOkay();			//"soft"-fail (action not possible)
			return service.buildResult();
			
		//Broadcast other actions
		}else{
			//connect to MQTT broker, build message, broadcast
			try{
				//payload
				JSONObject payload = JSON.make(
						"deviceType", deviceType,
						"deviceIndex", (deviceNumber == Integer.MIN_VALUE)? "" : String.valueOf(deviceNumber),
						"roomType", roomType,
						"roomIndex", (roomNumber == Integer.MIN_VALUE)? "" : String.valueOf(roomNumber)
				);
				JSON.put(payload, "action", JSON.make(
						"type", actionType,
						"value", targetSetValue,
						"valueType", targetValueType
				));
				JSON.put(payload, "input", JSON.make(
						"textRaw", nluResult.input.textRaw,
						"text", nluResult.input.text,
						"user", nluResult.input.user.getUserID()
				));
				
				//connect
				SepiaMqttClientOptions mqttOptions = new SepiaMqttClientOptions()
						.setAutomaticReconnect(false)
						.setCleanSession(true)
						.setConnectionTimeout(6);
				if (Is.notNullOrEmpty(this.mqttUserName)){
					mqttOptions.setUserName(this.mqttUserName);
				}
				if (Is.notNullOrEmpty(this.mqttPassword)){
					mqttOptions.setPassword(this.mqttPassword);
				}
				SepiaMqttClient mqttClient = new SepiaMqttClient(mqttBroker, mqttOptions);
				mqttClient.connect();
				//publish
				mqttClient.publish(sepiaMqttTopic, new SepiaMqttMessage(payload.toJSONString())
						.setQos(0)
						.setRetained(false)
				);
				mqttClient.disconnect();
				mqttClient.close();
			
			}catch (Exception e){
				//ERROR
				e.printStackTrace();
				//fail answer
				service.setStatusFail(); 			//"hard"-fail (probably connection error)
				return service.buildResult();
			}
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
		service.setStatusSuccess();
		ServiceResult result = service.buildResult();
		return result;
	}
}
