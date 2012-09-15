/*--------------------------------------------------------------------------*
  | Copyright (C) 2006 Christopher Kohlhaas                                  |
  |                                                                          |
  | This program is free software; you can redistribute it and/or modify     |
  | it under the terms of the GNU General Public License as published by the |
  | Free Software Foundation. A copy of the license has been included with   |
  | these distribution in the COPYING file, if not go to www.fsf.org .       |
  |                                                                          |
  | As a special exception, you are granted the permissions to link this     |
  | program with every library, which license fulfills the Open Source       |
  | Definition as published by the Open Source Initiative (OSI).             |
  *--------------------------------------------------------------------------*/

package org.rapla.storage.xml;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;


public class ReservationWriter extends ClassifiableWriter {
    public ReservationWriter(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    protected void printReservation(Reservation r) throws IOException,RaplaException {
        openTag("rapla:reservation");
        printId(r);
        printVersion( r);
        printOwner(r);
        printTimestamp(r);
        closeTag();
        //      System.out.println(((Entity)r).getId() + " Name: " + r.getName() +" User: " + r.getUser());
        printClassification(r.getClassification());
        {
            Appointment[] appointments = r.getAppointments();
            for (int i = 0; i< appointments.length; i ++) {
                printAppointment(appointments[i]);
            }
        }

        Allocatable[] allocatables = r.getAllocatables();
        // Print allocatables that dont have a restriction
        for (int i=0; i< allocatables.length; i ++) {
            Allocatable allocatable = allocatables[i];
            if (r.getRestriction( allocatable ).length > 0 ) 
            {
                continue;
            }
            openTag("rapla:allocate");
            printIdRef( allocatable );
            closeElementTag();
        }

        closeElement("rapla:reservation");
    }
    
    public void writeObject( RaplaObject object ) throws IOException, RaplaException
    {
        printReservation( (Reservation) object);
    }

    protected void printAppointment(Appointment appointment) throws IOException,RaplaException {
        openTag("rapla:appointment");
        if (isIdOnly()) {
            printId( appointment );
        } 
        printVersion( appointment);

        att("start-date",dateTimeFormat.formatDate( appointment.getStart()));

        if (appointment.isWholeDaysSet()) {
            boolean bCut = appointment.getEnd().after(appointment.getStart());
            att("end-date",dateTimeFormat.formatDate(appointment.getEnd(),bCut));
        } else {
            att("start-time",dateTimeFormat.formatTime( appointment.getStart()));
            att("end-date",dateTimeFormat.formatDate( appointment.getEnd()));
            att("end-time",dateTimeFormat.formatTime( appointment.getEnd()));
        }

        Reservation reservation = appointment.getReservation();
		Allocatable[] allocatables = reservation.getRestrictedAllocatables(appointment);
        if (appointment.getRepeating() == null && allocatables.length == 0) 
        {
            closeElementTag();
        }
        else
        {
            closeTag();
            if (appointment.getRepeating() != null) {
                printRepeating(appointment.getRepeating());
            }
            for (int i=0; i< allocatables.length; i ++) {
                Allocatable allocatable = allocatables[i];
                openTag("rapla:allocate");
                printIdRef( allocatable );
                closeElementTag();
            }
            closeElement("rapla:appointment");
        }
    }

    private void printRepeating(Repeating r) throws IOException {
        openTag("rapla:repeating");
        if (r.getInterval()!=1)
            att("interval",String.valueOf(r.getInterval()));
        att("type",r.getType().toString());
        if (r.isFixedNumber()) {
            att("number",String.valueOf(r.getNumber()));
        } else {
            if (r.getEnd() != null)
                att("end-date"
                    ,dateTimeFormat.formatDate(r.getEnd(),true));
        }
        Date[] exceptions = r.getExceptions();
        if (exceptions.length==0) {
            closeElementTag();
            return;
        }
        closeTag();
        for (int i=0;i<exceptions.length;i++) {
            openElement("rapla:exception");
            openTag("rapla:date");
            att("date",dateTimeFormat.formatDate( exceptions[i]));
            closeElementTag();
            closeElement("rapla:exception");
        }
        closeElement("rapla:repeating");
    }

    void printReservations() throws IOException, RaplaException {
        openElement("rapla:reservations");
        Iterator<Reservation> it = cache.getIterator(Reservation.class);
        while (it.hasNext()) {
            printReservation(it.next());
        }
        closeElement("rapla:reservations");
    }



}



