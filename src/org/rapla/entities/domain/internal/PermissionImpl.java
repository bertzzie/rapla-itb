/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org .       |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, of which license fullfill the Open Source     |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.entities.domain.internal;

import java.util.Date;
import java.util.Iterator;

import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.ReadOnlyException;
import org.rapla.entities.User;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.EntityReferencer;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.internal.ReferenceHandler;

public class PermissionImpl
    implements
        Permission
        ,EntityReferencer
        ,java.io.Serializable
{
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;
    
    boolean readOnly = false;
    ReferenceHandler referenceHandler = new ReferenceHandler();
    Date pEnd = null;
    Date pStart = null;
    Long maxAdvance = null;
    Long minAdvance = null;
    int accessLevel = ALLOCATE_CONFLICTS;

    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        referenceHandler.resolveEntities( resolver );
    }

    public void setUser(User user) {
        checkWritable();
        if (user != null)
            referenceHandler.put("group",null);
        referenceHandler.put("user",(RefEntity<?>)user);
    }

    public void setEnd(Date end) {
        checkWritable();
        this.pEnd = end;
        if ( end != null )
            this.maxAdvance = null;
    }

    public Date getEnd() {
        return pEnd;
    }

    public void setStart(Date start) {
        checkWritable();
        this.pStart = start;
        if ( start != null )
            this.minAdvance = null;
    }

    public Date getStart() {
        return pStart;
    }

    public void setMinAdvance(Long minAdvance) {
        checkWritable();
        this.minAdvance = minAdvance;
        if ( minAdvance != null )
            this.pStart = null;
    }

    public Long getMinAdvance() {
        return minAdvance;
    }

    public void setMaxAdvance(Long maxAdvance) {
        checkWritable();
        this.maxAdvance = maxAdvance;
        if ( maxAdvance != null )
            this.pEnd = null;
    }

    public Long getMaxAdvance() {
        return maxAdvance;
    }

    public void setReadOnly(boolean enable) {
        this.readOnly = enable;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void checkWritable() {
        if ( readOnly )
            throw new ReadOnlyException( this );
    }

    public boolean affectsUser(User user) {
        User pUser = getUser();
        Category pGroup = getGroup();
        if ( pUser == null  && pGroup == null ) {
            return true;
        }
        if ( pUser != null  && user.equals( pUser ) ) {
            return true;
        } else if ( pGroup != null ) {
            Category[] uGroups = user.getGroups();
            for ( int i = 0; i < uGroups.length; i++ ) {
                if ( pGroup.equals ( uGroups[i] )
                     || pGroup.isAncestorOf ( uGroups[i] )
                     ) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public User getUser() {
        return (User) referenceHandler.get("user");
    }

    public void setGroup(Category group) {
        if (group != null)
            referenceHandler.put("user",null);
        referenceHandler.put("group",(RefEntity<?>)group);
    }

    public Period getPeriod() {
        return (Period) referenceHandler.get("period");
    }

    public void setPeriod(Period period) {
        referenceHandler.put("period",(RefEntity<?>)period);
    }

    public ReferenceHandler getReferenceHandler() {
        return referenceHandler;
    }

    public Iterator<RefEntity<?>> getReferences() {
        return referenceHandler.getReferences();
    }

    public boolean isRefering( RefEntity<?> object ) {
        return referenceHandler.isRefering( object );
    }

    public Category getGroup() {
        return (Category) referenceHandler.get("group");
    }

    public Date getMinAllowed(Date today) {
        if ( pStart != null )
            return pStart;
        if ( minAdvance != null)
            return new Date( today.getTime()
                             + DateTools.MILLISECONDS_PER_DAY * minAdvance.longValue() );
        return null;
    }

    public Date getMaxAllowed(Date today) {
        if ( pEnd != null )
            return pEnd;
        if ( maxAdvance != null)
            return new Date( today.getTime()
                             + DateTools.MILLISECONDS_PER_DAY * (maxAdvance.longValue() + 1) );
        return null;
    }

    public boolean covers( Date start, Date end, Date today ) {
        if ( pStart != null && (start == null || start.before ( pStart ) ) ) {
            //System.out.println( " start before permission ");
            return false;
        }
        if ( pEnd != null && ( end == null || pEnd.before ( end ) ) ) {
            //System.out.println( " end before permission ");
            return false;
        }
        if ( minAdvance != null ) {
            long pStartTime = today.getTime()
                + DateTools.MILLISECONDS_PER_DAY * minAdvance.longValue();

            if ( start == null || start.getTime() < pStartTime ) {
                //System.out.println( " start before permission " + start  + " < " + pStartTime );
                return false;
            }
        }
        if ( maxAdvance != null ) {
            long pEndTime = today.getTime()
                + DateTools.MILLISECONDS_PER_DAY * (maxAdvance.longValue() + 1);
            if ( end == null || pEndTime < end.getTime() ) {
                //System.out.println( " end after permission " + end  + " > " + pEndTime );
                return false;
            }
        }
        return true;
    }

    public PermissionImpl clone() {
        PermissionImpl clone = new PermissionImpl();
        // This must be done first
        clone.referenceHandler = (ReferenceHandler) referenceHandler.clone();
        clone.accessLevel = accessLevel;
        clone.pEnd = pEnd;
        clone.pStart = pStart;
        clone.minAdvance = minAdvance;
        clone.maxAdvance = maxAdvance;
        return clone;
    }

}
