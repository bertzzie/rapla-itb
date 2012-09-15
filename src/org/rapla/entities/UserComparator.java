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
package org.rapla.entities;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;



public class UserComparator implements Comparator<User> {
    Locale locale;
    Collator collator;
    public UserComparator(Locale locale) {
        this.locale = locale;
        collator = Collator.getInstance(locale);
    }
    public int compare(User u1,User u2) {
        if ( u1.equals(u2)) return 0;

        int result = collator.compare(
                                      u1.getUsername()
                                      ,u2.getUsername()
                                      );
        if ( result !=0 )
            return result;
        else
            return (u1.hashCode() < u2.hashCode()) ? -1 : 1;
    }
}

