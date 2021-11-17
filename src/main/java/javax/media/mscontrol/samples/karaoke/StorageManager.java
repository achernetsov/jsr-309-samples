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

/**
 * Centralizing fake data needed in the karaoke sample
 */
public class StorageManager {

	// Dynamic storage system simulation
	/**
	 * Returns single singer record path
	 * 
	 * @param s
	 *            singer
	 */
	public static URI getMyKaraokeRecordURI(Singer s) {
		return null;
	}

	/**
	 * Returns chorus session record path
	 * 
	 * @param cs
	 *            chorus session
	 */
	public static URI getConfKaraokeRecordURI(ChorusSession cs) {
		return null;
	}

	// Presence server simulation
	/**
	 * Returns available friends (suppose retrieved from a server presence)
	 * 
	 * @param s
	 *            singer friends list
	 * @return IP addresses
	 */
	public static Vector<String> getAvailableFriends(Singer s) {
		return null;
	}
}
