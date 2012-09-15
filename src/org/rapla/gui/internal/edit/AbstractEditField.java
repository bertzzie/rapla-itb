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
package org.rapla.gui.internal.edit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;

/** Base class for most rapla edit fields. Provides some mapping
    functionality such as reflection invocation of getters/setters.
    A fieldName "username" will result in a getUsername() and setUsername()
    method.
*/
public abstract class AbstractEditField extends RaplaGUIComponent
    implements EditField

{
    final static int DEFAULT_LENGTH = 30;
    String fieldName;

    ArrayList<ChangeListener> listenerList = new ArrayList<ChangeListener>();

    public AbstractEditField(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    abstract public JComponent getComponent();
    MappingDelegate delegate;

    protected void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    void setDelegate(MappingDelegate delegate) {
         this.delegate = delegate;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public boolean isBlock() {
        return false;
    }

    public boolean isVariableSized() {
        return false;
    }

    public String getName() {
        if (delegate != null)
            return delegate.getName();

        return getString(fieldName.toLowerCase());
    }

    protected String setMethodName() {
        return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    protected String getMethodName() {
        return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    private Method getMethod(String name,Object o,int params) throws RaplaException {
        Method[] methods = o.getClass().getMethods();
        for (int i=0;i<methods.length;i++)
            if (methods[i].getName().equals(name) && methods[i].getParameterTypes().length==params)
                return methods[i];
        throw new RaplaException(new NoSuchMethodException(name));
    }

    public void mapTo(Object o) throws RaplaException {
        if (delegate != null) {
            delegate.mapTo(o);
            return;
        }
        Method method = getMethod(setMethodName(),o,1);
        try {
            method.invoke(o,new Object[] {getValue()});
        } catch (InvocationTargetException ex) {
            throw new RaplaException(ex.getTargetException());
        } catch (Exception ex) {
            throw new RaplaException(ex);
        }
    }

    public void mapFrom(Object o) throws RaplaException {
        if (delegate != null) {
            delegate.mapFrom(o);
            return;
        }

        Method method = getMethod(getMethodName(),o,0);
        try {
            setValue(method.invoke(o,new Object[] {}));
        } catch (InvocationTargetException ex) {
            throw new RaplaException(ex.getTargetException());
        } catch (Exception ex) {
            throw new RaplaException(ex);
        }
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(listener);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.toArray(new ChangeListener[]{});
    }

    protected void fireContentChanged() {
        if (listenerList.size() == 0)
            return;
        ChangeEvent evt = new ChangeEvent(this);
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0;i<listeners.length; i++) {
            listeners[i].stateChanged(evt);
        }
    }

    abstract protected Object getValue() throws RaplaException;
    abstract protected void setValue(Object value) throws RaplaException;


}

