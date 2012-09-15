/*--------------------------------------------------------------------------*
 | Copyright (C) 2012 Christopher Kohlhaas                                  |
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
package org.rapla.plugin.tableview.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.tableview.RaplaTableColumn;
import org.rapla.plugin.tableview.ReservationTableColumn;
import org.rapla.servletpages.RaplaPageGenerator;

public class ReservationTableViewPage extends TableViewPage implements RaplaPageGenerator
{
    public ReservationTableViewPage( RaplaContext context, CalendarModel calendarModel ) throws RaplaException
    {
        super( context );
        this.model = calendarModel;
    }
    
    class ReservationRow implements Comparable<ReservationRow>
    {
        Reservation r;
        ReservationRow(Reservation r)
        {
            this.r = r;
        }
        
        private Date getStartDate() {
            return r.getFirstDate();
        }
        
        public int compareTo(ReservationRow o) {
            return getStartDate().compareTo(o.getStartDate());
        }

        public String getName() 
        {
            return r.getName( getLocale());
        }
    }

    public String getCalendarHTML() throws RaplaException {
        final Date startDate = model.getStartDate();
        final Date endDate = model.getEndDate();
     
        final Reservation[] reservations = model.getReservations(startDate, endDate);
       List<ReservationRow> rows = new ArrayList<ReservationRow>();
       for (Reservation r :reservations)
       {
           rows.add( new ReservationRow( r));
       }
       Collections.sort( rows);
       
       Map<?,?> map2 = getContainer().lookupServicesFor(RaplaExtensionPoints.RESERVATION_TABLE_COLUMN);
       Collection< ? extends ReservationTableColumn> reservationColumnPlugins = (Collection<? extends ReservationTableColumn>) map2.values();

       StringBuffer buf = new StringBuffer();
       buf.append("<table class='eventtable'>");
       
       for (RaplaTableColumn<Reservation> col: reservationColumnPlugins)
       {
    	   buf.append("<th>");
           buf.append(col.getColumnName());
           buf.append("</th>");
       }

       for (ReservationRow r :rows)
       {
           buf.append("<tr>");
           for (RaplaTableColumn<Reservation> col: reservationColumnPlugins)
           {
        	   buf.append("<td>");
        	   Reservation event = r.r;
               buf.append(col.getHtmlValue(event));
               buf.append("</td>");
           }
          
           buf.append("</tr>");
       }
       buf.append("</table>");
       final String result = buf.toString();
       return result;
    }
   
}

