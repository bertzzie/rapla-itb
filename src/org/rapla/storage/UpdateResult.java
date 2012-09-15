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
package org.rapla.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;

public class UpdateResult 
{
    User user;
    List<UpdateOperation> operations = new ArrayList<UpdateOperation>();
    
    public UpdateResult(User user) {
        this.user = user;
    }
    
    public void addOperation(final UpdateOperation operation) {
        if ( operation == null)
            throw new IllegalStateException( "Operation can't be null" );
        operations.add(operation);
    }
    
    public User getUser() {
        return user;
    }
    
    public Set<RaplaObject> getRemoveObjects() {
        return getObject( Remove.class);
    }

    public Set<RaplaObject> getChangeObjects() {
        return getObject( Change.class);
    }

    public Set<RaplaObject> getAddObjects() {
        return getObject( Add.class);
    }
    
    @SuppressWarnings("unchecked")
	public <T extends UpdateOperation> Iterator<T> getOperations( final Class<T> operationClass) {
        Iterator<UpdateOperation> operationsIt =  operations.iterator();
        if ( operationClass == null)
            throw new IllegalStateException( "OperationClass can't be null" );
        
        List<T> list = new ArrayList<T>();
        while ( operationsIt.hasNext() ) {
            UpdateOperation obj = operationsIt.next();
            if ( operationClass.isInstance( obj ))
                list.add( (T)obj );
        }
        
        return list.iterator();
    }

    protected <T extends UpdateOperation> Set<RaplaObject> getObject( final Class<T> operationClass ) {
        Set<RaplaObject> set = new HashSet<RaplaObject>();
        if ( operationClass == null)
            throw new IllegalStateException( "OperationClass can't be null" );
        Iterator<? extends UpdateOperation> it= getOperations( operationClass);
        while (it.hasNext() ) {
            UpdateOperation next = it.next();
            RaplaObject current = next.getCurrent();
			set.add( current);
        }
        return set;
    }
    
    
    static public class Add implements UpdateOperation {
    	RaplaObject currentObj; // the actual represantation of the object
    	RaplaObject newObj; // the object in the state when it was addes
        public Add(RaplaObject currentObj, RaplaObject newObj) {
            this.currentObj = currentObj;
            this.newObj = newObj;
        }
        public RaplaObject getCurrent() {
            return currentObj;
        }
        public RaplaObject getNew() {
            return newObj;
        }
    }

    static public class Remove implements UpdateOperation {
    	RaplaObject currentObj; // the actual represantation of the object
        public Remove(RaplaObject currentObj) {
            this.currentObj = currentObj;
        }
        public RaplaObject getCurrent() {
            return currentObj;
        }
    }
    
    static public class Change implements UpdateOperation{
    	RaplaObject currentObj; // the actual representation of the object
    	RaplaObject newObj; // the object in the state when it was changed
    	RaplaObject oldObj; // the object in the state before it was changed
        public Change(RaplaObject currentObj, RaplaObject newObj, RaplaObject oldObj) {
            this.currentObj = currentObj;
            this.newObj = newObj;
            this.oldObj = oldObj;
        }
        public RaplaObject getCurrent() {
            return currentObj;
        }
        public Object getNew() {
            return newObj;
        }
        public Object getOld() {
            return oldObj;
        }
    }
}

interface UpdateOperation {
    public RaplaObject getCurrent();
}
    

