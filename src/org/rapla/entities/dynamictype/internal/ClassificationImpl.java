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
package org.rapla.entities.dynamictype.internal;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.ReadOnlyException;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.storage.CannotExistWithoutTypeException;
import org.rapla.entities.storage.DynamicTypeDependant;
import org.rapla.entities.storage.EntityReferencer;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.ReferenceHandler;

/** Use the method <code>newClassification()</code> of class <code>DynamicType</code> to
 *  create a classification. Once created it is not possible to change the
 *  type of a classifiction. But you can replace the classification of an
 *  object implementing <code>Classifiable</code> with a new one.
 *  @see DynamicType
 *  @see org.rapla.entities.dynamictype.Classifiable
 */
public class ClassificationImpl implements Classification,java.io.Serializable, DynamicTypeDependant, EntityReferencer {
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;

    boolean readOnly = false;

    transient String nameString;
    transient boolean isNameUpToDate;
    transient ParsedAnnotation lastParsedAnnotation;

    /** stores the nonreference values like integers,boolean and string.*/
    HashMap<Object,Object> attributeValueMap = new HashMap<Object,Object>();

    /** stores the references to the dynamictype and the reference values */
    ReferenceHandler referenceHandler = new ReferenceHandler();


    ClassificationImpl(DynamicTypeImpl dynamicType) {
        referenceHandler.put("parent",dynamicType);
    }

    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        referenceHandler.resolveEntities( resolver);
    }

    public void setReadOnly(boolean enable) {
        this.readOnly = enable;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void checkWritable() {
        if ( readOnly )
            throw new ReadOnlyException( this );
    }

    public boolean isRefering(RefEntity<?> obj) {
        return referenceHandler.isRefering(obj);
    }

    public Iterator<RefEntity<?>> getReferences() {
        return referenceHandler.getReferences();
    }

    public DynamicType getType() {
        return (DynamicType) referenceHandler.get("parent");
    }

    public String getName(Locale locale) {
        DynamicTypeImpl type = (DynamicTypeImpl)getType();
        ParsedAnnotation parsedAnnotation = type.getParsedAnnotation( DynamicTypeAnnotations.KEY_NAME_FORMAT );
        if ( parsedAnnotation == null) {
            return type.toString();
        }

        if (isNameUpToDate)
        {
            if (parsedAnnotation.equals(lastParsedAnnotation))
                return nameString;
        }
        lastParsedAnnotation =  parsedAnnotation;
        nameString = parsedAnnotation.formatName(type, this, locale);
        isNameUpToDate = true;
        return nameString;
    }

    public String getValueAsString(Attribute attribute,Locale locale)
    {
        Object value = getValue(attribute);
        if (value == null)
            return "";
        if (value instanceof Category) {
            Category rootCategory = (Category) attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
            return ((Category) value).getPath(rootCategory, locale);
        }
        if (value instanceof Date) {
            DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM,locale);
            format.setTimeZone(DateTools.getTimeZone());
            return format.format((Date) value);
        } else {
            return value.toString();
        }
    }

    public Attribute getAttribute(String key) {
        return getType().getAttribute(key);
    }

    public Attribute[] getAttributes() {
        return getType().getAttributes();
    }

    public boolean needsChange(DynamicType newType) {
    	if ( !hasType (newType )) {
            return false;
    	}
        if ( !newType.getElementKey().equals( getType().getElementKey()))
        	return true;
        for (String referenceKey :referenceHandler.getReferenceKeys()) {
            RefEntity<?> attribute = ((RefEntity<?>)findAttributeByReferenceKey( getType(), referenceKey));
            if (attribute == null)
            	continue;
            
            if (((DynamicTypeImpl)getType()).hasAttributeChanged( (DynamicTypeImpl)newType , attribute.getId()))
            	return true;
        }
        for (Object id:attributeValueMap.keySet()) {
            if (((DynamicTypeImpl)getType()).hasAttributeChanged( (DynamicTypeImpl)newType , id))
            	return true;
        }
        return false;
    }

    boolean hasType(DynamicType type) {
        return getType().equals( type);
    }

    public void commitChange(DynamicType type) {
        if ( !hasType (type )) {
            return;
        }
        // update referenced values
        referenceHandler.put("parent", (RefEntity<?>) type);
        Collection<Attribute> attributes = new ArrayList<Attribute>();
        Collection<Object> removedKeys = new ArrayList<Object>();
        Collection<Object> removedIds = new ArrayList<Object>();
        for (String referenceKey : referenceHandler.getReferenceKeys())
        {
        	if ( referenceKey.equals ("parent") )
        		continue;
        	Attribute attribute = findAttributeByReferenceKey(type, referenceKey ) ;
        	if (attribute != null )
        		attributes.add( attribute);
        	else
        		removedKeys.add( referenceKey );
        }
       for  (Object id:attributeValueMap.keySet()) {
            Attribute attribute = findAttribute(type, id );
            if (attribute != null) {
            	attributes.add ( attribute );
            } else {
            	removedIds.add( id );
            }
        }

        for (Attribute attribute: attributes) 
        {
    		Object oldValue = getValue( attribute);
    		Object newValue = attribute.convertValue(oldValue);
    		setValue( attribute,newValue);
        }

        for (Object key: removedKeys) {
        	referenceHandler.removeId (key.toString() );
        }
        for (Object id: removedIds) {
        	attributeValueMap.remove ( id.toString() );
        }
        isNameUpToDate = false;
    }

    /** find the attribute of the given type that matches the id */
    private Attribute findAttribute(DynamicType type,Object id) {
        Attribute[] typeAttributes = type.getAttributes();
        for (int i=0; i<typeAttributes.length; i++) {
            if (((RefEntity<?>)typeAttributes[i]).getId().equals(id)) {
                return typeAttributes[i];
            }
        }
        return null;
    }

    /** find the attribute of the given type that matches the id */
    private static Attribute findAttributeByReferenceKey(DynamicType type,String key) {
        Attribute[] typeAttributes = type.getAttributes();
        for (int i=0; i<typeAttributes.length; i++) {
            if (((RefEntity<?>)typeAttributes[i]).getId().toString().equals(key)) {
                return typeAttributes[i];
            }
        }
        return null;
    }


    public void setValue(String key,Object value) {
    	Attribute attribute = getAttribute( key );
    	if ( attribute == null ) {
    		throw new NoSuchElementException("No attribute found for key " + key);
    	}

    	setValue( attribute,value);
    }

    public Object getValue(String key) {
    	Attribute attribute = getAttribute( key );
    	if ( attribute == null ) {
    		throw new NoSuchElementException("No attribute found for key " + key);
    	}

    	return getValue(getAttribute(key));
    }

    public void setValue(Attribute attribute,Object value) {
        checkWritable();
        if (attribute.getType().equals(AttributeType.STRING)
            && value != null
            && ((String)value).length() == 0)
            value = null;

        Object attributeId = ((RefEntity<?>)attribute).getId();
        if (attribute.getType().equals(AttributeType.CATEGORY)) {
            referenceHandler.put(attributeId.toString(), (RefEntity<?>)value);
            // we need to remove it from the other the other map
            // Important! The map is for attributes objects not for their string representations
            attributeValueMap.remove(attributeId);
        } else {
        	attributeValueMap.put(attributeId,value);
        	referenceHandler.removeId(attributeId.toString());
        }
        isNameUpToDate = false;
    }

    public Object getValue(Attribute attribute) {
    	if ( attribute == null ) {
    		throw new NullPointerException("Attribute can't be null");
    	}
        Object attributeId = ((RefEntity<?>)attribute).getId();

        // first lookup in attribute map
        Object o = attributeValueMap.get(attributeId);

        // not found, then lookup in the reference map
        if ( o == null)
        	o = referenceHandler.get(attributeId.toString());

        if (o != null)
        	return o;
        
        if ( attribute.getType().is(AttributeType.BOOLEAN))
        {
            return Boolean.FALSE;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
	public Object clone() {
        ClassificationImpl clone = new ClassificationImpl((DynamicTypeImpl)getType());
        clone.referenceHandler = (ReferenceHandler) referenceHandler.clone();
        clone.attributeValueMap = (HashMap<Object,Object>) attributeValueMap.clone();
        clone.isNameUpToDate = false;
        clone.readOnly = false;// clones are always writable
        return clone;
    }

     public String toString() {
         if (getType() == null) {
             return super.toString();
         }
         StringBuffer buf = new StringBuffer();
         Attribute[] att = getAttributes();
         for ( int i=0;i<att.length; i++){
             if ( i> 0)
                 buf.append(", ");
             buf.append( att[i].getKey());
             buf.append("=");
             buf.append( getValue( att[i]));
         }
         return buf.toString();
     }

    public void commitRemove(DynamicType type) throws CannotExistWithoutTypeException 
    {
        throw new CannotExistWithoutTypeException();
    }
}