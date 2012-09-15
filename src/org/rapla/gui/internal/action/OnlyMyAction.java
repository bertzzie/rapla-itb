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

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;

public class OnlyMyAction extends RaplaAction {
    CalendarSelectionModel model;

    public OnlyMyAction(RaplaContext sm,CalendarSelectionModel model) throws RaplaException {
        super( sm);
        this.model = model;
        this.setEnabled( true);
        putValue(NAME,getString("only_own_reservations"));        
        //putValue(SMALL_ICON,getIcon("icon.filter"));
    }

    public CalendarSelectionModel getModel() {
        return model;
    }

    public void setModel(CalendarSelectionModel model) {
    }

    public void actionPerformed(ActionEvent evt) {
       try {
           boolean isSelected = model.isOnlyCurrentUserSelected();
           if ( !isSelected ) {
               model.selectUser( getUser());
           } else {
               model.selectUser( null );
           }
           firePropertyChange("model",new Object(), model);
       }
       catch (RaplaException ex)
       {
       }
       
    }


}
