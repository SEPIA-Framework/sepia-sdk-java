package net.b07z.sepia.sdk.services.uid1007;

import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Demo for a workout help service. This is mainly to demonstrate follow-up messages.
 * 
 * @author Florian Quirin
 *
 */
public class WorkoutHelperDemo implements ServiceInterface {
	
	//Command name of your service (will be combined with userId to be unique)
	private static final String CMD_NAME = "workout_helper";
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang) {
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Ein einfaches Workout starten.");
		//OTHER
		}else{
			samples.add("Start a simple workout.");
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
				.addAnswer(successAnswer, 0, "Ok, <1> Minuten Workout in 3, 2, 1, los gehts!")
				.addAnswer(successAnswer, 0, "Ok <user_name>, <1> Minuten Workout in 3, 2, 1, los!")
				.addAnswer(successWithTimeAdjust, 0, "Hab die Zeit etwas angepasst <user_name>, <1> Minuten Workout in 3, 2, 1, los!")
				.addAnswer(okAnswer, 0, "Sorry aber das hat nicht geklappt. Ich weiß nicht warum.")
				.addAnswer(followUpFinish, 0, "Fertig mit dem Training! Gut gemacht!")
				.addAnswer(followUpIntervalMinutes, 0, "<1> Minuten noch! Weiter so!")
				.addAnswer(followUpIntervalSeconds, 0, "<1> Sekunden noch! Fast geschafft!")
				;
			return answerPool;
		
		//Fall back to English
		}else{
			answerPool
				.addAnswer(successAnswer, 0, "Ok, <1> minutes workout in 3, 2, 1, go!.")
				.addAnswer(successAnswer, 0, "Ok <user_name>, <1> minutes workout in 3, 2, 1, go!.")
				.addAnswer(successWithTimeAdjust, 0, "I've adjusted the time a bit <user_name>, <1> minutes workout in 3, 2, 1, go!.")
				.addAnswer(okAnswer, 0, "Sorry that did not work out. I don't know why.")
				.addAnswer(followUpIntervalMinutes, 0, "<1> minutes to go! Keep it up!")
				.addAnswer(followUpIntervalSeconds, 0, "<1> seconds to go! Almost there!")
				.addAnswer(followUpFinish, 0, "Done with the training! Good job!")
				;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers need to start with a certain prefix
	private static final String failAnswer = "error_0a";
	private static final String successAnswer = ServiceAnswers.ANS_PREFIX + "workout_helper_success_0a";
	private static final String successWithTimeAdjust = ServiceAnswers.ANS_PREFIX + "workout_helper_success_0b";
	private static final String okAnswer = ServiceAnswers.ANS_PREFIX + "workout_helper_ok_0a";
	private static final String followUpFinish = ServiceAnswers.ANS_PREFIX + "workout_helper_fu_finish_0a";
	private static final String followUpIntervalMinutes = ServiceAnswers.ANS_PREFIX + "workout_helper_fu_interval_0a";
	private static final String followUpIntervalSeconds = ServiceAnswers.ANS_PREFIX + "workout_helper_fu_interval_0b";

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
		info.addCustomTriggerSentence("Start a simple workout.", EN);
		String DE = Language.DE.toValue();
		info.addCustomTriggerSentence("Ein einfaches Workout starten.", DE);
		
		//Regular expression triggers:
		info.setCustomTriggerRegX(".*\\b("
					+ "(start)\\b.* (training|work(-| |)out)"
				+ ")\\b.*", EN);
		info.setCustomTriggerRegX(".*\\b("
					+ "(starte)\\b.* (training|work(-| |)out|uebung(en|))" + "|"
					+ "(training|work(-| |)out|uebung(en|)) (starten)"
				+ ")\\b.*", DE);
		info.setCustomTriggerRegXscoreBoost(5);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		
		//This service has only an optional parameter to set the a training time.
		//Optional parameters will be extracted from initial sentence but not asked automatically but set to a default if not given.
		
		Parameter p1 = new Parameter(PARAMETERS.TIME)
				.setRequired(false);
		info.addParameter(p1);
		
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module 
		//with serviceBuilder.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(failAnswer)
			.addOkayAnswer(okAnswer);
			//.addCustomAnswer("askTimeAndDate", askTimeAndDate); 	//optional, just for info
		
		//Add answer parameters that are used to replace <1>, <2>, ... in your answers.
		//The name is arbitrary but you need to use the same one in getResult(...) later for api.resultInfoPut(...)
		info.addAnswerParameters("time"); 	//<1>=time, ...
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()),
				getAnswersPool(nluResult.language));
		
		//get optional parameters:
		
		//-time
		long timeMs = 1000 * 60 * 5; 		//5min
		long maxTimeMs = 1000 * 60 * 60;	//60min
		long minTimeMs = 1000 * 60 * 2;		//2min
		Parameter dateTimeParameter = nluResult.getOptionalParameter(PARAMETERS.TIME, timeMs);
		if (!dateTimeParameter.isDataEmpty()){
			timeMs = JSON.getLongOrDefault((JSONObject) dateTimeParameter.getDataFieldOrDefault(InterviewData.TIME_DIFF), "total_ms", timeMs);
		}
		if (timeMs > maxTimeMs){
			timeMs = maxTimeMs;
			api.setCustomAnswer(successWithTimeAdjust);
		}else if (timeMs < minTimeMs){
			timeMs = minTimeMs;
			api.setCustomAnswer(successWithTimeAdjust);
		}else if (timeMs % 60000 > 0){
			api.setCustomAnswer(successWithTimeAdjust);
		}
		//NOTE: I'm cheating a bit here to save some answer variations by allowing only full minutes as time ;-)
		long timeMinutes = Math.round(timeMs / 60000.0d);
		long halfTimeMinutes = (long) Math.floor(timeMinutes / 2.0d);
				
		//Set answer parameters as defined in getInfo():
		api.resultInfoPut("time", timeMinutes);
		
		//Schedule intervals
		if (nluResult.input.isDuplexConnection()){
			//System.out.println(nluResult.input.connection);
			//System.out.println(nluResult.input.msgId);
			//System.out.println(nluResult.input.duplexData);
			//Finish
			api.runInBackground(timeMinutes * 60 * 1000, () -> {
				//initialize follow-up result
				ServiceBuilder service = new ServiceBuilder(nluResult);
				service.answer = Answers.getAnswerString(nluResult, followUpFinish);
				service.status = "success";
				/*boolean wasSent =*/ service.sendFollowUpMessage(nluResult.input, service.buildResult());
				return;
			});
			//Interval minutes
			if (halfTimeMinutes >= 2){
				api.runInBackground(halfTimeMinutes * 60 * 1000, () -> {
					//initialize follow-up result
					ServiceBuilder service = new ServiceBuilder(nluResult);
					service.answer = Answers.getAnswerString(nluResult, followUpIntervalMinutes, halfTimeMinutes);
					service.status = "success";
					/*boolean wasSent =*/ service.sendFollowUpMessage(nluResult.input, service.buildResult());
					return;
				});
			}
			//Interval 30s seconds
			long seconds = 30;
			api.runInBackground((timeMinutes * 60 * 1000) - (seconds * 1000), () -> {
				//initialize follow-up result
				ServiceBuilder service = new ServiceBuilder(nluResult);
				service.answer = Answers.getAnswerString(nluResult, followUpIntervalSeconds, seconds);
				service.status = "success";
				/*boolean wasSent =*/ service.sendFollowUpMessage(nluResult.input, service.buildResult());
				return;
			});
		}
				
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
}
