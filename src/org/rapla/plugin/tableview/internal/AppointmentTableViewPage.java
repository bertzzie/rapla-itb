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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.tableview.AppointmentTableColumn;
import org.rapla.servletpages.RaplaPageGenerator;

public class AppointmentTableViewPage extends TableViewPage implements RaplaPageGenerator
{
    public AppointmentTableViewPage( RaplaContext context, CalendarModel calendarModel ) throws RaplaException
    {
        super( context );
        this.model = calendarModel;
    }
    
    public String getCalendarHTML() throws RaplaException {
        final List<AppointmentBlock> blocks = model.getBlocks();
       
       Map<?,?> map2 = getContainer().lookupServicesFor(RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN);
       Collection< ? extends AppointmentTableColumn> appointmentColumnPlugins = (Collection<? extends AppointmentTableColumn>) map2.values();

       StringBuffer buf = new StringBuffer();
       buf.append("<table class='eventtable'>");
       
       for (AppointmentTableColumn col: appointmentColumnPlugins)
       {
    	   buf.append("<th>");
           buf.append(col.getColumnName());
           buf.append("</th>");
       }

       for (AppointmentBlock b :blocks)
       {
           buf.append("<tr>");
           for (AppointmentTableColumn col: appointmentColumnPlugins)
           {
        	   buf.append("<td>");
               buf.append(col.getHtmlValue(b));
               buf.append("</td>");
           }
          
           buf.append("</tr>");
       }
       buf.append("</table>");
       final String result = buf.toString();
       return result;
    }
   
}

