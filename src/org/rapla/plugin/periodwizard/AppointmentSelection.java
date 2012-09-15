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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.calendar.RaplaTime;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.internal.edit.reservation.AbstractAppointmentEditor;
import org.rapla.gui.internal.edit.reservation.AppointmentIdentifier;
import org.rapla.gui.toolkit.WeekdayChooser;


class AppointmentSelection extends AbstractAppointmentEditor
{
    ArrayList<AppointmentPanel> appointmentList = new ArrayList<AppointmentPanel>();
    JPanel appointmentListPanel = new JPanel();
    Period period;
    Reservation reservation;
    
    public AppointmentSelection(RaplaContext sm) throws RaplaException {
        super( sm);
        appointmentListPanel.setLayout(new BoxLayout(appointmentListPanel
                                                     ,BoxLayout.Y_AXIS));

            addNewRow();
            addNewRow();
            addNewRow();
            addNewRow();
            addNewRow();
    }

    public void newAppointment(Date start,Date end) throws RaplaException {
        Iterator<AppointmentPanel> it = appointmentList.iterator();
        while (it.hasNext()) {
            AppointmentPanel panel = it.next();
            if (!panel.isUsed()) {
                panel.newAppointment(start,end);
                return;
            }
        }
        addNewRow();
        AppointmentPanel panel = appointmentList.get(appointmentList.size()-1);
        panel.newAppointment(start,end);
        appointmentListPanel.revalidate();
        appointmentListPanel.repaint();
    }

    public void removeAllAppointments() {
        Iterator<AppointmentPanel> it = appointmentList.iterator();
        while (it.hasNext()) {
            AppointmentPanel panel = it.next();
            panel.clearAll();
        }
    }

    public void addNewRow() {
        AppointmentPanel panel = new AppointmentPanel(getRaplaLocale().getLocale());
        appointmentListPanel.add(panel.getComponent());
        appointmentList.add(panel);
    }

    public void removeAppointmentPanel(AppointmentPanel panel) {
        appointmentListPanel.remove(panel.getComponent());
        appointmentList.remove(panel);
        if (appointmentList.size() <5)
            addNewRow();
        appointmentListPanel.revalidate();
        appointmentListPanel.repaint();
    }

    public JComponent getComponent() {
        return appointmentListPanel;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;

    }
    public void setPeriod(Period period) {
        this.period = period;
    }

    protected void fireAppointmentAdded(Collection<Appointment> appointment) {
        super.fireAppointmentAdded(appointment);
        Iterator<AppointmentPanel> it = appointmentList.iterator();
        while (it.hasNext())
            ((AppointmentPanel) it.next()).updateIndex();
    }

    protected void fireAppointmentRemoved(Collection<Appointment> appointment) {
        super.fireAppointmentRemoved(appointment);
        Iterator<AppointmentPanel> it = appointmentList.iterator();
        while (it.hasNext())
            ((AppointmentPanel) it.next()).updateIndex();
    }

    public class AppointmentPanel implements ActionListener,DateChangeListener {
        AppointmentIdentifier identifier = new AppointmentIdentifier();
        JPanel content = new JPanel();
        private Calendar calendar;
        WeekdayChooser weekdayChooser;
        JLabel newLabel = new JLabel();
        JButton delete = new JButton();
        RaplaTime startTime;
        JLabel startLabel = new JLabel();
        RaplaTime endTime;
        JLabel endLabel = new JLabel();
        Appointment appointment;
        private boolean listenerEnabled = true;
        public AppointmentPanel(Locale locale) {
            calendar = Calendar.getInstance(DateTools.getTimeZone(),locale);

            TableLayout tableLayout = new TableLayout(new double[][] {
                {
                    TableLayout.PREFERRED
                    ,28
                    ,TableLayout.PREFERRED
                    ,5
                    ,TableLayout.PREFERRED
                    ,TableLayout.PREFERRED
                    ,5
                    ,TableLayout.PREFERRED
                    ,TableLayout.PREFERRED
                } ,
                {
                    3
                    ,TableLayout.PREFERRED
                    ,6
                }
            });
            content.setLayout(tableLayout);
            startTime = new RaplaTime(locale);
            startTime.setTimeZone(DateTools.getTimeZone());
            endTime = new RaplaTime(locale);
            endTime.setTimeZone(DateTools.getTimeZone());
            setAppointmentVisible(false);
            weekdayChooser = new WeekdayChooser(locale);
            weekdayChooser.setSelectedItem(null);
            weekdayChooser.addActionListener(this);
            delete.addActionListener(this);

            content.add("0,1", delete);
            content.add("1,1,r,f", identifier);
            content.add("0,1,1,1", newLabel);
            content.add("2,1", weekdayChooser);
            content.add("4,1,c,r", startLabel);
            content.add("5,1", startTime);
            content.add("7,1,c,r", endLabel);
            content.add("8,1", endTime);

            newLabel.setText(getString("new_appointment")+":");
            startLabel.setText(getString("time_at") + ": ");
            endLabel.setText(getString("time_until") + ": ");
            delete.setText(getString("delete"));
            delete.setIcon(getIcon("icon.delete"));
        }

        public JComponent getComponent() {
            return content;
        }

        public void clearAll() {
            if (!isUsed())
                return;

            reservation.removeAppointment(appointment);
            fireAppointmentRemoved(Collections.singleton(appointment));
            appointment = null;
            weekdayChooser.setSelectedItem(null);
            setAppointmentVisible(false);
        }

        public boolean isUsed() {
            return appointment != null;
        }

        public void newAppointment(Date startDate,Date endDate) throws RaplaException {
            calendar.setTime(startDate);
            weekdayChooser.selectWeekday(calendar.get(Calendar.DAY_OF_WEEK));
            startTime.setTime(startDate);
            endTime.setTime(endDate);
            content.revalidate();
        }

        public void updateIndex() {
            if (!isUsed())
                return;
            Appointment[] apps = reservation.getAppointments();
            for (int i=0;i<apps.length;i++) {
                if ( apps[i].equals( appointment ) ) {
                    identifier.setIndex( i );
                    identifier.setText( getRaplaLocale().formatNumber( i+1 ) );
                }
            }
        }

        private void newAppointment() throws RaplaException {
            RaplaLocale f = getRaplaLocale();
            Appointment[] apps = reservation.getAppointments();
            Date startTime;
            Date endTime;
            if (apps.length>0) {
                startTime = apps[apps.length-1].getStart();
                endTime = apps[apps.length-1].getEnd();
            } else {
                startTime = new Date(DateTools.MILLISECONDS_PER_HOUR * getCalendarOptions().getWorktimeStart());
                endTime = new Date(startTime.getTime() + DateTools.MILLISECONDS_PER_HOUR);
            }

            Date startDate = f.toDate(getDate(),startTime);
            Date endDate = f.toDate(getDate(),endTime);
            createAppointment(startDate,endDate);
        }

        public void createAppointment(Date startDate,Date endDate) throws RaplaException {
            appointment = getModification().newAppointment(startDate, endDate);
            appointment.setRepeatingEnabled(true);
            Repeating repeating = appointment.getRepeating();
            repeating.setType( RepeatingType.WEEKLY );
            repeating.setEnd(period.getEnd());
            startTime.setTime(appointment.getStart());
            endTime.setTime(appointment.getEnd());
            reservation.addAppointment(appointment);
            startTime.addDateChangeListener(this);
            endTime.addDateChangeListener(this);

            fireAppointmentAdded(Collections.singleton(appointment));
        }

        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() == weekdayChooser && weekdayChooser.getSelectedWeekday()>=0) {
                selectFirstInPeriod(weekdayChooser.getSelectedWeekday());
                if (appointment == null) {
                    try {
                        newAppointment();
                    } catch (Exception ex) {
                        showException(ex,getComponent());
                    }
                    setAppointmentVisible(true);
                }
                update();
            } if (evt.getSource() == delete) {
                removeAppointmentPanel(this);
                reservation.removeAppointment(appointment);
                fireAppointmentRemoved(Collections.singleton(appointment));
                appointment = null;
            }
        }

        /** Selects the first appearence of the specified weekday in the period as startdate*/
        private void selectFirstInPeriod(int dayOfWeek) {
            calendar.setTime(period.getStart());
            calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            if (calendar.getTime().before(period.getStart())) {
                calendar.add(Calendar.DAY_OF_WEEK,7);
            }
        }

        /** returns the selected date (not the time) */
        private Date getDate() {
            return calendar.getTime();
        }

        public void dateChanged(DateChangeEvent evt) {
            if (!listenerEnabled)
                return;
            try {
                listenerEnabled = false;
                RaplaLocale f = getRaplaLocale();
                long duration = appointment.getEnd().getTime() - appointment.getStart().getTime();
                if (evt.getSource() == startTime) {
                    Date newStart = f.toDate(getDate(),startTime.getTime());
                    Date newEnd = new Date(newStart.getTime() + duration);
                    if (newEnd.getTime() >= getDate().getTime() + DateTools.MILLISECONDS_PER_DAY) {
                        newEnd = new Date(
                                          getDate().getTime()
                                          + DateTools.MILLISECONDS_PER_DAY
                                          - DateTools.MILLISECONDS_PER_MINUTE
                                          );
                    }
                    endTime.setTime(newEnd);
                    getLogger().debug("enddate adjusted");
                }
                if (evt.getSource() == endTime) {
                    Date newEnd = f.toDate(getDate(),endTime.getTime());
                    if (appointment.getStart().after(newEnd)) {
                        startTime.setTime(newEnd);
                        getLogger().debug("startdate adjusted");
                    }
                }
            } finally {
                listenerEnabled = true;
            }
            update();
        }

        private void update() {
            RaplaLocale f = getRaplaLocale();
            Date start = f.toDate(getDate(),startTime.getTime());
            Date end =  f.toDate(getDate(),endTime.getTime());
            appointment.move(start,end);
            fireAppointmentChanged(Collections.singleton(appointment));
        }

        private void setAppointmentVisible(boolean visible) {
            newLabel.setVisible(!visible);
            delete.setVisible(visible);
            startLabel.setVisible(visible);
            startTime.setVisible(visible);
            endLabel.setVisible(visible);
            endTime.setVisible(visible);
            identifier.setVisible(visible);
        }

    }
}
