/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
package org.rapla.facade;

import org.rapla.entities.User;
import org.rapla.framework.RaplaException;
/** Encapsulates the methods responsible for authentification.
*/

public interface UserModule {
    /** The login method establishes the connection and loads data.
     * It should clear the password char array.
     * @return false on an invalid login.
     * @throws RaplaException if the connection can't be established.
     */
    boolean login(String username,char[] password) throws RaplaException;

    /** logout of the current user */
    void logout() throws RaplaException;

    /** returns if a session is active. True between a successful login and logout. */
    boolean isSessionActive();


    /** throws an Exception if no user has loged in.
        @return the user that has loged in. */
    User getUser() throws RaplaException;

    /** the admin can switch to another user!*/
    void switchTo(User user);
    /** returns true if the admin has switched to anoter user!*/
    boolean canSwitchBack();

    void changePassword(User user,char[] oldPassword,char[] newPassword) throws RaplaException;
    boolean canChangePassword();
}





