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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.internal.common.NamedListCellRenderer;

public class ListField extends AbstractEditField implements ActionListener, FocusListener {
    JPanel panel;
    JComboBox field;
    protected String nothingSelected;
    Vector<?> list;

    public ListField(RaplaContext sm,String fieldName,Vector<?> v)  throws RaplaException {
        this(sm,fieldName);
        setVector(v);
    }

    protected ListField(RaplaContext sm,String fieldName) throws RaplaException {
        super( sm);
        setFieldName(fieldName);
        panel = new JPanel();
        panel.setOpaque(false);
        field = new JComboBox();
        field.addActionListener(this);
        panel.setLayout(new BorderLayout());
        panel.add(field,BorderLayout.WEST);
        field.setRenderer(new NamedListCellRenderer(getI18n().getLocale()));
        nothingSelected = getString("nothing_selected");
		field.addFocusListener(this);
    }

    protected void setVector(Vector<?> v) {
        this.list = v;
        field.setModel(new DefaultComboBoxModel(v));
    }

    
    public void setRenderer(ListCellRenderer renderer) {
        field.setRenderer( renderer );
    }

    protected Object getValue() {
        Object value = field.getSelectedItem();
        if (list.contains(nothingSelected) && nothingSelected.equals(value)) {
            return null;
        } else {
            return value;
        }
    }
    protected void setValue(Object value) {
        if (list.contains(nothingSelected) && value==null) {
            field.setSelectedItem(nothingSelected);
        } else {
            field.setSelectedItem(value);
        }
    }
    public JComponent getComponent() {
        return panel;
    }

    public void actionPerformed(ActionEvent evt) {
        fireContentChanged();
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
