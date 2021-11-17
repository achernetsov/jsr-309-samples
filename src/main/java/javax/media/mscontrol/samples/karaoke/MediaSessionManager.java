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

import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.spi.DriverManager;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

/**
 * Manage JSR 309 application MediaSessions
 * 
 */
public class MediaSessionManager {

	private static Logger log = Logger.getLogger(MediaSessionManager.class);

	/**
	 * Single static MediaSessionFactory instance
	 */
	static MsControlFactory theMsControlFactory;
	static {
		try {
			theMsControlFactory = DriverManager.getDrivers().next().getFactory(null);
		} catch (Exception e) {
			log.fatal("Cannot create MediaSessionFactory :", e);
			System.exit(0);
		}
	}

	/**
	 * MediaSession factory
	 * 
	 * @return new MediaSession
	 * @throws ServletException
	 */
	public static MediaSession createMediaSession() throws MsControlException {
		return theMsControlFactory.createMediaSession();
	}

}
