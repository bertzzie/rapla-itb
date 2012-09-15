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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.avalon.framework.container.ContainerUtil;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.Command;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationHelper;
import org.rapla.facade.ModificationEvent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.AppointmentListener;
import org.rapla.gui.AppointmentStatusFactory;
import org.rapla.gui.ReservationController;
import org.rapla.gui.ReservationEdit;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.EmptyLineBorder;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RaplaFrame;
import org.rapla.gui.toolkit.RaplaWidget;
import org.rapla.plugin.RaplaExtensionPoints;

class ReservationEditImpl extends AbstractAppointmentEditor implements ReservationEdit
{
    protected Reservation mutableReservation;

    JToolBar toolBar = new JToolBar();
    RaplaButton saveButtonTop = new RaplaButton();
    RaplaButton saveButton = new RaplaButton();
    RaplaButton deleteButton = new RaplaButton();
    RaplaButton closeButton = new RaplaButton();

    JPanel mainContent = new JPanel();
    //JPanel split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    RaplaFrame frame;

    ReservationInfoEdit reservationInfo;
    AppointmentListEdit appointmentEdit ;
    AllocatableSelection allocatableEdit;

    boolean bSaving = false;
    boolean bDeleting = false;
    boolean bSaved;
    boolean bNew;
    TableLayout tableLayout = new TableLayout(new double[][] {
            {TableLayout.FILL}
            ,{TableLayout.PREFERRED,TableLayout.PREFERRED,TableLayout.FILL}
        } );

    private final Listener listener = new Listener();
    
    List<AppointmentListener> appointmentListeners = new ArrayList<AppointmentListener>();

    ReservationEditImpl(RaplaContext sm) throws RaplaException {
        super( sm);
        reservationInfo = new ReservationInfoEdit(sm);
        appointmentEdit = new AppointmentListEdit(sm);
        allocatableEdit = new AllocatableSelection(sm,true);

        //      horizontalSplit.setTopComponent(appointmentEdit.getComponent());
        //horizontalSplit.setBottomComponent(allocatableEdit.getComponent());
        /*
        try {
            // If run on jdk < 1.3 this will throw a MethodNotFoundException
            // horizontalSplit.setResizeWeight(0.1);
            JSplitPane.class.getMethod("setResizeWeight",new Class[] {double.class}).invoke(horizontalSplit,new Object[] {new Double(0.1)});
        } catch (Exception ex) {
        }
        */

        frame = new RaplaFrame(sm);
        mainContent.setLayout( tableLayout );
        mainContent.add(reservationInfo.getComponent(),"0,0");
        mainContent.add(appointmentEdit.getComponent(),"0,1");
        mainContent.add(allocatableEdit.getComponent(),"0,2");
        //allocatableEdit.getComponent().setVisible(false);
        saveButtonTop.setAction( listener );
        saveButton.setAction( listener );
        toolBar.setFloatable(false);
        saveButton.setAlignmentY(JButton.CENTER_ALIGNMENT);
        deleteButton.setAlignmentY(JButton.CENTER_ALIGNMENT);
        closeButton.setAlignmentY(JButton.CENTER_ALIGNMENT);
        JPanel buttonsPanel = new JPanel();
        //buttonsPanel.add(deleteButton);
        buttonsPanel.add(saveButton);
        buttonsPanel.add(closeButton);
        toolBar.add(saveButtonTop);
        toolBar.add(deleteButton);
        deleteButton.setAction( listener );
        closeButton.addActionListener(listener);
        appointmentEdit.addAppointmentListener(allocatableEdit);
        appointmentEdit.addAppointmentListener(listener);
        allocatableEdit.addChangeListener(listener);
        reservationInfo.addChangeListener(listener);
        reservationInfo.addDetailListener(listener);
        frame.addVetoableChangeListener(listener);

        frame.setIconImage( getI18n().getIcon("icon.edit_window_small").getImage());
        
        JPanel contentPane = (JPanel) frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        mainContent.setBorder(BorderFactory.createLoweredBevelBorder());
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(buttonsPanel, BorderLayout.SOUTH);
        contentPane.add(mainContent, BorderLayout.CENTER);
        Dimension dimension = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(new Dimension(
                                    Math.min(dimension.width,990)
                                    // BJO 00000032 temp fix for filter out of frame bounds
                                     ,Math.min(dimension.height-10,720)
                                    //,Math.min(dimension.height-10,1000) 
                                    )
                      );
        
        Border  emptyLineBorder = new EmptyLineBorder();
        //BorderFactory.createEmptyBorder();
        Border border2 = BorderFactory.createTitledBorder(emptyLineBorder,getString("reservation.appointments"));
        Border border3 = BorderFactory.createTitledBorder(emptyLineBorder,getString("reservation.allocations"));
        appointmentEdit.getComponent().setBorder(border2);
        allocatableEdit.getComponent().setBorder(border3);
        
        saveButton.setText(getString("save"));
        saveButton.setIcon(getIcon("icon.save"));
        
        saveButtonTop.setText(getString("save"));
        saveButtonTop.setMnemonic(KeyEvent.VK_S);
        saveButtonTop.setIcon(getIcon("icon.save"));
        
        deleteButton.setText(getString("delete"));
        deleteButton.setIcon(getIcon("icon.delete"));
        
        closeButton.setText(getString("abort"));
        closeButton.setIcon(getIcon("icon.abort"));

    }

    protected void setSaved(boolean flag) {
        bSaved = flag;
        saveButton.setEnabled(!flag);
        saveButtonTop.setEnabled(!flag);
    }

    /* (non-Javadoc)
     * @see org.rapla.gui.edit.reservation.IReservationEdit#isModifiedSinceLastChange()
     */
    public boolean isModifiedSinceLastChange() {
        return !bSaved;
    }

    final private ReservationControllerImpl getPrivateReservationController() {
        return (ReservationControllerImpl) getService(ReservationController.ROLE);
    }

    
    public void addAppointment( Date start, Date end) throws RaplaException
    {
       	Appointment appointment = getModification().newAppointment( start, end );
       	AppointmentController controller = appointmentEdit.getController();
		Repeating repeating= controller.getRepeating();
        if ( repeating!= null  )
        {
        	appointment.setRepeatingEnabled( true);
        	appointment.getRepeating().setFrom(repeating);
        }
       	
       	mutableReservation.addAppointment(appointment);
    	setReservation( mutableReservation, appointment );
        setSaved(false);
        frame.requestFocus();   
    }

    /* (non-Javadoc)
     * @see org.rapla.gui.edit.reservation.IReservationEdit#addAppointment(java.util.Date, java.util.Date, java.lang.String, int)
     */
    public void addAppointment(Date start, Date end, RepeatingType repeatingType, Integer repeatings) throws RaplaException {
    	Appointment appointment = getModification().newAppointment( start, end );
         if ( repeatingType != null ) {
         	ReservationHelper.makeRepeatingForPeriod( getPeriodModel(),appointment, repeatingType , repeatings);
         }
        mutableReservation.addAppointment(appointment);
    	setReservation( mutableReservation, appointment );
        setSaved(false);
        frame.requestFocus();
    }


    void deleteReservation() throws RaplaException {
        if (bDeleting)
            return;
        getLogger().debug("Reservation has been deleted.");
        DialogUI dlg = DialogUI.create(
                getContext()
                ,mainContent
                ,true
                ,getString("warning")
                ,getString("warning.reservation.delete")
        );
        dlg.setIcon(getIcon("icon.warning"));
        dlg.start();
        closeWindow();
    }

    void updateReservation(Reservation newReservation) throws RaplaException {
        if (bSaving)
            return;
        getLogger().debug("Reservation has been changed.");
        DialogUI dlg = DialogUI.create(
                getContext()
                ,mainContent
                ,true
                ,getString("warning")
                ,getString("warning.reservation.update")
        );
        try {
            dlg.setIcon(getIcon("icon.warning"));
            dlg.start();
            setReservation((Reservation) getModification().edit(newReservation) , null);
        } catch (RaplaException ex) {
            showException(ex,frame);
        }
    }

    void refresh(ModificationEvent evt) throws RaplaException {
        allocatableEdit.refresh(evt);
    }

    void editReservation(Reservation mutableReservation, Appointment appointment, boolean bNew) throws RaplaException  {
        setSaved(!bNew);
        //printBlocks( appointment );
        this.bNew = bNew;
        deleteButton.setEnabled(!bNew);

        setReservation(mutableReservation, appointment);

        setTitle();
        boolean packFrame = false;
        frame.place( true, packFrame);
        frame.setVisible( true );
        // Insert into open ReservationEditWindows, so that
        // we can't edit the same Reservation in different windows
        getPrivateReservationController().addReservationEdit(this);
        // #TODO this should be done in allocatableEdit
        //allocatableEdit.content.setDividerLocation(0.5);
        //frame.requestFocus();
        reservationInfo.requestFocus();
        getLogger().debug("New Reservation-Window created");
    }

/*
    private void printBlocks( Appointment appointment )
    {
        AppointmentBlockArray array = new AppointmentBlockArray();
        {
        Calendar cal = getRaplaLocale().createCalendar();
        cal.set( Calendar.YEAR, 2004);
        Date start = cal.getTime();
        cal.set( Calendar.YEAR, 2007);
        Date end = cal.getTime();
        appointment.createBlocks(start, end, array);
        }
        for ( int i=0;i< array.size();i++)
        {
            Date start = new Date(array.getStartAt( i ));
            Date end = new Date(array.getEndAt( i ));
            SimpleDateFormat format = new SimpleDateFormat();
            format.setTimeZone( getRaplaLocale().getTimeZone());
            System.out.println(  format.format(start) + " - " +  format.format(end));
        }
    }
*/
    /* (non-Javadoc)
     * @see org.rapla.gui.edit.reservation.IReservationEdit#getReservation()
     */
    public Reservation getReservation() {
        return mutableReservation;
    }

    private void setTitle() {
        String title = getI18n().format((bNew) ?
                                        "new_reservation.format" : "edit_reservation.format"
                                        ,getName(mutableReservation));
        frame.setTitle(title);
    }

    private void setReservation(Reservation newReservation, Appointment appointment) throws RaplaException {
        this.mutableReservation = newReservation;
        appointmentEdit.setReservation(mutableReservation, appointment);
        allocatableEdit.setReservation(mutableReservation);
        reservationInfo.setReservation(mutableReservation);

        List<AppointmentStatusFactory> statusFactories = new ArrayList<AppointmentStatusFactory>();
        Map<?,?> map = getContainer().lookupServicesFor(RaplaExtensionPoints.APPOINTMENT_STATUS);
       	for (Object entry:map.values())
       	{
       		statusFactories.add((AppointmentStatusFactory) entry);
       	}
       	
        JPanel status =appointmentEdit.getListEdit().getStatusBar(); 
        status.removeAll();
        
        for (AppointmentStatusFactory factory: statusFactories)
        {
        	RaplaWidget statusWidget = factory.createStatus(getContext(), this);
        	status.add( statusWidget.getComponent());
        }
        
    }


    public void closeWindow() {
        ContainerUtil.dispose(appointmentEdit);
        getPrivateReservationController().removeReservationEdit(this);
        frame.dispose();
        getLogger().debug("Edit window closed.");
    }


    class Listener extends AbstractAction implements AppointmentListener,ChangeListener,VetoableChangeListener, ReservationInfoEdit.DetailListener {
        private static final long serialVersionUID = 1L;

    // Implementation of ReservationListener
        public void appointmentRemoved(Collection<Appointment> appointment) {
            setSaved(false);
            ReservationEditImpl.this.fireAppointmentRemoved(appointment);
        }

        public void appointmentAdded(Collection<Appointment> appointment) {
            setSaved(false);
            ReservationEditImpl.this.fireAppointmentAdded(appointment);
        }

        public void appointmentChanged(Collection<Appointment> appointment) {
            setSaved(false);
            ReservationEditImpl.this.fireAppointmentChanged(appointment);
        }

        public void stateChanged(ChangeEvent evt) {
        	if (evt.getSource() == reservationInfo) {
        		getLogger().debug("ReservationInfo changed");
        		setSaved(false);
                setTitle();
        	}
            if (evt.getSource() == allocatableEdit) {
                getLogger().debug("AllocatableEdit changed");
                setSaved(false);
            }
        }
        public void detailChanged() {
            boolean isMain = reservationInfo.isMainView();
            if ( isMain != appointmentEdit.getComponent().isVisible() ) {
                appointmentEdit.getComponent().setVisible( isMain );
                allocatableEdit.getComponent().setVisible( isMain );
                if ( isMain ) {
                    tableLayout.setRow(0, TableLayout.PREFERRED);
                    tableLayout.setRow(1, TableLayout.PREFERRED);
                    tableLayout.setRow(2, TableLayout.FILL);
                } else {
                    tableLayout.setRow(0, TableLayout.FILL);
                    tableLayout.setRow(1, 0);
                    tableLayout.setRow(2, 0);
                }
                mainContent.validate();
            }
        }


        public void actionPerformed(ActionEvent evt) {
            try {
                if (evt.getSource() == saveButton || evt.getSource() == saveButtonTop) {
                    save();
                }
                if (evt.getSource() == deleteButton) {
                    delete();
                }
                if (evt.getSource() == closeButton) {
                    if (canClose())
                        closeWindow();
                }
            } catch (RaplaException ex) {
                showException(ex, null);
            }
        }

        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        	if (!canClose())
                throw new PropertyVetoException("Don't close",evt);
            closeWindow();
        }
    };

    protected boolean canClose() {
        if (!isModifiedSinceLastChange())
            return true;

		try {
        DialogUI dlg = DialogUI.create(
                    getContext()
                        ,mainContent
                            ,true
                            ,getString("confirm-close.title")
                            ,getString("confirm-close.question")
                            ,new String[] {
                                getString("confirm-close.ok")
                                ,getString("back")
                            }
                            );
			dlg.setIcon(getIcon("icon.question"));
            dlg.setDefault(1);
            dlg.start();
            return (dlg.getSelectedIndex() == 0) ;
		} catch (RaplaException e) {
			return true;
		}

    }


    /* (non-Javadoc)
     * @see org.rapla.gui.edit.reservation.IReservationEdit#save()
     */
    public void save() throws RaplaException {
    	save( true );
    }

    public void save(boolean confirm) throws RaplaException {
        if (mutableReservation.getAllocatables().length == 0 ) {
            DialogUI dialog = DialogUI.create(
                    getContext()
                    ,frame
                    ,true
                    ,getString("warning")
                    ,getString("warning.no_allocatables_selected")
                    ,new String[] {
                    getString("continue")
                    ,getString("back")
            }
            );
            dialog.setIcon(getIcon("icon.warning"));
            dialog.setDefault(1);
            dialog.start();
            if (dialog.getSelectedIndex() != 0)
                return;
        }
        getPrivateReservationController().save(mutableReservation,frame,new SaveCommand(), confirm);
    }

    /* (non-Javadoc)
     * @see org.rapla.gui.edit.reservation.IReservationEdit#delete()
     */
    public void delete() throws RaplaException {
        try {
            DialogUI dlg = getInfoFactory().createDeleteDialog(new Object[] {mutableReservation}
                                                               ,frame);
            dlg.start();
            if (dlg.getSelectedIndex() == 0) {
                bDeleting = true;
                getModification().remove( mutableReservation );
                closeWindow();
            }
        } finally {
            bDeleting = false;
        }
    }

    class SaveCommand implements Command {
        public void execute() {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                getModification().checkReservation(mutableReservation);
                bSaving = true;
                getModification().store(mutableReservation);
                setSaved(true);
            } catch (RaplaException ex) {
                showException(ex,frame);
            } finally {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                if (bSaved)
                    closeWindow();
                bSaving = false;
            }
        }
    }
    
    
	
}
