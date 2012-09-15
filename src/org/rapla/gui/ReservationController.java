package org.rapla.gui;

import java.awt.Component;
import java.awt.Point;
import java.util.Date;

import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaException;

/** Use the ReservationController to modify or create a {@link Reservation}.
    This class handles all interactions with the user. Examples:
    <li>
    If you edit a reservation it will first check, if there is already is an
    open edit-window for the reservation and will give focus to that window instead of
    creating a new one.
    </li>
    <li>
    If you move or delete an repeating appointment it will display dialogs
    where the user will be asked if he wants to delete/move the complete appointment
    or just the occurance on the selected date.
    </li>
    <li>
    If conflicts are found, a conflict panel will be displayed on saving.
    </li>
 */
public interface ReservationController
{

    public final static String ROLE = ReservationController.class.getName();

    void edit( Reservation reservation ) throws RaplaException;
    void edit( Appointment appointment ) throws RaplaException;
    boolean save(Reservation reservation,Component sourceComponent,boolean showOnlyWhenConflicts) throws RaplaException;

    public ReservationEdit[] getEditWindows();

    /** copys an appointment without interaction */
    Appointment copyAppointment( Appointment appointment ) throws RaplaException;

    void deleteAppointment( Appointment appointment, Date from, Component sourceComponent, Point point )  throws RaplaException;

    Appointment copyAppointment( Appointment appointment, Date from, Component sourceComponent, Point point ) throws RaplaException;

    void pasteAppointment( Date start, Component sourceComponent, Point point, boolean asNewReservation, boolean keepTime ) throws RaplaException;

    /**
     * @param keepTime when moving only the date part and not the time part is modified*/
    void moveAppointment( Appointment appointment, Date from, Date newStart, Component sourceComponent, Point point, boolean keepTime ) throws RaplaException;

  
    /**
     * @param keepTime when moving only the date part and not the time part is modified*/
    void resizeAppointment( Appointment appointment, Date from, Date newStart, Date newEnd, Component sourceComponent, Point p, boolean keepTime ) throws RaplaException;
    
	void exchangeAllocatable(Reservation reservation, Appointment app,
			Date onDate, Allocatable oldAlloc, Allocatable newAlloc, Component sourceComponent, Point p) throws RaplaException;
	
	boolean isAppointmentOnClipboard();
	
}