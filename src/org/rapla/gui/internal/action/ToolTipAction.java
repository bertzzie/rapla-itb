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

import javax.swing.JMenuItem;

import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaAction;

public class ToolTipAction extends RaplaAction {
    
   public static final String CONFIG_ENTRY = "org.rapla.showTooltips";
   
   public ToolTipAction(RaplaContext sm) throws RaplaException {
        super( sm );
        this.setEnabled( true);
        putValue(NAME,getString("show_tips"));        
        //putValue(SMALL_ICON,getIcon("icon.unchecked"));
    }

    public void actionPerformed(ActionEvent evt) {

    	JMenuItem toolTip = (JMenuItem)evt.getSource();
    	if(toolTip.isSelected()) {
    		toolTip.setSelected(false);
            javax.swing.ToolTipManager.sharedInstance().setEnabled(false);
            toolTip.setIcon(getIcon("icon.unchecked"));
    	}
    	else {
    		toolTip.setSelected(true);
            javax.swing.ToolTipManager.sharedInstance().setEnabled(true);
            toolTip.setIcon(getIcon("icon.checked"));
    	}
    	
    	if ( isModifyPreferencesAllowed())
    	{
    	    try {
                 Preferences prefs = this.newEditablePreferences();
                 prefs.putEntry( CONFIG_ENTRY, ""+ toolTip.isSelected());
                 getModification().store( prefs);
             } catch (Exception ex) {
                 showException(  ex, null );
             }
    	}
    }
}
