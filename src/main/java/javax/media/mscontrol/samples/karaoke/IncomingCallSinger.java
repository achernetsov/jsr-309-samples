/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Copyright (c) 2008-2009 Hewlett-Packard, Inc. All rights reserved.
 * Copyright (c) 2008-2009 Oracle and/or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 * 
 * This code should only be used for further understanding of the
 * specifications and is not of production quality in terms of robustness,
 * scalability etc.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
package javax.media.mscontrol.samples.karaoke;

import java.net.URI;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.RTC;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;

import org.apache.log4j.Logger;

/**
 * Karaoke service user (has join the service by calling it)
 * 
 */
public class IncomingCallSinger extends Singer {

	private static Logger log = Logger.getLogger(IncomingCallSinger.class);

	/**
	 * Constructor - Instantiate required JSR 309 core objects
	 */
	public IncomingCallSinger(final SipServletRequest request, KaraokeServlet theServlet) throws Exception {
		super(theServlet);
		try {

			mySipSession = request.getSession();

			// Set a specific SdpPortManager listener for handling SDP
			// negotiation for an incoming call
			mySDPPortSet.addListener(new MediaEventListener<SdpPortManagerEvent>() {
				public void onEvent(SdpPortManagerEvent event) {
					try {
						// The media server has handled SDP negotiation
						if (event.getEventType().equals(SdpPortManagerEvent.ANSWER_GENERATED)) {
							// Send a 200 OK, with negotiated SDP attached.
							byte[] sdpAnswer = event.getMediaServerSdp();
							SipServletMessage msg = request.createResponse(200, "OK");
							msg.setContent(sdpAnswer, "application/sdp");
							msg.send();
						} else {
							// SDP not accepted
							request.createResponse(500, "Unsupported Media Type").send();
						}
					} catch (Exception e) {
						log.error("Cannot handle IncomingCallSinger SdpPortManagerEvent",e);
						myMediaSession.release();
					}
				}
			});
			// Launch SDP negotiation on MediaServer side
			mySDPPortSet.processSdpOffer(request.getRawContent());
		} catch (Exception e) {
			log.fatal("Cannot create singer as answer to incoming call", e);
			myMediaSession.release();
			throw new ServletException();
		}
	}

	@Override
	public void startService() throws MsControlException {
		setMyState(State.PlayingIntro);
		playPrompt(URI.create("/prompt/intro.3gp"));
	}

	/**
	 * Start karaoke (play and record) - only for Single singer use-case
	 */
	public void startKaraoke() throws MsControlException {
		myMainMediaGroup.getPlayer().play(
				URI.create("/prompt/karaokeData.3gp"), RTC.NO_RTC,
				Parameters.NO_PARAMETER);
		myMainMediaGroup.getRecorder().record(
				StorageManager.getMyKaraokeRecordURI(this), rtcRecord,
				Parameters.NO_PARAMETER);
	}

	/**
	 * Create new multi-party karaoke (ChorusSession), join this singer to it
	 * 
	 * @throws ServletException
	 */
	public void createNewChorusSession() throws Exception {
		myChorusSession = ChorusSessionManager.createChorusSession();
		joinMyChorusSession();
		ChorusSessionManager.inviteFriends(StorageManager.getAvailableFriends(this), myKaraokeServlet, myChorusSession);
	}
}
