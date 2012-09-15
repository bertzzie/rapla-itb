/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas, Bettina Lademann                |
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
package org.rapla.gui.internal.action;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Date;

import javax.swing.Icon;

import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationHelper;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.RaplaAction;
import org.rapla.gui.ReservationController;
import org.rapla.gui.ReservationEdit;
import org.rapla.gui.ReservationWizard;

public class AppointmentAction extends RaplaAction {
    public final static int DELETE = 1;
    public final static int COPY = 2;
    public final static int PASTE = 3;
    public final static int CUT = 4;
    public final static int NEW = 5;
    public final static int EDIT = 6;
    public final static int VIEW = 7;
    public final static int CHANGE_ALLOCATABLE = 8;
    public final static int ADD_TO_RESERVATION = 9;
    public final static int PASTE_AS_NEW = 10;
    
    private boolean keepTime = false;
    Component parent;
    Point point;
    int type;
    Date start;
    Date end;
    Collection<Allocatable> allocatables;
    Appointment appointment;

	ReservationEdit reservationEdit;
    ReservationWizard wizard;
    
    public AppointmentAction(RaplaContext sm,Component parent,Point point) throws RaplaException 
    {
        super( sm);
        this.parent = parent;
        this.point = point;
    }

    public AppointmentAction setNew(ReservationWizard wizard,Collection<Allocatable> allocatables,Date start,Date end ) 
    {
        this.start = start;
        this.end = end;
        this.wizard = wizard;
        this.allocatables = allocatables;
        this.type = NEW;
        putValue(NAME,  wizard.toString() );
        Icon icon = wizard.getIcon();
        if ( icon == null)
        {
        	icon = getIcon("icon.new");
        }
		putValue(SMALL_ICON, icon);
/*        
        if ( repeatingType != null)
        {
            putValue(SMALL_ICON, getIcon("icon.new_repeating"));
        }
        else
        {
            putValue(SMALL_ICON, getIcon("icon.single")); 
        }
        */
        boolean canAllocate = canAllocate(start, end, allocatables);
        setEnabled( canAllocate);
        return this;
    }
    
   

	private boolean canAllocate(Date start, Date end,
			Collection<Allocatable> allocatables) 
	{
		boolean canAllocate = true;
        for (Allocatable allo:allocatables)
        {
        	if (!canAllocate( start, end, allo))
        	{
        		canAllocate = false;
        	}
        }
		return canAllocate;
	}

    public AppointmentAction setAddTo(ReservationEdit reservationEdit,Date start,Date end,Collection<Allocatable> allocatables) 
    {
    	this.reservationEdit = reservationEdit;
        this.start = start;
        this.end = end;
        this.allocatables = allocatables;
        this.type = ADD_TO_RESERVATION;
        String name2 = getName(reservationEdit.getReservation());
		
        String value = name2.trim().length() > 0 ? "'" + name2 + "'" : getString("new_reservation");
		putValue(NAME, value);
        putValue(SMALL_ICON, getIcon("icon.new"));
        boolean canAllocate = canAllocate(start, end, allocatables);
        setEnabled( canAllocate);
        return this;
    }

    private boolean canAllocate(Date start, Date end, Allocatable allocatables) {
        if ( allocatables == null) {
            return true;
        }
        try {
            return allocatables.canAllocate( getUser(), start, end, getQuery().today() );
        } catch (RaplaException ex) {
            return false;
        }
    }

    public AppointmentAction setCopy(Appointment appointment,Date from) {
        this.appointment = appointment;
        this.start = from;
        this.type = COPY;
        putValue(NAME, getString("copy"));
        putValue(SMALL_ICON, getIcon("icon.copy"));
        setEnabled(canModify(appointment.getReservation()));
        return this;
    }

    public AppointmentAction setPaste(Date start) {
        this.start = start;
        this.type = PASTE;
        putValue(NAME, getString("paste"));
        putValue(SMALL_ICON, getIcon("icon.paste"));
        setEnabled(isAppointmentOnClipboard());
        return this;
    }

    public AppointmentAction setPasteAsNew(Date start) {
        this.start = start;
        this.type = PASTE_AS_NEW;
        putValue(NAME, getString("paste_as") + " " + getString( "new_reservation" ) );
        putValue(SMALL_ICON, getIcon("icon.paste_new"));
        setEnabled(isAppointmentOnClipboard());
        return this;
    }

    /**
     * Context menu entry to delete an appointment.
     */
    public AppointmentAction setDelete(Appointment appointment, Date from){
    	this.appointment = appointment;
    	this.start = from;
    	this.type = DELETE;
    	putValue(NAME, getI18n().format("delete.format", getString("appointment")));
    	putValue(SMALL_ICON, getIcon("icon.delete"));
    	setEnabled(canModify(appointment.getReservation()));
    	return this;
    }

    public AppointmentAction setView(Appointment appointment) {
        this.appointment = appointment;
        this.type = VIEW;
        putValue(NAME, getString("view"));
        putValue(SMALL_ICON, getIcon("icon.help"));
        User owner = appointment.getReservation().getOwner();
        try 
        {
            User user = getUser();
            boolean canView = getQuery().canReadReservationsFromOthers( user) || user.equals( owner);
            setEnabled( canView);
        } 
        catch (RaplaException ex)
        {
            getLogger().error( "Can't get user",ex);
        }
        return this;
    }

    public AppointmentAction setEdit(Appointment appointment, Date from) {
        this.start = from;
        this.appointment = appointment;
        this.type = EDIT;
        putValue(SMALL_ICON, getIcon("icon.edit"));
        
        boolean canExchangeAllocatables = getQuery().canExchangeAllocatables(appointment.getReservation());
		boolean canModify = canModify(appointment.getReservation());
		String text = !canModify && canExchangeAllocatables ?  getString("exchange_allocatables") : getString("edit");
		putValue(NAME, text);
        return this;
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            switch (type) {
            case DELETE: delete();break;
            case COPY: copy();break;
            case PASTE: paste(false);break;
            case PASTE_AS_NEW: paste( true);break;
            case NEW: newReservation();break;
            case ADD_TO_RESERVATION: addToReservation();break;
            case EDIT: edit();break;
            case VIEW: view();break;
            }
        } catch (RaplaException ex) {
            showException(ex,parent);
        } // end of try-catch
    }

    public void view() throws RaplaException {
        getInfoFactory().showInfoDialog(appointment.getReservation(),parent,point);
    }

    public void edit() throws RaplaException {
        appointment.getReservation().setSelectedSlotDate(start);
        getReservationController().edit( appointment);
    }

    private void delete() throws RaplaException {
        getReservationController().deleteAppointment(appointment,start,parent,point);
    }

    private void copy() throws RaplaException 
    {
    	
        Appointment copy = getReservationController().copyAppointment(appointment,start,parent,point);
                
        
        
    }
    
    public boolean isKeepTime() {
		return keepTime;
	}

	public void setKeepTime(boolean keepTime) {
		this.keepTime = keepTime;
	}


    private void paste(boolean asNewReservation) throws RaplaException {
        
		ReservationController reservationController = getReservationController();
		
		reservationController.pasteAppointment(	start
                                               ,parent
                                               ,point
                                               ,asNewReservation, keepTime);
    }

    private void newReservation() throws RaplaException {
        CalendarModel model = getService(CalendarModel.class);
		wizard.start( parent, model);
    }

    /* (non-Javadoc)
     * @see org.rapla.gui.edit.reservation.IReservationController#startNew(java.util.Date, java.util.Date, org.rapla.entities.domain.Allocatable, java.lang.String, org.rapla.entities.dynamictype.DynamicType, int)
     */
    public void create(Date start,Date end,Collection<Allocatable> allocatables, RepeatingType repeatingType, DynamicType type, int repeatings) throws RaplaException {
        Reservation mutableReservation = getModification().newReservation();
        if ( type != null) {
            mutableReservation.setClassification( type.newClassification());
        }
        Appointment appointment = getModification().newAppointment( start, end );
        if ( repeatingType != null ) {
            ReservationHelper.makeRepeatingForPeriod(getPeriodModel(), appointment, repeatingType, repeatings );
        }
        mutableReservation.addAppointment(appointment);
        if (allocatables != null)
        {
        	for (Allocatable allocatable:allocatables)
        	{
        		mutableReservation.addAllocatable(allocatable);
        	}
        }
        appointment.getReservation().setSelectedSlotDate(start);
        getReservationController().edit( appointment);
    }

    private void addToReservation() throws RaplaException {
    	
    	reservationEdit.addAppointment(start,end);
    }

    public boolean isAppointmentOnClipboard() {
        return (getReservationController().isAppointmentOnClipboard());
    }
    



}
