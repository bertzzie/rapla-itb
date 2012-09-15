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
package org.rapla.plugin.periodview;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.html.AbstractHTMLView;
import org.rapla.components.calendarview.html.HTMLPeriodView;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Period;
import org.rapla.facade.PeriodModel;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.plugin.abstractcalendar.AbstractHTMLCalendarPage;
import org.rapla.plugin.abstractcalendar.AbstractRaplaBlock;
import org.rapla.plugin.abstractcalendar.GroupAllocatablesStrategy;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;

public class HTMLPeriodViewPage extends AbstractHTMLCalendarPage
{
    public HTMLPeriodViewPage( RaplaContext context, CalendarModel calendarModel ) throws RaplaException
    {
        super( context, calendarModel );
    }

    protected AbstractHTMLView createCalendarView() {
        HTMLPeriodView periodView = new HTMLPeriodView() {
          public Block sameAppointment(int day, Block bl) {
                Iterator<Block> it2 = slots[day].iterator();
                Block b;
                while (it2.hasNext()) {
                    b = it2.next();
                    if ( ((AbstractRaplaBlock)b).getAppointment()
                         ==  ((AbstractRaplaBlock)bl).getAppointment()
                        ) {
                        return b;
                    }
                }
                return null;
            }
        };
 
        periodView.setLocale( getRaplaLocale().getLocale() );
        periodView.setTimeZone(getRaplaLocale().getTimeZone());

        Date start = model.getStartDate();
        Date end = model.getEndDate();
        Date selectedDate = model.getSelectedDate();
        PeriodModel periodModel =getPeriodModel();
        Period selectedPeriod = periodModel.getNearestPeriodForStartDate( start, end);

	/*
	 * If the user does not change the start date then
	 * the choosen period is displayed.
	 * If the user choose another date, then 7 weeks are displayed.
	 */
	if ( start == null ) {
	    start = getQuery().today();
            end = DateTools.addDays(start, 7 * 7);
	} else {
	    if ( selectedPeriod == null
		 || selectedPeriod.getStart().getTime()
		 != selectedDate.getTime()) {
		end = DateTools.addDays(selectedDate, 7 * 7);
	    } else {
		start = selectedPeriod.getStart();
		end  = selectedPeriod.getEnd();
	    }
	}

        periodView.setToDate( start );
        periodView.setPeriodEnd( end );

        return periodView;
    }

    protected RaplaBuilder createBuilder() throws RaplaException {
        RaplaBuilder builder = super.createBuilder();

        GroupAllocatablesStrategy strategy = new GroupAllocatablesStrategy( getRaplaLocale().getLocale() );
        boolean compactColumns = getCalendarOptions().isCompactColumns() ||  builder.getAllocatables().size() ==0 ;
        strategy.setFixedSlotsEnabled( !compactColumns);
        builder.setBuildStrategy( strategy );

        return builder;
    }

    protected int getIncrementSize() {
        return Calendar.WEEK_OF_YEAR;
    }

	@Override
	protected void configureView() throws RaplaException {
		CalendarOptions opt = getCalendarOptions();
		Set<Integer> excludeDays = opt.getExcludeDays();
		view.setExcludeDays( excludeDays );
		
	}

}

