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
package javax.media.mscontrol.samples.addressbook;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class AddressBookServlet extends SipServlet {
	final static long serialVersionUID = -1;
	SipFactory factory;
	Map<String, Object> params = new HashMap<String, Object>();

	@Override
	public void init() throws ServletException {
		super.init();
		factory = (SipFactory)getServletContext().getAttribute("javax.servlet.sip.SipFactory");
	}

	@Override
	protected void doInvite(SipServletRequest req) throws ServletException, IOException {
		if(req.isInitial()){
			SipApplicationSession sipApp = factory.createApplicationSession();
			// This service uses 3 legs, so to be sure of the persistence of the service
			// the AddressBookSession is stocked as an attribute of the SipApplicationSession 
			// instead of the SipSession
			AddressBookSession service = new AddressBookSession(req.getSession(), factory);
			sipApp.setAttribute("address-book", service);
			service.init(req);
		}
	}
	
	@Override
	protected void doSuccessResponse(SipServletResponse resp) throws ServletException, IOException {
		final AddressBookSession service = (AddressBookSession) resp.getApplicationSession().getAttribute("address-book");
		// ACK the third leg and terminate the address book VXML dialog
		resp.createAck().send();
		service.terminateDialog();
	}

	@Override
	protected void doAck(SipServletRequest req) throws ServletException, IOException {
		final AddressBookSession service = (AddressBookSession) req.getApplicationSession().getAttribute("address-book");
		service.startDialog(params);
		
	}

	@Override
	protected void doBye(SipServletRequest req) throws ServletException, IOException {
		SipApplicationSession sipApp = req.getApplicationSession();
		final AddressBookSession service = (AddressBookSession) sipApp.getAttribute("address-book");
		if(!service.getMyState().equals(DialogState.TRANSFERRING)){
			// If a Bye is received before the reception of the transfer event 
			// the address book VXML dialog is stopped
			service.terminateDialog();
		}
		// Needs to send BYE request to the other legs
	}
}
