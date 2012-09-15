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
package org.rapla.plugin.monthview;

import java.util.Calendar;
import java.util.Set;

import org.rapla.components.calendarview.html.AbstractHTMLView;
import org.rapla.components.calendarview.html.HTMLMonthView;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.plugin.abstractcalendar.AbstractHTMLCalendarPage;
import org.rapla.plugin.abstractcalendar.GroupAllocatablesStrategy;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;

public class HTMLMonthViewPage extends AbstractHTMLCalendarPage
{
    public HTMLMonthViewPage( RaplaContext context,  CalendarModel calendarModel ) throws RaplaException
    {
        super( context, calendarModel );
    }

    protected AbstractHTMLView createCalendarView() {
        HTMLMonthView monthView = new HTMLMonthView();
        return monthView;
    }

    protected RaplaBuilder createBuilder() throws RaplaException {
        RaplaBuilder builder = super.createBuilder();
        builder.setSmallBlocks( true );

        GroupAllocatablesStrategy strategy = new GroupAllocatablesStrategy( getRaplaLocale().getLocale() );
        boolean compactColumns = getCalendarOptions().isCompactColumns() ||  builder.getAllocatables().size() ==0 ;
        strategy.setFixedSlotsEnabled( !compactColumns);
        builder.setBuildStrategy( strategy );

        return builder;
    }

    protected int getIncrementSize() {
        return Calendar.MONTH;
    }

	@Override
	protected void configureView() throws RaplaException {
		CalendarOptions opt = getCalendarOptions();
		Set<Integer> excludeDays = opt.getExcludeDays();
		view.setExcludeDays( excludeDays );
	}

}

