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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

/**
 * Karaoke service SipServlet. Handle media service SIP requests
 * 
 */
@SuppressWarnings("serial")
public class KaraokeServlet extends SipServlet {

	private static Logger log = Logger.getLogger(KaraokeServlet.class);

	/**
	 * Factory for outgoing SIP requests objects
	 */
	private final SipFactory sipFactory = (SipFactory) this.getServletContext()
			.getAttribute("javax.servlet.sip.SipFactory");

	/**
	 * Karaoke participants map
	 */
	private static Map<SipSession, Singer> allSingers = new HashMap<SipSession, Singer>();

	// Incoming call use-case
	/**
	 * Initiate the service by negotiating SDP
	 */
	@Override
	protected void doInvite(SipServletRequest request) throws ServletException {
		try {
			// Create a new singer and add it to the singers map
			Singer singer = new IncomingCallSinger(request, this);
			allSingers.put(request.getSession(), singer);
		} catch (Exception e) {
			log.fatal("Error while handling doInvite request");
			throw new ServletException();
		}
	}

	/**
	 * Start the service
	 */
	@Override
	protected void doAck(SipServletRequest req) throws ServletException,
			IOException {
		try {
			// Start karaoke service
			allSingers.get(req.getSession()).startService();
		} catch (Exception e) {
			log.fatal("Error while starting service");
			throw new ServletException();
		}
		
	}

	/**
	 * Send a SIP BYE to the UA
	 */
	protected void sendBye(SipSession theSipSession) throws IOException {
		removeSinger(theSipSession);
		SipServletRequest req = theSipSession.createRequest("BYE");
		req.send();
	}

	// Outgoing call use-case
	/**
	 * Initiate an outgoing call
	 * 
	 * @param friendIpAddress
	 * @param localSDP
	 * @throws Exception
	 */
	public void sendInvite(String friendIpAddress, byte[] localSDP)
			throws Exception {
		SipServletRequest request = sipFactory.createRequest(sipFactory
				.createApplicationSession(), "INVITE", sipFactory
				.createAddress("sip:karaoke@as_sipservlet:5060"), sipFactory
				.createAddress(friendIpAddress));
		request.setContent(localSDP, "application/sdp");
		request.send();
	}

	/**
	 * Modify SDP of the invited friend
	 */
	@Override
	protected void doSuccessResponse(SipServletResponse response)
			throws ServletException {
		// 200 OK success response corresponds to end of SDP negotiation
		if (allSingers.containsKey(response.getSession())) {
			// Suppose that SIP 200 OK message has only one body, containing the
			// negotiated SDP
			try {
				// Set response as SipSession attribute to answer it later
				response.getSession().setAttribute("UNACK_RESPONSE",response);
				((OutgoingCallSinger) allSingers.get(response.getSession()))
						.setSdpAnswer(response.getRawContent(), response.getSession());
			} catch (Exception ioe) {
				log.error("Error while modifying SDP while handling success response", ioe);
				throw new ServletException();
			}
		}
		// else 200 OK success response arrives after a BYE has been sent:
		// nothing to do, media session has already been released
	}

	/**
	 * Send SIP ACK response, add singer to the map
	 * 
	 * @param outgoingCallSinger
	 * @param response
	 */
	public void sendAck(Singer outgoingCallSinger)
			throws IOException {
		SipServletResponse response = (SipServletResponse)((SipSession)allSingers.get(outgoingCallSinger)).getAttribute("UNACK_RESPONSE");
		SipServletMessage ackMessage = response.createAck();
		ackMessage.send();
		allSingers.put(response.getSession(), outgoingCallSinger);
	}

	// For both incoming and outgoing calls use-cases
	/**
	 * Remove Singer hanging up
	 */
	@Override
	protected void doBye(SipServletRequest req) throws ServletException,
			IOException {
		// Release singer
		allSingers.get(req.getSession()).release("User Agent hangs up");
		// Remove it from allSingers map
		removeSinger(req.getSession());
		SipServletMessage msg = req.createResponse(200, "OK");
		msg.send();
	}

	/**
	 * Remove Singer from the singers map
	 * 
	 * @param key
	 */
	public void removeSinger(SipSession key) {
		allSingers.remove(key);
	}

}
