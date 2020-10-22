package net.b07z.sepia.sdk.services.uid1007;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerStatics;
import net.b07z.sepia.server.assist.answers.ServiceAnswers;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.CustomParameter;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceRequirements;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.CsvUtils;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Sdk;

/**
 * Service that loads Covid-19 data via the public ECDC API and generates info messages presenting total cases, deaths and recent cases for a specific country (or "world" summary).   
 * 
 * @author Florian Quirin
 * 
 */
public class CoronaDataEcdc implements ServiceInterface {
	
	//USED: 		https://opendata.ecdc.europa.eu/covid19/casedistribution/csv/
	//ALTERNATIVE?:	https://dashboards-dev.sprinklr.com/data/9043/global-covid19-who-gis.json
	
	//Command name of your service (will be combined with userId to be unique, e.g. 'uid1007.mqtt')
	private static final String CMD_NAME = "corona_data";
	
	@Override
	public ServiceRequirements getRequirements(){
		return new ServiceRequirements()
			.serverMinVersion("2.5.0")
		;
	}
	
	@Override
	public TreeSet<String> getSampleSentences(String lang) {
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Zeig mir die aktuellen Corona Daten aus Deutschland.");
			samples.add("Wie viele Coronavirus Fälle gibt es in Deutschland?");
		//OTHER
		}else{
			samples.add("Show me Corona data from England.");
			samples.add("How many Coronavirus cases are there in the UK?");
		}
		return samples;
	}
	
	@Override
	public ServiceAnswers getAnswersPool(String language) {
		ServiceAnswers answerPool = new ServiceAnswers(language);
		
		//Build German answers
		if (language.equals(LANGUAGES.DE)){
			answerPool
				.addAnswer(successAnswer, 	0, "Hier sind die aktuellen, weltweiten Corona Zahlen des ECDC.  "
						+ "Fälle insgesamt: <2>.  Tote insgesamt: <3>.")
				.addAnswer(successWithCountry, 	0, "Hier sind die aktuellen Corona Zahlen des ECDC für <1>.  "
						+ "Fälle insgesamt: <2>.  Tote insgesamt: <3>.  Neue Fälle: <4>.")
				.addAnswer(okAnswer, 		0, "Die Anfrage ist angekommen aber ich kann sie nicht bearbeiten.")
			;
			return answerPool;
		
		//Or default to English
		}else{
			answerPool	
				.addAnswer(successAnswer, 	0, "Here are the recent, worldwide Corona numbers from the ECDC.  "
						+ "Total cases: <2>.  Total deaths: <3>.")
				.addAnswer(successWithCountry, 	0, "Here are the recent ECDC Corona numbers for <1>.  "
						+ "Total cases: <2>.  Total deaths: <3>.  New cases: <4>.")
				.addAnswer(okAnswer, 		0, "Message received but I could not fulfill your request.")
			;
			return answerPool;
		}
	}
	//We keep a reference here for easy access in getResult - Note that custom answers start with a specific prefix, the system answers don't.
	private static final String successAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_success_0a";
	private static final String successWithCountry = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_success_0b";
	private static final String okAnswer = ServiceAnswers.ANS_PREFIX + CMD_NAME + "_still_ok_0a";
	private static final String failAnswer = "error_0a";

	@Override
	public ServiceInfo getInfo(String language){
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.otherAPI, Content.data, false);
		
		//Should be available publicly or only for the developer? Set this when you are done with testing and want to release
		//info.makePublic();
		
		//Command
		info.setIntendedCommand(Sdk.getMyCommandName(this, CMD_NAME));
		
		String EN = Language.EN.toValue();
		String DE = Language.DE.toValue();
		
		//Regular expression triggers
		//NOTE: use 'normalized' text here, e.g. lower-case and no special characters ?, !, ., ', etc. ... for ä, ö, ü, ß, ... use ae, oe, ue, ss, ...
		info.setCustomTriggerRegX(".*\\b("
				+ "(corona|covid(-19|))((-| |)virus|) (data|numbers|cases|deaths)|"
				+ "(sick|ill|died|deaths) (by|through)( the|) (corona|covid(-19|))"
				+ ")\\b.*", EN
		);
		info.setCustomTriggerRegX(".*\\b("
				+ "(corona|covid(-19|))((-| |)virus|)( |-|)(daten|zahlen|faelle|tote|kranke)|"
				+ "(erkrankt|krank(e|)|gestorben|tote) (an|durch)( den| das|) (corona|covid(-19|))"
				+ ")\\b.*", DE
		);
		info.setCustomTriggerRegXscoreBoost(3);		//boost service to increase priority over similar ones
		
		//Optional parameters will be set if found or ignored (no question, default value)
		Parameter p1 = new Parameter(new CountryCode3());
		p1.setDefaultValue("");
		info.addParameter(p1);
		
		//Answers (these are the default answers, you can trigger a custom answer at any point in the module 
		//with serviceBuilder.setCustomAnswer(..)):
		info.addSuccessAnswer(successAnswer)
			.addFailAnswer(failAnswer)
			.addOkayAnswer(okAnswer)
			.addCustomAnswer("successWithCountry", successWithCountry)
		;
		
		//Add answer parameters that are used to replace <1>, <2>, ... in your answers.
		//The name is arbitrary but you need to use the same one in getResult(...) later for api.resultInfoPut(...)
		info.addAnswerParameters("country", "cases", "deaths", "new_cases", "new_deaths"); 	//<1>=..., <2>=...
		
		return info;
	}

	@Override
	public ServiceResult getResult(NluResult nluResult){
		//initialize result
		ServiceBuilder service = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()),
				getAnswersPool(nluResult.language));
		
		//get optional parameters:
		
		//this is the custom 'CountryCode3' parameter defined below
		Parameter countryCodeP = nluResult.getRequiredParameter(CountryCode3.class.getName());
		String threeLetterCode = countryCodeP.getValueAsString();
		String countryLocalName = (String) countryCodeP.getDataFieldOrDefault(InterviewData.VALUE_LOCAL);
		//String foundName = (String) countryCodeP.getDataFieldOrDefault(InterviewData.VALUE_LOCAL);
		
		String countryFilter = null;
		if (Is.notNullOrEmpty(threeLetterCode)){
			countryFilter = threeLetterCode;
		}
		
		//get data
		try{
			JSONObject data = getCovid19Data(countryFilter);
			if (Is.nullOrEmpty(data)){
				service.setStatusOkay();		//"soft"-fail (no data)
				return service.buildResult();
			
			}else{
				//JSON.prettyPrint(data);			//DEBUG
				String defaultLocalUnknown = AnswerStatics.get(AnswerStatics.UNKNOWN, nluResult.language);
				//try to get recent
				String recentNewCases = null;
				try{
					JSONObject details1 = (JSONObject) ((JSONArray) data.get("details")).get(1); 	//should be yesterday
					recentNewCases = Converters.obj2StringOrDefault(details1.get("cases"), defaultLocalUnknown);
				}catch (Exception e){
					recentNewCases = defaultLocalUnknown;
				}
				//set all answer parameters
				service.resultInfoPut("country", countryLocalName);
				service.resultInfoPut("cases", Converters.obj2StringOrDefault(data.get("cases"), defaultLocalUnknown));
				service.resultInfoPut("deaths", Converters.obj2StringOrDefault(data.get("deaths"), defaultLocalUnknown));
				service.resultInfoPut("new_cases", recentNewCases);
				
				//choose the country or "world" answer
				if (countryFilter != null){
					service.setCustomAnswer(successWithCountry);
				}
				service.setStatusSuccess();
				return service.buildResult();
			}
			
		}catch (Exception e){
			//e.printStackTrace();
			
			//fail answer
			service.setStatusFail(); 			//"hard"-fail (probably connection error)
			return service.buildResult();
		}
	}
	
	//----------------- custom parameters -------------------
	
	/**
	 * Parameter handler that tries to extract a country and assign the three letters ISO 3166-1 alpha-3 country code.<br>
	 * <br>
	 * See: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3 <br>
	 * <br>
	 * Currently only supports a few countries, e.g.: England, Germany, Spain, Italy, Brazil, USA.
	 */
	public static class CountryCode3 extends CustomParameter {
		
		//Regular expressions for the supported countries in DE and EN:
		private static String gbrRegEx_en = "english|england|britain|british|united kongdom|uk|gbr|gb";
		private static String gbrRegEx_de = "englisch(en|e|)|england|(gross|)britannien|britisch(en|e|)|uk|vereinigtes koenigreich|uk|gbr|gb";
		
		private static String deuRegEx_en = "german(y|)|deu|de";
		private static String deuRegEx_de = "deutsch(land|en|e|)|deu|de";
		
		private static String espRegEx_en = "spanish|spain|esp";
		private static String espRegEx_de = "spanisch(en|e|)|spanien|esp";
		
		private static String itaRegEx_en = "italian|italy|ita";
		private static String itaRegEx_de = "italienisch(en|e|)|italien|ita";
		
		private static String braRegEx_en = "brazil|brazilian|bra";
		private static String braRegEx_de = "brasilianisch(en|e|)|brasilien|bra";
		
		private static String usaRegEx_en = "american|america|united states|usa";
		private static String usaRegEx_de = "amerikanisch(en|e|)|amerika|usa";
		
		private static String turRegEx_en = "turkish|turkey|tur";
		private static String turRegEx_de = "tuerkisch(en|e|)|tuerkei|tur";
		
		private static String all_en = 
			gbrRegEx_en + "|" + 
			deuRegEx_en + "|" + 
			espRegEx_en + "|" + 
			itaRegEx_en + "|" + 
			braRegEx_en + "|" + 
			usaRegEx_en + "|" + 
			turRegEx_en
		;
		private static String all_de = 
			gbrRegEx_de + "|" + 
			deuRegEx_de + "|" + 
			espRegEx_de + "|" + 
			itaRegEx_de + "|" + 
			braRegEx_de + "|" + 
			usaRegEx_de + "|" + 
			turRegEx_de
		;

		@Override
		public String extract(String input) {
			String extracted = "";
			String found = "";
			
			//German
			if (this.language.equals(LANGUAGES.DE)){
				//find any of the supported countries
				found = NluTools.stringFindFirst(input, all_de);
				if (Is.nullOrEmpty(found)){
					return "";
				//construct the country result with ISO code + local name
				}else if (NluTools.stringContains(found, gbrRegEx_de)){
					extracted = "GBR;;England;;";
				}else if (NluTools.stringContains(found, deuRegEx_de)){
					extracted = "DEU;;Deutschland;;";
				}else if (NluTools.stringContains(found, espRegEx_de)){
					extracted = "ESP;;Spanien;;";
				}else if (NluTools.stringContains(found, itaRegEx_de)){
					extracted = "ITA;;Italien;;";
				}else if (NluTools.stringContains(found, braRegEx_de)){
					extracted = "BRA;;Brasilien;;";
				}else if (NluTools.stringContains(found, usaRegEx_de)){
					extracted = "USA;;USA;;";
				}else if (NluTools.stringContains(found, turRegEx_de)){
					extracted = "TUR;;Türkei;;";
				}
			//English
			}else if (this.language.equals(LANGUAGES.EN)){
				//find any of the supported countries
				found = NluTools.stringFindFirst(input, all_en);
				if (Is.nullOrEmpty(found)){
					return "";
				//construct the country result with ISO code + local name
				}else if (NluTools.stringContains(found, gbrRegEx_en)){
					extracted = "GBR;;England;;";
				}else if (NluTools.stringContains(found, deuRegEx_en)){
					extracted = "DEU;;Germany;;";
				}else if (NluTools.stringContains(found, espRegEx_en)){
					extracted = "ESP;;Spain;;";
				}else if (NluTools.stringContains(found, itaRegEx_en)){
					extracted = "ITA;;Italy;;";
				}else if (NluTools.stringContains(found, braRegEx_en)){
					extracted = "BRA;;Brazil;;";
				}else if (NluTools.stringContains(found, usaRegEx_en)){
					extracted = "USA;;USA;;";
				}else if (NluTools.stringContains(found, turRegEx_en)){
					extracted = "TUR;;Turkey;;";
				}
			
			//Other languages
			}else{
				Debugger.println("Custom parameter 'CountryCode3' does not support language: " + this.language, 1);
				return "";
			}
			extracted = extracted + found;	//Format: "[3-L-CODE];;[LOCAL NAME];;[INPUT FOUND]"
			
			return extracted;
		}
		
		@Override
		public String build(String input){
			//anything extracted?
			if (input.isEmpty()){
				return "";			
			}else{
				String[] exData = input.split(";;");
				//validate
				if (exData.length != 3 || exData[0].length() != 3){
					Debugger.println("Custom parameter 'CountryCode3' saw wrong ex. format: " + input, 1);
					return "";
				}
				//build result
				JSONObject itemResultJSON = JSON.make(
					InterviewData.VALUE, exData[0].toUpperCase(),	//3 letter code
					InterviewData.VALUE_LOCAL, exData[1],			//local name for 3 letter code 
					InterviewData.FOUND, exData[2]					//string match found in input
				);
				this.buildSuccess = true;
				return itemResultJSON.toJSONString();
			}
		}		
	}
	
	//----------- CORONA DATA GRABBER ------------
	
	//Call ECDC API for COvid-19 Data
	private static JSONObject getCovid19Data(String country) throws Exception {
		Stream<String> csvDataAsString = getLinesFromUrl("https://opendata.ecdc.europa.eu/covid19/casedistribution/csv/"); 
		if (csvDataAsString == null){
			return null;
		}		
		List<List<String>> data = CsvUtils.readStreamAsRows(
				csvDataAsString, 
				CsvUtils.DEFAULT_SEPARATOR, CsvUtils.DEFAULT_QUOTE, row -> {
					//return true;
					if (country == null || row.contains(country)){
						return true;
					}else{
						return false;
					}
				}
		);
		int casesSum = 0;
		int deathsSum = 0;
		if (country == null){
			//List<String> header = data.remove(0);
			data.remove(0);
			for (List<String> row : data){
				casesSum += Integer.parseInt(row.get(4));
				deathsSum += Integer.parseInt(row.get(5));
			}
			
		}else{
			for (List<String> row : data){
				casesSum += Integer.parseInt(row.get(4));
				deathsSum += Integer.parseInt(row.get(5));
			}
		}
		JSONObject summary = new JSONObject();
		JSONArray details = new JSONArray();
		JSON.put(summary, "country", (country == null? "WORLD" : country));
		JSON.put(summary, "cases", casesSum);
		JSON.put(summary, "deaths", deathsSum);
		if (country != null){
			int N = data.size();
			//for (int i=N-1; i>N-4; i--){
			for (int i=0; (i<3 && i<N); i++){
				List<String> row = data.get(i);
				JSON.add(details, JSON.make(
						"date", row.get(0),
						"cases", row.get(4),
						"deaths", row.get(5)
				));
			}
		}
		JSON.put(summary, "details", details);
		return summary;
	}
	
	//API result to String stream for CSV parser
	private static Stream<String> getLinesFromUrl(String url) throws Exception{
		String res = apacheHttpGET(url);
		//[dateRep, day, month, year, cases, deaths, countriesAndTerritories, geoId, countryterritoryCode, popData2018]
		if (res != null){
			return Arrays.stream(res.split("(\\r\\n|\\n)"));
		}else{
			return null;
		}
	}
	
	//Custom HTTP GET call
	private static String apacheHttpGET(String url) throws Exception {
		CloseableHttpClient httpclient = HttpClientBuilder.create()
				.disableRedirectHandling()
				.disableCookieManagement()
				.setUserAgent(Connectors.USER_AGENT)
				.build();
		HttpGet httpGet = new HttpGet(url);
		String responseData = null;
		try (CloseableHttpResponse response = httpclient.execute(httpGet);){
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200){
				return null;
    		}else{
			    HttpEntity resEntity = response.getEntity();
			    if (resEntity != null){
			    	ContentType ct = ContentType.getOrDefault(resEntity);
			    	Charset charset = ct.getCharset();
			    	if (charset == null){
			            charset = StandardCharsets.UTF_8;
			        }
		        	responseData = EntityUtils.toString(resEntity, charset);
		        }
			    EntityUtils.consume(resEntity);
			    return responseData;
    		}
		}catch (Exception e){
			return null;
		}
	}
}
