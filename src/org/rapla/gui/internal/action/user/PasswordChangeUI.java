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
package org.rapla.gui.internal.action.user;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.RaplaWidget;


public class PasswordChangeUI extends RaplaGUIComponent
    implements
    RaplaWidget
{
    JPanel panel = new JPanel();
    GridLayout gridLayout1 = new GridLayout();
     // The Controller for this Dialog

    JLabel label1 = new JLabel();
    JLabel label2 = new JLabel();
    JLabel label3 = new JLabel();

    JPasswordField tf1 = new JPasswordField(10);
    JPasswordField tf2 = new JPasswordField(10);
    JPasswordField tf3 = new JPasswordField(10);

    public PasswordChangeUI(RaplaContext sm) throws RaplaException{
        this(sm,true);
    }

    public PasswordChangeUI(RaplaContext sm,boolean askForOldPassword) throws RaplaException{
        super( sm);
        panel.setLayout(gridLayout1);
        gridLayout1.setRows(askForOldPassword ? 3 : 2);
        gridLayout1.setColumns(2);
        gridLayout1.setHgap(10);
        gridLayout1.setVgap(10);
        if (askForOldPassword) {
            panel.add(label1);
            panel.add(tf1);
        }

        panel.add(label2);
        panel.add(tf2);
        panel.add(label3);
        panel.add(tf3);
        label1.setText(getString("old_password") + ":");
        label2.setText(getString("new_password") + ":");
        label3.setText(getString("password_verification") + ":");
    }

    public JComponent getComponent() {
        return panel;
    }

    public char[] getOldPassword() {
        return tf1.getPassword();
    }

    public char[] getNewPassword() {
        return tf2.getPassword();
    }

    public char[] getPasswordVerification() {
        return tf3.getPassword();
    }
}









