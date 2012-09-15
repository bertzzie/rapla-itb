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

package org.rapla.components.calendar;



import java.awt.Color;
import java.util.Calendar;
/** Renders the weekdays (or any other day of week, if selected) in a special color. */
public class WeekendHighlightRenderer implements DateRenderer {

    Color m_weekendBackgroundColor = new Color(0xe2, 0xf3, 0xff);

    boolean[] m_highlightedDays = new boolean[10];

    public WeekendHighlightRenderer() {
        setHighlight(Calendar.SATURDAY,true);
        setHighlight(Calendar.SUNDAY,true);
    }

    /** Default color is #e2f3ff */
    public void setWeekendBackgroundColor(Color color) {
        m_weekendBackgroundColor = color;
    }
    /**
       enable/disable the highlighting for the selected day.
       Default highlighted days are saturday and sunday.
     */
    public void setHighlight(int day,boolean highlight) {
        m_highlightedDays[day] = highlight;
    }

    public boolean isHighlighted(int day) {
        return m_highlightedDays[day];
    }

    /** returns the value of #WEEKEND_BACKGROUND if day is set
        for highlight (default is saturday and sunday)
        and null if not.
        */
    public Color getBackgroundColor(int dayOfWeek,int day,int month, int year) {
        if (m_highlightedDays[dayOfWeek])
            return m_weekendBackgroundColor;
        return null;
    }

    /** returns null   */
    public String getToolTipText(int dayOfWeek,int day,int month, int year) {
        return null;
    }
}
