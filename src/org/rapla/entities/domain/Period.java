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
package org.rapla.entities.domain;

import java.util.*;

import org.rapla.entities.Entity;
import org.rapla.entities.Named;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
/**
Most universities and schools are planning for fixed periods/terms
rather than arbitrary dates. Rapla provides support for this periods.
*/
public interface Period extends Entity<Period>,RaplaObject,Comparable<Period>,Named {
    final RaplaType TYPE = new RaplaType(Period.class, "period");
    
    Date getStart();
    Date getEnd();
    int getWeeks();
    String getName();

    void setStart(Date start);
    void setEnd(Date end);
    void setName(String name);

    boolean contains(Date date);
    /** returns the week of the specified date relative to the period.
        @throws NoSuchElementException if the period doesn't contain the date
     */
    int weekOf(Date date);
    String toString();


    public static Period[] PERIOD_ARRAY = new Period[0];
}








