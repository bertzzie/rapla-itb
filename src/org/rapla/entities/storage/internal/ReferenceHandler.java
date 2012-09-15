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
package org.rapla.entities.storage.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.rapla.components.util.Assert;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.storage.EntityReferencer;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.RefEntity;

/** The ReferenceHandler takes care of serializing and deserializing references to Entity objects.
<p>
    The references will be serialized to the ids of the corresponding entity. Deserialization of
    the ids takes place in the contextualize method. You need to provide an EntityResolver on the Context.
</p>
<p>
The ReferenceHandler support both named and unnamed References. Use the latter one, if you don't need to refer to the particular reference by name and if you want to keep the order of the references.

<pre>

// put a named reference
referenceHandler.put("owner",user);

// put unnamed reference
Iterator it = resources.iterator();
while (it.hasNext())
   referenceHandler.add(it.next());

// returns
User referencedUser = referenceHandler.get("owner");

// returns both the owner and the resources
Itertor references = referenceHandler.getReferences();
</pre>

</p>
    @see EntityResolver
 */
public class ReferenceHandler implements EntityReferencer, java.io.Serializable, Cloneable{
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;
    
    private HashMap<String,ReferenceEntry> map;
    private ArrayList<ReferenceEntry> list;
    private transient boolean contextualizeCalled;

    // added for performance reasons
    private transient boolean referencesUpToDate;
    private transient List<RefEntity<?>> referenceList;

    /**
     * @see org.rapla.entities.storage.EntityReferencer#resolveEntities(org.rapla.entities.storage.EntityResolver)
     */
    public void resolveEntities(EntityResolver resolver) throws EntityNotFoundException {
        try {
            if (map != null) {
                Iterator<ReferenceEntry> it = map.values().iterator();
                while (it.hasNext()) {
                    ReferenceEntry entry =  it.next();
                    entry.reference = resolver.resolve(entry.id);
                }
            }
            if (list != null) {
                Iterator<ReferenceEntry> it = list.iterator();
                while (it.hasNext()) {
                    ReferenceEntry entry =  it.next();
                    entry.reference = resolver.resolve(entry.id);
                }
            }
        } catch (EntityNotFoundException ex) {
            clearReferences();
            throw ex;
        }
        contextualizeCalled = true;
        referencesUpToDate = false;
    }

    /** Use this method if you want to implement deserialization of the object manualy.
     * You have to add the reference-ids to other entities immediatly after the constructor.
     * @throws IllegalStateException if contextualize has been called before.
     */
    public void addId(Object id) {
        if (contextualizeCalled)
            throw new IllegalStateException("Contextualize has been called before.");
        if (list == null)
            list = new ArrayList<ReferenceEntry>(3);
        Assert.notNull(id);

        ReferenceEntry entry = new ReferenceEntry();
        entry.id = id ;
        if ( list.contains( entry))
        {
            return;
        }
        list.add(entry);
    }

    /** Use this method if you want to implement deserialization of the object manualy.
     * You have to add the reference-ids to other entities immediatly after the constructor.
     * @throws IllegalStateException if contextualize has been called before.
     */
    public void putId(String key,Object id) {
        if (contextualizeCalled)
            throw new IllegalStateException("Contextualize has been called before.");
        if (map == null)
            map = new HashMap<String,ReferenceEntry>(5);

        if (id == null) {
            map.remove(key);
            return;
        }

        ReferenceEntry entry = new ReferenceEntry();
        entry.id = id;
        map.put(key, entry);
    }

    public boolean removeId(String key) {
        if (map == null)
            return false;
        if  ( map.remove(key) != null ) {
            referencesUpToDate = false;
            return true;
        } else {
            return false;
        }
    }

    
    public Object getId(String key) {
        if (map == null)
            throw new IllegalStateException("Map is empty.");
        ReferenceEntry entry = (ReferenceEntry)map.get(key);
        if (entry != null)
            return entry.id;
        throw new IllegalStateException("Key not found." + key);
    }

    public void put(String key,RefEntity<?> entity) {
        if (map == null)
            map = new HashMap<String,ReferenceEntry>(5);
        if (entity == null) {
            map.remove(key);
            return;
        }
        
        ReferenceEntry entry = new ReferenceEntry();
        entry.id = entity.getId() ;
        entry.reference = entity;
        map.put(key,entry);
        referencesUpToDate = false;
    }

    public RefEntity<?> get(String key) {
        if (map == null)
            return null;
        ReferenceEntry entry  = (ReferenceEntry)map.get(key);
        if (entry == null)
            return null;
        return entry.reference;
    }


    public void add(RefEntity<?> entity) {
        if (isRefering(entity))
            return;
        if (list == null)
            list = new ArrayList<ReferenceEntry>(3);
        ReferenceEntry entry = new ReferenceEntry();
        entry.id = entity.getId() ;
        entry.reference = entity;
        list.add(entry);
        referencesUpToDate = false;
    }

    public boolean remove(RefEntity<?> entity) {
        if (!isRefering(entity)) {
            return false;
        }
        if (list != null) {
            Iterator<ReferenceEntry> it = list.iterator();
            while (it.hasNext()) {
                ReferenceEntry entry =  it.next();
                if (entry.reference.equals(entity))
                    it.remove();
            }
        }
        if (map != null) {
            Iterator<String> it = map.keySet().iterator();
            while (it.hasNext()) {
                ReferenceEntry entry = map.get(it.next());
                if (entry.reference.equals(entity))
                    it.remove();
            }
        }
        referencesUpToDate = false;
        return true;
    }

    private Collection<RefEntity<?>> getReferenceList() {
        if (referencesUpToDate)
            return referenceList;
        referenceList = new ArrayList<RefEntity<?>>(5);
        if (list != null) {
            Iterator<ReferenceEntry> it = list.iterator();
            while (it.hasNext()) {
                ReferenceEntry entry =  it.next();
                if (entry.reference == null)
                    throw new IllegalStateException("Contextualize was not called. References need to be resolved in context.");
                referenceList.add(entry.reference);
            }
        }
        if (map != null) {
            Iterator<String> it = map.keySet().iterator();
            while (it.hasNext()) {
                ReferenceEntry entry = (ReferenceEntry) map.get(it.next());
                if (entry.reference == null)
                    throw new IllegalStateException("Contextualize was not called. References need to be resolved in context.");
                referenceList.add(entry.reference);
            }
        }
        referencesUpToDate = true;
        return referenceList;
    }

    public boolean isRefering(RefEntity<?> obj) {
        if (list == null && map == null)
            return false;
        Collection<RefEntity<?>> referenceList = getReferenceList();
        return referenceList.contains(obj);
    }

    @SuppressWarnings("unchecked")
	public Iterator<RefEntity<?>> getReferences() {
        if (list == null && map == null)
            return Collections.EMPTY_LIST.iterator();
        return getReferenceList().iterator();
    }
    
    public Iterable<String> getReferenceKeys() {
        if (map == null)
            return Collections.emptySet();
        return map.keySet();
    }
    
    public void clearReferences() {
        if (map != null)
            map.clear();
        if (list != null)
            list.clear();
        referencesUpToDate = false;
    }

    @SuppressWarnings("unchecked")
	public Object clone() {
        ReferenceHandler clone;
		try {
			clone = ((ReferenceHandler)super.clone());
		} catch (CloneNotSupportedException e) {
			throw new NullPointerException("Clone not supperted");
		}
        if (map != null)
            clone.map = (HashMap<String,ReferenceEntry>) map.clone();
        if (list != null)
            clone.list = (ArrayList<ReferenceEntry>) list.clone();
        
        return clone;
    }

    class ReferenceEntry implements java.io.Serializable {
        private static final long serialVersionUID = 1;
        transient RefEntity<?> reference;
        Object id;
        public boolean equals(Object obj)
        {
            if ( !(obj instanceof ReferenceEntry))
            {
                return false;
            }
            Object id2 = ((ReferenceEntry)obj).id;
            if ( id2 == null)
            {
                return false;
            }
            return id2.equals( id);
        }
      
    }
    
    


}
