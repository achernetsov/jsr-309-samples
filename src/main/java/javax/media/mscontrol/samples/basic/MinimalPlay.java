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
package javax.media.mscontrol.samples.basic;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.spi.DriverManager;
import java.net.URI;


/**
 * Very basic sample code which play a 3gp file through an established network connection.
 * <br>This code does not use a real SIP interface, it merely points out the places where such an
 * interface should be used.
 */
public class MinimalPlay {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		// ----> in real case, received from sip  
		byte[] REMOTE_SDP = new String(
				"v=0\n" +
				"m=audio 1234 RTP/AVP  0 \n" +
				"c=IN IP4 192.168.145.1\n" +
				"a=rtpmap:0 PCMU/8000\n"
				).getBytes();
		// initialization stage begin
		// create the MsControlFactory
		final MsControlFactory theMsControlFactory = DriverManager.getDrivers().next().getFactory(null);
		// initialization stage end
		
		// instantiate a media session for this call
		final MediaSession myMediaSession = theMsControlFactory.createMediaSession();
		
		// create a media connection from media session
		final NetworkConnection myNetworkConnection = myMediaSession.createNetworkConnection(NetworkConnection.BASIC);
		
		// Get the RTP port manager
		final SdpPortManager myPortMgr = myNetworkConnection.getSdpPortManager();

		MediaEventListener<SdpPortManagerEvent> myNetworkConnectionListerner = new MediaEventListener<SdpPortManagerEvent>() {
			
			public void onEvent(SdpPortManagerEvent event) {
				try {
					if (SdpPortManagerEvent.ANSWER_GENERATED.equals(event.getEventType())) {
						
						@SuppressWarnings("unused")
						byte[] lanswer = event.getMediaServerSdp();
						// <----  send sdp answers through sip  (200 ok)
						
						// wait ack from UAC 
						//----> ACK received
						
						// create MediaGroup with basic media capabilities: file play/record dtmf detection
						MediaGroup myMediaGroup = myMediaSession.createMediaGroup(MediaGroup.PLAYER);
						//join
						myMediaGroup.join(Joinable.Direction.DUPLEX, myNetworkConnection);

						myMediaGroup.getPlayer().addListener(new MediaEventListener<PlayerEvent>() {
							 /**
						     * Invoked to pass the result of play()
						     */
						    public void onEvent(PlayerEvent anEvent) {
						    	if (PlayerEvent.END_OF_PLAY_LIST.equals(anEvent.getQualifier())) {
						    		// play completed 
						    	}
						    	// send bye to the remote UAC
						    	//<-------BYE
						    	// possibly wait for 200 ok
						    	myMediaSession.release();
						    }
						});
						// play an audio video 3gp file
						myMediaGroup.getPlayer().play(new URI("http://hp.com/myFile.3gp"),RTC.NO_RTC,Parameters.NO_PARAMETER);
						
					}
					else {
						// offers rejected by media server
						//send error response (anEvent.getError()) 
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block					
				} 
			}
		};
		// register listener
		myPortMgr.addListener(myNetworkConnectionListerner);
		// modify media connection to get the answer.
		myPortMgr.processSdpOffer(REMOTE_SDP);
		
		// if BYE is received, call myMediaSession.release();		
	}
}
