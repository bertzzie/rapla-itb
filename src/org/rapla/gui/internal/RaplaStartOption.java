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
package org.rapla.gui.internal;

import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;

public class RaplaStartOption extends RaplaGUIComponent implements OptionPanel {
    JPanel panel = new JPanel();
    JTextField calendarName;
    Preferences preferences;
    
    public final static String TITLE = "org.rapla.title";
    
    public RaplaStartOption(RaplaContext sm) throws RaplaException {
        super( sm);
        
        double pre = TableLayout.PREFERRED;
        double fill = TableLayout.FILL;
        panel.setLayout( new TableLayout(new double[][] {{pre, 5, pre,5, pre}, {pre,fill}}));
  
        calendarName = new JTextField();
        addCopyPaste( calendarName);
        calendarName.setColumns(30);
        panel.add( new JLabel(getString("calendarname")),"0,0"  );
        panel.add( calendarName,"2,0");
        calendarName.setEnabled(true);
    }

    public JComponent getComponent() {
        return panel;
    }
    public String getName(Locale locale) {
        return getString("options");
    }

    public void setPreferences( Preferences preferences) {
        this.preferences = preferences;

    }

    public void show() throws RaplaException {
        String name = preferences.getEntryAsString( TITLE,"");
        calendarName.setText(name);
    }

    public void commit() {
        String title = calendarName.getText();
        if ( title.trim().length() > 0)
        {
            preferences.putEntry( TITLE,title );
        }
        else
        {
            preferences.putEntry( TITLE, (String)null);
        }
    }


}
