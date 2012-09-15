
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
package org.rapla.plugin.defaultwizard;

import java.awt.Component;
import java.util.Collection;
import java.util.Date;

import javax.swing.Icon;

import org.rapla.components.util.DateTools;
import org.rapla.components.util.TimeInterval;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.ReservationWizard;

/** This ReservationWizard displays no wizard and directly opens a ReservationEdit Window
*/
public class DefaultWizard extends RaplaGUIComponent implements ReservationWizard {
    public DefaultWizard(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    public void start(Component component,CalendarModel model) throws RaplaException {
    	DynamicType type = model.guessNewEventType();
    	Collection<Allocatable> markedAllocatables = model.getMarkedAllocatables();
    	Collection<TimeInterval> markedIntervals = model.getMarkedIntervals();
		Date startDate = null;
    	Date endDate = null;
    	if ( markedIntervals.size() > 0)
    	{
    		TimeInterval first = markedIntervals.iterator().next();
    		startDate = first.getStart();
    		endDate = first.getEnd();
    	}
        if ( startDate == null )
        {
        	Date selectedDate = model.getSelectedDate();
			if ( selectedDate == null)
        	{
        		selectedDate = getQuery().today();
        	}
            Date time = new Date (DateTools.MILLISECONDS_PER_HOUR * getCalendarOptions().getWorktimeStart());
            startDate = getRaplaLocale().toDate(selectedDate,time);
        }
        if ( endDate == null)
        {
        	endDate = new Date(startDate.getTime() + DateTools.MILLISECONDS_PER_HOUR);
        }
        Reservation r = getModification().newReservation();
        if (type != null)
            r.setClassification(type.newClassification());
        Appointment appointment =  getModification().newAppointment(startDate, endDate);
        r.addAppointment(appointment);
        if ( markedAllocatables == null || markedAllocatables.size() == 0)
        {
	        Allocatable[] allocatables = model.getSelectedAllocatables();
	        if ( allocatables.length == 1)
	        {
	            r.addAllocatable( allocatables[0]);
	        }
        }
        else
        {
        	for ( Allocatable alloc: markedAllocatables)
        	{
        		r.addAllocatable( alloc);
        	}
        }
        getReservationController().edit( r );
    }

    public String toString() {
        return getString("reservation.create_without_wizard");
    }


	public Icon getIcon() 
	{
		return null;
	}
}




