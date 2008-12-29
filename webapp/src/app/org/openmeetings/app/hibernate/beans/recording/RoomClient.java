package org.openmeetings.app.hibernate.beans.recording;

import java.util.Date;
import java.util.LinkedList;

/**
 * 
 * @hibernate.class table="roomclient"
 * lazy="false"
 *
 */
public class RoomClient {
	 
	private Long roomClientId = null;
	
	/*
	 * login name
	 */
	private String username = "";
	
	/*
	 * a unique id
	 */
	private String streamid = "";
	
	/*
	 * an unique PUBLIC id,
	 * this ID is needed as people can reconnect and will get a new 
	 * streamid, but we need to know if this is still the same user
	 * this Public ID can be changing also if the user does change the 
	 * security token (private SID)
	 * the private  Session ID is not written to the RoomClient-Class
	 * as every instance of the RoomClient is send to all connected users
	 * 
	 */
	private String publicSID = "";
	
	/*
	 * true indicates that this user is Moderating
	 * in Events rooms (only 1 Video) this means that this user is currently 
	 * sharing its video/audio
	 * 
	 */
	private Boolean isMod = false;
	private Date connectedSince;
	private String formatedDate;
	
	/*
	 * the color of the user, only needed in 4x4 Conference, in these rooms each user has its own
	 * color 
	 */
	private String usercolor;
	/*
	 * no longer needed since broadCastId is now the new unique id
	 * 
	 * @deprecated
	 */
	private Integer userpos;
	/*
	 * client IP
	 */
	private String userip;
	/*
	 * client Port
	 */
	private int userport;
	/*
	 * current room idd while conferencing
	 */
	private Long room_id;
	
	private Date roomEnter = null;
	
	/*
	 * this is the id this user is currently using to broadcast a stream
	 * default value is -2 cause otherwise this can due to disconnect
	 */
	private long broadCastID = -2;
	
	/*
	 * some vars _not_ directly connected to the user-record from the database
	 * cause a user is not _forced_ to login he can also be an invited user, so user_id
	 * might be null or 0 even if somebody is already in a conference room
	 * 
	 */
	private Long user_id = null;
	private String firstname = "";
	private String lastname = "";
	private String mail;
	private String lastLogin;
	private String official_code;
	private String picture_uri;
	private String language = "";
	
	/*
	 * these vars are necessary to send notifications from the chatroom of a 
	 * conference to outside of the conference room
	 */
	private Boolean isChatNotification = false;
	private Long chatUserRoomId = null;
	
	/*
	 * avsettings can be:
	 * av - video and audio
	 * a - audio only
	 * v - video only
	 * n - no av only static Image
	 */
	private String avsettings = "";
	
	private String swfurl;
	private Boolean isRecording = false;
	private String roomRecordingName;
	
	public RoomClient() {
		super();
	}
	
	public void setUserObject(Long user_id, String username, String firstname, String lastname) {
		this.user_id = user_id;
		this.username = username;
		this.firstname = firstname;
		this.lastname = lastname;
	}
	
	public void setUserObject(String username, String firstname, String lastname) {
		this.username = username;
		this.firstname = firstname;
		this.lastname = lastname;
	}
	
	public RoomClient(String avsettings, long broadCastID, Long chatUserRoomId,
			Date connectedSince, String firstname, String formatedDate,
			Boolean isChatNotification, Boolean isMod, Boolean isRecording,
			String language, String lastLogin, String lastname, String mail,
			String official_code, String picture_uri, String publicSID,
			Date roomEnter, String roomRecordingName, Long room_id,
			String streamid, String swfurl, Long user_id, String usercolor,
			String userip, String username, int userport, Integer userpos) {
		super();
		this.avsettings = avsettings;
		this.broadCastID = broadCastID;
		this.chatUserRoomId = chatUserRoomId;
		this.connectedSince = connectedSince;
		this.firstname = firstname;
		this.formatedDate = formatedDate;
		this.isChatNotification = isChatNotification;
		this.isMod = isMod;
		this.isRecording = isRecording;
		this.language = language;
		this.lastLogin = lastLogin;
		this.lastname = lastname;
		this.mail = mail;
		this.official_code = official_code;
		this.picture_uri = picture_uri;
		this.publicSID = publicSID;
		this.roomEnter = roomEnter;
		this.roomRecordingName = roomRecordingName;
		this.room_id = room_id;
		this.streamid = streamid;
		this.swfurl = swfurl;
		this.user_id = user_id;
		this.usercolor = usercolor;
		this.userip = userip;
		this.username = username;
		this.userport = userport;
		this.userpos = userpos;
	}
	
	/**
     * 
     * @hibernate.id
     *  column="roomclient_id"
     *  generator-class="increment"
     */
	public Long getRoomClientId() {
		return roomClientId;
	}
	public void setRoomClientId(Long roomClientId) {
		this.roomClientId = roomClientId;
	}

	/**
     * @hibernate.property
     *  column="connected_since"
     *  type="java.util.Date"
     */
	public Date getConnectedSince() {
		return connectedSince;
	}
	public void setConnectedSince(Date connectedSince) {
		this.connectedSince = connectedSince;
	}
	
	/**
     * @hibernate.property
     *  column="is_mod"
     *  type="boolean"
     */
	public Boolean getIsMod() {
		return isMod;
	}
	public void setIsMod(Boolean isMod) {
		this.isMod = isMod;
	}
	
	/**
     * @hibernate.property
     *  column="username"
     *  type="string"
     */
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	/**
     * @hibernate.property
     *  column="streamid"
     *  type="string"
     */
	public String getStreamid() {
		return streamid;
	}
	public void setStreamid(String streamid) {
		this.streamid = streamid;
	}

	/**
     * @hibernate.property
     *  column="formated_date"
     *  type="string"
     */
	public String getFormatedDate() {
		return formatedDate;
	}
	public void setFormatedDate(String formatedDate) {
		this.formatedDate = formatedDate;
	}

	/**
     * @hibernate.property
     *  column="usercolor"
     *  type="string"
     */
	public String getUsercolor() {
		return usercolor;
	}
	public void setUsercolor(String usercolor) {
		this.usercolor = usercolor;
	}

	/**
     * @hibernate.property
     *  column="userpos"
     *  type="int"
     */
	public Integer getUserpos() {
		return userpos;
	}
	public void setUserpos(Integer userpos) {
		this.userpos = userpos;
	}

	/**
     * @hibernate.property
     *  column="userip"
     *  type="string"
     */
	public String getUserip() {
		return userip;
	}
	public void setUserip(String userip) {
		this.userip = userip;
	}

	/**
     * @hibernate.property
     *  column="swfurl"
     *  type="string"
     */
	public String getSwfurl() {
		return swfurl;
	}
	public void setSwfurl(String swfurl) {
		this.swfurl = swfurl;
	}
	
	/**
     * @hibernate.property
     *  column="userport"
     *  type="int"
     */
	public int getUserport() {
		return userport;
	}
	public void setUserport(int userport) {
		this.userport = userport;
	}
	
	/**
     * @hibernate.property
     *  column="firstname"
     *  type="string"
     */
	public String getFirstname() {
		return firstname;
	}
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	/**
     * @hibernate.property
     *  column="language"
     *  type="string"
     */
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
     * @hibernate.property
     *  column="last_login"
     *  type="string"
     */
	public String getLastLogin() {
		return lastLogin;
	}
	public void setLastLogin(String lastLogin) {
		this.lastLogin = lastLogin;
	}

	/**
     * @hibernate.property
     *  column="lastname"
     *  type="string"
     */
	public String getLastname() {
		return lastname;
	}
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	/**
     * @hibernate.property
     *  column="mail"
     *  type="string"
     */
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}

	/**
     * @hibernate.property
     *  column="official_code"
     *  type="string"
     */
	public String getOfficial_code() {
		return official_code;
	}
	public void setOfficial_code(String official_code) {
		this.official_code = official_code;
	}

	/**
     * @hibernate.property
     *  column="picture_uri"
     *  type="string"
     */
	public String getPicture_uri() {
		return picture_uri;
	}
	public void setPicture_uri(String picture_uri) {
		this.picture_uri = picture_uri;
	}

	/**
     * @hibernate.property
     *  column="user_id"
     *  type="long"
     */
	public Long getUser_id() {
		return user_id;
	}
	public void setUser_id(Long user_id) {
		this.user_id = user_id;
	}

	/**
     * @hibernate.property
     *  column="room_id"
     *  type="long"
     */
	public Long getRoom_id() {
		return room_id;
	}
	public void setRoom_id(Long room_id) {
		this.room_id = room_id;
	}

	/**
     * @hibernate.property
     *  column="room_enter"
     *  type="java.util.Date"
     */
	public Date getRoomEnter() {
		return roomEnter;
	}
	public void setRoomEnter(Date roomEnter) {
		this.roomEnter = roomEnter;
	}

	/**
     * @hibernate.property
     *  column="is_chat_notification"
     *  type="boolean"
     */
	public Boolean getIsChatNotification() {
		return isChatNotification;
	}
	public void setIsChatNotification(Boolean isChatNotification) {
		this.isChatNotification = isChatNotification;
	} 

	/**
     * @hibernate.property
     *  column="chat_user_room_id"
     *  type="long"
     */
	public Long getChatUserRoomId() {
		return chatUserRoomId;
	}
	public void setChatUserRoomId(Long chatUserRoomId) {
		this.chatUserRoomId = chatUserRoomId;
	}

	/**
     * @hibernate.property
     *  column="is_recording"
     *  type="boolean"
     */
	public Boolean getIsRecording() {
		return isRecording;
	}
	public void setIsRecording(Boolean isRecording) {
		this.isRecording = isRecording;
	}

	/**
     * @hibernate.property
     *  column="room_recording_name"
     *  type="string"
     */
	public String getRoomRecordingName() {
		return roomRecordingName;
	}
	public void setRoomRecordingName(String roomRecordingName) {
		this.roomRecordingName = roomRecordingName;
	}

	/**
     * @hibernate.property
     *  column="avsettings"
     *  type="string"
     */
	public String getAvsettings() {
		return avsettings;
	}
	public void setAvsettings(String avsettings) {
		this.avsettings = avsettings;
	}

	/**
     * @hibernate.property
     *  column="broadcast_id"
     *  type="long"
     */
	public long getBroadCastID() {
		return broadCastID;
	}
	public void setBroadCastID(long broadCastID) {
		this.broadCastID = broadCastID;
	}

	/**
     * @hibernate.property
     *  column="public_sid"
     *  type="string"
     */
	public String getPublicSID() {
		return publicSID;
	}
	public void setPublicSID(String publicSID) {
		this.publicSID = publicSID;
	}

}