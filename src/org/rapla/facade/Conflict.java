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
package org.rapla.facade;

import org.rapla.entities.Named;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;

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

public interface Conflict extends Named, RaplaObject
{
    static public final RaplaType TYPE = new RaplaType(Conflict.class,"conflict");
    /** @return the first Reservation, that is involed in the conflict.*/
    public Reservation getReservation1();
    /** The appointment of the first reservation, that causes the conflict. */
    public Appointment getAppointment1();
    /** @return the allocatable, allocated for the same time by two different reservations. */
    public Allocatable getAllocatable();
    /** @return the second Reservation, that is involed in the conflict.*/
    public Reservation getReservation2();
    /** @return The User, who created the second Reservation.*/
    public User getUser2();
    /** The appointment of the second reservation, that causes the conflict. */
    public Appointment getAppointment2();

    public static final Conflict[] CONFLICT_ARRAY= new Conflict[] {};

}










