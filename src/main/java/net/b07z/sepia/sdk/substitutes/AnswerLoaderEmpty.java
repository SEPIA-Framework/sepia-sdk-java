package net.b07z.sepia.sdk.substitutes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.assist.answers.AnswerLoader;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.core.data.Answer;

/**
 * Substitution for a proper {@link AnswerLoader} that simply returns the answer key (or direct answer).
 */
public class AnswerLoaderEmpty implements AnswerLoader {
	
	//stores the references to all answers in different languages
	Map<String, Map<String, List<Answer>>> answers = new HashMap<>();
	
	@Override
	public void setupAnswers(){}
	
	@Override
	public void updateAnswerPool(Map<String, Map<String, List<Answer>>> answersPool){
		this.answers = answersPool;
	}
		
	@Override
	public String getAnswer(NluResult nluResult, String key){
		return key;
	}
	@Override
	public String getAnswer(NluResult nluResult, String key, Object... wildcards){
		return key;
	}
	@Override
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key){
		return key;
	}
	@Override
	public String getAnswer(Map<String, List<Answer>> memory, NluResult nluResult, String key, Object... wildcards){
		return key;
	}
}
