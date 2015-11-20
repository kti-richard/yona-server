package nu.yona.server

import groovyx.net.http.RESTClient
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import groovy.json.*

import javax.mail.*
import javax.mail.search.*

class CreateUserOnBuddyRequestTest extends Specification {

	def adminServiceBaseURL = System.properties.'yona.adminservice.url'
	def YonaServer adminService = new YonaServer(adminServiceBaseURL)

	def analysisServiceBaseURL = System.properties.'yona.analysisservice.url'
	def YonaServer analysisService = new YonaServer(analysisServiceBaseURL)

	def appServiceBaseURL = System.properties.'yona.appservice.url'
	def YonaServer appService = new YonaServer(appServiceBaseURL)
	@Shared
	def timestamp = YonaServer.getTimeStamp()

	@Shared
	def richardQuinPassword = "R i c h a r d"
	def bobDunnPassword = "B o b"
	@Shared
	def richardQuinURL
	@Shared
	def richardQuinLoginID
	@Shared
	def bobDunnGmail = "bobdunn325@gmail.com"
	@Shared
	def bobDunnGmailPassword = "bobbydunn"
	@Shared
	def bobDunnInviteURL
	@Shared
	def bobDunnURL
	@Shared
	def bobDunnLoginID
	@Shared
	def richardQuinBobBuddyURL
	@Shared
	def bobDunnRichardBuddyURL
	@Shared
	def bobDunnBuddyMessageAcceptURL
	@Shared
	def bobDunnBuddyMessageProcessURL
	@Shared
	def richardQuinBuddyMessageAcceptURL
	@Shared
	def richardQuinBuddyMessageProcessURL 

	def 'Add user Richard Quin'(){
		given:

		when:
			def response = appService.addUser("""{
				"firstName":"Richard ${timestamp}",
				"lastName":"Quin ${timestamp}",
				"nickName":"RQ ${timestamp}",
				"mobileNumber":"+${timestamp}11",
				"devices":[
					"Nexus 6"
				],
				"goals":[
					"news"
				]
			}""", richardQuinPassword)
			richardQuinURL = appService.stripQueryString(response.responseData._links.self.href)
			richardQuinLoginID = response.responseData.vpnProfile.loginID;

		then:
			response.status == 201
			richardQuinURL.startsWith(appServiceBaseURL + appService.USERS_PATH)

		cleanup:
			println "URL Richard: " + richardQuinURL
	}

	def 'Richard requests Bob to become his buddy'(){
		given:

		when:
			def beforeRequestDateTime = new Date()
			def response = appService.requestBuddy(richardQuinURL, """{
				"_embedded":{
					"user":{
						"firstName":"Bob ${timestamp}",
						"lastName":"Dun ${timestamp}",
						"emailAddress":"${bobDunnGmail}",
						"mobileNumber":"+${timestamp}12"
					}
				},
				"message":"Would you like to be my buddy?"
			}""", richardQuinPassword)
			richardQuinBobBuddyURL = response.responseData._links.self.href
			
			def bobDunnInviteEmail = getMessageFromGmail(bobDunnGmail, bobDunnGmailPassword, beforeRequestDateTime)
			def bobDunnInviteURLMatch = bobDunnInviteEmail.content =~ /$appServiceBaseURL[\w\-\/=\?&+]+/
			assert bobDunnInviteURLMatch
			bobDunnInviteURL = bobDunnInviteURLMatch.group()

		then:
			response.status == 201
			response.responseData._embedded.user.firstName == "Bob ${timestamp}"
			richardQuinBobBuddyURL.startsWith(richardQuinURL)

		cleanup:
			println "URL buddy Richard: " + richardQuinBobBuddyURL
			println "Invite URL Bob: " + bobDunnInviteURL

	}
	
	def 'Bob Dunn downloads the app and opens the link sent in the email with the app; app retrieves data to prefill'(){
		given:

		when:
			def response = appService.getUser(bobDunnInviteURL, true, null)

		then:
			response.status == 200
			response.responseData.firstName == "Bob ${timestamp}"
			response.responseData.lastName == "Dun ${timestamp}"
			response.responseData.mobileNumber == "+${timestamp}12"
	}
	
	def 'Bob Dunn adjusts data and submits; app saves with new password'(){
		given:

		when:
			def response = appService.updateUser(bobDunnInviteURL, """{
				"firstName":"Bob ${timestamp}",
				"lastName":"Dunn ${timestamp}",
				"nickName":"BD ${timestamp}",
				"mobileNumber":"+${timestamp}12",
				"devices":[
					"iPhone 6"
				],
				"goals":[
					"gambling"
				]
			}""", bobDunnPassword)
			if(response.status == 200)
			{
				bobDunnURL = appService.stripQueryString(response.responseData._links.self.href)
			}

		then:
			response.status == 200
			response.responseData.firstName == "Bob ${timestamp}"
			response.responseData.lastName == "Dunn ${timestamp}"
			response.responseData.mobileNumber == "+${timestamp}12"
			response.responseData.nickName == "BD ${timestamp}"
			response.responseData.devices.size() == 1
			response.responseData.devices[0] == "iPhone 6"
			//TODO: updating of goals is not yet supported
			//response.responseData.goals.size() == 1
			//response.responseData.goals[0] == "gambling"
			//TODO: buddy is not added yet, needs to be fixed
			//response.responseData._embedded.buddies != null
			//response.responseData._embedded.buddies.size() == 1
	}
	
	def 'Check if user is now retrievable with new password'(){
		given:

		when:
			def response = appService.getUser(bobDunnURL, true, bobDunnPassword)

		then:
			response.status == 200
			response.responseData.firstName == "Bob ${timestamp}"
			response.responseData.lastName == "Dunn ${timestamp}"
			response.responseData.mobileNumber == "+${timestamp}12"
			response.responseData.nickName == "BD ${timestamp}"
			response.responseData.devices.size() == 1
			response.responseData.devices[0] == "iPhone 6"
			//TODO: updating of goals is not yet supported
			//response.responseData.goals.size() == 1
			//response.responseData.goals[0] == "gambling"
			//TODO: buddy is not added yet, needs to be fixed
			//response.responseData._embedded.buddies != null
			//response.responseData._embedded.buddies.size() == 1
	}
	
	def 'Delete users'(){
		given:

		when:
			def responseRichard = appService.deleteUser(richardQuinURL, richardQuinPassword)
			def responseBob = appService.deleteUser(bobDunnURL, bobDunnPassword)

		then:
			responseRichard.status == 200
			responseBob.status == 200
	}
	
	def getMessageFromGmail(login, password, sentAfterDateTime)
	{
		def host = "imap.gmail.com";
	
		Properties props = new Properties()
		props.setProperty("mail.store.protocol", "imap")
		props.setProperty("mail.imap.host", host)
		props.setProperty("mail.imap.port", "993")
		props.setProperty("mail.imap.ssl.enable", "true");
		def session = Session.getDefaultInstance(props, null)
		def store = session.getStore("imap")
		
		def inbox
		def lastMessage
		try
		{
			println "Connecting to imap store"
			store.connect(host, login, password)
			inbox = store.getFolder("INBOX")
			inbox.open(Folder.READ_WRITE)
			return getMessageFromInbox(inbox, sentAfterDateTime)
		}
		finally
		{
			 if(inbox) 
			 {
			    inbox.close(true)
			 }
			 store.close()
		}
	}
	
	def getMessageFromInbox(inbox, sentAfterDateTime)
	{
		def maxWaitSeconds = 15
		def pollSeconds = 3
		def retries = maxWaitSeconds / pollSeconds
		sleep(100)
		for (def i = 0; i <retries; i++) 
		{
			println "Reading messages from inbox"
			def messages = inbox.search(
				//new ReceivedDateTerm(ComparisonTerm.GT,sentAfterDateTime))
				new FlagTerm(new Flags(Flags.Flag.SEEN), false))
			if(messages.size() > 0)
			{
				def lastMessage = messages[0]
				def lastMessageMap = [subject:lastMessage.getSubject(), content:lastMessage.getContent()]
				println "Found message: " + lastMessageMap.subject
				println lastMessageMap.content
				return lastMessageMap
			}
			sleep(pollSeconds * 1000)
		}
		assert false
	}
}