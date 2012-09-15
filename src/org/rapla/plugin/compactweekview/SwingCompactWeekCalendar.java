
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

package org.rapla.plugin.compactweekview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.rapla.components.calendar.DateRenderer;
import org.rapla.components.calendar.DateRendererAdapter;
import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.swing.AbstractSwingCalendar;
import org.rapla.components.calendarview.swing.SwingCompactWeekView;
import org.rapla.components.calendarview.swing.ViewListener;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.plugin.abstractcalendar.AbstractRaplaBlock;
import org.rapla.plugin.abstractcalendar.AbstractRaplaSwingCalendar;
import org.rapla.plugin.abstractcalendar.GroupAllocatablesStrategy;
import org.rapla.plugin.abstractcalendar.RaplaBuilder;
import org.rapla.plugin.abstractcalendar.RaplaCalendarViewListener;
import org.rapla.plugin.abstractcalendar.SwingRaplaBuilder;


public class SwingCompactWeekCalendar extends AbstractRaplaSwingCalendar
{
    public SwingCompactWeekCalendar(RaplaContext sm,CalendarModel settings, boolean editable) throws RaplaException {
        super( sm, settings, editable);
    }
    
    protected AbstractSwingCalendar createView(boolean showScrollPane) {
        final DateRendererAdapter dateRenderer = new DateRendererAdapter(getService(DateRenderer.class), getRaplaLocale().getTimeZone(), getRaplaLocale().getLocale());
        SwingCompactWeekView compactWeekView = new SwingCompactWeekView( showScrollPane ) {
            @Override
            protected JComponent createColumnHeader(Integer column) {
                JLabel component = (JLabel) super.createColumnHeader(column);
                if ( column != null ) {
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
                }
                return component;
            }
            protected int getColumnCount() 
        	{
            	return getDaysInView();
        	}

        };
		return compactWeekView;

    }
    

    
    protected ViewListener createListener() throws RaplaException {
        return  new RaplaCalendarViewListener(getContext(), model, view.getComponent()) {
         /** override to change the allocatable to the row that is selected */
            protected void showPopupMenu(Component component,Point p,Date start,Date end, int slotNr)
            {

                if ( end.getTime()- start.getTime() == DateTools.MILLISECONDS_PER_DAY ) {
                    Calendar cal = getRaplaLocale().createCalendar();
                    cal.setTime ( start );
                    cal.set( Calendar.HOUR_OF_DAY, getCalendarOptions().getWorktimeStart());
                    start = cal.getTime();
                    end = new Date ( start.getTime() + 30 * DateTools.MILLISECONDS_PER_MINUTE );
                }
                
				Collection<Allocatable> markedAllocatables = getMarkedAllocatables();
				showContextPopup( component, p, start,end, markedAllocatables);
            }
            @Override
            protected Collection<Allocatable> getMarkedAllocatables() {
            	final List<Allocatable> selectedAllocatables = getSortedAllocatables();
				 
            	Set<Allocatable> allSelected = new HashSet<Allocatable>();
				if ( selectedAllocatables.size() == 1 ) {
					allSelected.add(selectedAllocatables.get(0));
				}
	               
				for ( int i =0 ;i< selectedAllocatables.size();i++)
				{
					int slot = i*view.getDaysInView();
					if ( ((AbstractSwingCalendar)view).isSelected(slot))
					{
						allSelected.add(selectedAllocatables.get(i));
					}
				}
				return allSelected;
            }
            
            @Override
			 public void moved(Block block, Point p, Date newStart, int slotNr) {
				 int index= slotNr / view.getDaysInView();//getIndex( selectedAllocatables, block );
				 if ( index < 0)
				 {
					 return;
				 }
				 
				 try 
				 {
					 final List<Allocatable> selectedAllocatables = getSortedAllocatables();
					 Allocatable newAlloc = selectedAllocatables.get(index);
					 AbstractRaplaBlock raplaBlock = (AbstractRaplaBlock)block;
					 Allocatable oldAlloc = raplaBlock.getGroupAllocatable();
					 Appointment app = raplaBlock.getAppointment();
					 if ( newAlloc != null && oldAlloc != null && !newAlloc.equals(oldAlloc))
					 {
						 Reservation reservation = raplaBlock.getReservation();
						 getReservationController().exchangeAllocatable(reservation,app,newStart, oldAlloc,newAlloc, getMainComponent(),p);
					 }
					 else
					 {
						 super.moved(block, p, newStart, slotNr);
					 }
					 
				 } 
				 catch (RaplaException ex) {
					showException(ex, getMainComponent());
				}
			
			 }

            /* if the selcted view is a month-view or compact-view, the start-time will not be the selected time,
            * but the time of the start-time of the appointment instead. The start-date is taken from the passed date.
            * */
            protected Date calcStartDate(Date date, Appointment appointment) {
                return getRaplaLocale().toDate( date, appointment.getStart() );
            }

        };
    }

    protected RaplaBuilder createBuilder() throws RaplaException {
        RaplaBuilder builder = new SwingRaplaBuilder(getContext());
        builder.setRepeatingVisible( view.isEditable());
        builder.setEditingUser( getUser() );
        builder.setExceptionsExcluded( !getCalendarOptions().isExceptionsVisible() || !view.isEditable());
        Date startDate = view.getStartDate();
		Date endDate = view.getEndDate();
		builder.setFromModel( model, startDate, endDate );
        builder.setSmallBlocks( true );
      
        String[] slotNames;
        final List<Allocatable> allocatables = getSortedAllocatables();
      	GroupAllocatablesStrategy strategy = new GroupAllocatablesStrategy( getRaplaLocale().getLocale() );
    	strategy.setFixedSlotsEnabled( true);
    	strategy.setResolveConflictsEnabled( false );
    	strategy.setAllocatables(allocatables) ;
    	builder.setBuildStrategy( strategy );
        slotNames = new String[ allocatables.size() ];
        for (int i = 0; i <allocatables.size(); i++ ) {
            slotNames[i] = allocatables.get(i).getName( getRaplaLocale().getLocale() );
        }
        builder.setSplitByAllocatables( true );
        ((SwingCompactWeekView)view).setLeftColumnSize( 100);
        ((SwingCompactWeekView)view).setSlots( slotNames );
        return builder;
    }

    protected void configureView() throws RaplaException {
        CalendarOptions calendarOptions = getCalendarOptions();
        Set<Integer> excludeDays = calendarOptions.getExcludeDays();
        view.setExcludeDays( excludeDays );
        view.setDaysInView( calendarOptions.getDaysInWeekview());
        int firstDayOfWeek = calendarOptions.getFirstDayOfWeek();
		view.setFirstWeekday( firstDayOfWeek);
        view.setToDate(model.getSelectedDate());
//        if ( !view.isEditable() ) {
//            view.setSlotSize( model.getSize());
//        } else {
//            view.setSlotSize( 200 );
//        }
    }

    public int getIncrementSize()
    {
        return Calendar.WEEK_OF_YEAR;
    }

  
    

}
