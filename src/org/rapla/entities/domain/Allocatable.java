/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
package org.rapla.entities.domain;

import java.util.Date;

import org.rapla.entities.Entity;
import org.rapla.entities.Named;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.Timestamp;
import org.rapla.entities.User;
import org.rapla.entities.dynamictype.Classifiable;

/** Objects that implement allocatable can be allocated by reservations.
    @see Reservation
 */
public interface Allocatable extends Entity<Allocatable>,RaplaObject,Named,Classifiable,Ownable,Timestamp {
    final RaplaType TYPE = new RaplaType(Allocatable.class, "resource");
    
    /** Conflicts for this allocatable should be ignored, if this flag is enabled.*/
    void setHoldBackConflicts(boolean enable);
    boolean isHoldBackConflicts();
    /** Static empty dummy Array. Mainly for using the toArray() method of the collection interface */
    Allocatable[] ALLOCATABLE_ARRAY = new Allocatable[0];
    void addPermission( Permission permission );
    boolean removePermission( Permission permission );

    /** returns if the user has the permission to allocate the resource in the given
        time. It returns <code>true</code> if for at least one permission calling
        <code>permission.covers()</code> 
        and <code>permission.affectsUser</code> yields <code>true</code>.
    */
    boolean canAllocate( User user, Date start, Date end, Date today );
    /** returns if the user has the permission to create a conflict for the resource.*/
    boolean canCreateConflicts( User user );
    /** returns if the user has the permission to modify the allocatable (and also its permission-table).*/
    boolean canModify( User user );
    /** returns if the user has the permission to read the allocations of this resource.*/
    boolean canRead( User user );
    
    Permission[] getPermissions();
    Permission newPermission();
	boolean isPerson();
}












