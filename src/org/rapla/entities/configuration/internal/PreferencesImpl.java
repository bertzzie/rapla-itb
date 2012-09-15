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
package org.rapla.entities.configuration.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.rapla.components.util.iterator.FilterIterator;
import org.rapla.components.util.iterator.NestedIterator;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.CannotExistWithoutTypeException;
import org.rapla.entities.storage.DynamicTypeDependant;
import org.rapla.entities.storage.EntityReferencer;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.Mementable;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleEntity;

public class PreferencesImpl extends SimpleEntity<Preferences>
    implements
        Preferences
        , DynamicTypeDependant
        ,java.io.Serializable
{
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;
    
    HashMap<String,Object> map = new HashMap<String,Object>();
    
    final public RaplaType getRaplaType() {return TYPE;}
    
    public void putEntry(String role,RaplaObject entry) {
        checkWritable();
        if ( entry == null)
        {
            map.remove( role);
        }
        else
        {
            map.put( role ,entry);
        }
    }
    
    public void putEntry(String role,String entry) {
        checkWritable();
        if ( entry == null)
        {
            map.remove( role);
        }
        else
        {
            map.put( role ,entry);
        }
    }
    
    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        super.resolveEntities( resolver);
        for (Iterator<EntityReferencer> it = getEntityReferencers();it.hasNext();){
            Object obj = it.next();
            ((EntityReferencer) obj).resolveEntities( resolver);
        }
    }
        
    public Object getEntry(String role) {
        return map.get( role );
    }

    public boolean hasEntry(String role) {
        return map.get( role ) != null;
    }

    public String getEntryAsString(String role) {
        return (String) map.get( role );
    }

    public String getEntryAsString(String role, String defaultValue) {
        String value = getEntryAsString( role);
        if ( value != null)
            return value;
        return defaultValue;
    }

    public Iterator<String> getPreferenceEntries() {
        return map.keySet().iterator();
    }

    private Iterator<EntityReferencer> getEntityReferencers() {
        return new FilterIterator<EntityReferencer>( map.values().iterator()) {
            protected boolean isInIterator(Object obj) {
                return obj instanceof EntityReferencer;
            }
        };
    }

    public Iterator<RefEntity<?>> getReferences() {
        return new NestedIterator<RefEntity<?>>( getEntityReferencers() ) {
            public Iterator<RefEntity<?>> getNestedIterator(Object obj) {
                return ((EntityReferencer) obj).getReferences();
            }
        };
    }
    
    public boolean isRefering(RefEntity<?> object) {
        for (Iterator<EntityReferencer> it = getEntityReferencers();it.hasNext();) {
            final EntityReferencer entityReferencer = (EntityReferencer) it.next();
            if (entityReferencer.isRefering( object)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return map.keySet().isEmpty();
    }
    
    static private void copy(PreferencesImpl source,PreferencesImpl dest) {
        dest.map.clear();
        for (Iterator<String> it = source.map.keySet().iterator();it.hasNext();)
        {
            String role =  it.next();
            Object entry = source.map.get( role );
            Object clone;
            if (entry instanceof Mementable )
            {
            	clone = ((Mementable<?>) entry).deepClone();
            }
            else 
            {
            	clone = entry;
            }
            dest.map.put( role , clone);
        }
    }

    @SuppressWarnings("unchecked")
	public void copy(Preferences obj) {
        super.copy((SimpleEntity<Preferences>) obj);
        copy((PreferencesImpl) obj,this);
    }

    public Preferences deepClone() {
        PreferencesImpl clone = new PreferencesImpl();
        super.deepClone(clone);
        copy(this,clone);
        return clone;
    }

    public Preferences clone() {
        PreferencesImpl clone = new PreferencesImpl();
        super.clone(clone);
        copy(this,clone);
        return clone;
    }

    /**
     * @see org.rapla.entities.Named#getName(java.util.Locale)
     */
    public String getName(Locale locale) {
        StringBuffer buf = new StringBuffer();
        if ( getOwner() != null) {
            buf.append( "Preferences of ");
            buf.append( getOwner().getName( locale));
        } else {
            buf.append( "Rapla Preferences!");
        }
        return buf.toString();
    }
	/* (non-Javadoc)
	 * @see org.rapla.entities.configuration.Preferences#getEntryAsBoolean(java.lang.String, boolean)
	 */
	public boolean getEntryAsBoolean(String role, boolean defaultValue) {
		String entry = getEntryAsString( role);
		if ( entry == null)
			return defaultValue;
		return Boolean.valueOf(entry).booleanValue();
	}
    
	/* (non-Javadoc)
	 * @see org.rapla.entities.configuration.Preferences#getEntryAsInteger(java.lang.String, int)
	 */
	public int getEntryAsInteger(String role, int defaultValue) {
		String entry = getEntryAsString( role);
		if ( entry == null)
			return defaultValue;
		return Integer.parseInt(entry);
	}
    
    public boolean needsChange(DynamicType type) {
        for (Iterator<Object> it = map.values().iterator();it.hasNext();) {
            Object obj = it.next();
            if ( obj instanceof DynamicTypeDependant) {
                if (((DynamicTypeDependant) obj).needsChange( type ))
                    return true;
            }
        }
        return false;
    }
    
    public void commitChange(DynamicType type) {
        for (Iterator<Object> it = map.values().iterator();it.hasNext();) {
            Object obj = it.next();
            if ( obj instanceof DynamicTypeDependant) {
                ((DynamicTypeDependant) obj).commitChange( type );
            }
        }
    }


    public void commitRemove(DynamicType type) throws CannotExistWithoutTypeException 
    {
        for (Iterator<Object> it = map.values().iterator();it.hasNext();) {
            Object obj = it.next();
            if ( obj instanceof DynamicTypeDependant) {
                ((DynamicTypeDependant) obj).commitRemove( type );
            }
        } 
    }

}












