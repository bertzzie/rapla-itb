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

import org.rapla.entities.RaplaObject;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.ReservationWizard;
import org.rapla.plugin.RaplaExtensionPoints;

public class ReservationAction extends  RaplaObjectAction{
    ReservationWizard m_wizard;
    public final static int NEW_WIZARD = 800;

	public ReservationAction(RaplaContext sm,Component parent,Point p) throws RaplaException 
	{
        super(sm,parent,p);
        raplaType = Reservation.TYPE;
	}

    public ReservationAction setNew(ReservationWizard wizard) {
        super.setNew( Reservation.TYPE);
        m_wizard = wizard;
        if (m_wizard == null) {
            m_wizard  = (ReservationWizard) getService( RaplaExtensionPoints.RESERVATION_WIZARD_EXTENSION );
        }
        if (m_wizard != null)
            putValue(NAME, m_wizard.toString());
        return this;
    }

    protected void edit() throws RaplaException 
    {
        getReservationController().edit( (Reservation) object );
    }


    protected boolean canModifiy(Object object) {
    	if (super.canModify( object) ) {
    		return true;
    	}
        if ( object instanceof Reservation ) {
        	return getQuery().canExchangeAllocatables( (Reservation) object) ;
        }
        return false;
    }

    public void changeObject(RaplaObject object) 
    {
        super.changeObject(object);
        if (type == NEW) {
            setEnabled( m_wizard != null);
        }

    }

    protected void newEntity() throws RaplaException 
    {
        CalendarModel service = getService( CalendarModel.class);
    /*
        @SuppressWarnings("unchecked")
        List<Allocatable> allocatables = model.getMarkedAllocatables();
		allocatables = Arrays.asList(model.getSelectedAllocatables());
		*/
		m_wizard.start(parent, service);
    }
    


}
