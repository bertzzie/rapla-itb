/*--------------------------------------------------------------------------*
 | Copyright (C) 2006-2003 Christopher Kohlhaas, Bettina Lademann           |
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

import javax.swing.table.AbstractTableModel;

import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.Named;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentFormater;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.Conflict;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class ConflictTableModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;

    Conflict[] conflicts;
    String[] columnNames;
    AppointmentFormater appointmentFormater;
    I18nBundle i18n;

    public ConflictTableModel(RaplaContext serviceManager,Conflict[] conflicts) throws RaplaException
    {
        this.conflicts = conflicts;
        i18n = (I18nBundle) serviceManager.lookup(I18nBundle.ROLE + "/org.rapla.RaplaResources");
        appointmentFormater = (AppointmentFormater) serviceManager.lookup(AppointmentFormater.ROLE);
        
        columnNames = new String[]
            {
                i18n.getString("conflict.reservation1")
                , i18n.getString("conflict.appointment1")
                , i18n.getString("conflict.resource")
                , i18n.getString("conflict.reservation2")
                , i18n.getString("conflict.user")
                , i18n.getString("conflict.appointment2")
            };
        
    }

    private String getName(Named named) {
        return named.getName(i18n.getLocale());
    }

    public String getColumnName(int c)
    {
       return columnNames[c];
    }

    public int getColumnCount()
    {
        return columnNames.length;
    }

    public int getRowCount()
    {
        return conflicts.length;
    }

    public Object getValueAt(int r, int c)
    {

        switch (c) {

        case 0: return(getName(conflicts[r].getReservation1()));
        case 1: return(appointmentFormater.getSummary(conflicts[r].getAppointment1()));
        case 2: return(getName(conflicts[r].getAllocatable()));
        case 3: return(getName(conflicts[r].getReservation1()));
        case 4: return(conflicts[r].getUser2().getUsername());
        case 5: return(appointmentFormater.getSummary(conflicts[r].getAppointment2()));
            }
        return null;
    }

    public Reservation getReservationAt(int i) {
        return conflicts[i].getReservation1();
    }

    public Appointment getAppointmentAt(int i) {
        return conflicts[i].getAppointment1();
    }

}









