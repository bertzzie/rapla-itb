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
package org.rapla.entities.configuration;

import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.Named;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;

/** Preferences store user-specific Information.
    You can store arbitrary configuration objects under unique role names.
    Each role can contain 1-n configuration entries.
    @see org.rapla.entities.User
 */
public interface Preferences extends Entity<Preferences>,RaplaObject,Ownable, Named {
    final RaplaType TYPE = new RaplaType(Preferences.class, "preferences");
    /** puts a new configuration entry to the role.*/
    void putEntry(String role,RaplaObject entry);

    void putEntry(String role,String entry);
    <T> T getEntry(String role ) throws EntityNotFoundException;
    boolean hasEntry(String role);

    String getEntryAsString(String role);
    String getEntryAsString(String role, String defaultValue);
    boolean getEntryAsBoolean(String role, boolean defaultValue);
    int getEntryAsInteger(String role, int defaultValue);
    /** returns if there are any preference-entries */
    boolean isEmpty();
}












