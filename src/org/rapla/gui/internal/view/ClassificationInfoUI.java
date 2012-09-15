
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
package org.rapla.gui.internal.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.rapla.entities.Timestamp;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

class ClassificationInfoUI extends HTMLInfo {
    public ClassificationInfoUI(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    public void insertClassificationTitle( Classifiable classifiable, StringBuffer buf ) {
        Classification classification = classifiable.getClassification();
        buf.append( "<strong>");
        Locale locale = getRaplaLocale().getLocale();
        encode( classification.getType().getName(locale), buf );
        buf.append( "</strong>");
    }

    protected void insertClassification( Classifiable classifiable, StringBuffer buf ) {
        insertClassificationTitle( classifiable, buf );
        Collection<Row> att = new ArrayList<Row>();
        att.addAll(getClassificationAttributes(classifiable, false));
        createTable(att,buf,false);
    }

    protected Collection<Row> getClassificationAttributes(Classifiable classifiable, boolean excludeAdditionalInfos) {
        Collection<Row> att = new ArrayList<Row>();
        Classification classification = classifiable.getClassification();

        Attribute[] attributes = classification.getAttributes();
        for (int i=0; i< attributes.length; i++) {
            Attribute attribute = attributes[i];
            String view = attribute.getAnnotation( AttributeAnnotations.KEY_EDIT_VIEW, AttributeAnnotations.VALUE_MAIN_VIEW );
            if ( view.equals(AttributeAnnotations.VALUE_NO_VIEW )) {
                continue;
            }
            if ( excludeAdditionalInfos && !view.equals( AttributeAnnotations.VALUE_MAIN_VIEW ) ) {
                continue;
            }
            Object value = classification.getValue(attribute);
            /*
            if (value == null)
                continue;
			*/	
            String name = getName(attribute);
            String valueString = null;
            Locale locale = getRaplaLocale().getLocale();
            if (value instanceof Boolean) {
                valueString = getString(((Boolean) value).booleanValue() ? "yes":"no");
            } else {
                valueString = classification.getValueAsString(attribute, locale);
            }
            att.add (new Row(name,encode(valueString)));
        }
        return att;
    }

    protected String getTooltip(Object object) {
        Classifiable classifiable = (Classifiable) object;
        StringBuffer buf = new StringBuffer();
        Collection<Row> att = new ArrayList<Row>();
        att.addAll(getClassificationAttributes(classifiable, false));
        createTable(att,buf,false);
        return buf.toString();
    }

    protected String createHTMLAndFillLinks(Object object,LinkController controller) {
        Classifiable classifiable = (Classifiable) object;
        StringBuffer buf = new StringBuffer();
        insertClassification( classifiable, buf );
        return buf.toString();
    }
    
    void insertModificationRow( Timestamp timestamp, StringBuffer buf ) {
        final Date createTime = timestamp.getCreateTime();
        final Date lastChangeTime = timestamp.getLastChangeTime();
        if ( lastChangeTime != null)
        {
            buf.append("<div style=\"font-size:7px;margin-bottom:4px;\">");
            if ( createTime != null)
            {
                buf.append(getString("created_at"));
                buf.append(" ");
                buf.append(getRaplaLocale().formatDate(createTime));
                buf.append(", ");
            }
            buf.append(getString("last_changed"));
            buf.append(" ");
            buf.append(getRaplaLocale().formatDate(lastChangeTime));
            buf.append("</div>");
            buf.append("\n");
        }
    }

}


