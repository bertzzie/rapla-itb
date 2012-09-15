package org.rapla.gui;

import java.util.Date;

import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaException;

public interface ReservationEdit
{
    boolean isModifiedSinceLastChange();
    @Deprecated
    void addAppointment( Date start, Date end, RepeatingType repeatingType, Integer repeatings ) throws RaplaException;
    void addAppointment( Date start, Date end) throws RaplaException;
	
    Reservation getReservation();
    void save() throws RaplaException;
    void delete() throws RaplaException;
    
    void addAppointmentListener(AppointmentListener listener);
    void removeAppointmentListener(AppointmentListener listener);
}