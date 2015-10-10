package nu.yona.server

import groovyx.net.http.RESTClient
import groovy.json.*

import java.text.SimpleDateFormat

class YonaServer {
	final GOALS_PATH = "/goals/"
	final USERS_PATH = "/users/"
	final ANALYSIS_ENGINE_PATH = "/analysisEngine/"
	final BUDDIES_PATH_FRAGMENT = "/buddies/"
	final DIRECT_MESSAGE_PATH_FRAGMENT = "/messages/direct/"
	final ANONYMOUS_MESSAGES_PATH_FRAGMENT = "/messages/anonymous/"

	JsonSlurper jsonSlurper = new JsonSlurper()
	RESTClient restClient

	YonaServer (baseURL)
	{
		restClient = new RESTClient(baseURL)
	}

	def getTimeStamp()
	{
		def formatter = new SimpleDateFormat("yyyyMMddhhmmss")
		formatter.format(new Date())
	}

	def addGoal(jsonString)
	{
		createResource(GOALS_PATH, jsonString)
	}

	def addUser(jsonString, password)
	{
		createResourceWithPassword(USERS_PATH, jsonString, password)
	}

	def getUser(userURL, boolean includePrivateData, password = null)
	{
		if (includePrivateData) {
			getResourceWithPassword(userURL, password, ["includePrivateData": "true"])
		} else {
			getResourceWithPassword(userURL, password)
		}
	}

	def deleteUser(userURL, password)
	{
		deleteResourceWithPassword(userURL, password)
	}

	def requestBuddy(userPath, jsonString, password)
	{
		createResourceWithPassword(userPath + BUDDIES_PATH_FRAGMENT, jsonString, password)
	}

	def getAllGoals()
	{
		getResource(GOALS_PATH)
	}

	def getDirectMessages(userPath, password)
	{
		getResourceWithPassword(userPath + DIRECT_MESSAGE_PATH_FRAGMENT, password)
	}

	def getAnonymousMessages(userPath, password)
	{
		getResourceWithPassword(userPath + ANONYMOUS_MESSAGES_PATH_FRAGMENT, password)
	}

	def createResourceWithPassword(path, jsonString, password)
	{
		createResource(path, jsonString, ["Yona-Password": password])
	}

	def createResource(path, jsonString, headers = [:])
	{
		postJson(path, jsonString, headers);
	}

	def deleteResourceWithPassword(path, password)
	{
		deleteResource(path, ["Yona-Password": password])
	}

	def deleteResource(path, headers = [:])
	{
		restClient.delete(path: path, headers: headers)
	}

	def getResourceWithPassword(path, password, parameters = [:])
	{
		getResource(path, password ?  ["Yona-Password": password] : [ : ], parameters)
	}

	def postMessageActionWithPassword(path, jsonString, password)
	{
		postMessageAction(path, jsonString, ["Yona-Password": password])
	}

	def postMessageAction(path, jsonString, headers = [:])
	{
		postJson(path, jsonString, headers);
	}

	def postToAnalysisEngine(jsonString)
	{
		postJson(ANALYSIS_ENGINE_PATH, jsonString);
	}

	def getResource(path, headers = [:], parameters = [:])
	{
		restClient.get(path: path,
			contentType:'application/json',
			headers: headers,
			query: parameters)
	}

	def postJson(path, jsonString, headers = [:])
	{
		def object = jsonSlurper.parseText(jsonString)
		restClient.post(path: path,
			body: object,
			contentType:'application/json',
			headers: headers)
	}

	def stripQueryString(url)
	{
		url - ~/\?.*/
	}

}
