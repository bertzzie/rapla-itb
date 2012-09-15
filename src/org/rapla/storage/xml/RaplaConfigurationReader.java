/*--------------------------------------------------------------------------*
  | Copyright (C) 2006 Christopher Kohlhaas                                  |
  |                                                                          |
  | This program is free software; you can redistribute it and/or modify     |
  | it under the terms of the GNU General Public License as published by the |
  | Free Software Foundation. A copy of the license has been included with   |
  | these distribution in the COPYING file, if not go to www.fsf.org .       |
  |                                                                          |
  | As a special exception, you are granted the permissions to link this     |
  | program with every library, which license fulfills the Open Source       |
  | Definition as published by the Open Source Initiative (OSI).             |
  *--------------------------------------------------------------------------*/

package org.rapla.storage.xml;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.NamespacedSAXConfigurationHandler;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class RaplaConfigurationReader extends RaplaXMLReader  {
    boolean delegating = false;
    
    public RaplaConfigurationReader(RaplaContext context) throws RaplaException {
        super(context);
    }
    
    NamespacedSAXConfigurationHandler configurationHandler = new NamespacedSAXConfigurationHandler() {
            boolean aelfredFailed;
            
            protected String getLocationString()
            {
                if ( aelfredFailed )
                    return "Unknown";
                try {
                    // Aelfred causes an error after xslt processing.
                    return super.getLocationString();
                } catch (Exception ex) {
                    aelfredFailed = true;
                    getLogger().warn("Can't get location string. Probably an aelfread sax error.");
                    return "Unknown";
                }
            }
        };

    public void clear() {
        configurationHandler.clear();
        // The locator will be reseted after a clear
        configurationHandler.setDocumentLocator(getLocator());
    }

    public void processElement(String namespaceURI,String localName,String qName,Attributes atts)
        throws SAXException
    {
        if ( RAPLA_NS.equals(namespaceURI) && localName.equals("config")) 
            return;
        delegating = true;
        configurationHandler.startElement(namespaceURI, localName, qName, atts);
    }

    public void processEnd(String namespaceURI,String localName,String qName)
        throws SAXException
    {
        if ( RAPLA_NS.equals(namespaceURI) && localName.equals("config")) 
            return;
        
        configurationHandler.endElement(namespaceURI, localName, qName);
        delegating = false;
        
    }

    public void startPrefixMapping(String prefix,
                                   String uri) throws SAXException
    {
       // configurationHandler.startPrefixMapping(prefix, uri);
    }

    public void setDocumentLocator(Locator locator) {
        configurationHandler.setDocumentLocator(locator);
        super.setDocumentLocator(locator);
    }

    public void processCharacters(char[] ch,int start,int length)
        throws SAXException 
     {

        if ( delegating ){
            configurationHandler.characters(ch,start,length);
        }
    }


    public RaplaObject getType() {
        return new RaplaConfiguration(getConfiguration());
    }
    
    private Configuration getConfiguration() {
        Configuration conf = configurationHandler.getConfiguration();
        return conf;
    }
}

