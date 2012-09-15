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
package org.rapla.gui.internal.edit.reservation;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableColumn;

import org.rapla.components.iolayer.IOInterface;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.Command;
import org.rapla.components.util.DateTools;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.Conflict;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.ModificationModule;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.ReservationController;
import org.rapla.gui.ReservationEdit;
import org.rapla.gui.internal.common.RaplaClipboard;
import org.rapla.gui.internal.view.ConflictInfoOldUI;
import org.rapla.gui.internal.view.HTMLInfo.Row;
import org.rapla.gui.internal.view.ReservationInfoUI;
import org.rapla.gui.toolkit.DialogUI;

public class ReservationControllerImpl extends RaplaGUIComponent
    implements
    ModificationListener, ReservationController
{
    /** We store all open ReservationEditWindows with their reservationId
     * in a map, to lookup if the reservation is already beeing edited.
     That prevents editing the same Reservation in different windows
     */
    Collection<ReservationEdit> editWindowList = new ArrayList<ReservationEdit>();

    public ReservationControllerImpl(RaplaContext sm) throws RaplaException {
        super(sm);
        getUpdateModule().addModificationListener(this);
    }

    void addReservationEdit(ReservationEdit editWindow) {
        editWindowList.add(editWindow);
    }

    void removeReservationEdit(ReservationEdit editWindow) {
        editWindowList.remove(editWindow);
    }

    public void edit(Reservation reservation) throws RaplaException {
        startEdit(reservation,null);
    }

    public void edit(Appointment appointment)throws RaplaException {
        startEdit(appointment.getReservation(), appointment);
    }

    public ReservationEdit[] getEditWindows() {
        return  editWindowList.toArray( new ReservationEdit[] {});
    }

    private ReservationEditImpl newEditWindow() throws RaplaException {
        ReservationEditImpl c = new ReservationEditImpl(getContext());
        return c;
    }

    private ReservationEdit startEdit(Reservation reservation,Appointment appointment)
        throws RaplaException {
        // Lookup if the reservation is already beeing edited
        ReservationEditImpl c = null;
        Iterator<ReservationEdit> it = editWindowList.iterator();
        while (it.hasNext()) {
            c = (ReservationEditImpl)it.next();
            if (c.getReservation().isIdentical(reservation))
                break;
            else
                c = null;
        }

        if (c != null) {
            c.frame.requestFocus();
            c.frame.toFront();
        } else {
            c = newEditWindow();
            ModificationModule mod = getModification();
            boolean bNew = false;
            if ( reservation.isPersistant()) {
                reservation = (Reservation) mod.edit(reservation);
            } else {
                try {
                    getModification().getPersistant( reservation);
                } catch ( EntityNotFoundException ex)  {
                    bNew = true;
                }
            }
            // only is allowed to exchange allocations
            c.editReservation(reservation, appointment, bNew);
            if ( !canModify( reservation) ) 
            {
            	c.deleteButton.setEnabled( false);
                disableComponentAndAllChildren(((ReservationEditImpl)c).appointmentEdit.getComponent());
                disableComponentAndAllChildren(((ReservationEditImpl)c).reservationInfo.getComponent());
            }
        }
        return c;
    }

    static void disableComponentAndAllChildren(Container component) {
        component.setEnabled( false );
        Component[] components = component.getComponents();
        for ( int i=0; i< components.length; i++)
        {
            if ( components[i] instanceof Container) {
                disableComponentAndAllChildren( (Container) components[i] );
            }
        }
    }



    public void deleteAppointment(Appointment appointment,Date from,Component sourceComponent,Point point) throws RaplaException {
        Reservation reservation = appointment.getReservation();
        Reservation mutableReservation = (Reservation)getModification().edit(reservation);
        Appointment app = mutableReservation.findAppointment(appointment);

        DialogAction result = showDialog(appointment, "delete", true,from, sourceComponent, point);
        

        if (result == DialogAction.SINGLE) {
            Repeating repeating = app.getRepeating();
			if ( repeating != null)
			{
				repeating.addException(from);
			}
			else
			{
				mutableReservation.removeAppointment(appointment);
			}
            save(mutableReservation,sourceComponent);
            return;
        }
        
        if (result == DialogAction.EVENT) {
            getModification().remove( mutableReservation );
            return;
        }

        // remove appointment if there are other appointments in the reservation
        if ( result == DialogAction.SERIE) {
            // remove all allocatables that are restricted to the removed appointment
            Allocatable[] allocatables = mutableReservation.getAllocatables();
            for (int i=0;i<allocatables.length;i++) {
                 Appointment[] restriction =
                    mutableReservation.getRestriction( allocatables[i] );
                if (restriction.length == 1 && restriction[0].equals(app))
                    mutableReservation.removeAllocatable( allocatables[i] );
            }
            mutableReservation.removeAppointment(app);
            getModification().store(mutableReservation);
        } // end of else
    }

    public Appointment copyAppointment(Appointment appointment) throws RaplaException {
        return (Appointment) getModification().clone(appointment);
    }

    enum DialogAction
    {
    	EVENT,
    	SERIE,
    	SINGLE,
    	CANCEL
    }
    
    private DialogAction showDialog(Appointment appointment
            ,String action
            ,boolean includeEvent
            ,Date from
            ,Component sourceComponent
            ,Point point
    		) throws RaplaException
    {
        Reservation reservation = appointment.getReservation();
        getLogger().info(action + " '" + appointment + "' for reservation '" +  reservation + "'");
        List<String> optionList = new ArrayList<String>();
        List<Icon> iconList = new ArrayList<Icon>();
        List<DialogAction> actionList = new ArrayList<ReservationControllerImpl.DialogAction>();
        String dateString = getRaplaLocale().formatDate(from);
        
        if ( reservation.getAppointments().length <=1 ||  includeEvent)
        {
        	optionList.add(getString("reservation"));
        	iconList.add(getIcon("icon.edit_window_small"));
        	actionList.add(DialogAction.EVENT);
        }
        if ( appointment.getRepeating() != null && reservation.getAppointments().length > 1)
        {
        	String shortSummary = getAppointmentFormater().getShortSummary(appointment);
        	optionList.add(getString("serie") + ": " + shortSummary);
        	iconList.add(getIcon("icon.repeating"));
        	actionList.add(DialogAction.SERIE);
        }
        if ( appointment.getRepeating() != null || reservation.getAppointments().length > 1)
        {
        	optionList.add(getI18n().format("single_appointment.format",dateString));
        	iconList.add(getIcon("icon.single"));
        	actionList.add( DialogAction.SINGLE);
        }
        if (optionList.size() > 1) {
          
			DialogUI dialog = DialogUI.create(
                    getContext()
                    ,sourceComponent
                    ,true
                    ,getString(action)
                    ,getString(action+ "_appointment.format")
                    ,optionList.toArray(new String[] {})
            );
            dialog.setIcon(getIcon("icon.question"));
            for ( int i=0;i< optionList.size();i++)
            {
            	dialog.getButton(i).setIcon(iconList.get( i));
            }
            
            dialog.start(point);
            int index = dialog.getSelectedIndex();
            if ( index < 0)
            {
            	return DialogAction.CANCEL;
            }
            return actionList.get(index);
        }
        else
        {
        	if ( action.equals("delete"))
        	{
        		 DialogUI dlg = getInfoFactory().createDeleteDialog( new Object[]{ appointment}, sourceComponent);
        		 dlg.start();
        		 if (dlg.getSelectedIndex() != 0)
        			 return DialogAction.CANCEL;
        	       
        	}
        }
        if ( actionList.size() > 0)
        {
        	return actionList.get( 0 );
        }
        return DialogAction.EVENT;
    }
    
    public Appointment copyAppointment(
    		                           Appointment appointment
                                       ,Date from
                                       ,Component sourceComponent
                                       ,Point point)
        throws RaplaException
    {
    	RaplaClipboard raplaClipboard = getClipboard();
         
        DialogAction result = showDialog(appointment, "copy", true,from, sourceComponent, point);
        Reservation sourceReservation = appointment.getReservation();
       
        // copy info text to system clipboard
        {
	        StringBuffer buf = new StringBuffer();
	        ReservationInfoUI reservationInfoUI = new ReservationInfoUI(getContext());
	    	boolean excludeAdditionalInfos = false;
	    
			List<Row> attributes = reservationInfoUI.getAttributes(sourceReservation, null, null, excludeAdditionalInfos);
			for (Row row:attributes)
			{
				buf.append( row.getField());
			}
			String string = buf.toString();
			
			try
			{
				final IOInterface service = getIOService();
				
			    if (service != null) {
			    	StringSelection transferable = new StringSelection(string);
					
					service.setContents(transferable, null);
			    } 
			}
			catch (AccessControlException ex)
			{
			}
        }
	        
        Allocatable[] restrictedAllocatables = sourceReservation.getRestrictedAllocatables(appointment);
       
        if ( result == DialogAction.SINGLE)
        {
        	Appointment copy = copyAppointment(appointment);
        	copy.setRepeatingEnabled(false);
        	copy.move(from);
        	
			raplaClipboard.setAppointment(copy, false, sourceReservation, restrictedAllocatables);
        	return copy;
        }
        else if ( result == DialogAction.EVENT)
        {
        	int num  = getAppointmentIndex(appointment);
        	Reservation reservation = appointment.getReservation();
        	Reservation clone = getModification().clone( reservation);
        	Appointment[] clonedAppointments = clone.getAppointments();
        	if ( num >= clonedAppointments.length)
        	{
        		return null;
        	}
        	
        	Appointment clonedAppointment = clonedAppointments[num];
     		raplaClipboard.setAppointment(clonedAppointment, true, clone, restrictedAllocatables);
     	   
        	return clonedAppointment;
        }
        else
        {
        	Appointment copy = copyAppointment(appointment);
    		raplaClipboard.setAppointment(copy, false, sourceReservation, restrictedAllocatables);
        	return copy;
        }
        
    }

	public int getAppointmentIndex(Appointment appointment) {
		int num;
		Reservation reservation = appointment.getReservation();
		num = 0;
		for (Appointment app:reservation.getAppointments())
		{
		
			if ( appointment == app)
			{
				break;
			}
			num++;
		}
		return num;
	}

    public void dataChanged(ModificationEvent evt) throws RaplaException {
        Iterator<ReservationEdit> it = editWindowList.iterator();

        while (it.hasNext()) {
            ReservationEditImpl c = (ReservationEditImpl)it.next();
            c.refresh(evt);
        
            if (evt.hasChanged(c.getReservation())) {
                Iterator<RaplaObject> it2 = evt.getChanged().iterator();
                Reservation updatedReservation = null;
                
                while (it2.hasNext()) {
                    Object obj =it2.next();
                    if ( !( obj instanceof Reservation )) {
                        continue;
                    }
                    Reservation newReservation = (Reservation) obj;
                    if (newReservation.isIdentical(c.getReservation())) {
                        updatedReservation = newReservation;
                        break;
                    }
                }
                if ( updatedReservation != null)
                {
                    c.updateReservation(updatedReservation);
                }
                else
                {
                    c.closeWindow();
                }

            } else if (evt.isRemoved(c.getReservation())) {
                c.deleteReservation();
            }
        }
    }

    private void setRestrictions(
                                 Appointment appointment
                                 ,Allocatable[] restrictedAllocatables
                                 ,Reservation reservation
                                 )
    {
        for (int i=0;i<restrictedAllocatables.length;i++)
        {
            Allocatable allocatable = restrictedAllocatables[i];
            Appointment[] restriction = reservation.getRestriction(allocatable);
            HashSet<Appointment> newRestriction = new HashSet<Appointment>(Arrays.asList(restriction));
            newRestriction.add( appointment );
            reservation.setRestriction(allocatable,
                     newRestriction.toArray( Appointment.EMPTY_ARRAY)
                    );
        }
    }

	private RaplaClipboard getClipboard() 
	{
        return getService(RaplaClipboard.class);
    }
	
    public boolean isAppointmentOnClipboard() {
        return (getClipboard().getAppointment() != null);
    }


    
    public void pasteAppointment(
    		Date start
                                 ,Component sourceComponent
                                 ,Point point
                                 ,boolean asNewReservation
                                 ,boolean keepTime
                                 )
        throws RaplaException
    {
    	RaplaClipboard clipboard = getClipboard();
    	Appointment appointment = clipboard.getAppointment();
    	if ( appointment == null)
    	{
    		return;
    	}
		Reservation mutableReservation = null;
		Date appStart = appointment.getStart();
        long offset = getOffset(appStart, start, keepTime);

		boolean copyWholeReservation = clipboard.isWholeReservation();
		if ( asNewReservation)
        {
			Reservation sourceReservation = clipboard.getReservation();
        	mutableReservation = (Reservation) getModification().clone( sourceReservation );
            Appointment[] appointments = mutableReservation.getAppointments();
            for ( int i=0;i<appointments.length;i++)
            {
                Appointment app = appointments[i]; 
                if ( copyWholeReservation)
            	{
                	app.move(new Date(app.getStart().getTime() + offset));
            	}
                else
                {
                	mutableReservation.removeAppointment( app );
                }
            }
        }
        else
        {
        	Reservation destReservation = clipboard.getReservation();
			mutableReservation = (Reservation) getModification().edit( destReservation);
        }
		if ( !copyWholeReservation  )
		{
			Appointment app = copyAppointment(appointment);
         	app.move(new Date(app.getStart().getTime() + offset));
         	mutableReservation.addAppointment(app);
         	Allocatable[] restrictedAllocatables = clipboard.getRestrictedAllocatables();
			setRestrictions(app, restrictedAllocatables, mutableReservation);
        }
        
		getLogger().info("Paste appointment '" + appointment 
				          + "' for reservation '" + mutableReservation 
				          + "' at " + start);

		save(mutableReservation, sourceComponent);
    }

    public void moveAppointment(Appointment appointment,Date from,Date newStart,Component sourceComponent,Point p, boolean keepTime) throws RaplaException {
        if ( newStart.equals(from))
            return;
        getLogger().info("Moving appointment " + appointment + " from " + from + " to " + newStart);
        resizeAppointment( appointment, from, newStart, null, sourceComponent, p, keepTime);
    }

    public void resizeAppointment(Appointment appointment, Date from, Date newStart, Date newEnd, Component sourceComponent, Point p, boolean keepTime) throws RaplaException {
        boolean resizing = newEnd != null;
        Reservation reservation = appointment.getReservation();
        boolean includeEvent = !resizing;
		DialogAction result = showDialog(appointment, "move", includeEvent,from, sourceComponent, p);
        if ( result == DialogAction.CANCEL) {
        	return;
        }
        Reservation mutableReservation = (Reservation)getModification().edit(reservation);
        Appointment app = mutableReservation.findAppointment(appointment);
        if ( app == null) {
            getLogger().warn("Can't find the appointment!");
            return;
        }
     
        long offset = getOffset(from, newStart, keepTime);

    	Appointment copy = null;
    	Collection<Appointment> appointments;
        // Move the complete serie
        if ( result == DialogAction.SERIE  ) 
        {
        	appointments = Collections.singleton( app);
        }
        else if (result == DialogAction.EVENT)
        {
        	appointments = Arrays.asList( mutableReservation.getAppointments()) ;
        }
        else
        {
        	copy = copyAppointment(appointment);
        	appointments = Arrays.asList( copy );
        	copy.setRepeatingEnabled(false);
        }
    	for (Appointment ap:appointments)
    	{
            // we adjust the start end end-time of the first appointment relative to the next from time
    		long startTime = result == DialogAction.SINGLE ? from.getTime() : ap.getStart().getTime();
			if (  resizing ) {
                newStart = new Date( startTime + offset);
                newEnd = new Date(offset + newEnd.getTime());
                ap.move(newStart, newEnd);
            } else {
				newStart = new Date( startTime + offset);
                ap.move(newStart);
            }
    	}
    	if ( copy != null)
        {
			mutableReservation.addAppointment(copy);
            setRestrictions(copy
                    ,mutableReservation.getRestrictedAllocatables(app)
                    ,mutableReservation);
            Repeating repeating = app.getRepeating();
			if ( repeating != null)
			{
				repeating.addException( from );
			}
			else
			{
				mutableReservation.removeAppointment( app);
			}
        }
        save(mutableReservation,sourceComponent);
    }

	public long getOffset(Date appStart, Date newStart, boolean keepTime) {
		Calendar calendar = getRaplaLocale().createCalendar();        		
        calendar.setTime( newStart);
		if ( keepTime)
        {
        	Calendar cal2 = getRaplaLocale().createCalendar();        		
            cal2.setTime( appStart);
        	calendar.set(Calendar.HOUR_OF_DAY, cal2.get( Calendar.HOUR_OF_DAY));
        	calendar.set(Calendar.MINUTE, cal2.get( Calendar.MINUTE));
           	calendar.set(Calendar.SECOND, cal2.get( Calendar.SECOND));
           	calendar.set(Calendar.MILLISECOND, cal2.get( Calendar.MILLISECOND));
        }
        Date newStartAdjusted = calendar.getTime();
        long offset = newStartAdjusted.getTime() - appStart.getTime();
		return offset;
	}

    public boolean save(Reservation reservation,Component sourceComponent,boolean showOnlyWhenConflicts) throws RaplaException {
        SaveCommand saveCommand =new SaveCommand(reservation);
        save(reservation,sourceComponent,saveCommand,showOnlyWhenConflicts);
        return saveCommand.hasSaved();
    }

    boolean save(Reservation reservation,Component sourceComponent) throws RaplaException {
        return save(reservation,sourceComponent,true);
    }

    void save(Reservation reservation
              ,Component sourceComponent
              ,Command saveCommand
              ,boolean showOnlyWhenConflicts) throws RaplaException {
        Conflict[] conflicts =  getQuery().getConflicts(reservation);
        getModification().checkReservation(reservation);

        // Only show when conflicts are found ?
        if (conflicts.length == 0 && showOnlyWhenConflicts) {
            try {
                saveCommand.execute();
            } catch (Exception ex) {
                showException(ex,sourceComponent);
            }
            return;
        }

        Appointment[] appointments = reservation.getAppointments();
        Appointment duplicatedAppointment = null;
        for (int i=0;i<appointments.length;i++) {
            for (int j=i + 1;j<appointments.length;j++)
                if (appointments[i].matches(appointments[j])) {
                    duplicatedAppointment = appointments[i];
                    break;
                }
        }

        JComponent infoComponent = getInfoFactory().createInfoComponent(reservation);
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),getString("confirm.dialog.question")));
        content.setLayout(new TableLayout(new double[][] {
            {TableLayout.FILL}
            ,{TableLayout.PREFERRED,TableLayout.PREFERRED,TableLayout.PREFERRED,2,TableLayout.FILL}
        }));
        if (duplicatedAppointment != null) {
            JLabel warningLabel = new JLabel();
            warningLabel.setForeground(java.awt.Color.red);
            warningLabel.setText
                (getI18n().format
                 (
                  "warning.duplicated_appointments"
                  ,getAppointmentFormater().getShortSummary(duplicatedAppointment)
                  )
                 );
            content.add(warningLabel,"0,0");
        }
        if (conflicts.length > 0) {
            JLabel warningLabel = new JLabel();
            warningLabel.setText(getString("warning.conflict"));
            warningLabel.setForeground(java.awt.Color.red);
            content.add(warningLabel,"0,1");
            content.add(getConflictPanel(conflicts),"0,2");
        }
        content.add(infoComponent,"0,4");
        DialogUI dialog = DialogUI.create(
                getContext()
                ,sourceComponent
                    ,true
                    ,content
                    ,new String[] {
                            getString("save")
                            ,getString("back")
                    }
        );

        if (conflicts.length > 0)
            dialog.setDefault(1);
        dialog.getButton(0).setIcon(getIcon("icon.save"));
        dialog.getButton(1).setIcon(getIcon("icon.cancel"));
        dialog.setTitle(getI18n().format("confirm.dialog.title",getName(reservation)));
        dialog.start();
        if (dialog.getSelectedIndex()  == 0) {
            try {
                saveCommand.execute();
            } catch (Exception ex) {
                showException(ex,sourceComponent);
                return;
            }
        }
    }

    class SaveCommand implements Command {
        Reservation reservation;
        boolean saved;
        public SaveCommand(Reservation reservation) {
            this.reservation = reservation;
        }

        public void execute() throws RaplaException {
            getModification().store( reservation );
            saved = true;
        }

        public boolean hasSaved() {
            return saved;
        }
    }

    private JComponent getConflictPanel(Conflict[] conflicts) throws RaplaException {
        ConflictInfoOldUI panel = new ConflictInfoOldUI();
        ConflictTableModel model = new ConflictTableModel(getContext(),conflicts);
        panel.getTable().setModel(model);
        TableColumn column = panel.getTable().getColumn(getString("conflict.appointment2"));
        panel.getTable().removeColumn(column);
        column = panel.getTable().getColumn(getString("conflict.reservation1"));
        panel.getTable().removeColumn(column);
        return panel.getComponent();
    }

    public void exchangeAllocatable(Reservation reservation,Appointment appointment,Date date,Allocatable oldAllocatable,Allocatable newAllocatable,Component sourceComponent, Point point)
			 throws RaplaException 
	{
    	Appointment copy = null;
		Appointment[] restriction = reservation.getRestriction(oldAllocatable);
		boolean includeEvent = restriction.length ==  0;
		Date from = date;
		DialogAction result = showDialog(appointment, "exchange_allocatables", includeEvent, from, sourceComponent, point);
        if (result == DialogAction.CANCEL)
            return;
        if (result == DialogAction.SINGLE && appointment.getRepeating() != null) {
            copy = copyAppointment(appointment);
            copy.setRepeatingEnabled(false);
            Calendar cal = getRaplaLocale().createCalendar();
            long start = appointment.getStart().getTime();
			int hour = DateTools.getHourOfDay(start);
			int minute = DateTools.getMinuteOfHour(start);
			cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY,hour);
            cal.set(Calendar.MINUTE, minute);
            copy.move(cal.getTime());
        }
        Reservation modifiableReservation = getModification().edit(reservation);
        if (result == DialogAction.EVENT && includeEvent )
        {
        	modifiableReservation.removeAllocatable( oldAllocatable);
        	if ( modifiableReservation.hasAllocated( newAllocatable))
        	{
        		modifiableReservation.setRestriction( newAllocatable, Appointment.EMPTY_ARRAY);
        	}
        	else
        	{
        		modifiableReservation.addAllocatable(newAllocatable);
        	}
        }
        else
        {
        	Appointment[] apps = modifiableReservation.getAppointmentsFor(oldAllocatable);
			if ( copy != null)
			{
			    modifiableReservation.findAppointment( appointment).getRepeating().addException( date );
			    modifiableReservation.addAppointment( copy);
			    List<Allocatable> all =new ArrayList<Allocatable>(Arrays.asList(modifiableReservation.getAllocatablesFor(appointment)));
			    all.remove(oldAllocatable);
			    for ( Allocatable a:all)
			    {
			    	Appointment[] restr = reservation.getRestriction( a);
			    	if ( restr.length > 0)
			    	{
			    		ArrayList<Appointment> newRestrictions = new ArrayList<Appointment>( Arrays.asList( restr));
			    		newRestrictions.add( copy );
			    		reservation.setRestriction(a, newRestrictions.toArray(new Appointment[] {}));
			    	}
			    }
			    modifiableReservation.setRestriction(oldAllocatable,apps);
			}
			else
			{
				if ( apps.length == 1)
				{
					modifiableReservation.removeAllocatable(oldAllocatable);
				}
				else
				{
					List<Appointment> appointments = new ArrayList<Appointment>(Arrays.asList( apps));
					appointments.remove( appointment);
					modifiableReservation.setRestriction(oldAllocatable, appointments.toArray(Appointment.EMPTY_ARRAY));
				}
			}
			if ( copy != null)
			{
				appointment = copy;
			}
			if ( modifiableReservation.hasAllocated( newAllocatable))
			{
				Appointment[] existingRestrictions =modifiableReservation.getRestriction(newAllocatable);
				Set<Appointment> newRestrictions = new LinkedHashSet<Appointment>( Arrays.asList(existingRestrictions));
				if ( existingRestrictions.length ==0 || newRestrictions.contains( appointment))
				{
					// is already allocated, do nothing
					return;
				}
				else
				{
					newRestrictions.add(appointment); 
				}
				modifiableReservation.setRestriction(newAllocatable, newRestrictions.toArray(Appointment.EMPTY_ARRAY));
			}										
			else
			{
				modifiableReservation.addAllocatable( newAllocatable);
				if ( modifiableReservation.getAppointments().length > 1)
				{
					modifiableReservation.setRestriction(newAllocatable, new Appointment[] {appointment});
				}
			}
        }
		getModification().store( modifiableReservation);
	}


}



