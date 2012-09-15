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
package org.rapla.gui.internal.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentStartComparator;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;


public class RaplaClipboard extends RaplaGUIComponent implements ModificationListener
{
    public static final String ROLE = RaplaClipboard.class.getName();
    private Appointment appointment;
    private Reservation reservation;
    private boolean wholeReservation;
  


	private Allocatable[] restrictedAllocatables;

    public RaplaClipboard( RaplaContext sm ) throws RaplaException
    {
        super( sm );
        getUpdateModule().addModificationListener( this );
    }

    public void dataChanged( ModificationEvent evt ) throws RaplaException
    {
        if ( appointment == null )
            return;
        if ( evt.isRemoved(  appointment) || evt.isRemoved( appointment.getReservation()))
        {
            clearAppointment();
        }
    }

    private void clearAppointment()
    {
        this.appointment = null;
        this.wholeReservation = false;
        this.restrictedAllocatables = null;
    }

    public void setAppointment( Appointment appointment, boolean wholeReservation, Reservation destReservation, Allocatable[] restrictedAllocatables )
    {
        this.appointment = appointment;
        this.wholeReservation = wholeReservation;
        this.reservation = destReservation;
        this.restrictedAllocatables = restrictedAllocatables;
      
    }
    
    public void setReservation(Reservation copyReservation)
    {
    	ArrayList<Appointment> appointmentList = new ArrayList<Appointment>(Arrays.asList(copyReservation.getAppointments()));
    	Collections.sort( appointmentList, new AppointmentStartComparator());
    	Appointment appointment = appointmentList.get(0);
		setAppointment(appointment,true,copyReservation, copyReservation.getAllocatablesFor(appointment));
    }
    
    public boolean isWholeReservation() 
    {
  		return wholeReservation;
  	}
    
    public Appointment getAppointment()
    {
        return appointment;
    }
    
    
    public Allocatable[] getRestrictedAllocatables()
    {
        return restrictedAllocatables;
    }

	public Reservation getReservation() 
	{
		return reservation;
	}

}

/*
 class AllocationData implements Transferable {
 public static final DataFlavor allocationFlavor = new DataFlavor(java.util.Map.class, "Rapla Allocation");
 private static DataFlavor[] flavors = new DataFlavor[] {allocationFlavor};

 Map data;

 AllocationData(Map data) {
 this.data = data;
 }

 public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
 if (isDataFlavorSupported(flavor))
 return data;
 else
 throw new UnsupportedFlavorException(flavor);
 }

 public DataFlavor[] getTransferDataFlavors() {
 return flavors;
 }

 public boolean isDataFlavorSupported(DataFlavor flavor) {
 return flavor.equals(allocationFlavor);
 }

 }*/

