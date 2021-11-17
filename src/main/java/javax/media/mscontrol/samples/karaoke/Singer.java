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
import java.net.URI;

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
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.resource.RTC;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

/**
 * Karaoke participant
 * 
 */
public abstract class Singer {

	private static Logger log = Logger.getLogger(Singer.class);

	// SIP references
	public final KaraokeServlet myKaraokeServlet;
	public SipSession mySipSession;

	// Media Control API instances
	// Required core objects
	public final MediaSession myMediaSession;
	public final NetworkConnection myNetworkConnection;
	public final SdpPortManager mySDPPortSet;
	public MediaGroup myMainMediaGroup;
	// Optional core objects, used for specific service features
	public MediaGroup myPlayerMediaGroup;
	public MediaMixer myMediaMixer;

	// Multi-party karaoke - optional feature
	protected ChorusSession myChorusSession;

	// Common RTC
	public RTC[] rtcPrompt = { MediaGroup.SIGDET_STOPPLAY };
	public RTC[] rtcRecord = { new RTC(Player.PLAY_COMPLETION, Recorder.STOP) };

	/**
	 * Constructor: Instantiate minimal/required JSR 309 core objects
	 */
	public Singer(KaraokeServlet theServlet) throws Exception {
		myKaraokeServlet = theServlet;
		// One MediaSession per Singer
		myMediaSession = MediaSessionManager.createMediaSession();
		myNetworkConnection = myMediaSession.createNetworkConnection(NetworkConnection.BASIC);
		mySDPPortSet = myNetworkConnection.getSdpPortManager();
		/*
		 * main MediaGroup, one per Participant - multi-functions, includes
		 * 'all_resources' (at least Player, Recorder, SignalDetector)
		 */
		myMainMediaGroup = myMediaSession.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
		myMainMediaGroup.getPlayer().addListener(
				new MainMediaGroupPlayerListener());
		myMainMediaGroup.getSignalDetector().addListener(
				new MainMediaGroupSignalDetectorListener());
		myNetworkConnection.join(Joinable.Direction.DUPLEX, myMainMediaGroup);
	}

	/**
	 * Start karaoke service
	 */
	public abstract void startService() throws MsControlException;

	/**
	 * Play a prompt
	 * 
	 * @param stream
	 *            file location
	 */
	public void playPrompt(URI stream) throws MsControlException {
		myMainMediaGroup.getPlayer().play(stream, rtcPrompt,
				Parameters.NO_PARAMETER);
	}

	/**
	 * Connect singer to ChorusSession
	 */
	public void joinMyChorusSession() throws MsControlException {
		myChorusSession.join(myNetworkConnection, this);
		/*
		 * Note that previous join in duplex direction between chorusMediaMixer and
		 * myNetworkConnection implies degradation in the existing connection
		 * between myNetworkConnection and myMainMediaGroup Consequently, now
		 * myMainMediaGroup is joined to myNetworkConnnection in RECV only direction,
		 * i.e. following join command is not required:
		 * myMainMediaGroup.join(Joinable.Direction.RECV, myNetworkConnection);
		 */
		playPrompt(URI.create("/prompt/joinedPleaseWait.3gp"));
	}

	/**
	 * Start ChorusSession on the Singer side (start single singer streams
	 * record)
	 */
	public void recordMeInChorusSession() throws MsControlException {
		myState = State.KaraokeStarted;
		myMainMediaGroup.getRecorder().record(
				StorageManager.getMyKaraokeRecordURI(this), RTC.NO_RTC,
				Parameters.NO_PARAMETER);
	}

	/**
	 * Stop recording singer inside ChorusSession
	 */
	public void stopRecordingMe() throws MsControlException {
		myMainMediaGroup.stop();
		myState = State.SelectChorusListeningOption;
		playPrompt(URI.create("/prompt/listenChorus.3gp"));
	}

	/**
	 * Launch chorus listening
	 * 
	 * @param option
	 *            indicates selected listening option '1' whole chorus session
	 *            singers and original music '2' whole chorus singer without
	 *            music '3' only this singer stream and music '4' only this
	 *            singer stream without music
	 */
	public void listenKaraoke(String option) throws MsControlException,
			MediaConfigException {
		if (myChorusSession == null) { // not required, can only be a
			// single singer if here
			// Single singer
			if (option.equals("1")) {
				// Original audio stream and UA recorded audio stream must
				// be merged through a MediaMixer
				instantiateMix();
				myPlayerMediaGroup.getPlayer().play(
						URI.create("/prompt/karaokeData.3gp"), RTC.NO_RTC,
						Parameters.NO_PARAMETER);
			}
			// In both cases (with or without music), play UA recorded
			// stream
			myMainMediaGroup.getPlayer().play(
					StorageManager.getMyKaraokeRecordURI(this), RTC.NO_RTC,
					Parameters.NO_PARAMETER);
		} else {
			// Inside a ChorusSession
			listenChorus(Integer.valueOf(option));
		}
	}

	/**
	 * Listen to chorus
	 * 
	 * @param option
	 *            listening option '1' play whole chorus, original music and
	 *            whole chorus recorded audio streams '2' play only recorded
	 *            chorus without original karaoke stream (clip) '3' play singer
	 *            stream on karaoke original music '4' play only singer stream
	 * @throws Exception
	 */
	public void listenChorus(int option) throws MediaConfigException,
			MsControlException {
		switch (option) {
		case (1):
			instantiateMix();
			// Play original karaoke
			myMainMediaGroup.getPlayer().play(
					URI.create("/prompt/karaokeData.3gp"), RTC.NO_RTC,
					Parameters.NO_PARAMETER);
			// Play whole chorus singers audio streams
			myPlayerMediaGroup.getPlayer().play(
					StorageManager.getConfKaraokeRecordURI(myChorusSession),
					RTC.NO_RTC, Parameters.NO_PARAMETER);
		case (2):
			myMainMediaGroup.getPlayer().play(
					StorageManager.getConfKaraokeRecordURI(myChorusSession),
					RTC.NO_RTC, Parameters.NO_PARAMETER);
		case (3):
			instantiateMix();
			// Play original karaoke
			myMainMediaGroup.getPlayer().play(
					URI.create("/prompt/karaokeData.3gp"), RTC.NO_RTC,
					Parameters.NO_PARAMETER);
			// Play singer stream
			myPlayerMediaGroup.getPlayer().play(
					StorageManager.getMyKaraokeRecordURI(this), RTC.NO_RTC,
					Parameters.NO_PARAMETER);
		case (4):
			myMainMediaGroup.getPlayer().play(
					StorageManager.getMyKaraokeRecordURI(this), RTC.NO_RTC,
					Parameters.NO_PARAMETER);
		}
	}

	/**
	 * Send SIP BYE to UA
	 */
	public void sendBye() throws IOException {
		myKaraokeServlet.sendBye(mySipSession);
	}

	/**
	 * Participant states and specific behavior for each one
	 */
	enum State {
		Initial, PlayingIntro {
			// Choice between 2 service options:
			// - '1' single singer
			// - '2' mutli-singers ('ChorusSession')
			// - other cases not managed, useless code overload
			public void onSignalDetectorEvent(SignalDetectorEvent event,
					Singer s) throws MsControlException {
				if (event.getSignalString().equalsIgnoreCase("1")) {
					// basic service, single singer session
					s.myState = State.KaraokeStarted;
					((IncomingCallSinger) s).startKaraoke();
				} else if (event.getSignalString().equalsIgnoreCase("2")) {
					// enhanced service, many singers session
					s.myState = State.WaitOtherSingers;
					try {
						((IncomingCallSinger) s).createNewChorusSession();
					} catch (Exception e) {
						log.error("Error while creating new chorus session", e);
						throw new MsControlException("Error while creating new chorus session", e);
					}
				}
			}
		},
		WaitOtherSingers {
			public void onSignalDetectorEvent(SignalDetectorEvent event,
					Singer s) throws MsControlException {
				if (event.getSignalString().equalsIgnoreCase("0")) {
					s.myChorusSession.start();
				}
			}
		},
		KaraokeStarted {
			// for single UA case
			public void onPlayerEvent(PlayerEvent event, Singer s)
					throws MsControlException {
				if (event.getEventType().equals(PlayerEvent.PLAY_COMPLETED)) {
					s.myState = State.SelectListeningOption;
					s.playPrompt(URI.create("/prompt/listen.3gp"));
				}
			}
		},
		SelectListeningOption {
			// single UA case
			public void onSignalDetectorEvent(SignalDetectorEvent event,
					Singer s) throws MsControlException {
				// Launch karaokeListening, with an option retrieved (indicated
				// by DMTF signal)
				try {
					s.myState = State.ListeningKaraoke;
					s.listenKaraoke(event.getSignalString());
				} catch (Exception e) {
					log.error("Error while listening karaoke", e);
					throw new MsControlException("Error while listening karaoke", e);
				}
			}
		},
		SelectChorusListeningOption {
			// chorus session use-case
			public void onSignalDetectorEvent(SignalDetectorEvent event,
					Singer s) throws MsControlException {
				s.myState = State.ListeningKaraoke;
				try {
					s.listenChorus(Integer.valueOf(event.getSignalString()));
				} catch (Exception e) {
					log.error("Error while listening chorus session", e);
					throw new MsControlException("Error while listening chorus session", e);
				}
			}
		},
		ListeningKaraoke {
			public void onPlayerEvent(PlayerEvent event, Singer s)
					throws MsControlException {
				if (event.equals(PlayerEvent.PLAY_COMPLETED)) {
					try {
						// End of service - Release Singer
						s.sendBye();
						s.release("Karaoke listening finished");
					} catch (IOException ioe) {
						log.error("Error while sending bye to UA", ioe);
						throw new MsControlException("Error while sending bye to UA", ioe);
					}
				}
			}
		},
		// Specific OutgoingCallSinger states
		WaitingForRemoteSDP;

		/*
		 * Manage unexpected events implying MediaSession to be released
		 */
		public void onPlayerEvent(PlayerEvent event, Singer s)
				throws MsControlException {
			log.error("Unexpected player event: " + event + " in state "
					+ this + " - releasing");
			//anEvent.getSession().release();
			event.getSource().getMediaSession().release();
		}

		public void onSignalDetectorEvent(SignalDetectorEvent event, Singer s)
				throws MsControlException {
			log.error("Unexpected signal detector event: " + event
					+ " in state " + this + " - releasing");
			event.getSource().getMediaSession().release();
		}

	};

	private State myState = State.Initial;

	public void setMyState(State newState) {
		log.info("Changing state from " + myState + " to " + newState);
		myState = newState;
	}

	public State getMyState() {
		return myState;
	}

	/**
	 * Set up media mixing: second MediaGroup (player resource only),
	 * MediaMixer, and connection from both MediaGroups and NetworkConnection to
	 * the MediaMixer
	 * 
	 * @throws MsControlException
	 * @throws MediaConfigException
	 */
	public void instantiateMix() throws MsControlException,
			MediaConfigException {
		if (myPlayerMediaGroup == null) {
			myPlayerMediaGroup = myMediaSession.createMediaGroup(MediaGroup.PLAYER);
			myMediaMixer = myMediaSession.createMediaMixer(MediaMixer.AUDIO);
		}
		myPlayerMediaGroup.join(Joinable.Direction.SEND, myMediaMixer);
		myMainMediaGroup.join(Joinable.Direction.SEND, myMediaMixer);
		myNetworkConnection.join(Joinable.Direction.RECV, myMediaMixer);
	}

	/**
	 * Terminate Singer session
	 */
	public void release(String msg) {
		log.info("Releasing Singer MediaSession: " + msg);
		myKaraokeServlet.removeSinger(mySipSession);
		if (myChorusSession != null) {
			myChorusSession.bye(this);
		}
		myMediaSession.release();
	}

	/**
	 * MediaGroup PlayerListener
	 */
	class MainMediaGroupPlayerListener implements MediaEventListener<PlayerEvent> {
		public void onEvent(PlayerEvent event) {
			log.debug(event);
			try {
				myState.onPlayerEvent(event, Singer.this);
			} catch (MsControlException msce) {
				release("Error while handling Singer Main MediaGroup PlayerEvent");
			}
		}
	}

	/**
	 * MediaGroup SignalDetectorListener
	 */
	class MainMediaGroupSignalDetectorListener implements
			MediaEventListener<SignalDetectorEvent> {
		public void onEvent(SignalDetectorEvent event) {
			log.debug(event);
			try {
				myState.onSignalDetectorEvent(event, Singer.this);
			} catch (MsControlException msce) {
				release("Error while handling Singer Main MediaGroup SignalDetectorEvent");
			}
		}
	}

}
