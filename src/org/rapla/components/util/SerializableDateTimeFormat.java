package org.rapla.components.util;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.rapla.components.util.iterator.IntIterator;


/**
Provides methods for parsing and formating dates
and times in the following format: <br>
<code>2002-25-05</code> for dates and <code>13:00:00</code> for times.
This is according to the xschema specification for dates and time.
WARNING: Do not share instances of this class between threads. Its NOT thread safe
*/
public class SerializableDateTimeFormat
{
    Calendar calendar;

    public SerializableDateTimeFormat() {
        this( Calendar.getInstance( DateTools.getTimeZone()));
    }

    public SerializableDateTimeFormat(Calendar calendar) {
        this.calendar = calendar;
    }

    private Date parseDate( String date, String time, boolean fillDate ) throws ParseException {
    	if( date == null || date.length()==0  )
    	    throwParseDateException("empty", 0);
       
    	parseDate_(calendar,date, fillDate);
        if ( time != null ) {
        	parseTime_(calendar,time);
        } else {
            DateTools.cutDate( calendar );
        }
        //    logger.log( "parsed to " + calendar.getTime() );
        return calendar.getTime();
    }

	private void parseTime_(Calendar calendar,String time) throws ParseException {
		IntIterator it = new IntIterator();
		it.init( time, ':' );
		if ( !it.hasNext() )
		    throwParseTimeException( time, it.getPos() );
		calendar.set( Calendar.HOUR_OF_DAY, it.next() );
		if ( !it.hasNext() )
		    throwParseTimeException( time, it.getPos() );
		calendar.set( Calendar.MINUTE, it.next() );
		if ( !it.hasNext() )
		    throwParseTimeException( time, it.getPos() );
		calendar.set( Calendar.SECOND, it.next() );
		calendar.set( Calendar.MILLISECOND, 0 );
	}

	private void parseDate_(Calendar calendar,String date, boolean fillDate)
			throws ParseException {
		IntIterator it = new IntIterator();
		it.init( date, '-' );
		if ( !it.hasNext() )
		    throwParseDateException( date, it.getPos() );
		calendar.set( Calendar.YEAR, it.next() );
		if ( !it.hasNext() )
		    throwParseDateException( date, it.getPos() );
		calendar.set( Calendar.MONTH, it.next() -1 );
		if ( !it.hasNext() )
		    throwParseDateException( date, it.getPos() );
		calendar.set( Calendar.DATE, it.next() );
		if (fillDate )
		    calendar.add( Calendar.DATE, 1 );
	}

    private void throwParseDateException( String date, int pos ) throws ParseException {
        throw new ParseException( "No valid date format: " + date, pos );
    }

    private void throwParseTimeException( String time, int pos ) throws ParseException {
        throw new ParseException( "No valid time format: "  + time, pos );
    }

    /** The date-string must be in the following format <strong>2001-10-21</strong>.
    The format of the time-string is <strong>18:00:00</strong>.
    @return The parsed date
    @throws ParseException when the date cannot be parsed.
    */
    public Date parseDateTime( String date, String time) throws ParseException {
        return parseDate( date, time, false);
    }
    
    /** 
    The format of the time-string is <strong>18:00:00</strong>.
    @return The parsed time
    @throws ParseException when the date cannot be parsed.
    */
    public Date parseTime(  String time) throws ParseException {
    	if( time == null || time.length()==0  )
    	    throwParseDateException("empty", 0);
    	calendar.setTime(new Date());
    	parseTime_( calendar, time);
    	return calendar.getTime();
    }

    /** The date-string must be in the following format <strong>2001-10-21</strong>.
     * @param fillDate if this flag is set the time will be 24:00 instead of 0:00 <strong>
    When this flag is set the time parameter should be null</strong>
    @return The parsed date
    @throws ParseException when the date cannot be parsed.
    */
    public Date parseDate( String date, boolean fillDate ) throws ParseException {
        return parseDate( date, null, fillDate);
    }


   /** returns the time object in the following format:  <strong>13:00:00</strong>. <br> */
    public String formatTime( Date date ) {
        StringBuffer buf = new StringBuffer();
        if ( date != null)
        {
            calendar.setTime( date );
        }
        append( buf, calendar.get(Calendar.HOUR_OF_DAY), 2 );
        buf.append( ':' );
        append( buf, calendar.get(Calendar.MINUTE), 2 );
        buf.append( ':' );
        append( buf, calendar.get(Calendar.SECOND), 2 );
        return buf.toString();
    }

    /** returns the date object in the following format:  <strong>2001-10-21</strong>. <br>
    @param adaptDay if the flag is set 2001-10-21 will be stored as 2001-10-20.
    This is usefull for end-dates: 2001-10-21 00:00 is then interpreted as
    2001-10-20 24:00.
    */
    public String formatDate( Date date, boolean adaptDay ) {
        StringBuffer buf = new StringBuffer();

        if ( adaptDay )
            calendar.setTime( new Date( date.getTime() - DateTools.MILLISECONDS_PER_DAY));
        else
            calendar.setTime( date );
        append( buf, calendar.get(Calendar.YEAR), 4 );
        buf.append( '-' );
        append( buf, calendar.get(Calendar.MONTH)+1, 2 );
        buf.append( '-' );
        append( buf, calendar.get(Calendar.DATE), 2 );
        return buf.toString();

    }

    /** same as formatDate(date, false).
    @see #formatDate(Date,boolean)
    */
    public String formatDate(  Date date ) {
        return formatDate( date, false );
    }

    private void append( StringBuffer buf, int number, int minLength ) {
        int limit = 1;
        for ( int i=0;i<minLength-1;i++ ) {
            limit *= 10;
            if ( number<limit )
                buf.append( '0' );
        }
        buf.append( number );
    }


}
