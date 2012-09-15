/*--------------------------------------------------------------------------*
 | Copyright (C) 2006  Christopher Kohlhaas                                 |
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

package org.rapla.gui.edit.reservation.test;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.rapla.client.ClientService;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.gui.ReservationController;
import org.rapla.gui.ReservationEdit;
import org.rapla.gui.internal.edit.reservation.ReservationControllerImpl;
import org.rapla.gui.tests.GUITestCase;

public final class ReservationControllerTest extends GUITestCase
{
    public ReservationControllerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ReservationControllerTest.class);
    }

    public void testMain() throws Exception {
        ClientService clientService = getClientService();
        Reservation[] reservations = clientService.getFacade().getReservationsForAllocatable(null,null,null,null);
        ReservationControllerImpl c = (ReservationControllerImpl)
            clientService.getContext().lookup(ReservationController.ROLE);
        c.edit(reservations[0]);
        getLogger().info("ReservationController started");
    }

    public void testPeriodChange() throws Exception {
        Period[] periods = getFacade().getPeriods();
        getFacade().removeObjects( periods );
        Thread.sleep(500);
        ClientService clientService = getClientService();
        Reservation[] reservations = clientService.getFacade().getReservationsForAllocatable(null,null,null,null);
        ReservationControllerImpl c = (ReservationControllerImpl)
            clientService.getContext().lookup(ReservationController.ROLE);
        c.edit(reservations[0]);
        getLogger().info("ReservationController started");
        ReservationEdit editor = c.getEditWindows()[0];
        Date startDate = new Date();
        editor.addAppointment(startDate, new Date(startDate.getTime() + DateTools.MILLISECONDS_PER_DAY), RepeatingType.WEEKLY, 1);
        editor.save();
        Period period = getFacade().newPeriod();
        period.setStart( startDate );
        period.setEnd( new Date(startDate.getTime() + 3* DateTools.MILLISECONDS_PER_DAY) );
        getFacade().store( period );
        Thread.sleep(500);
    }
    public static void main(String[] args) {
        new ReservationControllerTest(ReservationControllerTest.class.getName()
                               ).interactiveTest("testMain");
    }
}

