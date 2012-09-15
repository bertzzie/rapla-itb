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
package org.rapla.storage.xml;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.Assert;
import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.components.util.xml.XMLWriter;
import org.rapla.entities.Annotatable;
import org.rapla.entities.Category;
import org.rapla.entities.MultiLanguageName;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.Timestamp;
import org.rapla.entities.User;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.storage.LocalCache;
import org.xml.sax.helpers.NamespaceSupport;

/** Stores the data from the local cache in XML-format to a print-writer.*/
abstract public class RaplaXMLWriter extends XMLWriter
    implements Namespaces
{
    protected LocalCache cache;

    String encoding = "utf-8";
    final static String OUTPUT_FILE_VERSION="1.0";
    protected NamespaceSupport namespaceSupport = new NamespaceSupport();
    private boolean isIdOnly;
    private boolean printVersion;
    private Map<String,RaplaType> localnameMap;
    Logger logger;
    Map<RaplaType,RaplaXMLWriter> writerMap;
    protected RaplaContext sm;
    public SerializableDateTimeFormat dateTimeFormat;
    @SuppressWarnings("unchecked")
	public RaplaXMLWriter( RaplaContext sm) throws RaplaException {
        this.sm = sm;
        enableLogging( (Logger) sm.lookup( Logger.class.getName()));
        this.writerMap = (Map<RaplaType,RaplaXMLWriter>) sm.lookup( PreferenceWriter.WRITERMAP );
        RaplaLocale raplaLocale = (RaplaLocale) sm.lookup(RaplaLocale.ROLE);
        dateTimeFormat = new SerializableDateTimeFormat(raplaLocale.createCalendar());
        this.localnameMap = (Map<String,RaplaType>)  sm.lookup(PreferenceReader.LOCALNAMEMAPENTRY);
        this.cache = (LocalCache)  sm.lookup(LocalCache.class.getName());
        this.isIdOnly = sm.has("idonly");
        this.printVersion = sm.has("printversion");
        Assert.notNull(cache);

        namespaceSupport.pushContext();
        for (int i=0;i<NAMESPACE_ARRAY.length;i++) {
            String prefix = NAMESPACE_ARRAY[i][1];
            String uri = NAMESPACE_ARRAY[i][0];
            if ( prefix != null) {
                namespaceSupport.declarePrefix(prefix, uri);
            }
        }
    }


    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    protected Logger getLogger() {
        return logger;
    }

  
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    protected void printTimestamp(Timestamp stamp) throws IOException,RaplaException {
        final Date createTime = stamp.getCreateTime();
        final Date lastChangeTime = stamp.getLastChangeTime();
        if ( createTime != null)
        {
            att("created-at", dateTimeFormat.formatDate( createTime));
        }
        if ( lastChangeTime != null)
        {
            att("last-changed", dateTimeFormat.formatDate( lastChangeTime));
        }
        User user = stamp.getLastChangedBy();
        if ( user != null) 
        {  
            att("last-changed-by", getId(user));
        }
    }
    protected void printTranslation(MultiLanguageName name) throws IOException {
        Iterator<String> it= name.getAvailableLanguages().iterator();
        while (it.hasNext()) {
            String lang = (String)it.next();
            String value = name.getName(lang);
            openTag("doc:name");
            att("lang",lang);
            closeTagOnLine();
            printEncode(value);
            closeElementOnLine("doc:name");
            println();
        }
    }

    protected void printAnnotations(Annotatable annotatable) throws IOException,RaplaException {
        String[] keys = annotatable.getAnnotationKeys();
        if ( keys.length == 0 )
            return;
        openElement("doc:annotations");
        for (int i=0;i<keys.length;i++) {
            String value = annotatable.getAnnotation(keys[i]);
            openTag("rapla:annotation");
            att("key", keys[i]);
            closeTagOnLine();
            printEncode(value);
            closeElementOnLine("rapla:annotation");
            println();
        }
        closeElement("doc:annotations");
    }


    protected void printAttributeValue(Attribute attribute, Object value) throws IOException,RaplaException {
    	if ( value == null)
    	{
    		return;
    	}
    	AttributeType type = attribute.getType();
        if (type.equals(AttributeType.CATEGORY))
        {
            CategoryImpl rootCategory = (CategoryImpl) attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
            if (isIdOnly()) {
                print(getId((RefEntity<?>)value));
            } else {
                final String pathForCategory = rootCategory.getPathForCategory((Category)value, false );
                if ( pathForCategory != null)
                {
                    print( pathForCategory);
                }
            }
        }
        else if (type.equals(AttributeType.DATE) )
        {
            final Date date;
            if  ( value instanceof Date) 
                date = (Date)value;
            else
                date = null;
            printEncode( dateTimeFormat.formatDate( date ) );
        }
        else
        {
            printEncode( value.toString() );
        }
    }


    protected void printOwner(Ownable obj) throws IOException,RaplaException {
        User user = obj.getOwner();
        if (user == null)
            return;
        att("owner", getId(user));
    }


    protected void printReference(RaplaObject entity) throws IOException {
        String localName = entity.getRaplaType().getLocalName();
        openTag("rapla:" + localName);
        if ( entity.getRaplaType().equals( DynamicType.TYPE) && !isIdOnly()) {
            att("keyref", ((DynamicType)entity).getElementKey());
        } else {
            att("idref",getId( entity));
        }
        closeElementTag();
    }


    protected String getId(RaplaObject entity) {
        SimpleIdentifier id = (SimpleIdentifier)((RefEntity<?>) entity).getId();
        return entity.getRaplaType().getLocalName() + "_" + id.getKey();
    }

    protected long getVersion(RaplaObject entity) {
        long version = ((RefEntity<?>) entity).getVersion();
        return version;
    }

    protected RaplaXMLWriter getWriterFor(RaplaType raplaType) throws RaplaException {
        RaplaXMLWriter writer = (RaplaXMLWriter)writerMap.get(raplaType);
        if ( writer == null) {
            throw new RaplaException("No writer for type " + raplaType);
        }
        writer.setIndentLevel( getIndentLevel());
        writer.setWriter( getWriter());
        return writer;
     }

    protected void printId(RaplaObject entity) throws IOException {
        att("id", getId( entity ));
    }

    protected void printVersion(RaplaObject entity) throws IOException {
        if ( printVersion)
        {
            att("version", String.valueOf(getVersion( entity )));
        }
    }

    protected void printIdRef(RaplaObject entity) throws IOException {
        att("idref", getId( entity ) );
    }

    /** Returns if the ids should be saved, even when keys are
     * available. */
    public boolean isIdOnly() {
        return isIdOnly;
    }

    public void writeObject(RaplaObject object) throws IOException, RaplaException {
        throw new RaplaException("Method not implemented by subclass " + this.getClass().getName());
    }

    public String getLocalNameForType(RaplaType raplaType) throws RaplaException{
        for (Iterator<Map.Entry<String,RaplaType>> it = localnameMap.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,RaplaType> entry =it.next();
            if (entry.getValue().equals( raplaType)) {
                return entry.getKey();
            }
        }
        throw new RaplaException("No writer declared for Type " + raplaType );
    }




}



