/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas, Bettina Lademann                |
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
package org.rapla.gui.internal.action;
import java.awt.event.ActionEvent;

import org.rapla.client.ClientService;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaAction;


public class RestartRaplaAction extends RaplaAction{
    public RestartRaplaAction(RaplaContext sm) throws RaplaException {
        super(sm);
        putValue(NAME,getString("restart_client"));
        putValue(SMALL_ICON,getIcon("icon.restart"));
    }

    public void actionPerformed(ActionEvent arg0) {
        getService(ClientService.class).restart();
    }


}
