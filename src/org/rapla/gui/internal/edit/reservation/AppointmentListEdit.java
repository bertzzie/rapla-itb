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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.AppointmentStartComparator;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.internal.edit.RaplaListEdit;
import org.rapla.gui.toolkit.RaplaWidget;


/** Default GUI for editing multiple appointments.*/
class AppointmentListEdit extends AbstractAppointmentEditor
    implements
        RaplaWidget
        ,Disposable
{

    private AppointmentController appointmentController;
    private RaplaListEdit listEdit;

    protected Reservation mutableReservation;
    private Listener listener = new Listener();
    DefaultListModel model = new DefaultListModel();
    // use sorted model to start with sorting
    // respect dependencies ! on other components
    Comparator comp = new AppointmentStartComparator();
    SortedListModel sortedModel = new SortedListModel(model, SortedListModel.SortOrder.ASCENDING,comp );
    
    AppointmentListEdit(RaplaContext sm) throws RaplaException {
        super( sm);
        appointmentController = new AppointmentController(sm);
        listEdit = new RaplaListEdit(getI18n(),appointmentController.getComponent(), listener);
        appointmentController.addChangeListener(listener);
        // activate this as a first step
         listEdit.getList().setModel(sortedModel);
        //listEdit.getList().setModel(model);
        listEdit.setColoredBackgroundEnabled(true);
        listEdit.setMoveButtonVisible(false);
        listEdit.getList().setCellRenderer(new AppointmentCellRenderer());
    }
    
    public RaplaListEdit getListEdit()
    {
    	return listEdit;
    }

    public JComponent getComponent() {
        return listEdit.getComponent();
    }

    public void setReservation(Reservation mutableReservation, Appointment appointment) {
        this.mutableReservation = mutableReservation;
        Appointment[] appointments = mutableReservation.getAppointments();
        model.clear();
        for (int i = 0; i<appointments.length; i++) {
            model.addElement(appointments[i]);
        }
        if ( appointment != null ) {
            listEdit.select( model.indexOf( appointment ) );
        } else if ( appointments.length> 0 ){
            listEdit.select(0);
        }
       
       
    }

    public void dispose() {
        ContainerUtil.dispose(  appointmentController );
    }

    private void removeAppointments() {
        Object[] objects = listEdit.getList().getSelectedValues();
        for (int i=0;i<objects.length;i++) {
            Appointment appointment = (Appointment) objects[i];
            model.removeElement(appointment);
            mutableReservation.removeAppointment(appointment);
            fireAppointmentRemoved(Collections.singleton(appointment));
        }
        listEdit.getList().requestFocus();
    }

    private void createNewAppointment() {
        try {
            Appointment[] appointments = mutableReservation.getAppointments();
            Appointment appointment;
            if (appointments.length == 0) {
                Date start = new Date(DateTools.cutDate(new Date()).getTime()
                                      + getCalendarOptions().getWorktimeStart()
                                      * DateTools.MILLISECONDS_PER_HOUR
                                      );
                Date end = new Date(start.getTime() + DateTools.MILLISECONDS_PER_HOUR);
                appointment = (Appointment) getModification().newAppointment( start, end );
            } else { // copy the last as a template and set to the slotStartDate
                // copy the last as a template and set to the slotStartDate
                //modification: copy the selected instead of the last
                //last is automatically selected
                final int selectedIndex = listEdit.getSelectedIndex();
                final int index = selectedIndex > -1 ? selectedIndex :appointments.length-1;
                final Appointment toClone = appointments[index];
                //this allows each appointment as template
                appointment = getReservationController().copyAppointment(toClone);

                Repeating repeating = appointment.getRepeating();
                if (repeating != null) {
                    repeating.clearExceptions();
                }
                
                
            }
            mutableReservation.addAppointment(appointment);
            model.addElement(appointment);

            fireAppointmentAdded(Collections.singleton(appointment));
            final int insertIndex = sortedModel.toSortedModelIndex(model.indexOf(appointment));
            listEdit.select(insertIndex);
            //listEdit.select(model.getSize()-1);
        } catch (RaplaException ex) {
            showException(ex, getComponent());
        }
    }
    
	/**
	 * Splits a repeating Appointment into single not repeating Appointments
	 * 
	 * @since Rapla 1.4
	 */
	private void splitAppointment()
	{
		try
		{
			// Generate time blocks from selected appointment
			List<AppointmentBlock> splits = new ArrayList<AppointmentBlock>();
			Appointment appointment = appointmentController.getAppointment();
			appointment.createBlocks(appointment.getStart(), DateTools.fillDate(appointment.getMaxEnd()), splits);
			Allocatable[] allocatablesFor = mutableReservation.getAllocatablesFor(appointment);
			// Switch the type of the appointment to single appointment
			appointmentController.noRepeating.doClick();
			
			
			mutableReservation.removeAppointment( appointment);
			model.removeElement( appointment);
			fireAppointmentRemoved(Collections.singleton(appointment));
			
			List<Appointment> newAppointments = new ArrayList<Appointment>();
			// Create single appointments for every time block
			for (AppointmentBlock block: splits)
			{
				Appointment newApp = (Appointment) getModification().newAppointment(new Date(block.getStart()), new Date(block.getEnd()));
				// Add appointment to list
				newAppointments.add( newApp );
				mutableReservation.addAppointment(newApp);
			}
			for (Allocatable alloc:allocatablesFor)
			{
				Appointment[] restrictions =mutableReservation.getRestriction(alloc);
				if ( restrictions.length > 0)
				{
					LinkedHashSet<Appointment> newRestrictions = new LinkedHashSet<Appointment>( Arrays.asList( restrictions));
					newRestrictions.addAll(newAppointments);
					mutableReservation.setRestriction(alloc, newRestrictions.toArray(Appointment.EMPTY_ARRAY));
				}
			}

			for (Appointment newApp:newAppointments)
			{
				model.addElement(newApp);
			}			
			fireAppointmentAdded(newAppointments);
			
			if ( newAppointments.size() > 0)
			{
				boolean shouldScroll = true;
				listEdit.getList().setSelectedValue( newAppointments.get(0), shouldScroll);
			}
		
		}
		catch (RaplaException ex)
		{
			showException(ex, getComponent());
		}
	}

   

    class AppointmentCellRenderer implements ListCellRenderer {
        Border focusBorder = UIManager.getBorder("List.focusCellHighlightBorder");
        Border emptyBorder = new EmptyBorder(1,1,1,1);

        Color selectionBackground = UIManager.getColor("List.selectionBackground");
        Color background = UIManager.getColor("List.background");

        AppointmentRow row = new AppointmentRow();
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            row.setAppointment((Appointment) value,index);
            row.setBackground((isSelected) ? selectionBackground : background);
            row.setBorder((cellHasFocus) ? focusBorder : emptyBorder);
            return row;
        }
    };

    class AppointmentRow extends JPanel {
        private static final long serialVersionUID = 1L;

        JPanel content = new JPanel();
        AppointmentIdentifier identifier = new AppointmentIdentifier();
        AppointmentRow() {
            double fill = TableLayout.FILL;
            double pre = TableLayout.PREFERRED;
            this.setLayout(new TableLayout(new double[][] {{pre,5,fill,10,pre},{1,fill,1}}));
            this.add(identifier,"0,1,l,f");
            this.add(content,"2,1,f,c");

            this.setMaximumSize(new Dimension(500,40));
            content.setOpaque(false);
            identifier.setOpaque(true);
            identifier.setBorder(null);
        }

        public void setAppointment(Appointment appointment,int index) {
            identifier.setText(getRaplaLocale().formatNumber(index + 1));
            identifier.setIndex(index);
            content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
            content.removeAll();
            JLabel label1 = new JLabel(getAppointmentFormater().getSummary(appointment));
            content.add( label1 );
            if (appointment.getRepeating() != null) {
                label1.setIcon( getIcon("icon.repeating") );
                Repeating r = appointment.getRepeating();
                List<Period> periods = getPeriodModel().getPeriodsFor(appointment.getStart());
                String repeatingString =
                    getAppointmentFormater().getSummary(r,periods);
                content.add(new JLabel(repeatingString));
                if ( r.hasExceptions() ) {
                    content.add(new JLabel( getAppointmentFormater().getExceptionSummary( r ) ) );
                }
            } else {
                label1.setIcon( getIcon("icon.single") );
            }
        }
    }

	class Listener implements ActionListener, ChangeListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if (evt.getActionCommand().equals("remove"))
			{
				removeAppointments();
			}
			else if (evt.getActionCommand().equals("new"))
			{
				createNewAppointment();
			}
			else if (evt.getActionCommand().equals("split"))
			{
				splitAppointment();
			}
			else if (evt.getActionCommand().equals("edit"))
			{
				Appointment app = (Appointment) listEdit.getList().getSelectedValue();
				appointmentController.setAppointment(app);
			}
		}
		
		public void stateChanged(ChangeEvent evt)
		{
			Appointment appointment = appointmentController.getAppointment();
			model.set(model.indexOf(appointment), appointment);
			boolean shouldScroll = true;
			listEdit.getList().setSelectedValue( appointment, shouldScroll);
			fireAppointmentChanged(Collections.singleton(appointment));
		}
	}
	
	public AppointmentController getController()
	{
		return appointmentController;
	}

	
}


