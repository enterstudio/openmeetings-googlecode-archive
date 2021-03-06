package org.openmeetings.app.rtp;

import java.util.HashMap;
import java.util.Iterator;

import org.openmeetings.app.data.basic.Sessionmanagement;
import org.openmeetings.app.data.conference.Roommanagement;
import org.openmeetings.app.data.user.Usermanagement;
import org.openmeetings.app.persistence.beans.recording.RoomClient;
import org.openmeetings.app.persistence.beans.rooms.Rooms;
import org.openmeetings.app.persistence.beans.user.Users;
import org.openmeetings.app.remote.red5.ClientListManager;
import org.openmeetings.app.remote.red5.ScopeApplicationAdapter;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author o.becherer
 * 
 */
public class RTPStreamingHandler {

	private static final Logger log = Red5LoggerFactory.getLogger(
			RTPStreamingHandler.class, ScopeApplicationAdapter.webAppRootKey);
	
	@Autowired
	private Sessionmanagement sessionManagement;
	@Autowired
	private Usermanagement userManagement;
	@Autowired
	private ClientListManager clientListManager;
	@Autowired
	private Roommanagement roommanagement;

	/** Contains all RTPSessions */
	private static HashMap<Rooms, RTPScreenSharingSession> rtpSessions = new HashMap<Rooms, RTPScreenSharingSession>();

	/** Minimal Limit for valid RTP Ports */
	public static final int minimalRTPPort = 22224;

	/** Maximum for valid RTP Ports */
	public static final int maximalRTPPort = 24000;

	/** Last Used Port */
	public static int last_used_port = minimalRTPPort;

	/** Define The Port a Sharer should send his RTPStream on */
	// ---------------------------------------------------------------------------------------------
	public static int getNextFreeRTPPort() {
		log.debug("getNextFreeRTPPort");

		last_used_port = last_used_port + 2;

		log.debug("getNextFreeRTPPort : " + last_used_port);
		return last_used_port;

	}

	// ---------------------------------------------------------------------------------------------

	/**
	 * Retrieving Session data for Room
	 */
	// ---------------------------------------------------------------------------------------------
	public RTPScreenSharingSession getSessionForRoom(String room,
			String sid, String publicSID) throws Exception {
		log.debug("getSessionForRoom");

		if (room == null || room.length() < 1)
			throw new Exception("InputVal room not valid");

		Long users_id = sessionManagement.checkSession(sid);
		Long user_level = userManagement.getUserLevelByID(users_id);

		Rooms myRoom = roommanagement.getRoomById(user_level,
				Long.parseLong(room));

		if (myRoom == null)
			throw new Exception("no room available for ID " + room);

		// Iterator<Rooms> miter = ;

		for (Iterator<Rooms> miter = rtpSessions.keySet().iterator(); miter
				.hasNext();) {

			RTPScreenSharingSession session = rtpSessions.get(miter.next());

			if (session.getRoom().getRooms_id().intValue() == myRoom
					.getRooms_id().intValue()) {

				// session = rtpSessions.get(rooms);

				return session;
			}

		}

		// if(session == null)

		// This should not happen at all
		log.debug("no RTPSession for Room " + room);

		return null;

	}

	// ---------------------------------------------------------------------------------------------

	/**
	 * 
	 * @author o.becherer
	 * 
	 *         Store Session for Room
	 */
	// ---------------------------------------------------------------------------------------------
	public RTPScreenSharingSession storeSessionForRoom(String room,
			Long sharing_user_id, String publicSID, String hostIP, int the_port)
			throws Exception {
		log.debug("storeSessionForRoom : Room = " + room + ", publicSID : "
				+ publicSID + ", hostIP" + hostIP);

		// Defining The IP of the Sharer (Moderator)
		// Should be retrieved via Clientlist to receive the "extern" IP, seen
		// by red5
		RoomClient rcl = clientListManager.getClientByPublicSID(
				publicSID);

		if (rcl == null)
			throw new Exception("Could not retrieve RoomClient for publicSID");

		RTPScreenSharingSession session = new RTPScreenSharingSession();

		if (room == null || room.length() < 1)
			throw new Exception("InputVal room not valid");

		Long user_level = userManagement.getUserLevelByID(sharing_user_id);
		Rooms myRoom = roommanagement.getRoomById(user_level,
				Long.parseLong(room));

		if (myRoom == null)
			throw new Exception("no Room for ID " + room);

		// Define Room
		session.setRoom(myRoom);

		// Define User
		Users user = userManagement.getUserById(sharing_user_id);

		if (user == null)
			throw new Exception("No User for id " + sharing_user_id);

		log.debug("storeSessionForRoom : User = " + user.getLogin());

		session.setSharingUser(user);

		// Define Sharers IP
		session.setSharingIpAddress(rcl.getUserip());
		log.debug("storeSessionForRoom : Sharers IP = " + rcl.getUserip());

		// Define RTP Port for Sharing User
		int port;

		if (the_port < 0)
			port = getNextFreeRTPPort();
		else
			port = the_port;

		log.debug("storeSessionForRoom : Incoming RTP Port = " + port);

		session.setIncomingRTPPort(port);

		// Pre-Define Viewers
		HashMap<String, RoomClient> clientsForRoom = clientListManager
				.getClientListByRoom(Long.parseLong(room));

		Iterator<String> siter = clientsForRoom.keySet().iterator();

		HashMap<String, Integer> viewers = new HashMap<String, Integer>();

		while (siter.hasNext()) {
			String key = siter.next();
			RoomClient client = clientsForRoom.get(key);

			int viewerPort = getNextFreeRTPPort();

			log.debug("Adding viewer : " + client.getPublicSID() + " - "
					+ viewerPort);

			viewers.put(client.getPublicSID(), viewerPort);

		}

		session.setViewers(viewers);
		log.debug("storeSessionForRoom : Added " + viewers.size()
				+ " Viewers to session");

		session.setPublicSID(publicSID);

		// RED5Host IP
		session.setRed5Host(hostIP);

		rtpSessions.put(myRoom, session);

		log.debug("storeSessionForRoom : sessionData stored");

		return session;

	}

	// ---------------------------------------------------------------------------------------------

	/**
	 * Remove Session
	 */
	// ---------------------------------------------------------------------------------------------
	public void removeSessionForRoom(String room, String sid)
			throws Exception {
		log.debug("removeSessionForRoom : " + room);

		if (room == null || room.length() < 1)
			throw new Exception("InputVal room not valid");

		Long users_id = sessionManagement.checkSession(sid);
		Long user_level = userManagement.getUserLevelByID(users_id);
		Rooms myRoom = roommanagement.getRoomById(user_level,
				Long.parseLong(room));

		if (myRoom == null)
			throw new Exception("no Room for ID " + room);

		Iterator<Rooms> miter = rtpSessions.keySet().iterator();

		while (miter.hasNext()) {
			Rooms rooms = miter.next();

			if (rooms.getRooms_id().intValue() == myRoom.getRooms_id()
					.intValue()) {
				rtpSessions.remove(rooms);
				break;
			}

		}

	}
	// ---------------------------------------------------------------------------------------------

}
