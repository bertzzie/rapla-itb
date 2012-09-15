/*--------------------------------------------------------------------------*
 | Copyright (C) 2006  Christopher Kohlhaas                                 |
 | Copyright (C) 2005  Thierry Excoffier, Universite Claude Bernard Lyon    |
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

package org.rapla.components.calendarview.html;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.Builder;
import org.rapla.components.util.Tools;

public class HTMLPeriodView extends AbstractHTMLView {
    /** shared calendar instance. Only used for temporary stored values. */
    protected HTMLSmallDaySlot[] slots;
    int weeksNr;
    int startHour, endHour;
    double cellHeight = 1.8, cellWidth = 10;

    /** formats the date and month in the selected locale and timeZone*/
    public static String formatDateMonth(Date date, Locale locale, TimeZone timeZone) {
        FieldPosition fieldPosition = new FieldPosition( DateFormat.YEAR_FIELD );
        StringBuffer buf = new StringBuffer();
        DateFormat format = DateFormat.getDateInstance( DateFormat.SHORT, locale);
        format.setTimeZone( timeZone );
        buf = format.format(date,
                           buf,
                           fieldPosition
                           );
        if ( fieldPosition.getEndIndex()<buf.length() ) {
            buf.delete( fieldPosition.getBeginIndex(), fieldPosition.getEndIndex()+1 );
        } else if ( (fieldPosition.getBeginIndex()>=0) ) {
            buf.delete( fieldPosition.getBeginIndex(), fieldPosition.getEndIndex() );
        }
        if (buf.charAt(buf.length()-1) == '/')
            return buf.substring(0,buf.length()-1);
        else
            return buf.toString();
    }

    /** formats the day of week, date and month in the selected locale and timeZone*/
    public static String formatDayOfWeekDateMonth(Date date, Locale locale, TimeZone timeZone) {
        SimpleDateFormat format =  new SimpleDateFormat("EEE", locale);
        format.setTimeZone(timeZone);
        return format.format(date) + " " + formatDateMonth( date,locale,timeZone ) ;
    }

    /** returns the name of the weekday */
    String formatDayOfWeek(Date date) {
        SimpleDateFormat format =  new SimpleDateFormat("EEEEE", locale);
        format.setTimeZone(getTimeZone());
        return format.format(date);
    }

    public void findWorkHour() {
        Calendar calendar = createCalendar();
		startHour = 24;
		endHour = 0;
		for(int i=0; i <weeksNr*7 ; i++) {
		    Iterator<Block> it = slots[i].iterator();
		    while (it.hasNext()) {
			Block bl = (Block)it.next();
			calendar.setTime(bl.getStart());
			if ( calendar.get(Calendar.HOUR_OF_DAY) < startHour )
			    startHour = calendar.get(Calendar.HOUR_OF_DAY);
			calendar.setTime(bl.getEnd());
			if (  calendar.get(Calendar.HOUR_OF_DAY) >= endHour ) {
			    endHour = calendar.get(Calendar.HOUR_OF_DAY);
			    if ( calendar.get(Calendar.MINUTE) > 0 )
				endHour++;
			}
		    }
		}
    }

    public void setPeriodEnd(Date end) {
        if ( end == null ) {
            this.setEndDate( null );
            return;
        }
        Calendar calendar = createCalendar();
        calendar.setTime( end );
        //  calendar.set(Calendar.DAY_OF_WEEK, 6);
        calendar.set(Calendar.HOUR_OF_DAY,23);
        calendar.set(Calendar.MINUTE,59);
        calendar.set(Calendar.SECOND,59);
        calendar.set(Calendar.MILLISECOND,999);
        this.setEndDate( calendar.getTime() );
    }

    protected void setEndDate(Date endDate) {
		if ( endDate == null ) {
		    super.setEndDate(null);
		    return;
		}
	        Calendar calendar = createCalendar();
		calendar.setTime( endDate );
		// calendar.set(Calendar.DAY_OF_WEEK, 6);
		calendar.set(Calendar.HOUR_OF_DAY,23);
		calendar.set(Calendar.MINUTE,59);
		calendar.set(Calendar.SECOND,59);
		calendar.set(Calendar.MILLISECOND,999);
		super.setEndDate(calendar.getTime());
	    }

	 public void calcMinMaxDates(Date date) {
	        Calendar calendar = createCalendar();
	        calendar.setTime( date );
	        // calendar.set(Calendar.DAY_OF_WEEK, 0);
	        calendar.set(Calendar.HOUR_OF_DAY,0);
	        calendar.set(Calendar.MINUTE,0);
	        calendar.set(Calendar.SECOND,0);
	        calendar.set(Calendar.MILLISECOND,0);
	        setStartDate(calendar.getTime());

		if ( getEndDate() == null ) {
		    calendar.add(Calendar.DATE, 7*7); // 7 weeks
		    setEndDate(calendar.getTime());
		}
    }

    public Collection<Block> getBlocks() {
        ArrayList<Block> list = new ArrayList<Block>();
        for (int i=0;i<slots.length;i++) {
            list.addAll(slots[i]);
        }
        return Collections.unmodifiableCollection( list );
    }

    private String HTMLPosition(double x, double y, double width, double height, String color) {
		return " style='position:absolute;"
		    + "left:" + (x*cellWidth + 0.1) + "em;"
		    + "top:" + y*cellHeight + "em;"
		    + "height:" + height*cellHeight + "em;"
		    + "width:" + (width*cellWidth - 0.2) + "em;"
		    + "background:" + color + ";"
		    + "'";
    }

    public Block sameAppointment(int day, Block bl) {

		Iterator<Block> it2 = slots[day].iterator();
		Block b;
		while (it2.hasNext()) {
		    b = it2.next();
		    if (  b.getStart().getTime()%(1000*3600*24)
			 == bl.getStart().getTime()%(1000*3600*24)
			 && b.getEnd().getTime()%(1000*3600*24)
			 == bl.getEnd().getTime()%(1000*3600*24)
			 ) {
		    	return b;
		    }
			/*if ( ((AbstractRaplaBlock)b).getAppointment()
			     ==  ((AbstractRaplaBlock)bl).getAppointment()
			    ) {
			    return b;
		    }*/
		}
		return null;
    }

    public void rebuild() {
        // calculate the blocks
        Iterator<Builder> it= builders.iterator();
        while (it.hasNext()) {
           Builder b= (Builder)it.next();
           b.prepareBuild(getStartDate(),getEndDate());
        }

        weeksNr = (int)((getEndDate().getTime() - getStartDate().getTime())/(1000*3600*24*7));

        slots = new HTMLSmallDaySlot[7*weeksNr];
        for (int i=0;i<slots.length;i++) {
            slots[i] = new HTMLSmallDaySlot(String.valueOf( i + 1));
        }

        it= builders.iterator();
        while (it.hasNext()) {
           Builder b= (Builder)it.next();
           if (b.isEnabled()) {
        	   b.build(this);
        	   }
        }
        Calendar calendar = createCalendar();
        calendar.setTime( getStartDate() );
		for (int i=0; i<7; i++) {
		    if ( !excludeDays.contains(new Integer(calendar.get(Calendar.DAY_OF_WEEK))) ) {
		    	break;
		    }
		    calendar.add(Calendar.DATE, 1);
		}
		calendar.add(Calendar.DATE, -1);

	    StringBuffer result = new StringBuffer();
		double y=4; // Top margin
		Calendar cal = createCalendar();

		findWorkHour();

        for (int j=0;j<7;j++) {

		    // Is the day empty ?
		    int i;
		    calendar.add(Calendar.DATE, 1);
		    for (i=0;i<weeksNr;i++) {
				if ( slots[j+i*7].size() != 0 ) {
				    break;
				}
			}
		    if ( i == weeksNr ) {
		    	continue;
		    }

		    if (excludeDays.contains(new Integer(calendar.get(Calendar.DAY_OF_WEEK)))) {
		    	continue;
		    }

		    double label_height = 1;

		    for (i=0;i<weeksNr;i++) {
				result.append("<div" + HTMLPosition(i,y,1,label_height, "") + " class='month_header'>");
				result.append(formatDayOfWeekDateMonth(calendar.getTime(),locale,getTimeZone()));
				result.append("</div>\n");
				calendar.add(Calendar.DATE, 7);
		    }
		    calendar.add(Calendar.DATE, -7*weeksNr);
		    y += label_height;

		    for(int h=startHour; h<endHour; h++) {
				for (i=0;i<weeksNr;i++) {
				    if ( slots[j+i*7] == null ) {
				    	continue;
				    }
				    Iterator<Block> it2= slots[j+i*7].iterator();
				    while (it2.hasNext()) {
						Block bl = it2.next();
						cal.setTime(bl.getStart());
						if ( cal.get(Calendar.HOUR_OF_DAY) <= h ) {
						    int minute = cal.get(Calendar.MINUTE);
						    int hour = cal.get(Calendar.HOUR_OF_DAY);
						    cal.setTime(bl.getEnd());
						    int minute_end = cal.get(Calendar.MINUTE);
						    int hour_end = cal.get(Calendar.HOUR_OF_DAY);
						    int last_week;
						    Block b;
						    for(last_week=i+1; last_week<weeksNr; last_week++){
								b = sameAppointment(last_week*7+j, bl);
								if ( b == null ) {
								    break;
								}
								slots[j+last_week*7].remove(b);
							}
						    String minutes, minutes_end ;
						    if (minute == 0) {
						    	minutes = "00";
						    } else {
						    	minutes	 = "" + minute ;
						    }
						    if (minute_end == 0) {
						    	minutes_end = "00";
						    } else {
						    	minutes_end = "" + minute_end;
						    }
						    String color = ((HTMLBlock)bl).getBackgroundColor() ;
						    if ( color == null )
							color = "";

						    String content = bl.toString();
						    if ( content == null )
                            {
						        content = "BUG";
                            }
                            // CKO Replaced for 1.3 compatibility
                            content = Tools.replaceAll(content, "<br>", ", ");
                            content = Tools.replaceAll(content, "\n", "");
						    result.append("<div"
								  + HTMLPosition(i
										 , y + minute/60.
										 , last_week - i
										 , hour_end - hour + (minute_end - minute)/60.
										 , color
										 )
								  + " class='period_day'>"
								  + "<small>" + hour + ":" + minutes
								  + "-"
								  + hour_end + ":" + minutes_end + "</small> "
								  + content
								  + "</div>\n"
								  );
						    it.remove();
						}
				    }
				}
			    y++ ;
		    }
		}
        m_html = result.toString();
    }

    public void addBlock(Block bl,int column, int slot) {
		//if ( ((AbstractRaplaBlock)bl).isException() )
		//    return;

		checkBlock( bl );

		int day = (int)((bl.getStart().getTime() - getStartDate().getTime())/(1000*3600*24));
		if ( day >= slots.length )
		    return;

        slots[day].putBlock(bl);
    }

	@Override
	protected boolean isEmpty(int column) {
		// TODO Auto-generated method stub
		return false;
	}

}
