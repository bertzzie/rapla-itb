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
package org.rapla.facade.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.facade.ModificationEvent;

/** Encapsulate the changes that are made in the backend-store.*/
public class ModificationEventImpl implements ModificationEvent
{
    Set<RaplaObject> updatedObjects;
    Set<RaplaObject> removedObjects;
    boolean isRefresh = false;
    Map<RaplaType,Set<RaplaObject>> updatedIndex;
    Map<RaplaType,Set<RaplaObject>> removedIndex;

    public ModificationEventImpl() {
        isRefresh = true;
    }

    public ModificationEventImpl(Set<RaplaObject> changedObjects,Set<RaplaObject> removedObjects) {
        isRefresh = false;
        this.updatedObjects = Collections.unmodifiableSet(changedObjects);
        this.removedObjects = Collections.unmodifiableSet(removedObjects);
        updatedIndex = new HashMap<RaplaType,Set<RaplaObject>>();
        Iterator<RaplaObject> it = updatedObjects.iterator();
        while (it.hasNext()) {
            addToIndex(updatedIndex,it.next());
        }
        makeIndexReadOnly(updatedIndex);
        removedIndex = new HashMap<RaplaType,Set<RaplaObject>>();
        it = removedObjects.iterator();
        while (it.hasNext()) {
            addToIndex(removedIndex,(RaplaObject)it.next());
        }
        makeIndexReadOnly(removedIndex);
    }

    private void addToIndex(Map<RaplaType,Set<RaplaObject>> index,RaplaObject object) {
        Set<RaplaObject> set =  index.get(object.getRaplaType());
        if (set == null) {
            set = new HashSet<RaplaObject>();
            index.put(object.getRaplaType(),set);
        }
        set.add(object);
    }

    private void makeIndexReadOnly(Map<RaplaType,Set<RaplaObject>> index) {
        Iterator<RaplaType> it = index.keySet().iterator();
        while (it.hasNext()) {
            RaplaType key = it.next();
            Set<RaplaObject> set = Collections.unmodifiableSet( index.get(key));
            index.put(key,set);
        }
    }

    /** All objects in the cache are modified. This is not selective. */
    private boolean isRefresh() {
        return isRefresh;
    }


    @SuppressWarnings("unchecked")
	private  <T extends RaplaObject> Set<T> retainObjects(Set<RaplaObject> set,Set<T> col) {
        HashSet<RaplaObject> tempSet = new HashSet<RaplaObject>(col.size());
        tempSet.addAll(col);
        tempSet.retainAll(set);
        if (tempSet.size() >0)
           return (Set<T>) tempSet;
        else
            return Collections.emptySet();
    }

    /** returns the modified objects from a given set.*/
    
    public <T extends RaplaObject> Set<T> getChanged(Set<T> col) {
        checkNotRefresh();
        return retainObjects(updatedObjects,col);
    }

    /** returns the removed objects from a given set.*/
    public <T extends RaplaObject> Set<T> getRemoved(Set<T> col) {
        checkNotRefresh();
        return retainObjects(removedObjects,col);
    }

    /** returns if the objects has changed.*/
    public boolean hasChanged(RaplaObject object) {
        if (isRefresh())
        {
            return true;
        }
        checkNotRefresh();
        return updatedObjects.contains(object);
    }

    /** returns if the objects was removed.*/
    public boolean isRemoved(RaplaObject object) {
        if (isRefresh())
        {
            return true;
        }
        checkNotRefresh();
        return removedObjects.contains(object);
    }

    /** returns if the objects has changed or was removed.*/
    public boolean isModified(RaplaObject object) {
        if (isRefresh())
        {
            return true;
        }
        checkNotRefresh();
        return hasChanged(object) || isRemoved(object);
    }

    /** returns if an objects of the specied type was changed or removed.*/
    public boolean isModified(RaplaType raplaType) {
        if (isRefresh())
        {
            return true;
        }
        checkNotRefresh();
        return updatedIndex.get(raplaType)!=null || removedIndex.get(raplaType)!=null;
    }

    private Set<RaplaObject> getFromIndex(Map<RaplaType,Set<RaplaObject>> index,RaplaType raplaType) {
        Set<RaplaObject> set =  index.get(raplaType);
        if (set != null)
            return set;
        else
            return Collections.emptySet();
    }

    /** returns if an objects of the specied type has changed .*/
    public Set<RaplaObject> getChanged(RaplaType raplaType) {
        checkNotRefresh();
        return getFromIndex(updatedIndex,raplaType);
    }

    /** returns if an objects of the specied type was removed .*/
    public Set<RaplaObject> getRemoved(RaplaType raplaType) {
        checkNotRefresh();
        return getFromIndex(removedIndex,raplaType);
    }

    /** returns all removed objects .*/
    public Set<RaplaObject> getRemoved() {
        checkNotRefresh();
        return removedObjects;
    }

    /** returns all changed object .*/
    public Set<RaplaObject> getChanged() {
        checkNotRefresh();
        return updatedObjects;
    }

    private void checkNotRefresh() {
        if (isRefresh())
            throw new IllegalStateException("Refresh is set. All objects could be changed!");
    }
}




