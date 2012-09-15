/*--------------------------------------------------------------------------*
 | Copyright (C) 2012 Christopher Kohlhaas                                  |
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
package org.rapla.gui;

import java.awt.Component;

import javax.swing.Icon;

import org.rapla.framework.RaplaException;

public interface ReservationWizard {
	/** Show a new wizard dialog.
        @param owner the owner-component for the wizard dialog
        @param model a reference to the current CalendarModel. Here you can get information about the currently selected objects and times
    */

    void start(Component owner,CalendarModel model) throws RaplaException;
    /** Wizards should override this method to return a meaningful name that will be displayed in the wizard list*/
    String toString();
	Icon getIcon();
   
    
  
}

