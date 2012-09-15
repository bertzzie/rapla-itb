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
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.calendar.RaplaCalendar;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class DateField extends AbstractEditField implements DateChangeListener, FocusListener {
    RaplaCalendar field;
    JPanel panel;
    public DateField(RaplaContext sm,String fieldName) throws RaplaException {
        super( sm);
        setFieldName(fieldName);
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        field = createRaplaCalendar();
        panel.add(field,BorderLayout.WEST);
        panel.setOpaque( false );
        field.setNullValuePossible( true);
        field.addDateChangeListener(this);
        field.addFocusListener(this);
    }

    protected Object getValue() {
        return field.getDate();
    }
    protected void setValue(Object object) {
        Date date = (Date) object;
        field.setDate(date);
    }

    public RaplaCalendar getCalendar() {
        return field;
    }

    public void dateChanged(DateChangeEvent evt) {
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

