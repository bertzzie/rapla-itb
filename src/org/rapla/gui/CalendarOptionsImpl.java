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
package org.rapla.gui;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.entities.domain.RepeatingEnding;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.framework.RaplaException;

/** <strong>WARNING!!</strong> This class should not be public to the outside. Please use the interface */
public class CalendarOptionsImpl implements CalendarOptions {
    public final static String CALENDAR_OPTIONS="org.rapla.calendarview";

    public static final String WORKTIME = "worktime";
    public static final String EXCLUDE_DAYS = "exclude-days";
    public static final String WEEKSTART = "exclude-days";
    public static final String ROWS_PER_HOUR = "rows-per-hour";
    public final static String EXCEPTIONS_VISIBLE="exceptions-visible";
    public final static String COMPACT_COLUMNS="compact-columns";
    public final static String COLOR_BLOCKS="color";
    public final static String COLOR_RESOURCES="resources";
    public final static String COLOR_EVENTS="reservations";
    public final static String COLOR_EVENTS_AND_RESOURCES="reservations_and_resources";
    public final static String COLOR_NONE="disabled";
    
    public final static String DAYS_IN_WEEKVIEW = "days-in-weekview";
    public final static String FIRST_DAY_OF_WEEK = "first-day-of-week";
    public final static String REPEATING="repeating"; 
    public final static String NTIMES="repeating.ntimes"; 
    public final static String CALNAME="calendar-name"; 
    int nTimes; 
    RepeatingEnding repeatingField;
    RepeatingType repeatingType;    


    public final static String REPEATINGTYPE="repeatingtype"; 
    public final static String EVENTTYPE = "eventtype";

  
    Set<Integer> excludeDays = new LinkedHashSet<Integer>();

    int maxtime = -1;
    int mintime = -1;
    int rowsPerHour = 4;
    boolean exceptionsVisible;
    boolean compactColumns = true;  // use for strategy.setFixedSlotsEnabled
    Configuration config;
    String colorField;
    String eventType;
    
    int daysInWeekview;
	int firstDayOfWeek;

    public CalendarOptionsImpl(Configuration config ) throws RaplaException {
        this.config = config;
        Configuration worktime = config.getChild( WORKTIME );
        StringTokenizer tokenizer = new StringTokenizer( worktime.getValue("8-18"), "-" );
        try {
            if ( tokenizer.hasMoreTokens() )
                mintime = new Integer( tokenizer.nextToken().toLowerCase().trim()).intValue( );
            if ( tokenizer.hasMoreTokens() )
                maxtime = new Integer( tokenizer.nextToken().toLowerCase().trim()).intValue( );
        } catch ( NumberFormatException e ) {
            throw new RaplaException( "Invalid time in " + worktime.getLocation( )
                                             + ". only numbers are allowed e.g. 8-18!");
        }
        if ( maxtime == 0)
        {
        	maxtime = 24;
        }
        if ( mintime == -1 || maxtime == -1 /*|| mintime >= maxtime*/ )
            throw new RaplaException("Invalid intervall in "
                                             + worktime.getLocation()
                                             + ". Use the following format 8-16 !");

        Configuration exclude = config.getChild( EXCLUDE_DAYS );
        tokenizer = new StringTokenizer( exclude.getValue(""), "," );
        while ( tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().toLowerCase().trim();
            try {
                excludeDays.add( new Integer(token) );
            } catch ( NumberFormatException e ) {
                throw new RaplaException("Invalid day in "
                                                 + exclude.getLocation()
                                                 + ". only numbers are allowed!");
            }
        } // end of while ()

        rowsPerHour = config.getChild( ROWS_PER_HOUR ).getValueAsInteger( 4 );
        exceptionsVisible = config.getChild(EXCEPTIONS_VISIBLE).getValueAsBoolean(false);

        colorField = config.getChild( COLOR_BLOCKS ).getValue( COLOR_EVENTS_AND_RESOURCES );

       
        repeatingField = RepeatingEnding.findForString(config.getChild( REPEATING ).getValue( RepeatingEnding.END_DATE.toString() ));
        nTimes = config.getChild( NTIMES ).getValueAsInteger( 1 );

        repeatingType = RepeatingType.findForString(config.getChild( REPEATINGTYPE ).getValue( RepeatingType.WEEKLY.toString() ));

        eventType = config.getChild( EVENTTYPE ).getValue(null);
        int firstDayOfWeekDefault = Calendar.getInstance().getFirstDayOfWeek();
		firstDayOfWeek = config.getChild(FIRST_DAY_OF_WEEK).getValueAsInteger(firstDayOfWeekDefault);
		
		daysInWeekview = config.getChild(DAYS_IN_WEEKVIEW).getValueAsInteger( 7 );

    }

    public Configuration getConfig() {
        return config;
    }

    public int getWorktimeStart() {
        return mintime;
    }

    public int getRowsPerHour() {
        return rowsPerHour;
    }

    public int getWorktimeEnd() {
        return maxtime;
    }

    public Set<Integer> getExcludeDays() {
        return excludeDays;
    }
    
// BJO 00000012
    public RepeatingEnding getRepeatingDuration() {
        return repeatingField;
    }

    public boolean isInfiniteRepeating() {
        return repeatingField.equals( RepeatingEnding.FOREVEVER );
    }
    
    public boolean isNtimesRepeating() {
    	boolean rt=false;
        if(repeatingField.equals( RepeatingEnding.FOREVEVER ))
        		rt = false;
        	else if(repeatingField.equals( RepeatingEnding.N_TIMES ))
    					rt = true;
        			else if(repeatingField.equals( RepeatingEnding.END_DATE ))
        				rt = true;
        return rt;
    }

    public boolean isUntilRepeating() {
        return repeatingField.equals( RepeatingEnding.END_DATE );
    }
    
	public int getnTimes() {
		return nTimes;
	}

    public boolean isExceptionsVisible() {
        return exceptionsVisible;
    }

    public boolean isCompactColumns() {
    	return compactColumns;
    }

    public boolean isResourceColoring() {
        return colorField.equals( COLOR_RESOURCES ) || colorField.equals( COLOR_EVENTS_AND_RESOURCES);
    }

    public boolean isEventColoring() {
        return colorField.equals( COLOR_EVENTS ) || colorField.equals( COLOR_EVENTS_AND_RESOURCES);
    }

	public RepeatingType getRepeatingType() {
		return repeatingType;
	}
	
    public String getEventType() {
        return eventType;
    }
    
    public int getDaysInWeekview() {
		return daysInWeekview;
	}

	public void setDaysInWeekview(int daysInWeekview) 
	{
		this.daysInWeekview = daysInWeekview;
	}

	public int getFirstDayOfWeek()
	{
		return firstDayOfWeek;
	}

	public void setFirstDayOfWeek(int firstDayOfWeek) {
		this.firstDayOfWeek = firstDayOfWeek;
	}
	



}
