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

import java.awt.FlowLayout;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.action.user.PasswordChangeAction;
import org.rapla.gui.toolkit.RaplaButton;


public class UserOption extends RaplaGUIComponent
    implements
        OptionPanel
{
    JPanel panel = new JPanel();
    RaplaButton changePassword;
    PasswordChangeAction passwordChangeAction;

    public UserOption(RaplaContext sm) throws RaplaException  {
        super(sm);
    }

    private void create() throws RaplaException {
        changePassword = new RaplaButton();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING));
        if (getUserModule().canChangePassword())
            panel.add(changePassword);

        passwordChangeAction = new PasswordChangeAction(getContext(),getComponent(),null);
        passwordChangeAction.changeObject(getUser());
        changePassword.setAction(passwordChangeAction);
    }

    public void show() throws RaplaException {
        if (changePassword == null)
            create();
    }

    public void setPreferences(Preferences preferences) {

    }


    /** does nothing*/
    public void commit() {
    }

    public String getName(Locale locale) {
        return getString("login");
    }

    public JComponent getComponent() {
        return panel;
    }

}






