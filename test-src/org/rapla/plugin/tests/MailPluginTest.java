/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.tests;

import java.util.Locale;

import org.rapla.MockMailer;
import org.rapla.ServletTestBase;
import org.rapla.components.mail.MailInterface;
import org.rapla.facade.ClientFacade;
import org.rapla.plugin.mail.internal.RaplaMailToUserOnServer;
import org.rapla.server.ServerService;

/** listens for allocation changes */
public class MailPluginTest extends ServletTestBase {
    ServerService raplaServer;

    ClientFacade facade1;
    Locale locale;


    public MailPluginTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
       
       // start the server
        raplaServer = (ServerService)
            getContext().lookup(ServerService.ROLE + "/" + getStorageName());
        // start the client service
        facade1 = (ClientFacade)
            getContext().lookup(ClientFacade.ROLE + "/remote-facade");
        facade1.login("homer","duffs".toCharArray());
        locale = Locale.getDefault();
    }
    
    protected String getStorageName() {
        return "storage-file";
    }
    
    protected void tearDown() throws Exception {
        facade1.logout();
        super.tearDown();
    }
    
    public void test() throws Exception 
    {
        RaplaMailToUserOnServer mail = new RaplaMailToUserOnServer(getContext());
        mail.sendMail( "homer","Subject", "MyBody");
        MockMailer mailMock = (MockMailer) raplaServer.getContext().lookup( MailInterface.ROLE);
        Thread.sleep( 1000);

        assertNotNull( mailMock.getMailBody() );
   
    }
    
 
}

