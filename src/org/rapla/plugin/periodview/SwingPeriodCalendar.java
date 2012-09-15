
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

package org.rapla.plugin.periodview;

import java.awt.Component;
import java.awt.Point;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.rapla.components.calendarview.swing.AbstractSwingCalendar;
import org.rapla.components.calendarview.swing.SwingPeriodView;
import org.rapla.components.calendarview.swing.ViewListener;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.facade.PeriodModel;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.plugin.abstractcalendar.AbstractRaplaSwingCalendar;
import org.rapla.plugin.abstractcalendar.GroupAllocatablesStrategy;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;
import org.rapla.plugin.abstractcalendar.RaplaCalendarViewListener;
import org.rapla.plugin.abstractcalendar.SwingRaplaBuilder;

/** @deprecated */
public class SwingPeriodCalendar extends AbstractRaplaSwingCalendar
{

    public SwingPeriodCalendar(RaplaContext sm,CalendarModel settings, boolean editable) throws RaplaException {
        super( sm, settings, editable);
    }

    protected AbstractSwingCalendar createView( boolean showScrollPane) 
    {
        return new SwingPeriodView( showScrollPane );
    }

    protected ViewListener createListener() throws RaplaException {
           return new RaplaCalendarViewListener(getContext(), model, view.getComponent()) {
            /** override to change start- and end-dates and the repeating count */
        	@Override
            protected void showPopupMenu(Component component,Point p,Date start,Date end, int slotNr, Collection<Allocatable> selectedAllocatables) {
                int repeatings = (int)((end.getTime() - start.getTime())/DateTools.MILLISECONDS_PER_WEEK) + 1;
                Calendar cal = getRaplaLocale().createCalendar();

                cal.setTime(getModel().getSelectedDate());
                int firstDay = cal.get(Calendar.DAY_OF_WEEK) ;

                cal.setTime ( end );
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                int day_of_week = cal.get(Calendar.DAY_OF_WEEK);
                while ( day_of_week < firstDay ) {
                    day_of_week += 7;
                }

                cal.setTime ( start );
                int day_of_week_end = cal.get(Calendar.DAY_OF_WEEK) ;
                while ( day_of_week_end < firstDay ) {
                    day_of_week_end += 7;
                }

                int decal = day_of_week - day_of_week_end;
                // System.out.println("start=" + start + "\nend=" + end + "\nfirstday=" + firstDay + " day_of_week=" + day_of_week + " dayofweekend=" + day_of_week_end + " decal=" + decal);
                if ( decal < 0 ) {
                    cal.add(Calendar.DATE, decal);
                    start = new Date ( cal.getTime().getTime() );
                    cal.add(Calendar.DATE, -decal);
                    repeatings++ ;
                }

                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                if ( decal > 0 )
                    cal.add(Calendar.DATE, decal);
                end = new Date ( cal.getTime().getTime() );
                //showPopupMenu( component, p, start,end,   selectedAllocatables, repeatings);
            }
        };
    }

	protected void configureView() throws RaplaException {
        CalendarOptions calendarOptions = getCalendarOptions();
        Set<Integer> excludeDays = calendarOptions.getExcludeDays();
        view.setExcludeDays( excludeDays );
        {
            SwingPeriodView view = (SwingPeriodView)this.view;
            view.setRowsPerHour( 1 );
            view.setRowSize( 16);
        }
//        if ( !view.isEditable() ) {
//            view.setSlotSize( model.getSize());
//        } else {
//            view.setSlotSize( 140 );
//        }

        ((SwingPeriodView)view).setWorktime( calendarOptions.getWorktimeStart(), calendarOptions.getWorktimeEnd());

        PeriodModel periodModel =getPeriodModel();
        Date start = model.getSelectedDate();
        Date end = model.getEndDate();
        Period selectedPeriod = periodModel.getNearestPeriodForStartDate( start, end);
        if ( selectedPeriod != null) {
            end  = selectedPeriod.getEnd();
        }
        view.setToDate( start );
        ((SwingPeriodView)view).setPeriodEnd( end );
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

    public int getIncrementSize()
    {
        return Calendar.WEEK_OF_YEAR;
    }
}
