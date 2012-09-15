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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;


public class BooleanField extends AbstractEditField implements ActionListener, FocusListener
{
    JPanel panel = new JPanel();
    JRadioButton field1 = new JRadioButton();
    JRadioButton field2 = new JRadioButton();

    public BooleanField(RaplaContext sm,String fieldName) throws RaplaException {
        super( sm);
        setFieldName( fieldName );
        field1.setOpaque( false );
        field2.setOpaque( false );
        panel.setOpaque( false );
        panel.setLayout( new BoxLayout(panel,BoxLayout.X_AXIS) );
        panel.add( field1 );
        panel.add( field2 );
        ButtonGroup group = new ButtonGroup();
        group.add( field1 );
        group.add( field2 );
        field2.setSelected( true );
        field1.addActionListener(this);
        field2.addActionListener(this);

        field1.setText(getString("yes"));
        field2.setText(getString("no"));
        field1.addFocusListener(this);

    }

    public Object getValue() {
        return field1.isSelected() ? Boolean.TRUE : Boolean.FALSE;
    }

    protected String getMethodName() {
        return "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    public void setValue(Object object) {
        if (object != null) {
            boolean selected = ((Boolean)object).booleanValue();
            field1.setSelected(selected);
            field2.setSelected(!selected);
        } else {
            field1.setSelected(false);
            field2.setSelected(true);
        }
    }

    public void actionPerformed(ActionEvent evt) {
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