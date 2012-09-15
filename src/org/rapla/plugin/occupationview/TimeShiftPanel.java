/*--------------------------------------------------------------------------*
 | Copyright (C) 2011 Bob Jordaens                                          |
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

package org.rapla.plugin.occupationview;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.avalon.framework.activity.Disposable;
import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.calendar.RaplaArrowButton;
import org.rapla.components.calendar.RaplaCalendar;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.calendar.RaplaTime;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.DateTools;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.RaplaWidget;
import org.rapla.gui.toolkit.RaplaButton;

public class TimeShiftPanel extends RaplaGUIComponent implements Disposable, RaplaWidget
{
    Collection listenerList = new ArrayList();

    JPanel panel = new JPanel();
    JButton prevButton = null;

    RaplaCalendar dateSelection;
    JButton nextButton = null;

    int incrementSize = Calendar.WEEK_OF_YEAR;
    int timeShifts = 0;
    CalendarModel model;
    Listener listener = new Listener();

    JLabel timeShiftTimesLabel = new JLabel();
    RaplaNumber timeShiftTimes;
    JLabel startTimeLabel = new JLabel();
    RaplaTime startTime;
    JLabel endTimeLabel = new JLabel();
    RaplaTime endTime;
    RaplaNumber freeSlot;
    JLabel freeSlotLabel = new JLabel();
    Calendar calendarDS = getRaplaLocale().createCalendar();
    int duration = 86400000;   
    JButton todayButton= new RaplaButton(getString("today"), RaplaButton.SMALL);
// BJO 00000101
    boolean isStartDayFirstDay = false;
// BJO 00000101
    
    public TimeShiftPanel(RaplaContext sm, CalendarModel model) throws RaplaException {
        super( sm );
        setChildBundleName( OccupationPlugin.RESOURCE_FILE);
        this.model = model;

        dateSelection = createRaplaCalendar();
// BJO 00000101
        ///dateSelection.setDate(setStartOfMonth(dateSelection.getDate()));
// BJO 00000101
        double pre = TableLayout.PREFERRED;
        //double fill = TableLayout.FILL;
        // columns 10, rows = 3
        double[][] sizes = {{0.02,pre,5,pre,2,pre,0.02,pre,5,0.02}
                            ,{/*0.5,*/pre/*,0.5*/}};
        TableLayout tableLayout = new TableLayout(sizes);
        JPanel calendarPanel = new JPanel();
        Border blackline = BorderFactory.createLineBorder(Color.black);
        
        TitledBorder dateBorder = BorderFactory.createTitledBorder(blackline,getI18n().getString("date")); 
        calendarPanel.setBorder(dateBorder);

        prevButton = new RaplaArrowButton('<', 28)
        {
            private static final long serialVersionUID = 1L;
            
            public String getToolTipText(MouseEvent e) {
                
                return getString("minus1month");
            }
            
            public Point getToolTipLocation(MouseEvent event) {
                return new Point((event.getX()), (event.getY() + 20));
              }
        };
        
        prevButton.setToolTipText(""); // needed to activate tooltip
        
        nextButton = new RaplaArrowButton('>', 28)
        {
            private static final long serialVersionUID = 1L;
            
            public String getToolTipText(MouseEvent e) {
                
                return getString("plus1month");
            }
            
            public Point getToolTipLocation(MouseEvent event) {
                return new Point((event.getX()), (event.getY() + 20));
              }
        };
        
        nextButton. setToolTipText(""); // needed to activate tooltip

        panel.setLayout(tableLayout);
        calendarPanel.add(dateSelection);
        calendarPanel.add(todayButton);
        calendarPanel.add(prevButton);
        calendarPanel.add(nextButton);
        panel.add(calendarPanel, "1, 0");
       
        startTimeLabel.setText(getString("start_time"));
        calendarPanel.add( startTimeLabel );
        startTime = new RaplaTime();
        startTime.setTimeZone(DateTools.getTimeZone());
        startTime.setRowsPerHour(1);
        startTime.setTime(00,00);
        calendarPanel.add(startTime);
        startTime.addDateChangeListener(listener);

        endTimeLabel.setText(getString("end_time"));
        calendarPanel.add( endTimeLabel );
        endTime =  new RaplaTime();
        endTime.setTimeZone(DateTools.getTimeZone());
        endTime.setRowsPerHour(1);
        endTime.setTime(00,00);
        calendarPanel.add(endTime);
        endTime.addDateChangeListener(listener);
        
        JPanel optionsPanel = new JPanel();
        TitledBorder optionsBorder = BorderFactory.createTitledBorder(blackline,getString("options.timeshift"));
        optionsPanel.setBorder(optionsBorder);
        panel.add(optionsPanel,"7,0");
        
        // columns = 7, rows = 2
        optionsPanel.setLayout( new TableLayout(new double[][] {{ pre, 5, pre, 5 , pre, 5, pre }, {10, pre }}));
        
        timeShiftTimesLabel.setText(getString("horizon"));
        optionsPanel.add(timeShiftTimesLabel,"0,1,l,f");
        timeShiftTimes = new RaplaNumber(new Double(1),new Double(0),new Double(12), false);
        optionsPanel.add(timeShiftTimes,"2,1,f,f");
        timeShiftTimes.addChangeListener(listener);
        timeShifts = getQuery().getPreferences(getUser()).getEntryAsInteger( OccupationOption.MONTHS,0);
        timeShiftTimes.setNumber(timeShifts);
        
        String startDay = getQuery().getPreferences(getUser()).getEntryAsString( OccupationOption.START_DAY,OccupationOption.TODAY);
        isStartDayFirstDay = startDay.equals(OccupationOption.FIRSTDAY) ? true : false;
        
        freeSlotLabel.setText(getString("freeSlot"));
        optionsPanel.add(freeSlotLabel,"4,1,l,f");
        freeSlot = new RaplaNumber(new Double(0),new Double(0),new Double(99), false);
        optionsPanel.add(freeSlot,"6,1,f,f");
        freeSlot.addChangeListener(listener);
        nextButton.addActionListener( listener );
        prevButton.addActionListener( listener );
        
        dateSelection.addDateChangeListener( listener );
        todayButton.addActionListener(listener);
        update();
    }

    boolean listenersEnabled = true;
    public void update() throws RaplaException
    {
        listenersEnabled = false;
        try {
            if ( model.getSelectedDate() == null) {
                model.setSelectedDate( getQuery().today());
            }
            Date date = model.getSelectedDate();
            String startDay = getQuery().getPreferences(getUser()).getEntryAsString( OccupationOption.START_DAY,OccupationOption.TODAY);
            isStartDayFirstDay = startDay.equals(OccupationOption.FIRSTDAY) ? true : false;
            dateSelection.setDate( setStartOfMonth(date));
        } finally {
            listenersEnabled = true;
        }
    }

    public void dispose() {
    }

    public void setNavigationVisible( boolean enable) {
        nextButton.setVisible( enable);
        prevButton.setVisible( enable);
    }

    /** possible values are Calendar.DATE, Calendar.WEEK_OF_YEAR, Calendar.MONTH and Calendar.YEAR.
        Default is Calendar.WEEK_OF_YEAR.
     */
    public void setIncrementSize(int incrementSize) {
        this.incrementSize = incrementSize;
    }

    /** registers new DateChangeListener for this component.
     *  An DateChangeEvent will be fired to every registered DateChangeListener
     *  when the a different date is selected.
     * @see DateChangeListener
     * @see DateChangeEvent
    */
    public void addDateChangeListener(DateChangeListener listener) {
        listenerList.add(listener);
    }

    /** removes a listener from this component.*/
    public void removeDateChangeListener(DateChangeListener listener) {
        listenerList.remove(listener);
    }

    public DateChangeListener[] getDateChangeListeners() {
        return (DateChangeListener[])listenerList.toArray(new DateChangeListener[]{});
    }

    /** An ActionEvent will be fired to every registered ActionListener
     *  when the a different date is selected.
    */
    protected void fireDateChange(Date date) {
        if (listenerList.size() == 0)
            return;
        DateChangeListener[] listeners = getDateChangeListeners();
        DateChangeEvent evt = new DateChangeEvent(this,date);
        for (int i = 0;i<listeners.length;i++) {
            listeners[i].dateChanged(evt);
        }
    }

    public JComponent getComponent() {
        return panel;
    }

    public Calendar getSelectedStartTime() {
        return toDateTime( dateSelection.getDate(), startTime.getTime()); 
    }
    
    public Calendar getSelectedEndTime() {
		Date date = dateSelection.getDate();
    	Calendar cal1 = getRaplaLocale().createCalendar();
    	cal1.setTime( date );
    	cal1.add(Calendar.MILLISECOND, duration);
    	return cal1;
    }
    
    private Calendar toDateTime( Date date, Date time)
    {
        Calendar cal1 = getRaplaLocale().createCalendar();
        Calendar cal2 = getRaplaLocale().createCalendar();
        cal1.setTime( date );
        cal2.setTime( time );
        cal1.set( Calendar.HOUR_OF_DAY, cal2.get( Calendar.HOUR_OF_DAY ) );
        cal1.set( Calendar.MINUTE, cal2.get( Calendar.MINUTE ) );
        cal1.set( Calendar.SECOND, cal2.get( Calendar.SECOND ) );
        cal1.set( Calendar.MILLISECOND, cal2.get( Calendar.MILLISECOND ) );
        return cal1;
    }

    
    private Integer getTimeMs(Date time)
    {
        Calendar cal = getRaplaLocale().createCalendar();
        cal.setTime( time );
        return (( cal.get( Calendar.HOUR_OF_DAY ) * 60 + cal.get( Calendar.MINUTE )) * 60 + cal.get( Calendar.SECOND )) * 1000 + cal.get( Calendar.MILLISECOND ) ;
    }
    
	public Date setStartOfMonth(Date startDate) {
	    if(isStartDayFirstDay) {
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(startDate);
	        cal.set(Calendar.DAY_OF_MONTH, 1);
	        return cal.getTime();
	    }
	    return startDate;
	}


    class Listener implements ActionListener, DateChangeListener, ChangeListener {

        public void actionPerformed(ActionEvent evt) {
            if (!listenersEnabled)
                return;
            
            Date date;
            try {
	            listenersEnabled = false;
	            
	            Calendar calendar = getRaplaLocale().createCalendar();
	            calendar.setTime(dateSelection.getDate());
	
	            if (evt.getSource() == prevButton)
	            	calendar.add(incrementSize,-1);
	            else
	            	if (evt.getSource() == nextButton) 
	            		calendar.add(incrementSize,1);	            
	            	else
	            		if (evt.getSource() == todayButton)
	            			calendar.setTime(new Date());
	            date = calendar.getTime();	            
            } finally {
                listenersEnabled = true;
            }
            
            dateSelection.setDate(date);
        }

        public void dateChanged(DateChangeEvent evt) {
            if ( !listenersEnabled)
                return;

        	try {
        		listenersEnabled = false;
        		
            	if (evt.getSource() == nextButton) {
            		Date date = evt.getDate();
                    updateDates(date);
            	}
            	else
	            	if (evt.getSource() == prevButton) {
	            		Date date = evt.getDate();
	            		updateDates(date);
	            	}
	            	else
		        		if (evt.getSource() == dateSelection) {
		        			Date date = evt.getDate();
		                    updateDates(date);
		            	}
		        		else
			            	if (evt.getSource() == startTime) {
			            		Date date = dateSelection.getDate();
			            		long newEnd   = getTimeMs(endTime.getTime());
			            		long newStart = getTimeMs(startTime.getTime());
			            		if (newEnd == 0) {
			            			if(newStart >= 0) {
			            				updateDates(date);
			            			}
			            		}
			            		else
			            			if( newStart < newEnd) {
			            				updateDates(date);
			            			}
			            	}
			            	else
				            	if (evt.getSource() == endTime) {
				            		Date date = dateSelection.getDate();
				            		long newEnd   = getTimeMs(endTime.getTime());
				            		long newStart = getTimeMs(startTime.getTime());
				            		if (newStart < newEnd) {
			            				updateDates(date);
			            			}
				            	}
            					//System.out.println("Unknown event: " + evt.toString());
            } finally {
                listenersEnabled = true;
            }
        }

        private void updateDates(Date date) { 	
            model.setSelectedDate( date );
            
        	duration = getTimeMs(endTime.getTime()); // 24:00 duration = 0
        	if(duration == 0)
        		duration += 86400000;
        	duration -= getTimeMs(startTime.getTime()); 
        	//System.out.println("Start=" + date.toString() + " Duration="+duration);
            fireDateChange( date ); 
        }

		public void stateChanged(ChangeEvent e) {
            Calendar calendar = getRaplaLocale().createCalendar();
            calendar.setTime(dateSelection.getDate());
            Date date = calendar.getTime();
            fireDateChange( date);   
		}
    }
}