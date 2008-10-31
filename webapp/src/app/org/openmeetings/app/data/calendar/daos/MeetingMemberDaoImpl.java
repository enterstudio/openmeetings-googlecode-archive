package org.openmeetings.app.data.calendar.daos;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openmeetings.app.data.basic.Configurationmanagement;
import org.openmeetings.app.data.basic.Languagemanagement;
import org.openmeetings.app.data.user.Addressmanagement;
import org.openmeetings.app.data.user.Usermanagement;
import org.openmeetings.app.hibernate.beans.adresses.Adresses;
import org.openmeetings.app.hibernate.beans.calendar.Appointment;
import org.openmeetings.app.hibernate.beans.calendar.AppointmentCategory;
import org.openmeetings.app.hibernate.beans.calendar.MeetingMember;
import org.openmeetings.app.hibernate.utils.HibernateUtil;

public class MeetingMemberDaoImpl {
	
	private static final Log log = LogFactory.getLog(Configurationmanagement.class);

	private MeetingMemberDaoImpl() {
	}

	private static MeetingMemberDaoImpl instance = null;

	public static synchronized MeetingMemberDaoImpl getInstance() {
		if (instance == null) {
			instance = new MeetingMemberDaoImpl();
		}

		return instance;
	}
	
	public MeetingMember getMeetingMemberById(Long meetingMemberId) {
		try {
			log.debug("getAppointmentCategoryById: "+ meetingMemberId);
			
			String hql = "select app from MeetingMember app " +
					"WHERE app.deleted != :deleted " +
					"AND app.meetingMemberId = :meetingMemberId";
			
			Object idf = HibernateUtil.createSession();
			Session session = HibernateUtil.getSession();
			Transaction tx = session.beginTransaction();
			Query query = session.createQuery(hql);
			query.setString("deleted", "true");
			query.setLong("meetingMemberId",meetingMemberId);
			
			MeetingMember meetingMember = (MeetingMember) query.uniqueResult();
			tx.commit();
			HibernateUtil.closeSession(idf);
			
			return meetingMember;
		} catch (HibernateException ex) {
			log.error("[getMeetingMemberById]: " + ex);
		} catch (Exception ex2) {
			log.error("[getMeetingMemberById]: " + ex2);
		}
		return null;
	}
	
	public MeetingMember updateMeetingMember(MeetingMember meetingMember) {
		if (meetingMember.getMeetingMemberId() > 0) {
			try {
				Object idf = HibernateUtil.createSession();
				Session session = HibernateUtil.getSession();
				Transaction tx = session.beginTransaction();
				session.update(meetingMember);
				tx.commit();
				HibernateUtil.closeSession(idf);
				return meetingMember;
			} catch (HibernateException ex) {
				log.error("[updateMeetingMember] ",ex);
			} catch (Exception ex2) {
				log.error("[updateMeetingMember] ",ex2);
			}
		} else {
			log.error("[updateUser] "+"Error: No MeetingMemberId given");
		}
		return null;
	}
	
	public Long updateMeetingMember(Long meetingMemberId, String firstname, String lastname, 
			 String memberStatus, String appointmentStatus, 
			 Long appointmentId, Long userid, String email) {
		try {
			
			
			MeetingMember gm = this.getMeetingMemberById(meetingMemberId);
			/*
			if (gm == null) {
				log.debug("ALERT Object with ID: "+ MeetingMemberId +" does not exist yet");
				return null;
			}*/
									
			gm.setFirstname(firstname);
			gm.setLastname(lastname);
			
			//gm.setLanguageId(Languagemanagement.getInstance().getFieldLanguageById(languageId));
			gm.setMemberStatus(memberStatus);
			gm.setAppointmentStatus(appointmentStatus);
			gm.setAppointment(AppointmentDaoImpl.getInstance().getAppointmentById(appointmentId));	
			gm.setDeleted("false");
			gm.setUpdatetime(new Date());
			gm.setUserid(Usermanagement.getInstance().getUser(userid));
			gm.setEmail(email);
			
			Object idf = HibernateUtil.createSession();
			Session session = HibernateUtil.getSession();
			Transaction tx = session.beginTransaction();
			
			session.update(gm);

			tx.commit();
			HibernateUtil.closeSession(idf);
			
			return meetingMemberId;
		} catch (HibernateException ex) {
			log.error("[updateMeetingMember]: ",ex);
		} catch (Exception ex2) {
			log.error("[updateMeetingMember]: ",ex2);
		}
		return null;
	}
	
	public Long addMeetingMember(String firstname, String lastname, String memberStatus,
			String appointmentStatus, Long appointmentId, Long userid, String email) {
		try {
			
			MeetingMember gm = new MeetingMember();
			
			gm.setFirstname(firstname);
			gm.setLastname(lastname);
			gm.setMemberStatus(memberStatus);
			gm.setAppointmentStatus(appointmentStatus);
			gm.setAppointment(AppointmentDaoImpl.getInstance().getAppointmentById(appointmentId));
			gm.setUserid(Usermanagement.getInstance().getUser(userid));
			gm.setEmail(email);
								
			gm.setStarttime(new Date());
			gm.setDeleted("false");
						
			Object idf = HibernateUtil.createSession();
			Session session = HibernateUtil.getSession();
			Transaction tx = session.beginTransaction();
			
			Long group_member_id = (Long)session.save(gm);

			tx.commit();
			HibernateUtil.closeSession(idf);
			
			return group_member_id;
		} catch (HibernateException ex) {
			log.error("[addMeetingMember]: ",ex);
		} catch (Exception ex2) {
			log.error("[addMeetingMember]: ",ex2);
		}
		return null;
	}
	
	public Long deleteMeetingMember(Long meetingMemberId) {
		try {
			
			MeetingMember gm = this.getMeetingMemberById(meetingMemberId);
			
			log.debug("ac: "+gm);
			
			if (gm == null) {
				log.debug("Already deleted / Could not find: "+meetingMemberId);
				//return;
			}
			gm.setUpdatetime(new Date());
			gm.setDeleted("true");
			
			Object idf = HibernateUtil.createSession();
			Session session = HibernateUtil.getSession();
			Transaction tx = session.beginTransaction();
			session.update(gm);
						
			tx.commit();
			HibernateUtil.closeSession(idf);
			return meetingMemberId;
		} catch (HibernateException ex) {
			log.error("[deleteAppointementCategory]: " + ex);
		} catch (Exception ex2) {
			log.error("[deleteAppointementCategory]: " + ex2);
		}
		return null;
	}

}