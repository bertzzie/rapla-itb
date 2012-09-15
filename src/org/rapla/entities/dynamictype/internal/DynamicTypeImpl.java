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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.rapla.components.util.Tools;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.IllegalAnnotationException;
import org.rapla.entities.MultiLanguageName;
import org.rapla.entities.Named;
import org.rapla.entities.RaplaType;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.Mementable;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleEntity;

public class DynamicTypeImpl extends SimpleEntity<DynamicType> implements DynamicType,Named,Mementable<DynamicType>,java.io.Serializable
{
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 2;

    // added an attribute array for performance reasons
    transient private boolean attributeArrayUpToDate = false;
    transient Attribute[] attributes;

    MultiLanguageName name  = new MultiLanguageName();
    String elementKey = "";

    HashMap<String,ParsedAnnotation> annotations = new HashMap<String,ParsedAnnotation>();

    public DynamicTypeImpl() {
    }

    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        super.resolveEntities( resolver);
        attributeArrayUpToDate = false;
    }

    public RaplaType getRaplaType() {return TYPE;}

    public Classification newClassification() {
    	if ( !isPersistant()) {
    		throw new IllegalStateException("You can only create Classifications from a persistant Version of DynamicType");
    	}
        final ClassificationImpl classification = new ClassificationImpl(this);
        // Array could not be up todate
        final Attribute[] attributes2 = getAttributes();
        for ( Attribute att: attributes2)
        {
            final Object defaultValue = att.defaultValue();
            if ( defaultValue != null)
            {
                classification.setValue(att, defaultValue);
            }   
        }
        return classification;
    }

    public Classification newClassification(Classification original) {
        if ( !isPersistant()) {
            throw new IllegalStateException("You can only create Classifications from a persistant Version of DynamicType");
        }
        final ClassificationImpl newClassification = (ClassificationImpl) newClassification();
        {
            Attribute[] attributes = original.getAttributes();
            for (int i=0;i<attributes.length;i++) {
                Attribute originalAttribute = attributes[i];
                String attributeKey = originalAttribute.getKey();
                Attribute newAttribute = newClassification.getAttribute( attributeKey );
                Object defaultValue = originalAttribute.defaultValue();
                Object originalValue = original.getValue( attributeKey );
                if ( newAttribute != null && originalValue != null && newAttribute.getType().equals( originalAttribute.getType()) 
                && ( defaultValue == null || !originalValue.equals(defaultValue)))		
                {
                    newClassification.setValue( newAttribute, newAttribute.convertValue( originalValue ));
                }
            }
            return newClassification;
        }
    };

    public ClassificationFilter newClassificationFilter() {
    	if ( !isPersistant()) {
    		throw new IllegalStateException("You can only create ClassificationFilters from a persistant Version of DynamicType");
    	}
        return new ClassificationFilterImpl(this);
    }

    public MultiLanguageName getName() {
        return name;
    }

    public void setReadOnly(boolean enable) {
        super.setReadOnly( enable );
        name.setReadOnly( enable );
    }

    public String getName(Locale locale) {
        return name.getName(locale.getLanguage());
    }

    public String getAnnotation(String key) {
        ParsedAnnotation parsedAnnotation = annotations.get(key);
        if ( parsedAnnotation != null) {
            return parsedAnnotation.getExternalRepresentation(this);
        } else {
            return null;
        }
    }
    
    public boolean isRefering(RefEntity<?> entity) {
        Attribute[] attributes = getAttributes();
        for ( int i=0;i<attributes.length;i++)
        {
            RefEntity<?> attribute = (RefEntity<?>)attributes[i];
            if ( attribute.isRefering( entity))
            {
                return true;
            }
        }
        return super.isRefering(entity);
    }

    public String getAnnotation(String key, String defaultValue) {
        String annotation = getAnnotation( key );
        return annotation != null ? annotation : defaultValue;
    }

    public void setAnnotation(String key,String annotation) throws IllegalAnnotationException {
        checkWritable();
        if (annotation == null) {
            annotations.remove(key);
            return;
        }
        annotations.put(key,new ParsedAnnotation(annotation, this));
    }

    public String[] getAnnotationKeys() {
        return annotations.keySet().toArray(Tools.EMPTY_STRING_ARRAY);
    }

    public void setElementKey(String elementKey) {
        checkWritable();
        this.elementKey = elementKey;
    }

    public String getElementKey() {
        return elementKey;
    }

    /** exchange the two attribute positions */
    public void exchangeAttributes(int index1, int index2) {
        checkWritable();
        Attribute[] attribute = getAttributes();
        Attribute attribute1 = attribute[index1];
        Attribute attribute2 = attribute[index2];
        getSubEntityHandler().clearReferences();
        for (int i=0;i<attributes.length;i++) {
            if (i == index1)
                getSubEntityHandler().add((RefEntity<?>)attribute2);
            else if (i == index2)
                getSubEntityHandler().add((RefEntity<?>)attribute1);
            else
                getSubEntityHandler().add((RefEntity<?>)attributes[i]);
        }
        attributeArrayUpToDate = false;
    }

    /** find an attribute in the dynamic-type that equals the specified attribute. */
    public Attribute findAttribute(Attribute copy) {
        return (Attribute) super.findEntity((RefEntity<?>)copy);
    }

    public Attribute findAttributeForId(Object id) {
        Attribute[] typeAttributes = getAttributes();
        for (int i=0; i<typeAttributes.length; i++) {
            if (((RefEntity<?>)typeAttributes[i]).getId().equals(id)) {
                return typeAttributes[i];
            }
        }
        return null;
    }


    public void removeAttribute(Attribute attribute) {
        checkWritable();
        if ( findAttribute( attribute ) == null) {
            return;
        }
        attributeArrayUpToDate = false;
        super.removeEntity((RefEntity<?>) attribute);
        if (this.equals(attribute.getDynamicType()))
            ((AttributeImpl) attribute).setParent(null);
    }

    public void addAttribute(Attribute attribute) {
        checkWritable();
        attributeArrayUpToDate = false;
        super.addEntity((RefEntity<?>) attribute);
        if (attribute.getDynamicType() != null
            && !this.isIdentical(attribute.getDynamicType()))
            throw new IllegalStateException("Attribute '" + attribute
                                            + "' belongs to another dynamicType :"
                                            + attribute.getDynamicType());
        ((AttributeImpl) attribute).setParent(this);
    }

    private void updateAttributeArray() {
        if (attributeArrayUpToDate)
            return;
        Collection<Attribute> attributeList = new ArrayList<Attribute>();
        Iterator<RefEntity<?>> it = super.getSubEntities();
        while (it.hasNext()) {
            RefEntity<?> o =  it.next();
            if (o.getRaplaType().equals(Attribute.TYPE)) {
                attributeList.add((Attribute)o);
            }
        }
        attributes =  attributeList.toArray(Attribute.ATTRIBUTE_ARRAY);
        attributeArrayUpToDate = true;
    }

    public boolean hasAttribute(Attribute attribute) {
        return getReferenceHandler().isRefering((RefEntity<?>)attribute);
    }

    public Attribute[] getAttributes() {
        updateAttributeArray();
        return attributes;
    }

    public Attribute getAttribute(String key) {
        Attribute[] attributes = getAttributes();
        for (int i=0;i<attributes.length;i++) {
            if (attributes[i].getKey().equals(key))
                return attributes[i];
        }
        return null;
    }

    ParsedAnnotation getParsedAnnotation(String key) {
        return  annotations.get( key );
    }

    @SuppressWarnings("unchecked")
	static private void copy(DynamicTypeImpl source,DynamicTypeImpl dest) {
        dest.annotations = (HashMap<String,ParsedAnnotation>) source.annotations.clone();
        dest.name = (MultiLanguageName) source.name.clone();
        dest.elementKey = source.elementKey;
        Iterator<RefEntity<?>> it = dest.getSubEntities();
        while ( it.hasNext()) {
            AttributeImpl att = (AttributeImpl) it.next();
            att.setParent(dest);
        }
        dest.attributeArrayUpToDate = false;
    }

    @SuppressWarnings("unchecked")
	public void copy(DynamicType obj) {
        super.copy((SimpleEntity<DynamicType>)obj);
        copy((DynamicTypeImpl) obj,this);
    }

    public DynamicType deepClone() {
        DynamicTypeImpl clone = new DynamicTypeImpl();
        super.deepClone(clone);
        copy(this,clone);
        return clone;
    }

    public DynamicType clone() {
        DynamicTypeImpl clone = new DynamicTypeImpl();
        super.clone(clone);
        copy(this,clone);
        return clone;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(" [");
        buf.append ( super.toString()) ;
        buf.append("] key=");
        buf.append( getElementKey() );
        buf.append(": ");
        if ( attributes != null ) {
            Attribute[] att = getAttributes();
            for ( int i=0;i<att.length; i++){
                if ( i> 0)
                    buf.append(", ");
                buf.append( att[i].getKey());
            }
        }
        return buf.toString();
    }

	/**
	 * @param newType
	 * @param attributeId
	 */
	public boolean hasAttributeChanged(DynamicTypeImpl newType, Object attributeId) {
    	Attribute oldAttribute = findAttributeForId(attributeId );
    	Attribute newAttribute = newType.findAttributeForId(attributeId );
    	if ((newAttribute == null ) ||  ( oldAttribute == null)) {
    		return true;
    	}
		if ( !newAttribute.getKey().equals( oldAttribute.getKey() )) {
			return true;
		}
		if ( !newAttribute.getType().equals( oldAttribute.getType())) {
			return true;
		}
		{
			String[] keys = newAttribute.getConstraintKeys();
			String[] oldKeys = oldAttribute.getConstraintKeys();
			if ( keys.length != oldKeys.length) {
				return true;
			}
			for ( int i=0;i< keys.length;i++) {
				if ( !keys[i].equals( oldKeys[i]) )
					return true;
				Object oldConstr = oldAttribute.getConstraint( keys[i]);
				Object newConstr = newAttribute.getConstraint( keys[i]);
				if ( oldConstr == null && newConstr == null)
					continue;
				if ( oldConstr == null || newConstr == null)
					return true;

				if ( !oldConstr.equals( newConstr))
					return true;
			}
		}
		return false;
	}

}


