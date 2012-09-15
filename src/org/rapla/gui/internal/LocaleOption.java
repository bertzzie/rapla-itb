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

import org.rapla.client.internal.LanguageChooser;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;

public class LocaleOption extends RaplaGUIComponent implements OptionPanel {
    JPanel panel = new JPanel();
    LanguageChooser languageChooser;
    Preferences preferences;
    
    public LocaleOption(RaplaContext sm) throws RaplaException {
        super( sm);
        languageChooser= new LanguageChooser( getLogger(), getContext());
        
        double pre = TableLayout.PREFERRED;
        double fill = TableLayout.FILL;
        panel.setLayout( new TableLayout(new double[][] {{pre, 5, pre,5, pre}, {pre,fill}}));

        panel.add( new JLabel(getString("language") + ": "),"0,0"  );
        panel.add( languageChooser.getComponent(),"2,0");
        //panel.add( new JLabel(getString("seconds")),"4,0"  );
        
    }

    public JComponent getComponent() {
        return panel;
    }
    public String getName(Locale locale) {
        return getString("language");
    }

    public void setPreferences( Preferences preferences) {
        this.preferences = preferences;

    }

    public void show() throws RaplaException {
        String language = preferences.getEntryAsString( RaplaLocale.LANGUAGE_ENTRY,null);
        languageChooser.setSelectedLanguage( language);
    }

    public void commit() {
        String language = languageChooser.getSelectedLanguage();
        preferences.putEntry( RaplaLocale.LANGUAGE_ENTRY,language );
    }


}
