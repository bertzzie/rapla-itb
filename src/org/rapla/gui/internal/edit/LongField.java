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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.calendar.RaplaNumber;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class LongField extends AbstractEditField implements ChangeListener, FocusListener {
    JPanel panel = new JPanel();
    RaplaNumber field;
    public LongField( RaplaContext sm,String fieldName ) throws RaplaException {
        this( sm,fieldName, null );
    }
    public LongField( RaplaContext sm, String fieldName, Long minimum ) throws RaplaException {
        super( sm);
        setFieldName( fieldName );
        panel.setLayout( new BorderLayout() );
        panel.setOpaque( false );
        field = new RaplaNumber( minimum, minimum, null, minimum == null );
        field.setColumns(8);
        field.addChangeListener(this);
        panel.add(field,BorderLayout.WEST);
        field.addFocusListener(this);
    }
    protected Object getValue() throws RaplaException {
        if (field.getNumber() != null)
            return new Long(field.getNumber().longValue());
        else
            return null;
    }
    protected void setValue(Object object) {
        if (object != null) {
            field.setNumber((Number)object);
        } else {
            field.setNumber(null);
        }
    }

    public void stateChanged(ChangeEvent evt) {
        fireContentChanged();
    }

    public JComponent getComponent() {
        return panel;
    }

    public void focusGained(FocusEvent evt) {

		Component focusedComponent = evt.getComponent(); 
		Component  parent = focusedComponent.getParent();
		if(parent instanceof JPanel) {
			((JPanel)parent).scrollRectToVisible(focusedComponent.getBounds(null)); 
		}

    }
    
    public void focusLost(FocusEvent evt) {

    }
}
