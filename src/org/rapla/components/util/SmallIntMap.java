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
package org.rapla.components.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
/**  <p>Same as map but for small positive int-values.</p> 

<p>This class is useful if you want to model a 1:1 relation between
an int value and an object.  </p>

<p> This map is only efficient for small ints because the hashMap size
is always larger or equal to the largest key. It is optimized for get.</p>
@see #get
*/
public class SmallIntMap implements java.io.Serializable {
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;
    
    Object[] positiveValues;
    Object[] negativeValues;
    Set<Integer> keys = new TreeSet<Integer>();
    public SmallIntMap() {
        this(10);
    }

    public SmallIntMap(int initSize) {
        positiveValues = new Object[initSize];
    }

    public SmallIntMap(int[] keys,Object[] values) {
    	Assert.isTrue(keys.length == values.length,"number of keys is different from number of values");
    	int maxSize = 0;
    	for (int i=0;i<keys.length;i++)
    	    if (keys[i]>maxSize) 
    		maxSize = keys[i];
    	this.positiveValues = new Object[maxSize + 1];
    	for (int i=0;i<keys.length;i ++) {
    	    put(keys[i],values[i]);
    	}
    }

    public Object get(int key) {
        if ( key >= 0) {
            if (key < positiveValues.length) { 
                return positiveValues[key];
            }
       } else { 
           if ( negativeValues != null && -key < negativeValues.length) {
               return negativeValues[-key];
           } 
       }
        return null;
    }

    public Object put(int key,Object value) {
		Integer keyObj = new Integer(key);
		if (!keys.contains(keyObj))
		    keys.add(keyObj);
        
        Object oldValue;
        if ( key >= 0) {
    		if (key>= positiveValues.length) {
    		    Object[] newValues = new Object[key * 2];
    		    System.arraycopy(positiveValues,0,newValues,0,positiveValues.length);
    		    positiveValues = newValues;
    		}
    		oldValue = positiveValues[key];
    		positiveValues[key] = value;
        } else {
            if ( negativeValues == null) {
                negativeValues = new Object[10];
            }
            if (-key>= negativeValues.length) {
                Object[] newValues = new Object[(-key) * 2];
                System.arraycopy(negativeValues,0,newValues,0,negativeValues.length);
                negativeValues = newValues;
            }
            oldValue = negativeValues[- key];
            negativeValues[ -key] = value;
        }
            
        return oldValue;
    }

    public void clear() {
    	positiveValues = new Object[10];
        negativeValues = null;
    	keys.clear();
    }

    public Object remove(int key) {
	    keys.remove(new Integer(key));
        Object oldValue = null;
        if ( key >= 0) {
            if (key < positiveValues.length) { 
               oldValue = positiveValues[key];
               positiveValues[key] = null;
            }
       } else { 
           if ( negativeValues != null && -key < negativeValues.length) {
               oldValue = negativeValues[-key];
               negativeValues[-key] = null;
           } 
       }
       return oldValue;
    }

    public int[] findMatchingKeys(Object value) {
    	ArrayList<Integer> matching = new ArrayList<Integer>();
    	Iterator<Integer> it = keys.iterator();
    	while (it.hasNext()) {
    	    Integer key = it.next();
    	    if (get(key.intValue()).equals(value))
    		matching.add(key);
    	}
    	int[] result = new int[matching.size()];
    	for (int i=0;i<result.length;i++) {
    	    result[i] = (matching.get(i)).intValue();
    	}
    	return result;
    }

    public Set<Integer> keySet() {
        return keys;
    }

    public Collection<?> values() {
    	Collection<Object> result = new ArrayList<Object>();
    	Iterator<Integer> it = keys.iterator();
    	while (it.hasNext()) 
    	    result.add(get((it.next()).intValue()));
    
    	return result;
    }


}
