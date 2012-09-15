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
package org.rapla.entities.domain.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.rapla.components.util.iterator.IteratorChain;
import org.rapla.components.util.iterator.NestedIterator;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.dynamictype.internal.ClassificationImpl;
import org.rapla.entities.internal.ModifiableTimestamp;
import org.rapla.entities.storage.CannotExistWithoutTypeException;
import org.rapla.entities.storage.DynamicTypeDependant;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.Mementable;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleEntity;

public class AllocatableImpl extends SimpleEntity<Allocatable> implements Allocatable,Mementable<Allocatable>,java.io.Serializable, DynamicTypeDependant, ModifiableTimestamp, Ownable {
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;
    
    private ClassificationImpl classification;
    private boolean holdBackConflicts;
    private ArrayList<PermissionImpl> permissions = new ArrayList<PermissionImpl>();
    private Date lastChanged;
    private Date createDate;
    
    transient private boolean permissionArrayUpToDate = false;
    transient private PermissionImpl[] permissionArray;

    AllocatableImpl() {
        this (null, null);
    }
    
    public AllocatableImpl(Date createDate, Date lastChanged ) {
        this.createDate = createDate;
        this.lastChanged = lastChanged;
        if (lastChanged == null)
            this.lastChanged = this.createDate;
    }

 
    
    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        super.resolveEntities( resolver);
        classification.resolveEntities( resolver);
        for (Iterator<PermissionImpl> it = permissions.iterator();it.hasNext();)
        {
             it.next().resolveEntities( resolver);
        }
    }

    public void setReadOnly(boolean enable) {
        super.setReadOnly( enable );
        classification.setReadOnly( enable );
        Iterator<PermissionImpl> it = permissions.iterator();
        while (it.hasNext()) {
            it.next().setReadOnly(enable);
        }
    }

    public Date getLastChangeTime() {
        return lastChanged;
    }

    public Date getCreateTime() {
        return createDate;
    }

    public void setLastChanged(Date date) {
        lastChanged = date;
    }
    
    public RaplaType getRaplaType() {
    	return TYPE;
    }
    
    // Implementation of interface classifiable
    public Classification getClassification() { return classification; }
    public void setClassification(Classification classification) {
        this.classification = (ClassificationImpl) classification;
    }

    public void setHoldBackConflicts(boolean enable) {
        holdBackConflicts = enable;
    }
    public boolean isHoldBackConflicts() {
        return holdBackConflicts;
    }

    public String getName(Locale locale) {
        Classification c = getClassification();
        if (c == null)
            return "";
        return c.getName(locale);
    }

    public boolean isPerson() {
    	final Classification classification2 = getClassification();
    	if ( classification2 == null)
    	{
    	    return false;
    	}
        final String annotation = classification2.getType().getAnnotation(DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE);
        return annotation != null && annotation.equals( DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION);
    }
    
    private boolean hasAccess( User user, int accessLevel ) {
        Permission[] permissions = getPermissions();
        if ( user == null || user.isAdmin() )
            return true;
      
        for ( int i = 0; i < permissions.length; i++ ) {
            Permission p = permissions[i];
            if ( p.affectsUser(user)
                 && (p.getAccessLevel() >= accessLevel  ))
            {
                return true;
            }
        }
        return false;
    }

    public boolean canCreateConflicts( User user ) {
        return hasAccess( user, Permission.ALLOCATE_CONFLICTS);
    }
    
    public boolean canModify(User user) {
        return hasAccess( user, Permission.ADMIN);
    }

    public boolean canRead(User user) {
        return hasAccess( user, Permission.READ );
    }
    
    public boolean canAllocate( User user, Date start, Date end, Date today ) {
        if (user == null)
            return false;

        Permission[] permissions = getPermissions();
        if ( user.isAdmin() )
            return true;

        for ( int i = 0; i < permissions.length; i++ ) {
            Permission p = permissions[i];
            if ( p.affectsUser(user)
                 && p.getAccessLevel() >= Permission.ALLOCATE
                 && (p.getAccessLevel() == Permission.ADMIN || p.covers( start, end, today ) ))
            {
                return true;
            }
        }
        return false;
    }

    public void addPermission(Permission permission) {
        checkWritable();
        permissionArrayUpToDate = false;
        permissions.add((PermissionImpl)permission);
    }

    public boolean removePermission(Permission permission) {
        checkWritable();
        permissionArrayUpToDate = false;
        return permissions.remove(permission);
    }

    public Permission newPermission() {
        return new PermissionImpl();
    }

    public Permission[] getPermissions() {
        updatePermissionArray();
        return permissionArray;
    }

    private void updatePermissionArray() {
        if ( permissionArrayUpToDate )
            return;

        permissionArray = permissions.toArray(new PermissionImpl[] {});
        permissionArrayUpToDate = true;
    }

    public Iterator<RefEntity<?>> getReferences() {
        return new IteratorChain<RefEntity<?>>
            (
             classification.getReferences()
             ,new NestedIterator<RefEntity<?>>( permissions.iterator() ) {
                     public Iterator<RefEntity<?>> getNestedIterator(Object obj) {
                         return ((PermissionImpl)obj).getReferences();
                     }
                 }
             );
    }

    public boolean needsChange(DynamicType type) {
        return classification.needsChange( type );
    }
    
    public void commitChange(DynamicType type) {
        classification.commitChange( type );
    }
    
    public void commitRemove(DynamicType type) throws CannotExistWithoutTypeException 
    {
        classification.commitRemove(type);
    }
        
    public boolean isRefering(RefEntity<?> object) {
        if (super.isRefering(object))
            return true;
        if (classification.isRefering(object))
            return true;
        Permission[] permissions = getPermissions();
        for ( int i = 0; i < permissions.length; i++ ) {
            if ( ((PermissionImpl) permissions[i]).isRefering( object ) )
                return true;
        }
        return false;
    }

    static private void copy(AllocatableImpl source,AllocatableImpl dest) {
        dest.permissionArrayUpToDate = false;
        dest.classification =  (ClassificationImpl) source.classification.clone();

        dest.permissions.clear();
        Iterator<PermissionImpl> it = source.permissions.iterator();
        while ( it.hasNext() ) {
            dest.permissions.add(it.next().clone());
        }

        dest.holdBackConflicts = source.holdBackConflicts;
        dest.createDate = source.createDate;
        dest.lastChanged = source.lastChanged;
    }

    @SuppressWarnings("unchecked")
	public void copy(Allocatable obj) {
        super.copy((SimpleEntity<Allocatable>)obj);
        copy((AllocatableImpl)obj,this);
    }

    public Allocatable deepClone() {
        AllocatableImpl clone = new AllocatableImpl();
        super.deepClone(clone);
        copy(this,clone);
        return clone;
    }

    public Allocatable clone() {
        AllocatableImpl clone = new AllocatableImpl();
        super.clone(clone);
        copy(this,clone);
        return clone;
    }
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(" [");
        buf.append(super.toString());
        buf.append(",");
        buf.append(super.getVersion());
        buf.append("] ");
        if ( getClassification() != null) {
            buf.append (getClassification().toString()) ;
        } 
        return buf.toString();
    }

    
}







