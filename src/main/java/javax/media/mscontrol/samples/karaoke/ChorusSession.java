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

import java.net.URI;
import java.util.Vector;

import javax.media.mscontrol.MediaConfigException;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.Joinable;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.RTC;

import org.apache.log4j.Logger;

/**
 * Karaoke mutli-party session
 * 
 */
public class ChorusSession {

	private static Logger log = Logger.getLogger(ChorusSession.class);

	private Vector<Singer> allSingers;

	// JSR 309 core objects associated to the karaoke session
	private final MediaSession chorusMediaSession;
	private final MediaMixer chorusMediaMixer;
	private final MediaGroup chorusMediaGroup;

	private RTC[] rtcPlayRecord = { new RTC(Player.PLAY_COMPLETION,
			Recorder.STOP) };

	/**
	 * Constructor
	 * 
	 * @param ms
	 *            MediaSession managing session specific objects
	 */
	public ChorusSession(MediaSession ms)
			throws MsControlException, MediaConfigException {
		chorusMediaSession = ms;
		chorusMediaMixer = chorusMediaSession
				.createMediaMixer(MediaMixer.AUDIO);
		chorusMediaGroup = chorusMediaSession
				.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
		chorusMediaGroup.getPlayer().addListener(
				(new ChorusMediaGroupPlayerListener()));
		chorusMediaMixer.join(Joinable.Direction.DUPLEX, chorusMediaGroup);
		allSingers = new Vector<Singer>();
	}

	/**
	 * Join ChorusSession
	 */
	public void join(NetworkConnection nc, Singer s) throws MsControlException {
		chorusMediaMixer.join(Joinable.Direction.DUPLEX, nc);
		chorusMediaGroup.getPlayer().play(
				URI.create("/prompt/chorusIntro.3gp"), RTC.NO_RTC,
				Parameters.NO_PARAMETER);
		allSingers.add(s);
	}

	/**
	 * Start chorus
	 */
	public void start() throws MsControlException {
		// Launch karaoke for each Singer of this ChorusSession
		for (Singer aSinger : allSingers)
			aSinger.recordMeInChorusSession();
		// Start playing and recording for the whole chorus session
		chorusMediaGroup.getPlayer().play(
				URI.create("/prompt/karaokeData.3gp"), rtcPlayRecord,
				Parameters.NO_PARAMETER);
		chorusMediaGroup.getRecorder().record(
				StorageManager.getConfKaraokeRecordURI(this), RTC.NO_RTC,
				Parameters.NO_PARAMETER);
	}

	/**
	 * Stop chorus
	 */
	public void stop() throws MsControlException {
		chorusMediaGroup.stop();
		for (Singer aSinger : allSingers)
			aSinger.stopRecordingMe();
	}

	/**
	 * Terminate chorus session (release both chorus session and all singers)
	 * 
	 * @param e
	 *            cause of chorus session end
	 */
	public void terminate(Exception e) {
		log.fatal("Terminate chorus session due to internal service error", e);
		// Release each Singer
		for (Singer aSinger : allSingers)
			aSinger.release("Release due to unexcepted error");
		// Release ChorusSession
		release("Unexpected error in chorus session");
	}

	/**
	 * Remove singer, release chorus MediaSession if there is no more Singer
	 * 
	 * @param s
	 *            member leaving chorus session
	 */
	public void bye(Singer s) {
		allSingers.remove(s);
		if (allSingers.isEmpty()) {
			release("No more singers in chorus");
		}
	}

	/**
	 * Release chorus MediaSession
	 * 
	 * @param msg
	 *            origin of chorus end
	 */
	public void release(String msg) {
		log.info("Releasing chorus session: " + msg);
		chorusMediaSession.release();
	}

	/**
	 * Inner class defining MediaGroup PlayerListener
	 */
	class ChorusMediaGroupPlayerListener implements MediaEventListener<PlayerEvent> {
		public void onEvent(PlayerEvent event) {
			log.debug(event);
			try {
				if (event.getQualifier().equals(PlayerEvent.END_OF_PLAY_LIST)) {
					stop();
				}
			} catch (MsControlException msce) {
				release("Error while handling chorus session player event");
			}

		}
	}
}
