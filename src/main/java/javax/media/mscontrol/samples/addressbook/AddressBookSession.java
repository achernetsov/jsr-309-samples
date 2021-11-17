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
package javax.media.mscontrol.samples.addressbook;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.vxml.VxmlDialog;
import javax.media.mscontrol.vxml.VxmlDialogEvent;
import javax.servlet.ServletException;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

public class AddressBookSession  {
	
	// Transmitted by the SipServlet
	final SipSession mySipSession;
	final SipFactory myFactory;

	// JSR 309 Elements
	MediaSession myMediaSession;
	static MsControlFactory myMsControlFactory;
	NetworkConnection myNetworkConnection;
	VxmlDialog myDialog;
	Map<String, Object> params;
	DialogState myState = DialogState.IDLE;
	
	private static String VXML_URL = "http://vxmlserver/addressbook.vxml";
	static Logger log = Logger.getLogger(AddressBookSession.class);
	
	
	public AddressBookSession(SipSession aSipSession, SipFactory aFactory) {
		mySipSession = aSipSession;
		myFactory = aFactory;
	}

	public void init(final SipServletRequest req) throws ServletException {
		try {
			// First, create a MediaSession that will host the media objects
			myMediaSession = myMsControlFactory.createMediaSession();

			// Create a NetworkConnection that will handle the UA's RTP streams
			myNetworkConnection = myMediaSession.createNetworkConnection(NetworkConnection.BASIC);
			// Get the RTP port manager
			final SdpPortManager myPortMgr = myNetworkConnection.getSdpPortManager();
			// Register a listener, to define what we'll do when the connection is setup.
			MediaEventListener<SdpPortManagerEvent> myNetworkConnectionListener = new MediaEventListener<SdpPortManagerEvent>() {
				public void onEvent(SdpPortManagerEvent event) {
					try {
						if (SdpPortManagerEvent.ANSWER_GENERATED.equals(event.getEventType())) {
							// The NetworkConnection has been setup properly.
							initDialog();
							// send a 200 OK, with negociated SDP attached
							byte[] sdpAnswer = event.getMediaServerSdp();
							SipServletMessage msg = req.createResponse(200, "OK");
							msg.setContent(sdpAnswer, "application/sdp");
							msg.send();
						} else {
							// sdp not accepted
							req.createResponse(500, "Unsupported Media Type").send();
						}
					} catch (Exception e) {
						myMediaSession.release();
					}
				}
			};
			myPortMgr.addListener(myNetworkConnectionListener);
			// Send the incoming SDP to the media server
			myPortMgr.processSdpOffer(req.getRawContent());
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	public void initDialog() throws Exception {
		myDialog = myMediaSession.createVxmlDialog(null);
		myDialog.addListener(new AddressBookVxmlListener());
		myNetworkConnection.join(Joinable.Direction.DUPLEX, myDialog); 
		myDialog.prepare(new URL(VXML_URL), null, null);
	}

	/** 
	 * Starts the address book VXML dialog
	 */
	public void startDialog(Map<String, Object> params) {
		// If the dialog is already prepared, start right away.
		if (myState == DialogState.PREPARED)
			myDialog.start(params);
		// else, save the params, and wait for the dialog to be prepared.
		else {
			setState(DialogState.START_REQUESTED);
			this.params = params;
		}
	}

	/** 
	 * Terminates the address book VXML dialog
	 */
	public void terminateDialog() {
		myDialog.terminate(true);
	}
	
	/** 
	 * Sends an Invite no sdp to the transferee
	 */
	public void transferDialog(String uri) throws ServletParseException, IOException {
		SipServletRequest request = myFactory.createRequest(mySipSession.getApplicationSession(), "INVITE", "sip:addressbook@sip-as-uri:5060", uri);
	    request.send();
	}
	
	private void setState(DialogState aState) {
		log.info("Moving from state " + myState + " to state "+  aState);
		myState = aState;
	}
	
	/**
	 * Listens to the VXMLDialog events
	 */
	class AddressBookVxmlListener implements MediaEventListener<VxmlDialogEvent> {
		public void onEvent(VxmlDialogEvent anEvent) {
			if (anEvent.getEventType().equals(VxmlDialogEvent.PREPARED)) {
				if (myState == DialogState.IDLE) {
					setState(DialogState.PREPARED);
				} else if (myState == DialogState.START_REQUESTED) {
					myDialog.start(params);
				}
			} 
			else if (anEvent.getEventType().equals(VxmlDialogEvent.STARTED)) {
				setState(DialogState.STARTED);
			} 
			else if (anEvent.getEventType().equals(VxmlDialogEvent.EXITED)) {
				setState(DialogState.TERMINATED);
				myMediaSession.release();
			} 
			else if (anEvent.getEventType().equals(VxmlDialogEvent.MIDCALL_EVENT_RECEIVED)) {
				// FIXME Need to fix the transfer event informations
				// eg: evt_name, type of transfer, transferee uri
				if(anEvent.getEventName().equals("dialogtransfer")){
					setState(DialogState.TRANSFERRING);
					try {
						transferDialog("TRANSFER_URI");
					} catch (ServletParseException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} 
			else
				log.error("Unexpected event: "+anEvent);
		}
	}

	public DialogState getMyState() {
		return myState;
	}

}
