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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.toolkit.RaplaColorList;

public class TextField extends AbstractEditField implements ActionListener,FocusListener,KeyListener {
    JTextComponent field;
    JComponent colorPanel;
    JScrollPane scrollPane;
    JButton colorChooserBtn ;
    JPanel color;
    Object oldValue;
    Color currentColor;
    public TextField(RaplaContext sm,String fieldName) throws RaplaException {
        this( sm,fieldName, 1, DEFAULT_LENGTH);
    }
        
    public TextField(RaplaContext sm,String fieldName, int rows, int columns) throws RaplaException {
        super( sm);
        setFieldName( fieldName );
        if ( rows > 1 ) {
            field = new JTextArea();
            scrollPane = new JScrollPane( field, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            ((JTextArea) field).setColumns( columns);
            ((JTextArea) field).setRows( rows );
        } else {
            field = new JTextField( columns);
        }
        addCopyPaste( field);
        field.addFocusListener(this);
        field.addKeyListener(this);
        if (fieldName.equals("color"))
        {
        	colorPanel = new JPanel();
        	color = new JPanel();
        	color.setPreferredSize(new Dimension(20,20));
        	
        	color.setBorder( BorderFactory.createEtchedBorder());
        	colorPanel.setLayout(new BorderLayout());
        	colorPanel.add( field, BorderLayout.CENTER);
            colorPanel.add( color, BorderLayout.WEST);
        	colorChooserBtn = new JButton();
        	if ( field instanceof JTextField)
        	{
        		((JTextField) field).setColumns( 7);
        	}
        	else
        	{
        		((JTextArea) field).setColumns( 7);
           }
            colorPanel.add( colorChooserBtn, BorderLayout.EAST);
            colorChooserBtn.setText( getString("change") );
            colorChooserBtn.addActionListener( new ActionListener() {

        		public void actionPerformed(ActionEvent e) {
        			currentColor = JColorChooser.showDialog(
        					colorPanel,
                             "Choose Background Color",
                              currentColor);
                    color.setBackground( currentColor );
                    if ( currentColor != null) {
                    	field.setText( RaplaColorList.getHexForColor( currentColor ));
                    }
                    fireContentChanged();
        		}

            });
        }
        setValue("");
    }

    protected Object getValue() {
        return field.getText().trim();
    }
    
    protected void setValue(Object object) {
        if (object == null)
            object = "";
        field.setText((String)object);
        oldValue = (String) object;
        
        
        if ( colorPanel != null) {
        	try
        	{
        		currentColor =  RaplaColorList.getColorForHex( object.toString() );
        	}
        	catch (NumberFormatException ex)
        	{
        		currentColor = null;
        	}
        	color.setBackground( currentColor );
        }
    }
    public JComponent getComponent() {
    	if ( colorPanel!= null)
    	{
    		return colorPanel;
    	}
    	if ( scrollPane != null ) {
            return scrollPane;
        } else {
            return field;
        }
    }

    public void actionPerformed(ActionEvent evt) {
        if (field.getText().equals(oldValue))
            return;
        oldValue = field.getText();
        fireContentChanged();
    }

    public void focusLost(FocusEvent evt) {
        if (field.getText().equals(oldValue))
            return;
        oldValue = field.getText();
        fireContentChanged();
    }

    public void focusGained(FocusEvent evt) {
    		Component focusedComponent = evt.getComponent(); 
    		Component  parent = focusedComponent.getParent();
    		if(parent instanceof JPanel) {
    			((JPanel)parent).scrollRectToVisible(focusedComponent.getBounds(null)); 
    		}
    }

    public void keyPressed(KeyEvent evt) {}
    public void keyTyped(KeyEvent evt) {}
    public void keyReleased(KeyEvent evt) {
        if (field.getText().equals(oldValue))
            return;
        oldValue = field.getText();
        fireContentChanged();
    }

}

