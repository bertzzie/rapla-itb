
/*--------------------------------------------------------------------------*
 | Copyright (C) 2006  Christopher Kohlhaas                                 |
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

import java.awt.Color;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.swing.JComponent;

import org.rapla.components.calendar.WeekendHighlightRenderer;
import org.rapla.components.calendarview.swing.AbstractSwingCalendar;
import org.rapla.components.calendarview.swing.SwingMonthView;
import org.rapla.components.calendarview.swing.ViewListener;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Appointment;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.plugin.abstractcalendar.AbstractRaplaSwingCalendar;
import org.rapla.plugin.abstractcalendar.GroupAllocatablesStrategy;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;
import org.rapla.plugin.abstractcalendar.RaplaCalendarViewListener;
import org.rapla.plugin.abstractcalendar.SwingRaplaBuilder;


public class SwingMonthCalendar extends AbstractRaplaSwingCalendar
{
    public SwingMonthCalendar(RaplaContext sm,CalendarModel settings, boolean editable) throws RaplaException {
        super( sm, settings, editable);
    }

    public static Color DATE_NUMBER_COLOR_HIGHLIGHTED = Color.black;

    protected AbstractSwingCalendar createView(boolean editable) {
        boolean showScrollPane = editable;

        /** renderer for weekdays in month-view */
        final WeekendHighlightRenderer weekdayRenderer = new WeekendHighlightRenderer();
        SwingMonthView monthView = new SwingMonthView( showScrollPane ) {
            
            protected JComponent createSlotHeader(int weekday) {
                JComponent component = super.createSlotHeader( weekday );
                if (isEditable()) {
                    component.setOpaque(true);
                    Color color = weekdayRenderer.getBackgroundColor( weekday, 1, 1, 1);
                    component.setBackground(color);
                }
                return component;
            }
            
            protected Color getNumberColor( Date date )
            {
                boolean today = DateTools.isSameDay(getQuery().today().getTime(), date.getTime());
                if ( today)
                {
                    return DATE_NUMBER_COLOR_HIGHLIGHTED;
                }
                else
                {
                    return super.getNumberColor( date );
                }
            }
        };
        monthView.setDaysInView( 25);
		return monthView;
    }

    protected ViewListener createListener() throws RaplaException {
        RaplaCalendarViewListener listener = new RaplaCalendarViewListener(getContext(), model, view.getComponent()) {
                /* if the selcted view is a month-view or compact-view, the start-time will not be the selected time,
                * but the time of the start-time of the appointment instead. The start-date is taken from the passed date.
                * */
                protected Date calcStartDate(Date date, Appointment appointment) {
                    return getRaplaLocale().toDate( date, appointment.getStart() );
                }

        };
        listener.setKeepTime( true);
		return listener;
    }

    protected RaplaBuilder createBuilder() throws RaplaException
    {
        RaplaBuilder builder = new SwingRaplaBuilder(getContext());
        builder.setRepeatingVisible( view.isEditable());
        builder.setEditingUser( getUser() );
        builder.setExceptionsExcluded( !getCalendarOptions().isExceptionsVisible() || !view.isEditable());
        builder.setFromModel( model, view.getStartDate(), view.getEndDate() );

        builder.setSmallBlocks( true );

        GroupAllocatablesStrategy strategy = new GroupAllocatablesStrategy( getRaplaLocale().getLocale() );
        boolean compactColumns = getCalendarOptions().isCompactColumns() ||  builder.getAllocatables().size() ==0 ;
        strategy.setFixedSlotsEnabled( !compactColumns);
        builder.setBuildStrategy( strategy );

        return builder;
    }

    protected void configureView() throws RaplaException {
        CalendarOptions calendarOptions = getCalendarOptions();
        Set<Integer> excludeDays = calendarOptions.getExcludeDays();

        view.setExcludeDays( excludeDays );
//        if ( !view.isEditable() ) {
//            view.setSlotSize( model.getSize());
//        } else {
//            view.setSlotSize( 150 );
//        }
        view.setToDate(model.getSelectedDate());
    }

    public int getIncrementSize()
    {
        return Calendar.MONTH;
    }



}
