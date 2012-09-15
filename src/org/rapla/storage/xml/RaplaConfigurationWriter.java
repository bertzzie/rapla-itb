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

import java.io.IOException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;


public class RaplaConfigurationWriter extends RaplaXMLWriter {

    public RaplaConfigurationWriter(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    public void writeObject(RaplaObject type) throws IOException, RaplaException {
        RaplaConfiguration raplaConfig = (RaplaConfiguration) type ;
        openElement("rapla:" + RaplaConfiguration.TYPE.getLocalName());
        try {
            printConfiguration(raplaConfig.getConfig() );
        } catch (ConfigurationException ex) {
            throw new RaplaException( ex );
        }
        closeElement("rapla:" + RaplaConfiguration.TYPE.getLocalName());
    }

    private void printConfiguration(final Configuration conf ) throws ConfigurationException, RaplaException, IOException {
        printConfiguration(namespaceSupport,conf);
    }

    /**
     * Serialize each Configuration element.  This method is called recursively.
     * Original code for this method is taken from  the org.apache.framework.configuration.DefaultConfigurationSerializer class
     * @param namespaceSupport a <code>NamespaceSupport</code> to use
     * @throws ConfigurationException if an error occurs
     * @throws IOException if an error occurs
     */
    protected void printConfiguration( final NamespaceSupport namespaceSupport,
                                     final Configuration element )
        throws IOException, ConfigurationException, RaplaException
    {
        namespaceSupport.pushContext();

        AttributesImpl attr = new AttributesImpl();
        String[] attrNames = element.getAttributeNames();

        if( null != attrNames )
        {
            for( int i = 0; i < attrNames.length; i++ )
            {
                attr.addAttribute( "", // namespace URI
                                   attrNames[ i ], // local name
                                   attrNames[ i ], // qName
                                   "CDATA", // type
                                   element.getAttribute( attrNames[ i ], "" ) // value
                );
            }
        }

        final String nsURI = element.getNamespace();
        String nsPrefix = namespaceSupport.getPrefix(nsURI);
        if (nsPrefix == null)
            nsPrefix = "";


        final String existingURI = namespaceSupport.getURI( nsPrefix );

        // ie, there is no existing URI declared for this prefix or we're
        // remapping the prefix to a different URI
        if( existingURI == null || !existingURI.equals( nsURI ) )
        {
            if( nsPrefix.equals( "" )  ) {
            } else {
                // (re)declare a mapping from nsPrefix to nsURI
                attr.addAttribute( "", "xmlns:" + nsPrefix, "xmlns:" + nsPrefix, "CDATA", nsURI );
            }
            //handler.startPrefixMapping( nsPrefix, nsURI );
            namespaceSupport.declarePrefix( nsPrefix, nsURI );
        }

        String localName = element.getName();
        String qName = element.getName();
        if( nsPrefix == null || nsPrefix.length() == 0 )
        {
            qName = localName;
        }
        else
        {
            qName = nsPrefix + ":" + localName;
        }

        openTag(qName);
        att(attr);

        String value = element.getValue( null );

        if( null == value )
        {
            Configuration[] children = element.getChildren();
            if (children.length > 0)
            {
                closeTag();
                for( int i = 0; i < children.length; i++ )
                {
                    printConfiguration( namespaceSupport, children[ i ] );
                }
                closeElement(qName);
            }
            else
            {
                closeElementTag();
            }
        }
        else
        {
            closeTagOnLine();
            print(value);
            closeElementOnLine(qName);
            println();
        }


        namespaceSupport.popContext();
    }


 }



