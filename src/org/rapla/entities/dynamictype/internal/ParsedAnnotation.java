/**
 *
 */
package org.rapla.entities.dynamictype.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

import org.rapla.components.util.Tools;
import org.rapla.entities.IllegalAnnotationException;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.storage.RefEntity;

class ParsedAnnotation implements Serializable {
    private static final long serialVersionUID = 1;

    /** the terminal format elements*/
    String[] nonVariables;
    /** the variable format elements*/
    Object[] variables;

    public ParsedAnnotation(String formatString, DynamicTypeImpl type) throws IllegalAnnotationException {

        ArrayList<Object> variablesList = new ArrayList<Object>();
        ArrayList<String> nonVariablesList = new ArrayList<String>();
        int pos = 0;
        int length = formatString.length();
        while (pos < length)
        {
            int start = formatString.indexOf('{',pos) + 1;
            if (start < 1) {
                nonVariablesList.add(formatString.substring(pos, length ));
                break;
            }
            int end = formatString.indexOf('}',start) ;
            if (end < 1 )
                throw new IllegalAnnotationException("Closing bracket } missing! in " + formatString);

            nonVariablesList.add(formatString.substring(pos, start -1));
            String key = formatString.substring(start,end).trim();
            Attribute attribute = type.getAttribute(key);
            if (attribute != null) {
                variablesList.add( ((RefEntity<?>)attribute).getId() );
            } else if (key.equals(type.getElementKey())) {
                variablesList.add( type.getId() );
            } else {
                throw new IllegalAnnotationException("Attribute for key '" + key
                                                + "' not found but defined in '" + formatString + "'"
                                                + "\n You have probably deleted or renamed the attribute. "
                                                );
            }
            pos = end + 1;
        }
        nonVariables = nonVariablesList.toArray(Tools.EMPTY_STRING_ARRAY);
        variables = variablesList.toArray();
    }

    public String getExternalRepresentation(DynamicTypeImpl type) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<nonVariables.length; i++) {
            buf.append(nonVariables[i]);
            if ( i < variables.length ) {
                Object variableId = variables[i];
                if ( type.getId().equals( variableId ) ) {
                    buf.append('{');
                    buf.append(type.getElementKey());
                    buf.append('}');
                }
                Attribute attribute = (Attribute) type.findAttributeForId( variableId );
                if ( attribute!= null) {
                    buf.append('{');
                    buf.append( attribute.getKey());
                    buf.append('}');
                }
            }
        }
        return buf.toString();
    }

    public String formatName(DynamicTypeImpl type,Classification classification,Locale locale) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<nonVariables.length; i++) {
            buf.append(nonVariables[i]);
            if ( i < variables.length ) {
                Object variableId = variables[i];
                if ( type.getId().equals( variableId ) ) {
                    buf.append(type.getName(locale));
                }
                Attribute attribute = (Attribute) type.findAttributeForId( variableId );
                if ( attribute!= null) {
                    buf.append(classification.getValueAsString(attribute, locale));
                }
            }
        }
        return buf.toString();
    }
}