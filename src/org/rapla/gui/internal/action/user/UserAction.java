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
package org.rapla.gui.internal.action.user;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;

import org.rapla.client.ClientService;
import org.rapla.entities.User;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.EditComponent;
import org.rapla.gui.RaplaAction;
import org.rapla.gui.internal.edit.EditDialog;


public class UserAction extends RaplaAction {
    Object object;
    Component parent;
    public final int NEW = 1;
    public final int SWITCH_TO_USER = 3;
    int type = NEW;

    public UserAction(RaplaContext sm,Component parent,Point point) throws RaplaException {
        super( sm);
        this.parent = parent;

    }

    public UserAction setNew() {
        type = NEW;
        putValue(NAME, getString("user"));
        putValue(SMALL_ICON, getIcon("icon.new"));
        update();
        return this;
    }

    public UserAction setSwitchToUser() {
        type = SWITCH_TO_USER;
        if (getUserModule().canSwitchBack()) {
            putValue(NAME, getString("switch_back"));
        } else {
            putValue(NAME, getString("switch_to"));
        }
        return this;
    }

    public void changeObject(Object object) {
        this.object = object;
        update();
    }

    private void update() {
        User user = null;
        try {
            user = getUser();
            if (type == NEW) {
                setEnabled(isAdmin());
            } else if (type == SWITCH_TO_USER) {
                setEnabled(getUserModule().canSwitchBack() ||
                           (object != null && isAdmin() && !user.equals(object )));
            }
        } catch (RaplaException ex) {
            setEnabled(false);
            return;
        }

    }

    public void actionPerformed(ActionEvent evt) {
        try {
            if (type == SWITCH_TO_USER) {
            	if (getUserModule().canSwitchBack()) {
                    getUserModule().switchTo(null);
                    (getService( ClientService.class)).restartGUI();
                    putValue(NAME, getString("switch_to"));
                } else if ( object != null ){
                    getUserModule().switchTo((User) object);
                    (getService( ClientService.class)).restartGUI();
                    putValue(NAME, getString("switch_back"));
                }
            } else if (type == NEW) {
                User newUser = (User) getModification().newUser();
                EditComponent ui = getEditController().createUI( newUser);
                EditDialog gui = new EditDialog(getContext(),ui);
                if (gui.start( newUser ,getString("user"), parent) == 0
                    && getUserModule().canChangePassword() )
                    changePassword(newUser,false);
                object = newUser;
            }
        } catch (RaplaException ex) {
            showException(ex, this.parent);
        }
    }

    public void changePassword(User user,boolean showOld) throws RaplaException{
        new PasswordChangeAction(getContext(),parent, null).changePassword( user, showOld);
    }


}
