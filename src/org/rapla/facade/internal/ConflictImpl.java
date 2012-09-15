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
package org.rapla.facade.internal;

import java.util.Locale;

import org.rapla.entities.Named;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.Conflict;

/**
 * A conflict is the allocation of the same resource at the same time by different
 * reservations. There's one conflict for each resource and each overlapping of
 * two allocations. So if there are 3 reservations that allocate the same 2 resources
 * on 2 days of the week, then we got ( 3 * 2 ) *  2 * 2 = 24 conflicts. Thats
 * 3 reservations, each conflicting with two other 2 reservations on 2 days with 2 resources.
 *
 * @version 1.0
 * @author Christopher Kohlhaas
 */

public class ConflictImpl implements Named, Conflict
{
    static public final RaplaType TYPE = new RaplaType(ConflictImpl.class,"conflict");

    Reservation reserv1;
    Appointment app1;
    Allocatable allocatable;
    Reservation reserv2;
    User user2;
    Appointment app2;

    public ConflictImpl(Reservation reserv1,
                    Appointment app1,
                    Allocatable allocatable,
                    Reservation reserv2,
                    User user2,
                    Appointment app2)
    {
        this.reserv1 = reserv1;
        this.app1 = app1;
        this.allocatable = allocatable;
        this.reserv2 = reserv2;
        this.user2 = user2;
        this.app2 = app2;
    }
    /** @return the first Reservation, that is involed in the conflict.*/
    public Reservation getReservation1() { return reserv1; }
    /** The appointment of the first reservation, that causes the conflict. */
    public Appointment getAppointment1() { return app1; }
    /** @return the allocatable, allocated for the same time by two different reservations. */
    public Allocatable getAllocatable() { return allocatable; }
    /** @return the second Reservation, that is involed in the conflict.*/
    public Reservation getReservation2() { return reserv2; }
    /** @return The User, who created the second Reservation.*/
    public User getUser2() { return user2; }
    /** The appointment of the second reservation, that causes the conflict. */
    public Appointment getAppointment2() { return app2; }

    public static final ConflictImpl[] CONFLICT_ARRAY= new ConflictImpl[] {};

    /**
     * @see org.rapla.entities.Named#getName(java.util.Locale)
     */
    public String getName(Locale locale) {
        return reserv1.getName( locale );
    }

    public boolean contains(Appointment appointment) {
        if ( appointment == null)
            return false;
        if  ( app1 != null && app1.equals( appointment))
            return true;
        if  ( app2 != null && app2.equals( appointment))
            return true;
        return false;
    }

    public boolean equals( Object obj) {
        if  (!(obj instanceof Conflict) || obj == null)
            return false;
        Conflict secondConflict = (Conflict) obj;

        if (!contains( secondConflict.getAppointment1()))
            return false;
        if (!contains( secondConflict.getAppointment2()))
            return false;
        if ( allocatable != null && !allocatable.equals( secondConflict.getAllocatable())) {
            return false;
        }
        return true;
    }
    public RaplaType getRaplaType()
    {
        return Conflict.TYPE;
    }

    public int hashCode() {
        long value = 0;
        if ( allocatable != null)
            value += allocatable.hashCode();
        if ( app1 != null)
            value += app1.hashCode();
        if ( app2 != null)
            value += app2.hashCode();
        return (int)value % Integer.MAX_VALUE;
    }

    public String toString()
    {
        Conflict conflict = (Conflict) this;
        StringBuffer buf = new StringBuffer();

        buf.append( conflict.getAllocatable());
        buf.append( " " );
        buf.append( conflict.getAppointment1());
        buf.append( "'" );
        buf.append( conflict.getReservation1() );
        buf.append( "'" );
        buf.append( "with");
        buf.append( " '" );
        buf.append( conflict.getReservation2() );
        buf.append( "' " );
        buf.append( " owner ");
        User user = conflict.getUser2();
		if ( user != null)
		{
			buf.append( user.getUsername());
		}
        return buf.toString();
    }

}










