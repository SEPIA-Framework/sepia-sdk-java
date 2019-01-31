package net.b07z.sepia.sdk.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

/**
 * Some tools to help making the HTTP connections etc. (that are not covered by core tools).
 * 
 * @author Florian Quirin
 *
 */
public class HttpTools {
	
	private static final Logger log = LoggerFactory.getLogger(HttpTools.class);
	
	/**
	 * HTTP POST file and parameters to URL.<br>
	 * NOTE: Use for SDK debugging only!
	 * @param url - POST url
	 * @param fileKey - what you would use in a form to name the file input (query parameter for file)
	 * @param file - File to upload
	 * @param stringPVs - key-value pair map with parameters
	 * @return success true/false
	 */
	public static boolean httpPostFileAndDebug(String url, String fileKey, File file, HashMap<String, String> stringPVs) throws IOException, ParseException{
		//Builder
		MultipartEntityBuilder reqEntityBuilder = MultipartEntityBuilder.create();
        
        //add file
        FileBody uploadedFile = new FileBody(file);
        reqEntityBuilder.addPart(fileKey, uploadedFile);
        
        //add string parameters
        for (Entry<String, String> pv : stringPVs.entrySet()){
        	reqEntityBuilder.addPart(pv.getKey(), new StringBody(pv.getValue(), ContentType.APPLICATION_FORM_URLENCODED));
        	//reqEntityBuilder.addPart(new BasicNameValuePair(pv.getKey(), pv.getValue()));
        }

        //execute POST
        return httpPostDataAndDebug(url, reqEntityBuilder);
	}
	/**
	 * HTTP POST data as text (String) and parameters to URL.<br>
	 * NOTE: Use for SDK debugging only!
	 * @param url - POST url
	 * @param textKey - what you would use in a form to name the file input (query parameter for file)
	 * @param text - Data to upload
	 * @param stringPVs - key-value pair map with parameters
	 * @return success true/false
	 */
	public static boolean httpPostTextAndDebug(String url, String textKey, String text, HashMap<String, String> stringPVs) throws IOException, ParseException{
		//Builder
		MultipartEntityBuilder reqEntityBuilder = MultipartEntityBuilder.create();
        
        //add text
        StringBody uploadedText = new StringBody(text, ContentType.DEFAULT_TEXT);
        reqEntityBuilder.addPart(textKey, uploadedText);
        
        //add string parameters
        for (Entry<String, String> pv : stringPVs.entrySet()){
        	reqEntityBuilder.addPart(pv.getKey(), new StringBody(pv.getValue(), ContentType.APPLICATION_FORM_URLENCODED));
        	//reqEntityBuilder.addPart(new BasicNameValuePair(pv.getKey(), pv.getValue()));
        }

        //execute POST
        return httpPostDataAndDebug(url, reqEntityBuilder);
	}
	
	/**
	 * HTTP POST data from a {@link MultipartEntityBuilder} to URL.<br>
	 * NOTE: Use for SDK debugging only!
	 * @param url - POST url
	 * @param reqEntityBuilder - POST data
	 * @return success true/false
	 */
	private static boolean httpPostDataAndDebug(String url, MultipartEntityBuilder reqEntityBuilder) throws IOException, ParseException{
		boolean success = false;
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httppost = new HttpPost(url);

            //build request
            HttpEntity reqEntity = reqEntityBuilder.build();
            httppost.setEntity(reqEntity);

            //execute
            log.info("Executing request " + httppost.getRequestLine());
            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
            	int code = response.getStatusLine().getStatusCode();
            	success = (code == 200);
            	StatusLine status = response.getStatusLine();
            	log.info("Response code: " + code);
            	log.info("Response status: " + status);
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                	String responseData = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                	log.info("RESULT: " + responseData);
                	success = success && !responseData.contains("401 not authorized");
                	if (responseData.contains("\"result\":\"fail\"") || responseData.contains("ERROR:")){
                		success = false;
                	}
                }
                EntityUtils.consume(resEntity);
            }   
        }
		return success;
	}
}
