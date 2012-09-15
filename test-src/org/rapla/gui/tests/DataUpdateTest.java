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
package org.rapla.gui.tests;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.rapla.RaplaTestCase;
import org.rapla.client.ClientService;
import org.rapla.entities.User;
import org.rapla.facade.ClientFacade;

public class DataUpdateTest extends RaplaTestCase {
    ClientFacade facade;
    ClientService clientService;
    Exception error;

    public DataUpdateTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DataUpdateTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        clientService = (ClientService)
            getContext().lookup(ClientService.class.getName() + "/rapla");
        facade = (ClientFacade)
            getContext().lookup(ClientFacade.ROLE);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testReload() throws Exception{
        error = null;
        User user = facade.getUsers()[0];
        refreshDelayed();
        // So we have to wait for the listener-thread
        Thread.sleep(1500);
        if (error != null)
            throw error;
        assertTrue( "User-list varied during refresh! ", facade.getUsers()[0].equals(user) );
    }

    private void refreshDelayed() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        facade.refresh();
                    } catch (Exception ex) {
                        error = ex;
                    }
                }
            });
    }

}





