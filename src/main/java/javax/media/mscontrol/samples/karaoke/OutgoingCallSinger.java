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

import javax.media.mscontrol.MediaConfigException;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

/**
 * Karaoke participant, invited to a ChorusSession by a IncomingCallSinger
 * 
 */
public class OutgoingCallSinger extends Singer {

	private static Logger log = Logger.getLogger(OutgoingCallSinger.class);

	/**
	 * Constructor
	 * 
	 * @param mySession
	 * @param theServlet
	 */
	public OutgoingCallSinger(final String myAddress,
			final KaraokeServlet theServlet, ChorusSession theChorusSession)
			throws Exception {
		super(theServlet);
		myChorusSession = theChorusSession;

		// Set a specific SdpPortManager listener for handling SDP
		// negotiation with media server in the outgoing call use-case
		mySDPPortSet.addListener(new MediaEventListener<SdpPortManagerEvent>() {
			public void onEvent(SdpPortManagerEvent event) {
				try {
					if (event.getEventType().equals(SdpPortManagerEvent.OFFER_GENERATED)
							&& getMyState() == State.Initial) {
						// the media server has allocated a new local SDP for mySDPPortSet						
						setMyState(State.WaitingForRemoteSDP);
						// Send SIP INVITE message with localSDP, get at
						// the same time allocated SipSession
						theServlet.sendInvite(myAddress, event.getMediaServerSdp());
						
					} else if (getMyState() == State.WaitingForRemoteSDP) {
						setMyState(State.WaitOtherSingers);
						// send ACK response
						myKaraokeServlet.sendAck(OutgoingCallSinger.this);
						// join chorusSession
						joinMyChorusSession();
					}

				} catch (Exception e) {
					log.fatal("Cannot handle OutgoingCallSinger SdpPortManagerEvent",e);
					release("Error while handling SdpPortManagerEvent "+ event.getQualifier());
				}
			}
		});
		// Ask for local SDP allocation on the MediaServer side
		mySDPPortSet.generateSdpOffer();
	}

	@Override
	public void startService() throws MsControlException {
		setMyState(State.WaitOtherSingers);
		playPrompt(URI.create("/prompt/friendIntro.3gp"));
	}

	@Override
	public void listenKaraoke(String option) throws MsControlException,
			MediaConfigException {
		listenChorus(Integer.valueOf(option));
	}

	/**
	 * Handle SDP negotiation with MediaServer
	 * 
	 * @param remoteSDP
	 * @param response
	 */
	public void setSdpAnswer(byte[] remoteSDP, SipSession sipSession) throws SdpPortManagerException {
		mySipSession = sipSession;
		mySDPPortSet.processSdpAnswer(remoteSDP);
		setMyState(State.WaitOtherSingers);
		try {
			// send ACK response
			myKaraokeServlet.sendAck(OutgoingCallSinger.this);
			// join chorusSession
			joinMyChorusSession();
		} catch (Exception e) {
			log.fatal("Cannot handle Exception", e);
			release("Cannot handle Exception: "+e);			
		}
	}

}
