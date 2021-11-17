/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Copyright (c) 2008 Hewlett-Packard, Inc. All rights reserved.
 * Copyright (c) 2008 Oracle and/or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 * 
 * This code should only be used for further understanding of the
 * specifications and is not of production quality in terms of robustness,
 * scalability etc.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
package javax.media.mscontrol.samples.karaoke;

import java.util.Iterator;
import java.util.Vector;

import javax.media.mscontrol.MediaConfigException;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;

import org.apache.log4j.Logger;

/**
 * Manage ChorusSession: allocations, references, release, and friends
 * invitations
 * 
 */
public class ChorusSessionManager {

	private static Logger log = Logger.getLogger(ChorusSessionManager.class);

	/**
	 * ChorusSession factory
	 * 
	 * @return new ChorusSession
	 * @throws Exception
	 */
	public static ChorusSession createChorusSession()
			throws MsControlException, MediaConfigException {
		// One MediaSession per ChorusSession
		MediaSession chorusMediaSession = MediaSessionManager
				.createMediaSession();
		ChorusSession newConf = new ChorusSession(chorusMediaSession);
		return newConf;
	}

	/**
	 * Initiate outgoing calls to available friends
	 * 
	 * @param availableFriendsIpAddress
	 *            user SIP address of available friends (suppose retrieved from
	 *            a presence server for instance)
	 */
	public static void inviteFriends(Vector<String> availableFriendsIpAddress,
			KaraokeServlet karaokeServlet, ChorusSession chorusSession) {
		Iterator<String> i = availableFriendsIpAddress.iterator();
		while (i.hasNext()) {
			try {
				String friendAddress = i.next();
				new OutgoingCallSinger(friendAddress, karaokeServlet,
						chorusSession);
			} catch (Exception e) {
				log.error("Error while creating a new OutgoingCallSinger", e);
			}
		}
	}

}
