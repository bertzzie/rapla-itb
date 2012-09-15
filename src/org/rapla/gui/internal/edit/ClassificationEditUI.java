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

import org.rapla.components.util.Assert;
import org.rapla.entities.Category;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;


public class ClassificationEditUI extends AbstractEditUI {
    public ClassificationEditUI(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    protected Classification classification;

    private String getAttName(String key) {
        return getName(classification.getAttribute(key));
    }

    protected Attribute getAttribute(int i) {
        return classification.getAttribute(((AbstractEditField) fields[i]).getFieldName());
    }

    private void setAttValue(String key,Object value) {
        classification.setValue( classification.getAttribute(key), value );
    }

    private Object getAttValue(String key) {
        return classification.getValue(classification.getAttribute(key));
    }

    private EditField createField(Attribute attribute) throws RaplaException {
        AttributeType type = attribute.getType();
        String key = attribute.getKey();
        AbstractEditField field = null;

        if (type.equals(AttributeType.STRING)) {
            Integer rows = new Integer(attribute.getAnnotation(AttributeAnnotations.KEY_EXPECTED_ROWS,"1"));
            Integer columns = new Integer(attribute.getAnnotation(AttributeAnnotations.KEY_EXPECTED_COLUMNS,String.valueOf(TextField.DEFAULT_LENGTH)));
            field = new TextField(getContext(),key, rows.intValue(), columns.intValue());
        } else if (type.equals(AttributeType.INT)) {
            field = new LongField(getContext(),key);
        } else if (type.equals(AttributeType.DATE)) {
            field = new DateField(getContext(),key);
        } else if (type.equals(AttributeType.BOOLEAN)) {
            field = new BooleanField(getContext(),key);
        } else if (type.equals(AttributeType.CATEGORY))
        {
            Category defaultCategory = (Category) attribute.defaultValue();
            Category rootCategory = (Category) attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
            if (rootCategory.getDepth() > 2) {
                field = new CategorySelectField(getContext(),key, rootCategory, defaultCategory);
            } else {
                field= new CategoryListField(getContext(),key, rootCategory);
            }
        }
        Assert.notNull(field, "Unknown AttributeType");
        field.setDelegate(new MyMappingDelegate(field));
        return field;
    }

    public void setObject(Object object) throws RaplaException {
        //classification = (Classification) ((Classification) object).clone();
        classification = (Classification) object;
        this.o = classification;
        DynamicType type = classification.getType();
        Attribute[] attributes = type.getAttributes();
        EditField[] fields = new EditField[attributes.length];
        for (int i=0;i<attributes.length;i++)
        {
            fields[i] = createField(attributes[i]);
            fields[i].mapFrom(object);
        }
        setFields(fields);
    }

    class MyMappingDelegate implements MappingDelegate {
        AbstractEditField field;
        MyMappingDelegate(AbstractEditField field) {
            this.field = field;
        }
        public void mapTo(Object o) throws RaplaException {
            setAttValue(field.getFieldName(),field.getValue());
        }
        public String getName() {
            return getAttName(field.getFieldName());
        }
        public void mapFrom(Object o) throws RaplaException {
            field.setValue(getAttValue(field.getFieldName()));
        }
    }

 


}

