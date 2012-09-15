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
import java.util.Set;

import org.rapla.entities.domain.RepeatingEnding;
import org.rapla.entities.domain.RepeatingType;


/** This class contains the.
 * Worktimes and dates (Useful for the week-view)
Configuration is done in the calendar option menu.

Hours belonging to the worktime get a different color in the
weekview. This is also the minimum interval that will be used for
printing.<br>

Excluded Days are only visible, when there is an appointment to
display.<br>
 */

public interface CalendarOptions {
    public static final String ROLE = CalendarOptions.class.getName();

    int getWorktimeStart();
    int getRowsPerHour();
    int getWorktimeEnd();
    Set<Integer> getExcludeDays();
    
    int getDaysInWeekview();
	int getFirstDayOfWeek();

    boolean isExceptionsVisible();
    boolean isCompactColumns();
    boolean isResourceColoring();
    boolean isEventColoring();
    //boolean isConflictsVisible();
    boolean isInfiniteRepeating();
	boolean isNtimesRepeating();
	boolean isUntilRepeating();
	int getnTimes();
	RepeatingEnding getRepeatingDuration();
	RepeatingType getRepeatingType();
	String getEventType();

}
