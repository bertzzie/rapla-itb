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
package org.rapla.client;

import org.apache.avalon.framework.activity.Startable;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.Container;

/** This service starts and manages the rapla-gui-client.
 */
public interface ClientService extends Startable, Container
{
    String SESSION_MAP = "org.rapla.SessionMap";
    String MAIN_COMPONENT = "org.rapl.MainComponent";

    void addRaplaClientListener(RaplaClientListener listener);
    void removeRaplaClientListener(RaplaClientListener listener);

    ClientFacade getFacade();
    /** setup a component with the services logger,context and servicemanager */
    boolean isRunning();
    /** restarts the GUI without the logout of the user */
    void restartGUI();
    /** restarts the complete Client and displays a new login*/
    void restart();
}
