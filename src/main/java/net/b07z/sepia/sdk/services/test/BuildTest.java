package net.b07z.sepia.sdk.services.test;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.CustomParameter;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Custom service for build testing.
 * 
 * @author Florian Quirin
 *
 */
public class BuildTest implements ServiceInterface{
	
	//Command name
	private static final String CMD_NAME = "build_test";
	//Answers used:
	private static final String successAnswer = "<direct>Build test successful!";
	private static final String okAnswer = "default_not_possible_0a";
	private static final String failAnswer = "error_0a";
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		samples.add("buildtest");
		return samples;
	}
	
	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.program, Content.data, false);
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		//Direct-match trigger sentences in different languages:
		String EN = Language.EN.toValue();
		info.addCustomTriggerSentence("buildtest", EN);
		
		//Parameters:
		Parameter p1 = new Parameter(new InnerBuildTestParameter())
				.setRequired(true);
		info.addParameter(p1);
		
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
		
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
	
	//--------- Inner class test ---------
	
	public static class InnerBuildTestParameter extends CustomParameter {
		@Override
		public String extract(String input) {
			return "buildtest-parameter";
		}
	}

}
