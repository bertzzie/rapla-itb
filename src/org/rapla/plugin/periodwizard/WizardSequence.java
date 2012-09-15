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
package org.rapla.plugin.periodwizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.apache.avalon.framework.activity.Disposable;
import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.swing.SwingWeekView;
import org.rapla.components.calendarview.swing.ViewListener;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.PeriodModel;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.edit.ClassificationField;
import org.rapla.gui.internal.edit.reservation.AllocatableSelection;
import org.rapla.gui.toolkit.DisposingTool;
import org.rapla.gui.toolkit.EmptyLineBorder;
import org.rapla.gui.toolkit.WizardDialog;
import org.rapla.gui.toolkit.WizardPanel;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;
import org.rapla.plugin.abstractcalendar.SwingRaplaBuilder;


class WizardSequence extends RaplaGUIComponent
    implements
        ModificationListener
        ,Disposable
{

    WizardDialog wizardDialog;
    Reservation reservation;
    Period period;
    ReservationInfo panel1;
    ReservationAllocation panel2;
    ReservationWeekview panel3;
    public WizardSequence(RaplaContext sm) throws RaplaException {
        super(sm);
        this.setChildBundleName( PeriodWizardPlugin.RESOURCE_FILE);
        panel1 = new ReservationInfo();
        panel2 = new ReservationAllocation();
        panel3 = new ReservationWeekview();

    }

    public ActionMap createActionMap() {
        ActionMap map = new ActionMap();
        map.put(WizardPanel.ABORT, new Handler(getString("abort"),getIcon("icon.abort")));
        map.put(WizardPanel.PREV, new Handler( getString("back"),getIcon("icon.arrow_left")));
        map.put(WizardPanel.NEXT, new Handler(null,null));
        map.put(WizardPanel.FINISH, new Handler(getString("save"), getIcon("icon.save") ));
        return map;
    }

    /** the handler encapsulate the state logic of the wizard */
    class Handler extends AbstractAction {
        private static final long serialVersionUID = 1L;
        
        Handler(String name, Icon icon) {
            putValue(NAME,name);
            putValue(SMALL_ICON, icon);
        }
        public void actionPerformed(ActionEvent evt) {
            try {
            	String command = evt.getActionCommand();
                WizardPanel panel = wizardDialog.getActivePanel();
                if (command.equals(WizardPanel.ABORT)) {
                    wizardDialog.close();
                }
                if (command.equals(WizardPanel.NEXT)) {
                    if (panel == panel1 && panel1.checkReservation()) {
                        panel2.appointmentSelection.setPeriod(period);
                        wizardDialog.start(panel2);
                    }
                    if (panel == panel2) {
                        Collection<Allocatable> allocatables = panel2.getAllocatables();
                        if (allocatables.size() == 0) {
                            showWarning(getString("warning.need_resource_or_person")
                                        ,wizardDialog);
                        } else {
                            panel3.setAllocatables(allocatables);
                            wizardDialog.start(panel3);
                            panel3.build();
                        }
                    }
                    if (panel == panel3) {
                        panel2.newAppointment(panel3.getStart(), panel3.getEnd());
                        wizardDialog.start(panel2);
                    }

                }
                if (command.equals(WizardPanel.PREV)) {
                    if (panel == panel2) {
                        wizardDialog.start(panel1);
                    }
                    if (panel == panel3) {
                        wizardDialog.start(panel2);
                    }
                }
                if (command.equals(WizardPanel.FINISH)) {
                    if (getReservationController().save(reservation,wizardDialog,false))
                        wizardDialog.close();
                }
            } catch (Exception ex) {
                showException(ex,wizardDialog);
            }
        }
    }

    public void start(Component owner,CalendarModel model,DynamicType dynamicType) throws RaplaException {
        wizardDialog = WizardDialog.createWizard(getContext(),owner,false);
        wizardDialog.setTitle(getString("reservation_wizard.title"));
        getLogger().debug("starting wizard");
        wizardDialog.setSize(800, 565);

        reservation = getModification().newReservation();
        if (dynamicType != null)
            reservation.setClassification(dynamicType.newClassification());
        panel1.setStart( model.getSelectedDate());
        panel1.setReservation(reservation);
        panel2.setReservation(reservation);
        getUpdateModule().addModificationListener(this);
        wizardDialog.addWindowListener(new DisposingTool(this));
        wizardDialog.setDefault(2);
        wizardDialog.start(panel1);
    }

    public void dataChanged(ModificationEvent evt) throws RaplaException {
        panel2.refresh(evt);
    }

    public void dispose() {
        try {
            getUpdateModule().removeModificationListener(WizardSequence.this);
        } catch (Exception ex) {
        }
    }

    class ReservationInfo
        implements
            WizardPanel
    {
        JPanel content = new JPanel();
        JLabel periodLabel = new JLabel();
        JLabel classificationLabel = new JLabel();
        ClassificationField classificationField;
        JComboBox periodSelection = new JComboBox();

        public ReservationInfo() throws RaplaException {
        	Period[] periods = getQuery().getPeriods();
        	periodSelection.setModel(new DefaultComboBoxModel(periods));
            classificationField = new ClassificationField(getContext());
            periodLabel.setText(getString("period") + ":");
            double pre=TableLayout.PREFERRED;
            double fill=TableLayout.FILL;
            double[][] sizes = new double[][] {
                {pre,5,fill}
                ,{pre,fill}
            };
            content.setLayout(new TableLayout(sizes));
            content.add(periodLabel,"0,0");
            content.add(periodSelection,"2,0");
            content.add(classificationField.getComponent(),"0,1,2,1");
        }

        public void setStart(Date start) throws RaplaException {
        	PeriodModel periodModel = getPeriodModel();
            Period period = null;
            if (start != null) {
                period = periodModel.getNearestPeriodForDate(start);
            }
            if ( period == null) {
            	period = periodModel.getNearestPeriodForDate( getQuery().today());
            }
            if (period != null) {
                periodSelection.setSelectedItem(period);
            }

            if ( periodModel.getSize() == 0) {
                throw new RaplaException(getString("error.no_period_found"));
            }

        }

        public void setReservation(Reservation reservation) throws RaplaException {
            classificationField.mapFrom(reservation);
        }

        public ActionMap getActionMap() {
            ActionMap map = createActionMap();
            map.get(PREV).setEnabled(false);
            map.get(NEXT).putValue(Action.NAME, getString("reservation_wizard.appointment_menu"));
            map.get(NEXT).putValue(Action.SMALL_ICON, getIcon("icon.arrow_right"));
            map.get(FINISH).setEnabled(false);
            return map;
        }

        public String getHelp() {
            return getString("reservation_wizard.panel1");
        }

        public String getDefaultAction() {
            return NEXT;
        }

        public boolean checkReservation() {
            try {
                classificationField.mapTo(reservation);
            } catch (RaplaException ex) {
                showException(ex,getComponent());
                return false;
            }
            String newName = getName( reservation );
            if (newName.length() ==0) {
                showWarning(getString("error.no_reservation_name"), getComponent());
                return false;
            }
            // We must clear all appointments because the period has been changed);
            if (period != null && !period.equals(periodSelection.getSelectedItem())) {
                panel2.removeAllAppointments();
            }

            period = (Period) periodSelection.getSelectedItem();
            if (period == null) {
                showWarning(getString("error.no_period_found"), getComponent());
                return false;
            }
            return true;
        }

        public JComponent getComponent() {
            return content;
        }
    }

    class ReservationAllocation
        implements
            WizardPanel
    {
        JPanel content = new JPanel();
        AppointmentSelection appointmentSelection;
        AllocatableSelection allocatableSelection;

        public ReservationAllocation() throws RaplaException {
            appointmentSelection = new AppointmentSelection(getContext());
            allocatableSelection = new AllocatableSelection(getContext());
            content.setLayout(new BorderLayout());
            content.add(appointmentSelection.getComponent(),BorderLayout.NORTH);
            content.add(allocatableSelection.getComponent(),BorderLayout.CENTER);
            Border emptyBorder = new EmptyLineBorder();
            appointmentSelection.getComponent().setBorder(BorderFactory.createTitledBorder(emptyBorder, getString("enter_appointments")));
            allocatableSelection.getComponent().setBorder(BorderFactory.createTitledBorder(emptyBorder, getString("select_persons_and_resources")));
        }

        public void setReservation(Reservation reservation) throws RaplaException {
            allocatableSelection.setReservation(reservation);
            appointmentSelection.setReservation(reservation);
            appointmentSelection.addAppointmentListener(allocatableSelection);
        }

        public void refresh(ModificationEvent evt) throws RaplaException {
            allocatableSelection.refresh(evt);
        }

        public void newAppointment(Date start,Date end) throws RaplaException {
            appointmentSelection.newAppointment(start,end);
        }

        public void removeAllAppointments() {
            appointmentSelection.removeAllAppointments();
        }

        public String getHelp() {
            return getString("reservation_wizard.panel2");
        }

        public String getDefaultAction() {
            return FINISH;
        }

        public ActionMap getActionMap() {
            ActionMap map = createActionMap();
            map.get(NEXT).putValue(Action.NAME, getString("reservation_wizard.search_free_appointment"));
            map.get(NEXT).putValue(Action.SMALL_ICON,getIcon("icon.calendar"));
            return map;
        }

        public Collection<Allocatable> getAllocatables() {
            return allocatableSelection.getMarkedAllocatables();
        }

        public JComponent getComponent() {
            return content;
        }
    }

    class ReservationWeekview
        implements
            WizardPanel
            ,ViewListener
    {
        JPanel content = new JPanel();
        SwingWeekView wv= new SwingWeekView();
        RaplaBuilder builder;
        Date start= null;
        Date end = null;
        Action nextAction;

        public ReservationWeekview() throws RaplaException {
            builder = new SwingRaplaBuilder(getContext());
            content.setLayout(new BorderLayout());
            content.add(wv.getComponent(),BorderLayout.CENTER);
            CalendarOptions opt = getCalendarOptions();
            wv.setTimeZone( DateTools.getTimeZone());
            wv.setExcludeDays( opt.getExcludeDays() );
            wv.setWorktime( opt.getWorktimeStart(), opt.getWorktimeEnd());
            wv.addBuilder(builder);
            //wv.setDateVisible(false);
            wv.addCalendarViewListener(this);
        }

        public void setAllocatables(Collection<Allocatable> markedAllocatables) throws RaplaException {
            Iterator<Allocatable> it = markedAllocatables.iterator();
            StringBuffer buf = new StringBuffer();
            int i=0;
            while (it.hasNext()) {
                if (i>0)
                    buf.append(", ");
                buf.append( getName( it.next() ) );
                i++;
            }
            String title = getI18n().format("list.format",period.getName(),buf.toString());
            content.setBorder(BorderFactory.createTitledBorder(title));
            wv.setToDate(period.getStart());
            Collection<Reservation> otherReservations =
                Arrays.asList(getQuery().getReservations((User) null
                                                         ,wv.getStartDate()
                                                         ,wv.getEndDate()
                                                         ,null)
                              );
            Collection<Reservation> reservations = new ArrayList<Reservation>(otherReservations);
            reservations.add(reservation);
            builder.selectAllocatables(markedAllocatables);
            builder.selectReservations(reservations);
        }

        public void build() {
            wv.rebuild();
            wv.scrollToStart();
        }

        public JComponent getComponent() {
            return content;
        }

        public String getHelp() {
            return getString("reservation_wizard.panel3");
        }


        public ActionMap getActionMap() {
            ActionMap map = createActionMap();
            nextAction = map.get(NEXT);
            nextAction.putValue(Action.NAME, getString("reservation_wizard.add_appointment"));
            nextAction.putValue(Action.SMALL_ICON, getIcon("icon.new"));
            nextAction.setEnabled(false);
            map.get(FINISH).setEnabled(false);
            return map;
        }

        public Date getStart() {
            return start;
        }

        public String getDefaultAction() {
            return FINISH;
        }

        public Date getEnd() {
            return end;
        }

        // Implementation of the weekview listener

        public void selectionChanged(Date start,Date end) {
            this.start = start;
            this.end = end;
            nextAction.setEnabled( start != null && end != null );
        }
        public void selectionPopup(Component component,Point p,Date start,Date end, int slotNr) {
        }
        public void blockPopup(Block block,Point p) {
        }
        public void blockEdit(Block block,Point p) {
        }
        public void moved(Block block,Point p,Date newStart, int slotNr) {
        }
        public void resized(Block block,Point p,Date newStart, Date newEnd, int slotNr) {
        }

    }

}

