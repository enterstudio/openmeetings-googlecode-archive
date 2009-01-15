package org.openmeetings.app.remote.red5;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.openmeetings.app.conference.whiteboard.WhiteboardManagement;
import org.openmeetings.app.data.logs.ConferenceLogDaoImpl;
import org.openmeetings.app.data.user.dao.UsersDaoImpl;
import org.openmeetings.app.hibernate.beans.recording.RoomClient;
import org.openmeetings.app.hibernate.beans.user.Users;
import org.openmeetings.app.quartz.scheduler.QuartzRecordingJob;
import org.openmeetings.app.quartz.scheduler.QuartzSessionClear;
import org.openmeetings.app.remote.PollService;
import org.openmeetings.app.remote.StreamService;
import org.openmeetings.app.remote.WhiteBoardService;
import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopeApplicationAdapter extends ApplicationAdapter implements
	IPendingServiceCallback, IStreamAwareScopeHandler {
	
	//Beans, see red5-web.xml
	private ClientListManager clientListManager = null;
	private EmoticonsManager emoticonsManager = null;
	private WhiteBoardService whiteBoardService = null;
	
	private static final Logger log = LoggerFactory.getLogger(ScopeApplicationAdapter.class);

	//This is the Folder where all executables are written
	//TODO:fix hardcoded name of webapp
	public static String batchFileFir = "webapps"+File.separatorChar
									+"openmeetings"+File.separatorChar
									+"jod" + File.separatorChar;
	public static String lineSeperator = System.getProperty("line.separator");
	
	//The Global WebApp Path
	public static String webAppPath = "";
	public static String webAppRootKey = "openmeetings";
	public static String configDirName = "conf";
	
	private static long broadCastCounter = 0;
	
	private static ScopeApplicationAdapter instance = null;
	
	public static synchronized ScopeApplicationAdapter getInstance(){
		return instance;
	}
	
	//Beans, see red5-web.xml
	public ClientListManager getClientListManager() {
		return clientListManager;
	}
	public void setClientListManager(ClientListManager clientListManager) {
		this.clientListManager = clientListManager;
	}
	public EmoticonsManager getEmoticonsManager() {
		return emoticonsManager;
	}
	public void setEmoticonsManager(EmoticonsManager emoticonsManager) {
		this.emoticonsManager = emoticonsManager;
	}
	public WhiteBoardService getWhiteBoardService() {
		return whiteBoardService;
	}
	public void setWhiteBoardService(WhiteBoardService whiteBoardService) {
		this.whiteBoardService = whiteBoardService;
	}

	public void resultReceived(IPendingServiceCall arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean appStart(IScope scope) {
		try {
			instance = this;
			
			//This System out is for testing SLF4J / LOG4J and custom logging n Red5
			//System.out.println("Custom Webapp start UP "+new Date());
			
			webAppPath = scope.getResource("/").getFile().getAbsolutePath();
			log.debug("webAppPath : "+webAppPath);
			//batchFileFir = webAppPath + File.separatorChar + "jod" + File.separatorChar;
			
			// init your handler here
			QuartzSessionClear quartzSessionClear = new QuartzSessionClear();
			QuartzRecordingJob quartzRecordingJob = new QuartzRecordingJob();
			//QuartzZombieJob quartzZombieJob = new QuartzZombieJob();
			addScheduledJob(300000,quartzSessionClear);
			addScheduledJob(3000,quartzRecordingJob);
			//addScheduledJob(2000,quartzZombieJob);
			
			//Spring Definition does not work here, its too early, Instance is not set yet
			EmoticonsManager.getInstance().loadEmot(scope);
			
		} catch (Exception err) {
			log.error("[appStart]",err);
		}
		return true;
	}
	

	
	@Override
	public boolean roomJoin(IClient client, IScope room) {
		try {
			
			IConnection conn = Red5.getConnectionLocal();
			IServiceCapableConnection service = (IServiceCapableConnection) conn;
			String streamId = client.getId();
			
			log.debug("### Client connected to OpenMeetings, register Client StreamId: " 
					+ streamId + " scope "+ room.getName());
			
			//Set StreamId in Client
			service.invoke("setId", new Object[] { streamId },this);

			RoomClient rcm = this.clientListManager.addClientListItem(streamId, room.getName(), conn.getRemotePort(), 
					conn.getRemoteAddress(), conn.getConnectParams().get("swfUrl").toString());
			
			//Log the User
			ConferenceLogDaoImpl.getInstance().addConferenceLog("ClientConnect", rcm.getUser_id(), 
					streamId, null, rcm.getUserip(), rcm.getScope());

		} catch (Exception err){
			log.error("roomJoin",err);
		}		
		return true;
	}
	
	/**
	 * this function is invoked directly after initial connecting
	 * @return
	 */
	public String getPublicSID() {
		IConnection current = Red5.getConnectionLocal();
		RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
		return currentClient.getPublicSID();
	}
	
	/**
	 * this function is invoked after a reconnect
	 * @param newPublicSID
	 */
	public void overwritePublicSID(String newPublicSID) {
		IConnection current = Red5.getConnectionLocal();
		RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
		currentClient.setPublicSID(newPublicSID);
		this.clientListManager.updateClientByStreamId(current.getClient().getId(), currentClient);
	}

	/**
	 * Logic must be before roomDisconnect cause otherwise you cannot throw a message to each one
	 * 
	 */
	@Override
	public void roomLeave(IClient client, IScope room) {
		log.debug("roomLeave " + client.getId() + " "+ room.getClients().size() + " " + room.getContextPath() + " "+ room.getName());
		try {
			IConnection current = Red5.getConnectionLocal();
			
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			
			this.roomLeaveByScope(currentClient, room);
			
		} catch (Exception err){
			log.error("[roomLeave]",err);
		}		
	}
	
	/**
	 * this means a user has left a room but only logically, he didn't leave the app he just left the room
	 * 
	 * FIXME: Is this really needed anymore if you re-connect to another scope?
	 * 
	 * Exit Room by Application
	 * 
	 */
	public void logicalRoomLeave() {
		log.debug("logicalRoomLeave ");
		try {
			IConnection current = Red5.getConnectionLocal();
			String streamid = current.getClient().getId();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(streamid);
			
			this.roomLeaveByScope(currentClient, current.getScope());
			
		} catch (Exception err){
			log.error("[roomDisconnect]",err);
		}		
	}	

	/**
	 * Removes the Client from the List, stops recording, adds the Room-Leave event to running recordings,
	 * clear Polls and removes Client from any list
	 * 
	 * @param currentClient
	 * @param currentScope
	 */
	private void roomLeaveByScope(RoomClient currentClient, IScope currentScope) {
		try {
			
			log.debug("currentClient "+currentClient);
			log.debug("currentScope "+currentScope);
			log.debug("currentClient "+currentClient.getRoom_id());
			
			Long room_id = currentClient.getRoom_id();
			
			//Log the User
			ConferenceLogDaoImpl.getInstance().addConferenceLog("roomLeave", currentClient.getUser_id(), 
					currentClient.getStreamid(), room_id, currentClient.getUserip(), "");
			
			
			//Remove User from Sync List's
			if (room_id != null) {
				this.whiteBoardService.removeUserFromAllLists(currentScope, currentClient);
			}

			log.debug("##### roomLeave :. " + currentClient.getStreamid()); // just a unique number
			log.debug("removing USername "+currentClient.getUsername()+" "+currentClient.getConnectedSince()+" streamid: "+currentClient.getStreamid());
			
			//stop and save any recordings
			if (currentClient.getIsRecording()) {
				log.debug("*** roomLeave Current Client is Recording - stop that");
				StreamService.stopRecordAndSave(currentScope, currentClient.getRoomRecordingName(), currentClient);
				
				//set to true and overwrite the default one cause otherwise no notification is send
				currentClient.setIsRecording(true);
			}

			
			//Notify all clients of the same currentScope (room) with domain and room
			//except the current disconnected cause it could throw an exception
			
			log.debug("currentScope "+currentScope);
			
			if (currentScope == null ) {
				return;
			}
			log.debug("currentScope "+currentScope.getConnections());
			if (currentScope.getConnections() == null ) {
				return;
			}
			
			Iterator<IConnection> it = currentScope.getConnections();
			while (it.hasNext()) {
				log.debug("hasNext == true");
				IConnection cons = it.next();
				log.debug("cons Host: "+cons);
				if (cons instanceof IServiceCapableConnection) {
					
					log.debug("sending roomDisconnect to " + cons);
					RoomClient rcl = this.clientListManager.getClientByStreamId(cons.getClient().getId());
					
					if (!currentClient.getStreamid().equals(rcl.getStreamid())){
						//Send to all connected users	
						((IServiceCapableConnection) cons).invoke("roomDisconnect",new Object[] { currentClient }, this);
						log.debug("sending roomDisconnect to " + cons);
						//only to the members of the current room
						if(room_id!=null && room_id.equals(rcl.getRoom_id())){			
							//add Notification if another user is recording
							log.debug("###########[roomLeave]");
							if (rcl.getIsRecording()){
								log.debug("*** roomLeave Any Client is Recording - stop that");
								StreamService.addRoomClientEnterEventFunc(rcl, rcl.getRoomRecordingName(), rcl.getUserip(), false);
								StreamService.stopRecordingShowForClient(cons, currentClient, rcl.getRoomRecordingName(), false);
							}
						}
					}
				}
			}	
			
			
			//Remove User AFTER cause otherwise the currentClient Object is NULL ?!
			this.clientListManager.removeClient(currentClient.getStreamid());
			//If this Room is empty clear the Room Poll List
			HashMap<String,RoomClient> rcpList = this.clientListManager.getClientListByRoom(room_id);
			log.debug("roomLeave rcpList size: "+rcpList.size());
			if (rcpList.size()==0){
				PollService.clearRoomPollList(room_id);
//				log.debug("clearRoomPollList cleared");
			}
			
		} catch (Exception err) {
			log.error("[roomLeaveByScope]",err);
		}
	}
	

	/**
	 * This method handles the Event after a stream has been added all conencted
	 * Clients in the same room will gat a notification
	 * 
	 * @return void
	 * 
	 */
	public void streamPublishStart(IBroadcastStream stream) {

		IConnection current = Red5.getConnectionLocal();
		String streamid = current.getClient().getId();
		RoomClient currentClient = this.clientListManager.getClientByStreamId(streamid);
		Long room_id = currentClient.getRoom_id();	
					
		// Notify all the clients that the stream had been started
		log.debug("start streamPublishStart broadcast start: "+ stream.getPublishedName() + "CONN " + current);
		
		Iterator<IConnection> it = current.getScope().getConnections();
		while (it.hasNext()) {
			IConnection conn = it.next();
			if (conn.equals(current)){
				RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
				if (rcl.getIsRecording()){
					StreamService.addRecordingByStreamId(current, streamid, currentClient, rcl.getRoomRecordingName());
				}
				continue;
			} else {
				if (conn instanceof IServiceCapableConnection) {
					RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
					//log.debug("is this users still alive? :"+rcl);
					//Check if the Client is in the same room and same domain 
					if(room_id!=null && room_id.equals(rcl.getRoom_id())){
						IServiceCapableConnection iStream = (IServiceCapableConnection) conn;
//							log.info("IServiceCapableConnection ID " + iStream.getClient().getId());
						iStream.invoke("newStream",new Object[] { currentClient }, this);
						if (rcl.getIsRecording()){
							StreamService.addRecordingByStreamId(current, streamid, currentClient, rcl.getRoomRecordingName());
						}
					}
				}
			}
		}		
	}

	
	/**
	 * This method handles the Event after a stream has been removed all connected
	 * Clients in the same room will get a notification
	 * 
	 * @return void
	 * 
	 */
	public void streamBroadcastClose(IBroadcastStream stream) {

		// Notify all the clients that the stream had been started
		log.debug("start streamBroadcastClose broadcast close: "+ stream.getPublishedName());
		try {
			RoomClient rcl = this.clientListManager.getClientByStreamId(Red5.getConnectionLocal().getClient().getId());
			
			sendClientBroadcastNotifications(stream,"closeStream",rcl);
		} catch (Exception e){
			log.error("[streamBroadcastClose]",e);
		}
	}

	/**
	 * This method handles the notification room-based
	 * 
	 * @return void
	 * 
	 */	
	private void sendClientBroadcastNotifications(IBroadcastStream stream,String clientFunction, RoomClient rc){
		try {

			// Store the local so that we do not send notification to ourself back
			IConnection current = Red5.getConnectionLocal();
			String streamid = current.getClient().getId();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(streamid);
			Long room_id = currentClient.getRoom_id();	
				
			
			// Notify all the clients that the stream had been started
			log.debug("sendClientBroadcastNotifications: "+ stream.getPublishedName());
			log.debug("sendClientBroadcastNotifications : "+ currentClient+ " " + currentClient.getStreamid());
			
//			Notify all clients of the same scope (room)
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				if (conn.equals(current)){
					//there is a Bug in the current implementation of the appDisconnect
					if (clientFunction.equals("closeStream")){
						RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
						if (clientFunction.equals("closeStream") && rcl.getIsRecording()){
							log.debug("*** sendClientBroadcastNotifications Any Client is Recording - stop that");
							StreamService.stopRecordingShowForClient(conn, currentClient, rcl.getRoomRecordingName(), false);
						}
						// Don't notify current client
						current.ping();
					}
					continue;
				} else {
					if (conn instanceof IServiceCapableConnection) {
						RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
						log.debug("is this users still alive? :"+rcl);
						//Check if the Client is in the same room and same domain 
						if(room_id!=null && room_id.equals(rcl.getRoom_id())){
							//conn.ping();
							IServiceCapableConnection iStream = (IServiceCapableConnection) conn;
	//							log.info("IServiceCapableConnection ID " + iStream.getClient().getId());
							iStream.invoke(clientFunction,new Object[] { rc }, this);
							log.debug("sending notification to " + conn+" ID: ");

							//if this close stream event then stop the recording of this stream
							if (clientFunction.equals("closeStream") && rcl.getIsRecording()){
								log.debug("*** sendClientBroadcastNotifications Any Client is Recording - stop that");
								StreamService.stopRecordingShowForClient(conn, currentClient, rcl.getRoomRecordingName(), false);
							}
						}
					}
				}
			}
		} catch (Exception err) {
			log.error("[sendClientBroadcastNotifications]" , err);
		}
	}
	
	/*
	 * Functions to handle the moderation
	 */
	
	/**
	 * This Method will be invoked by each client if he applies for the moderation
	 * 
	 * @param id the StreamId of the Client which should become the Moderator
	 * @return
	 */

	public String setModerator(String id) {
		String returnVal = "setModerator";
		try {
			log.debug("*..*setModerator id: " + id);
			
			IConnection current = Red5.getConnectionLocal();
			//String streamid = current.getClient().getId();
			
			RoomClient currentClient = this.clientListManager.getClientByStreamId(id);
			Long room_id = currentClient.getRoom_id();
			
			currentClient.setIsMod(new Boolean(true));
			//Put the mod-flag to true for this client
			this.clientListManager.updateClientByStreamId(id, currentClient);
			
			//Now set it false for all other clients of this room
			HashMap<String,RoomClient> clientListRoom = this.clientListManager.getClientListByRoom(room_id);
			for (Iterator<String> iter=clientListRoom.keySet().iterator();iter.hasNext();) {
				String streamId = iter.next();
				RoomClient rcl = clientListRoom.get(streamId);
				//Check if it is not the same like we have just declared to be moderating this room
				if( !id.equals(rcl.getStreamid())){
					log.debug("set to false for client: "+rcl);
					rcl.setIsMod(new Boolean(false));
					this.clientListManager.updateClientByStreamId(streamId, rcl);
				}				
			}
	
			//Notify all clients of the same scope (room)
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
				//Check if the Client is in the same room and same domain 
				if(room_id!=null && room_id.equals(rcl.getRoom_id())){
					if (conn instanceof IServiceCapableConnection) {
						((IServiceCapableConnection) conn).invoke("setNewModerator",new Object[] { currentClient }, this);
						log.debug("sending setNewModerator to " + conn);
					}
				}
			}
		} catch (Exception err){
			log.error("[setModerator]",err);
			returnVal = err.toString();
		}
		return returnVal;
	}  
	
	/**
	 * there will be set an attribute called "broadCastCounter"
	 * this is the name this user will publish his stream
	 * @return long broadCastId
	 */
	public long getBroadCastId(){
		try {
			IConnection current = Red5.getConnectionLocal();
			String streamid = current.getClient().getId();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(streamid);
			currentClient.setBroadCastID(broadCastCounter++);
			this.clientListManager.updateClientByStreamId(streamid, currentClient);
			return currentClient.getBroadCastID();
		} catch (Exception err){
			log.error("[getBroadCastId]",err);
		}
		return -1;
	}
	
	/**
	 * this must be set _after_ the Video/Audio-Settings have been chosen (see editrecordstream.lzx)
	 * but _before_ anything else happens, it cannot be applied _after_ the stream has started!
	 * avsettings can be:
	 * av - video and audio
	 * a - audio only
	 * v - video only
	 * n - no a/v only static image
	 * furthermore 
	 * @param avsetting
	 * @param newMessage
	 * @return
	 */
	public RoomClient setUserAVSettings(String avsettings, Object newMessage){
		try {

			IConnection current = Red5.getConnectionLocal();
			String streamid = current.getClient().getId();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(streamid);
			currentClient.setAvsettings(avsettings);
			Long room_id = currentClient.getRoom_id();					
			this.clientListManager.updateClientByStreamId(streamid, currentClient);
			
			HashMap<String,Object> hsm = new HashMap<String,Object>();
			hsm.put("client", currentClient);
			hsm.put("message", newMessage);			
			
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();				
				RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
				//Check if the Client is in the same room and same domain 
				if(room_id!=null && room_id.equals(rcl.getRoom_id())){	
					log.debug("setUserObjectOneFour Found Client to " + conn);
					log.debug("setUserObjectOneFour Found Client to " + conn.getClient());
					if (conn instanceof IServiceCapableConnection) {
						((IServiceCapableConnection) conn).invoke("sendVarsToMessageWithClient",new Object[] { hsm }, this);
						log.debug("sending setUserObjectNewOneFour to " + conn);
						
						//if somebody is recording in this room add the event
						if (rcl.getIsRecording()) {
							StreamService.addRoomClientAVSetEvent(currentClient, rcl.getRoomRecordingName(), conn.getRemoteAddress());
						}
					}
				}
			}
			
			return currentClient;
		} catch (Exception err){
			log.error("[setUserAVSettings]",err);
		}
		return null;
	}	
	
	/**
	 * This function is called once a User enters a Room
	 * 
	 * @param room_id
	 * @return
	 */
	public HashMap<String,RoomClient> setRoomValues(Long room_id){
		try {

			HashMap <String,RoomClient> roomClientList = new HashMap<String,RoomClient>();
			IConnection current = Red5.getConnectionLocal();
			String streamid = current.getClient().getId();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(streamid);
			currentClient.setRoom_id(room_id);
			currentClient.setRoomEnter(new Date());
			this.clientListManager.updateClientByStreamId(streamid, currentClient);
			
			//Log the User
			ConferenceLogDaoImpl.getInstance().addConferenceLog("roomEnter", currentClient.getUser_id(), streamid, room_id, currentClient.getUserip(), "");
			
			log.debug("##### setRoomValues : " + currentClient.getUsername()+" "+currentClient.getStreamid()); // just a unique number

			//Check for Moderation
			//LogicalRoom ENTER
			int roomcount = 0;
			HashMap<String,RoomClient> clientListRoom = this.clientListManager.getClientListByRoom(room_id);
			for (Iterator<String> iter=clientListRoom.keySet().iterator();iter.hasNext();) {
				String key = (String) iter.next();
				RoomClient rcl = this.clientListManager.getClientByStreamId(key);
				log.debug("#+#+#+#+##+## logicalRoomEnter ClientList key: "+rcl.getRoom_id()+" "+room_id);
				//Check if the Client is in the same room and same domain 
				//and is not the same like we have just declared to be moderating this room
				if(!streamid.equals(rcl.getStreamid())){
					log.debug("set to ++ for client: "+rcl.getStreamid()+" "+roomcount);
					roomcount++;
					//Add user to List
					roomClientList.put(key, rcl);
				}				
			}
			
			if (roomcount==0){
				log.debug("Room is empty so set this user to be moderation role");
				currentClient.setIsMod(true);
				this.clientListManager.updateClientByStreamId(streamid, currentClient);
			} else {
				log.debug("Room is already somebody so set this user not to be moderation role");
				currentClient.setIsMod(false);
				this.clientListManager.updateClientByStreamId(streamid, currentClient);
			}	
			
			return roomClientList;
		} catch (Exception err){
			log.error("[setRoomValues]",err);
		}
		return null;
	}	
	

	/**
	 * this is set initial directly after login/loading language
	 * @param userId
	 * @param username
	 * @param firstname
	 * @param lastname
	 * @param orgdomain
	 * @return
	 */
	public RoomClient setUsername(Long userId, String username, String firstname, String lastname){
		try {
			//log.debug("#*#*#*#*#*#*# setUsername userId: "+userId+" username: "+username+" firstname: "+firstname+" lastname: "+lastname);
			IConnection current = Red5.getConnectionLocal();			
			String streamid = current.getClient().getId();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(streamid);
			//log.debug("[setUsername] id: "+currentClient.getStreamid());
			currentClient.setUsername(username);
			currentClient.setUserObject(userId, username, firstname, lastname);
			
			//only fill this value from User-REcord
			//cause invited users have non
			//you cannot set the firstname,lastname from the UserRecord
			Users us = UsersDaoImpl.getInstance().getUser(userId);
			if (us!=null && us.getPictureuri()!=null){
				//set Picture-URI
				log.debug("###### SET PICTURE URI");
				currentClient.setPicture_uri(us.getPictureuri());
			}
			this.clientListManager.updateClientByStreamId(streamid, currentClient);
			return currentClient;
		} catch (Exception err){
			log.error("[setUsername]",err);
		}
		return null;
	}
	

	public int setAudienceModus(String colorObj, int userPos){
		try {
			IConnection current = Red5.getConnectionLocal();
			
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			log.debug("xmlcrm setUserObjectOneFour: "+currentClient.getUsername());
			currentClient.setUsercolor(colorObj);
			currentClient.setUserpos(userPos);
			Long room_id = currentClient.getRoom_id();	
						
			//Notify all clients of the same scope (room)
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				if (conn.equals(current)){
					continue;
				} else {				
					RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
					//Check if the Client is in the same room and same domain 
					if(room_id!=null && room_id.equals(rcl.getRoom_id())){	
						log.debug("*** setAudienceModus Found Client to " + conn);
						log.debug("*** setAudienceModus Found Client to " + conn.getClient());
						if (conn instanceof IServiceCapableConnection) {
							((IServiceCapableConnection) conn).invoke("setAudienceModusClient",new Object[] { currentClient }, this);
							log.debug("sending setAudienceModusClient to " + conn);
							//if any user in this room is recording add this client to the list
							if (rcl.getIsRecording()) {
								log.debug("currentClient "+currentClient.getPublicSID());
								StreamService.addRoomClientEnterEventFunc(currentClient, rcl.getRoomRecordingName(), currentClient.getUserip(), true);
							}							
						}
					}
				}
			}				
		} catch (Exception err) {
			log.error("[setUserObjectOne2Four]",err);
		}
		return -1;		
	}
	
	/**
	 * used by the Screen-Sharing Servlet to trigger events
	 * @param room_id
	 * @param message
	 * @return
	 */
	public HashMap<String,RoomClient> sendMessageByRoomAndDomain(Long room_id, Object message){
		HashMap <String,RoomClient> roomClientList = new HashMap<String,RoomClient>();
		try {			
			
			IScope globalScope = getContext().getGlobalScope();
			
			IScope webAppKeyScope = globalScope.getScope(ScopeApplicationAdapter.webAppRootKey);
			
			//log.debug("webAppKeyScope "+webAppKeyScope);
			
			IScope scopeHibernate = webAppKeyScope.getScope(room_id.toString());
			
			if (scopeHibernate!=null){

				//Notify all clients of the same scope (room)
				Iterator<IConnection> it = webAppKeyScope.getScope(room_id.toString()).getConnections();
				
				if (it!=null) {
					while (it.hasNext()) {
						IConnection conn = it.next();				
						RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
						//Check if the Client is in the same room and same domain 
						if(room_id!=null && room_id.equals(rcl.getRoom_id())){					
							if (conn instanceof IServiceCapableConnection) {
								((IServiceCapableConnection) conn).invoke("newMessageByRoomAndDomain",new Object[] { message }, this);
								log.debug("sending newMessageByRoomAndDomain to " + conn);
							}
						}
					}
				} else {
					log.debug("sendMessageByRoomAndDomain connections is empty ");
				}
			} else {
				log.debug("sendMessageByRoomAndDomain servlet not yet started ");
			}
			
		} catch (Exception err) {
			log.error("[getClientListBYRoomAndDomain]",err);
		}
		return roomClientList;
	}	
	
	/**
	 * 
	 * @return
	 */
	public RoomClient getCurrentModerator(){
		try {
			log.debug("*..*getCurrentModerator id: ");
			
			IConnection current = Red5.getConnectionLocal();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			Long room_id = currentClient.getRoom_id();		
			
			//log.debug("Who is this moderator? "+currentMod);
			
			return this.clientListManager.getCurrentModeratorByRoom(room_id);
		} catch (Exception err){
			log.error("[getCurrentModerator]",err);
		}
		return null;
	}
	

	public int sendVars(HashMap whiteboardObj) {
		//log.debug("*..*sendVars: " + whiteboardObj);
		try {
			// Check if this User is the Mod:
			IConnection current = Red5.getConnectionLocal();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			Long room_id = currentClient.getRoom_id();	
				
			log.debug("***** sendVars: " + whiteboardObj);
			
			WhiteboardManagement.getInstance().addWhiteBoardObject(room_id, whiteboardObj);
			
			int numberOfUsers = 0;
			
			//This is no longer necessary
			//boolean ismod = currentClient.getIsMod();
			
			//log.debug("*..*ismod: " + ismod);
	
			//if (ismod) {
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				if (conn instanceof IServiceCapableConnection) {
					RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
					//log.debug("*..*idremote: " + rcl.getStreamid());
					log.debug("*..* sendVars room_id: " + room_id + " rcl.getRoom_id " +rcl.getRoom_id() + " is euqal? "+ (room_id.equals(rcl.getRoom_id())));
					if (room_id!=null && room_id.equals(rcl.getRoom_id())) {
						//log.debug("*..* sendVars room_id IS EQUAL: " + currentClient.getStreamid() + " asd " + rcl.getStreamid() + " IS eq? " +currentClient.getStreamid().equals(rcl.getStreamid()));
						if (!currentClient.getStreamid().equals(rcl.getStreamid())) {
							((IServiceCapableConnection) conn).invoke("sendVarsToWhiteboard", new Object[] { whiteboardObj },this);
							log.debug("sending sendVarsToWhiteboard to " + conn + " rcl " + rcl);
							numberOfUsers++;
						}
						//log.debug("sending sendVarsToWhiteboard to " + conn);
						if (rcl.getIsRecording()){
							StreamService.addWhiteBoardEvent(rcl.getRoomRecordingName(),whiteboardObj);
						}								
					}
				}
			}			
			
			return numberOfUsers;
			//} else {
			//	// log.debug("*..*you are not allowed to send: "+ismod);
			//	return -1;
			//}
		} catch (Exception err) {
			log.error("[sendVars]",err);
		}
		return -1;
	}
	

	public int sendVarsModeratorGeneral(Object vars) {
		log.debug("*..*sendVars: " + vars);
		try {
			IConnection current = Red5.getConnectionLocal();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			Long room_id = currentClient.getRoom_id();	
				
			log.debug("***** id: " + currentClient.getStreamid());
			
			boolean ismod = currentClient.getIsMod();
	
			if (ismod) {
				log.debug("CurrentScope :"+current.getScope().getName());
				Iterator<IConnection> it = current.getScope().getConnections();
				while (it.hasNext()) {
					IConnection conn = it.next();
					if (conn instanceof IServiceCapableConnection) {
						RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
						log.debug("*..*idremote: " + rcl.getStreamid());
						log.debug("*..*my idstreamid: " + currentClient.getStreamid());
						if (!currentClient.getStreamid().equals(rcl.getStreamid()) && room_id!=null && room_id.equals(rcl.getRoom_id())) {
							((IServiceCapableConnection) conn).invoke("sendVarsToModeratorGeneral",	new Object[] { vars }, this);
							log.debug("sending sendVarsToModeratorGeneral to " + conn);
						}
					}
				}
				return 1;
			} else {
				// log.debug("*..*you are not allowed to send: "+ismod);
				return -1;
			}
		} catch (Exception err) {
			log.error("[sendVarsModeratorGeneral]",err);
		}
		return -1;
	}

	public int sendMessage(Object newMessage) {
		try {
			IConnection current = Red5.getConnectionLocal();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			Long room_id = currentClient.getRoom_id();	
				
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				if (conn instanceof IServiceCapableConnection) {
					RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
					log.debug("*..*idremote: " + rcl.getStreamid());
					log.debug("*..*my idstreamid: " + currentClient.getStreamid());
					if (room_id!=null && room_id.equals(rcl.getRoom_id())) {
						((IServiceCapableConnection) conn).invoke("sendVarsToMessage",new Object[] { newMessage }, this);
						log.debug("sending sendVarsToMessage to " + conn);		
					}
				}
			}
		} catch (Exception err) {
			log.error("[sendMessage]",err);
		}
		return 1;
	}


	public int sendMessageWithClient(Object newMessage) {
		try {
			IConnection current = Red5.getConnectionLocal();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			Long room_id = currentClient.getRoom_id();	
				
			
			HashMap<String,Object> hsm = new HashMap<String,Object>();
			hsm.put("client", currentClient);
			hsm.put("message", newMessage);
			
			//broadcast to everybody in the room/domain
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				if (conn instanceof IServiceCapableConnection) {
					RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
					log.debug("*..*idremote: " + rcl.getStreamid());
					log.debug("*..*my idstreamid: " + currentClient.getStreamid());
					if (room_id!=null && room_id.equals(rcl.getRoom_id())) {
						((IServiceCapableConnection) conn).invoke("sendVarsToMessageWithClient",new Object[] { hsm }, this);
						log.debug("sending sendVarsToMessageWithClient to " + conn);
					}
				}
			}
		} catch (Exception err) {
			log.error("[sendMessageWithClient] ",err);
			return -1;
		}
		return 1;
	}

	public int sendMessageWithClientById(Object newMessage, String clientId) {
		try {
			IConnection current = Red5.getConnectionLocal();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			
			log.debug("### sendMessageWithClientById ###"+clientId);
			
			HashMap<String,Object> hsm = new HashMap<String,Object>();
			hsm.put("client", currentClient);
			hsm.put("message", newMessage);
			
			//broadcast Message to specific user with id
			Iterator<IConnection> it = current.getScope().getConnections();
			while (it.hasNext()) {
				IConnection conn = it.next();
				if (conn instanceof IServiceCapableConnection) {
					log.debug("### sendMessageWithClientById 1 ###"+clientId);
					log.debug("### sendMessageWithClientById 2 ###"+conn.getClient().getId());
					if (conn.getClient().getId().equals(clientId)){
						((IServiceCapableConnection) conn).invoke("sendVarsToMessageWithClient",new Object[] { hsm }, this);
						log.debug("sendingsendVarsToMessageWithClient ByID to " + conn);
					}
				}
			}
		} catch (Exception err) {
			log.error("[sendMessageWithClient] ",err);
			return -1;
		}		
		return 1;
	}
	
	/**
	 * update the Session Object after changing the user-record
	 * 
	 * FIXME: This needs to be fixed after the rework of Application Adapter, see Issue 593
	 * 
	 * @param users_id
	 */
	public void updateUserSessionObject(Long users_id, String pictureuri){
		try {			
//			Users us = UsersDaoImpl.getInstance().getUser(users_id);
//			for (Iterator<String> itList = ClientList.keySet().iterator();itList.hasNext();) {
//				String red5Id  = itList.next();
//				RoomClient rcl = ClientList.get(red5Id);
//				
//				if (rcl.getUser_id().equals(users_id)){
//					log.info("updateUserSessionObject #### FOUND USER rcl1: "+rcl.getUser_id()+ " NEW PIC: "+pictureuri);
//					rcl.setPicture_uri(pictureuri);
//					rcl.setUsername(us.getLogin());
//					rcl.setFirstname(us.getFirstname());
//					rcl.setLastname(us.getLastname());
//					ClientList.put(red5Id, rcl);
//				}
//			}
		} catch (Exception err) {
			log.error("[updateUserSessionObject]",err);
		}
	}

	public synchronized void sendMessageWithClientByPublicSID(Object message, String publicSID) {
		try {
			//ApplicationContext appCtx = getContext().getApplicationContext();
			
			IScope globalScope = getContext().getGlobalScope();
			
			IScope webAppKeyScope = globalScope.getScope(ScopeApplicationAdapter.webAppRootKey);
			
			//log.debug("webAppKeyScope "+webAppKeyScope);
			
			IScope scopeHibernate = webAppKeyScope.getScope("hibernate");
			
			//log.debug("scopeHibernate "+scopeHibernate);
			
			if (scopeHibernate!=null){
				//Notify the clients of the same scope (room) with user_id
				Iterator<IConnection> it = webAppKeyScope.getScope("hibernate").getConnections();
				//log.debug("it "+it);
				if (it!=null) {
					while (it.hasNext()) {
						IConnection conn = it.next();		
						//log.debug("conn "+conn);
						//log.debug("conn.getClient().getId() "+conn.getClient().getId());
						RoomClient rcl = this.clientListManager.getClientByStreamId(conn.getClient().getId());
						//log.debug("rcl "+rcl+" rcl.getUser_id(): "+rcl.getPublicSID()+" publicSID: "+publicSID+ " IS EQUAL? "+rcl.getPublicSID().equals(publicSID));
						if (rcl.getPublicSID().equals(publicSID)){
							//log.debug("IS EQUAL ");
							((IServiceCapableConnection) conn).invoke("newMessageByRoomAndDomain",new Object[] { message }, this);
							log.debug("sendMessageWithClientByPublicSID RPC:newMessageByRoomAndDomain"+message);
						}
					}
				}
			} else {
				//Scope not yet started
			}
		} catch (Exception err) {
			log.error("[sendMessageWithClient] ",err);
			err.printStackTrace();
		}		
	}
	
	/**
	 * Get all ClientList Objects of that room and domain
	 * Used in lz.applyForModeration.lzx
	 * @return
	 */
	public HashMap<String,RoomClient> getClientListScope(){
		HashMap <String,RoomClient> roomClientList = new HashMap<String,RoomClient>();
		try {
			IConnection current = Red5.getConnectionLocal();
			RoomClient currentClient = this.clientListManager.getClientByStreamId(current.getClient().getId());
			log.debug("xmlcrm getClientListScope: "+currentClient.getUsername());			
			Long room_id = currentClient.getRoom_id();	
							
			return this.clientListManager.getClientListByRoom(room_id);
			
		} catch (Exception err) {
			log.debug("[getClientListScope]",err);
		}
		return roomClientList;
	}
	
	
}