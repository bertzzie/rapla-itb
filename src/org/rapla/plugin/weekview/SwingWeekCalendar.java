
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

package org.rapla.plugin.weekview;

import java.awt.Color;
import java.awt.Font;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateRenderer;
import org.rapla.components.calendar.DateRendererAdapter;
import org.rapla.components.calendarview.swing.AbstractSwingCalendar;
import org.rapla.components.calendarview.swing.SwingWeekView;
import org.rapla.components.util.DateTools;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.plugin.abstractcalendar.AbstractRaplaSwingCalendar;
import org.rapla.plugin.abstractcalendar.GroupAllocatablesStrategy;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;
import org.rapla.plugin.abstractcalendar.SwingRaplaBuilder;


public class SwingWeekCalendar extends AbstractRaplaSwingCalendar
{
    public SwingWeekCalendar( RaplaContext sm, CalendarModel model, boolean editable ) throws RaplaException
    {
        super( sm, model, editable );
    }

    protected AbstractSwingCalendar createView(boolean showScrollPane) {
        /** renderer for dates in weekview */
        final DateRendererAdapter dateRenderer = new DateRendererAdapter((DateRenderer) getService(DateRenderer.class), getRaplaLocale().getTimeZone(), getRaplaLocale().getLocale());
        SwingWeekView wv = new SwingWeekView( showScrollPane ) {
            
            protected JComponent createSlotHeader(Integer column) {
                JLabel component = (JLabel) super.createSlotHeader(column);
                Date date = getDateFromColumn(column);
                boolean today = DateTools.isSameDay(getQuery().today().getTime(), date.getTime());
                if ( today)
                {
                    component.setFont(component.getFont().deriveFont( Font.BOLD));
                }
                if (isEditable() && dateRenderer != null ) {
                     component.setOpaque(true);
                     Color color = dateRenderer.getBackgroundColor(date);
                     String toolTip = dateRenderer.getToolTipText(date);
                     component.setBackground(color);
                     component.setToolTipText(toolTip);
                }
                return component;
            }
            
        

            @Override
            public void rebuild() {
                super.rebuild();
                // update week
                weekTitle.setText(MessageFormat.format(getString("calendarweek.abbreviation"), getStartDate()));
            }
        };
        return wv;
    }

    public void dateChanged(DateChangeEvent evt) {
        super.dateChanged( evt );
        ((SwingWeekView)view).scrollDateVisible( evt.getDate());
    }

    protected RaplaBuilder createBuilder() throws RaplaException
    {
        RaplaBuilder builder = new SwingRaplaBuilder(getContext());
        builder.setRepeatingVisible( view.isEditable());
        builder.setEditingUser( getUser() );
        builder.setExceptionsExcluded( !getCalendarOptions().isExceptionsVisible() || !view.isEditable());
        builder.setFromModel( model, view.getStartDate(), view.getEndDate() );

        GroupAllocatablesStrategy strategy = new GroupAllocatablesStrategy( getRaplaLocale().getLocale() );
        boolean compactColumns = getCalendarOptions().isCompactColumns() ||  builder.getAllocatables().size() ==0 ;
        strategy.setFixedSlotsEnabled( !compactColumns);
        strategy.setResolveConflictsEnabled( true );
        builder.setBuildStrategy( strategy );
        return builder;
    }

    @Override
    protected void configureView() {
        SwingWeekView view = (SwingWeekView) this.view;
        CalendarOptions calendarOptions = getCalendarOptions();
        int rowsPerHour = calendarOptions.getRowsPerHour();
        int start = calendarOptions.getWorktimeStart();
        int end = calendarOptions.getWorktimeEnd();
        int hours = Math.max(1, end - start);
        view.setRowsPerHour( rowsPerHour );
        if ( rowsPerHour == 1 ) {
            if ( hours < 10)
            {
                view.setRowSize( 80);
            }
            else if ( hours < 15)
            {
                view.setRowSize( 60);
            }
            else
            {
                view.setRowSize( 30);
            }
        } else if ( rowsPerHour == 2 ) {
            if ( hours < 10)
            {
                view.setRowSize( 40);
            }
            else
            {
                view.setRowSize( 20);
            }
        } 
        else if ( rowsPerHour >= 4 )
        {
            view.setRowSize( 15);
        }
        view.setWorktime( calendarOptions.getWorktimeStart(), calendarOptions.getWorktimeEnd());
        int days = getDays( calendarOptions);
		view.setDaysInView( days);
		Set<Integer> excludeDays = calendarOptions.getExcludeDays();
		if ( days <3)
		{
			excludeDays = new HashSet<Integer>();
		}
        view.setExcludeDays( excludeDays );
        
        view.setFirstWeekday( calendarOptions.getFirstDayOfWeek());
        view.setToDate(model.getSelectedDate());

//        if ( !view.isEditable() ) {
//            view.setSlotSize( model.getSize());
//        } else {
      //      view.setSlotSize( 135 );
 //       }
    }

    /** overide this for dayly views*/
	protected int getDays(CalendarOptions calendarOptions) {
		return calendarOptions.getDaysInWeekview();
	}


    public void scrollToStart()
    {
        ((SwingWeekView)view).scrollToStart();
    }

    public int getIncrementSize() {
        return Calendar.WEEK_OF_YEAR;
    }

}
