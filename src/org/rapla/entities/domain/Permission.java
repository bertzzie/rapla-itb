/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org .       |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.entities.domain;

import java.util.Date;
import org.rapla.components.util.SmallIntMap;
import org.rapla.entities.Category;
import org.rapla.entities.User;

/** New feature to restrict the access to allocatables on a per user/group basis.
 * Specify absolute and relative booking-timeframes for each resource
 * per user/group. You can, for example, prevent modifing appointments
 * in the past, by setting the relative start-time to 0.
*/
public interface Permission
{
    String GROUP_CATEGORY_KEY = "user-groups";
    String GROUP_REGISTERER_KEY = "registerer";
    String GROUP_MODIFY_PREFERENCES_KEY = "modify-preferences";
    String GROUP_CAN_READ_EVENTS_FROM_OTHERS = "read-events-from-others";
    int DENIED = 0;
    int READ = 1;
    int ALLOCATE =2;
    int ALLOCATE_CONFLICTS = 3;
    int ADMIN = 4;

    String[] ACCESS_LEVEL_NAMES = {"denied","read","allocate","allocate-conflicts","admin"};
    final static int[] ACCESS_LEVEL_TYPES = {DENIED,READ,ALLOCATE,ALLOCATE_CONFLICTS,ADMIN};

    final static SmallIntMap ACCESS_LEVEL_NAMEMAP = new SmallIntMap(ACCESS_LEVEL_TYPES,ACCESS_LEVEL_NAMES);

    /** sets a user for the permission.
     * If a user is not null, the group will be set to null.
     */
    void setUser(User user);
    User getUser();

    /** sets a group for the permission.
     * If the group ist not null, the user will be set to null.
     */
    void setGroup(Category category);
    Category getGroup();

    /** set the minumum number of days a resource must be booked in advance. If days is null, a reservation can be booked anytime.
     * Example: If you set days to 7, a resource must be allocated 7 days before its acutual use */
    void setMinAdvance(Long days);
    Long getMinAdvance();

    /** set the maximum number of days a reservation can be booked in advance. If days is null, a reservation can be booked anytime.
    * Example: If you set days to 7, a resource can only be for the next 7 days. */
    void setMaxAdvance(Long days);
    Long getMaxAdvance();

    /** sets the starttime of the period in which the resource can be booked*/
    void setStart(Date end);
    Date getStart();

    /** sets the endtime of the period in which the resource can be booked*/
    void setEnd(Date end);
    Date getEnd();

    /** Convenince Method: returns the last date for which the resource can be booked */
    Date getMaxAllowed(Date today);
    /** Convenince Method: returns the first date for which the resource can be booked */
    Date getMinAllowed(Date today);

    /** returns if the user or a group of the user is affected by the permission.
     * Groups are hierarchical. If the user belongs
     * to a subgroup of the permission-group the user is also
     * affected by the permission.
     */
    boolean affectsUser( User user);

    /** returns if the permission covers the interval specified by the start and end date.
     * The current date must be passed to calculate the permissable
     * interval from minAdvance and maxAdvance.
    */
    boolean covers( Date start, Date end, Date currentDate);

    void setAccessLevel(int access);
    int getAccessLevel();

    /** Static empty dummy Array.
     * Mainly for using the toArray() method of the collection interface */
    Permission[] PERMISSION_ARRAY = new Permission[0];

}
