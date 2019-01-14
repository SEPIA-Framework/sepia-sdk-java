//some config stuff
var client_info = "web_app_tools";
var user_name = "Boss";
var userid = "";
var key = "";
var usertime_local = "";
var environment = "web_app";
var is_html_app = false;

//get credentials for server access
function login(successCallback){
	//call login or restore data
	var login; 		//TODO
	//transfer parameters
	if (login){
		user_name = login.user_name;
		language = login.language;
		userid = login.userid;
		key = login.key;
		usertime_local = login.usertime_local;
		//possible overwrite
		client_info = login.client_info;
		environment = login.environment;
		is_html_app = login.is_html_app;
		
		if (successCallback) successCallback();
	}
	return '';
}
function showCookieLS(){
	console.log('all cookies: ' + document.cookie);
	/*
	var cook = 'sepia_auth_' + client_info;
	console.log('localStorage: ' + localStorage.getItem(cook));
	console.log('sessionStorage: ' + sessionStorage.getItem(cook));
	*/
}

//get parameter from URL
function getURLParameter(name) {
	return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20'))||null
}
//get local date in required format
function getLocalDateTime(){
	var d = new Date();
	var HH = addZero(d.getHours());
	var mm = addZero(d.getMinutes());
	var ss = addZero(d.getSeconds());
	var dd = addZero(d.getDate());
	var MM = addZero(d.getMonth() + 1);
	var yyyy = d.getFullYear();
	return '' + yyyy + '.' + MM + '.' + dd + '_' + HH + ':' + mm + ':' + ss;
}
function addZero(i) {
	return (i < 10)? "0" + i : i;
}
//sha256 hash + salt
function getSHA256(data){
	return sjcl.codec.hex.fromBits(sjcl.hash.sha256.hash(data + "salty1"));
}

//escape html specific characters to show code in results view
function escapeHtml(codeString){
    return codeString.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

//CONTROLS AND SETTINGS

function openPage(pageId){
	$('#pages').find('.page').hide();
	$('#pages_menu').find('button').removeClass('active');
	$('#' + pageId).show();
	$('#' + pageId + '-menu-btn').addClass('active');
}
function exit(){
	//TODO: delete all tokens etc.
}

function showMessage(msg, skipCodeEscape){
	if (!skipCodeEscape) msg = escapeHtml(msg);
	document.getElementById('show_result').innerHTML = 
		"<div style='display:inline-block; white-space: pre; text-align: left;'>"
			+ msg +
		"</div>";
}

function getClient(){
	var customClient = $('#custom-client').val();
	if (customClient){
		sessionStorage.setItem('customClient', customClient);
		return customClient;
	}else{
		return client_info;
	}
}
function getKey(){
	var id = $('#id').val();
	var pwd = $('#pwd').val();
	updatePasswordSecurityWarning(pwd);
	if (id && pwd){
		sessionStorage.setItem('id', id);
		sessionStorage.setItem('pwd', pwd);
		if (pwd.length < 60){
			return (id + ";" + getSHA256(pwd));
		}else{
			return (id + ";" + pwd);
		}
	}else{
		return key;
	}
}
function updatePasswordSecurityWarning(_pwd){
	var pwd = _pwd || $('#pwd').val();
	if (!pwd){
		$('#pwd-security-indicator').removeClass('secure');
		$('#pwd-security-indicator').addClass('inactive');
		return;
	}
	if (pwd.length != 65){
		//simple hash - not secure
		$('#pwd-security-indicator').removeClass('secure');
		$('#pwd-security-indicator').removeClass('inactive');
	}else{
		//token - secure if user does logout
		$('#pwd-security-indicator').removeClass('inactive');
		$('#pwd-security-indicator').addClass('secure');
	}
}
function getServer(apiName){
	var url = "";
	var custom = $('#server').val();
	if (custom){
		sessionStorage.setItem('customServer', custom);
		url = custom;
	}else{
		sessionStorage.setItem('customServer', "");
		sessionStorage.setItem('server', server_select.value);
		url = server_select.options[server_select.selectedIndex].value;
	}
	if (endsWith(url, "/sepia/")){
		if (apiName){
			url += (apiName + "/");
		}else{
			console.error("API URL is incomplete: " + url);
			alert("API URL is incomplete: " + url + "\n" + "Please choose a different server for this operation.");
		}
	}
	return url;
}
function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

function buildLanguageSelectorOptions(){
	var html = '' 
			+ '<!-- officially supported -->'
			+ '<option value="de">German</option>	<option value="en">English</option>'
			+ '<!-- basic supported, no content -->'
			+ '<option value="es">Spanish</option>	<option value="fr">French</option>'
			+ '<option value="tr">Turkish</option>	<option value="sv">Swedish</option>'
			+ '<option value="ar">Arabic</option>	<option value="zh">Chinese</option>'
			+ '<option value="nl">Dutch</option>	<option value="el">Greek</option>'
			+ '<option value="it">Italian</option>	<option value="ja">Japanese</option>'
			+ '<option value="ko">Korean</option>	<option value="pl">Polish</option>'
			+ '<option value="pt">Portuguese</option><option value="ru">Russian</option>';
	return html;
}

//FUNCTION calls

function callFunSync(fun, N, i, finishedCallback){
	fun(function(){
		i++;
		if (i < N){
			callFunSync(fun, N, i, finishedCallback);
		}else{
			finishedCallback();
		}
	});
}
function callFunAsync(fun, N, j, finishedCallback){
	var callsFinished = 0;
	for (var i=j; i<N; i++){
		setTimeout(function(){
			fun(function(){
				callsFinished++;
				if (callsFinished >= N){
					finishedCallback();
				}
			});
		}, i*parseInt(delay_N.value));
	}
}

//REST calls (SEPIA API)

/* 	SAMPLE POST:
	genericPostRequest("chat", "createChannel", 
		{
			"channelId" : channelId,
			"members" : members,
			"isPublic" : false,
			"addAssistant" : true
		},
		function (data){
			console.log("SUCCESS:");
			console.log(data);
			showMessage(JSON.stringify(data, null, 2));
		},
		function (data){
			console.log("FAIL:");
			console.log(data);
			showMessage(JSON.stringify(data, null, 2));
		}
	);
*/

//generic GET request (SEPIA API)
function genericGetRequest(link, successCallback, errorCallback){
	//console.log("GET: " + link);
	showMessage("Loading ...");
	$.ajax({
		url: link,
		timeout: 10000,
		type: "get",
		//dataType: "jsonp",
		success: function(data) {
			//console.log(data);
			var jsonData = convertData(data);
			if (jsonData.result && jsonData.result === "fail"){
				if (errorCallback) errorCallback(jsonData);
			}else{
				if (successCallback) successCallback(jsonData);
			}
		},
		error: function(data) {
			console.log(data);
			var jsonData = convertData(data);
			if (errorCallback) errorCallback(jsonData);
		}
	});
}

//generic POST request to be used by other test methods (SEPIA API)
function genericPostRequest(apiName, apiPath, parameters, successCallback, errorCallback){
	var apiUrl = getServer(apiName) + apiPath;
	parameters.KEY = getKey();
	//parameters.GUUID = userid;	//<-- DONT USE THAT IF ITS NOT ABSOLUTELY NECESSARY (its bad practice and a much heavier load for the server!)
	//parameters.PWD = pwd;
	parameters.client = getClient();
	console.log('POST request to: ' + apiUrl);
	//console.log(parameters);
	showMessage("Loading ...");
	$.ajax({
		url: apiUrl,
		timeout: 10000,
		type: "POST",
		data: JSON.stringify(parameters),
		headers: {
			//"content-type": "application/x-www-form-urlencoded"
			//"content-type": "text/plain"
			"Content-Type": "application/json"
		},
		success: function(data) {
			//console.log(data);
			postSuccess(data, successCallback, errorCallback);
		},
		error: function(data) {
			console.log(data);
			postError(data, errorCallback);
		}
	});
}
//generic POST request to be used by other test methods
function genericFormPostRequest(apiName, apiPath, parameters, successCallback, errorCallback){
	var apiUrl = getServer(apiName) + apiPath;
	parameters.KEY = getKey();
	parameters.client = getClient();
	console.log('POST request to: ' + apiUrl);
	//console.log(parameters);
	showMessage("Loading ...");
	$.ajax({
		url: apiUrl,
		timeout: 10000,
		type: "POST",
		data: parameters,
		headers: {
			"content-type": "application/x-www-form-urlencoded"
		},
		success: function(data) {
			//console.log(data);
			postSuccess(data, successCallback, errorCallback);
		},
		error: function(data) {
			console.log(data);
			postError(data, errorCallback);
		}
	});
}
function postSuccess(data, successCallback, errorCallback){
	var jsonData = convertData(data);
	if (jsonData.result && jsonData.result === "fail"){
		if (errorCallback) errorCallback(jsonData);
	}else{
		if (successCallback) successCallback(jsonData);
	}
}
function postError(data, errorCallback){
	console.log("POST error");
	var jsonData = convertData(data);
	if (errorCallback) errorCallback(jsonData);
}
function convertData(data){
	if (data.readyState != undefined){
		if (data.readyState == 0){
			return ({
				msg: "Request was not sent! Plz check connection to server.",
				info: data
			});
		}
		if (data.readyState == 4 && data.status == 404){
			return ({
				msg: "Endpoint not found! Did you select the right server?",
				info: data
			});
		}
	}
	var jsonData;
	if (data.responseJSON){
		jsonData = data.responseJSON;
	}else if (data.responseText){
		try {
			jsonData = JSON.parse(data.responseText);
		}catch(err){
			jsonData = data.responseText;
		}
	}else{
		try {
			jsonData = JSON.parse(data);
		}catch(err){
			jsonData = data;
		}
	}
	return jsonData;
}

//REST calls (general HTTP)

function httpRequest(url, successCallback, errorCallback, method, data, headers, maxwait){
	showMessage("Loading ...");
	if (!maxwait) maxwait = 10000;
	var config = {
		url: url,
		timeout: maxwait,
		type: "GET",
		success: function(data) {
			showMessage("Result: success");
			//console.log(data);
			if (successCallback) successCallback(data);
		},
		error: function(xhr, status, error) {
			showMessage("Result: error");
			console.log(xhr);
			if (errorCallback) errorCallback(xhr, status, error);
		}
	};
	if (method){
		config.type = method;
	}
	if (data){
		config.data = data;
	}
	if (headers){
		config.headers = headers;
	}
	console.log(config.type + ' request to: ' + url);
	$.ajax(config);
}