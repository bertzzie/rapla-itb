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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.internal.common.NamedListCellRenderer;

/****************************************************************
 * This is the base-class for all Classification-Panels         *
 ****************************************************************/
public class ClassificationField extends AbstractEditField
    implements
            ActionListener
{
    JPanel content = new JPanel();
    JComboBox typeSelector;
    ClassificationEditUI editUI;

    DynamicType oldDynamicType;
    Classification classification;
    Classification oldClassification;

    public ClassificationField(RaplaContext sm) throws RaplaException {
        super( sm);
        editUI = new ClassificationEditUI(sm);
        setFieldName("type");
        content.setBorder(BorderFactory.createEmptyBorder(3,2,3,2));
    }

    public boolean isBlock() {
        return true;
    }

    public boolean isVariableSized() {
        return true;
    }

    public void mapTo(Object o) throws RaplaException {
        Classifiable classifiable = (Classifiable) o;
        classifiable.setClassification((Classification)editUI.getObject());
        editUI.mapToObject();
    }

    public void mapFrom(Object o) throws RaplaException {
        content.removeAll();
        Classifiable classifiable = (Classifiable) o;
        classification = classifiable.getClassification();
        editUI.setObject(classification);
        oldClassification = classification;
        RaplaType raplaType = ((RaplaObject)classifiable).getRaplaType();
        String classificationType = null;
        if (Reservation.TYPE.equals(raplaType)) {
        	classificationType = DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION;
        } else 	if ( Allocatable.TYPE.equals( raplaType )) {
        	
        	if ( ((Allocatable) classifiable).isPerson()) {
        		classificationType = DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION;
        	} else {
        		classificationType = DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION;
        	}
        }
        DynamicType[] types = getQuery().getDynamicTypes(classificationType);
        DynamicType dynamicType = classification.getType();
        oldDynamicType = dynamicType;

        typeSelector =  new JComboBox( types );
        typeSelector.setSelectedItem(dynamicType);
        typeSelector.setRenderer(new NamedListCellRenderer(getI18n().getLocale()));
        typeSelector.addActionListener(this);

        content.setLayout(new BorderLayout());
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.add(typeSelector,BorderLayout.WEST);
        content.add(container,BorderLayout.NORTH);

        JComponent editComponent = editUI.getComponent();

        JScrollPane scrollPane = new JScrollPane(editComponent
                                                 ,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                                                 ,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewportView(editComponent);

        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.setMinimumSize(new Dimension(300, 100));
        scrollPane.setPreferredSize(new Dimension(500, 240));

        content.add(scrollPane,BorderLayout.CENTER);
    }

    protected Object getValue() {
        return null;
    }

    protected void setValue(Object object) {
    }

    // The DynamicType has changed
    public void actionPerformed(ActionEvent event) {
        try {
            Object source = event.getSource();
            if (source == typeSelector ) {
                DynamicType dynamicType = (DynamicType) typeSelector.getSelectedItem();
                if (dynamicType.equals(oldDynamicType))
                    editUI.setObject(oldClassification);
                else
                    editUI.setObject( dynamicType.newClassification( classification ) );
            }
        } catch (RaplaException ex) {
            showException(ex,content);
        }
    }

    public JComponent getComponent() {
        return content;
    }
}


