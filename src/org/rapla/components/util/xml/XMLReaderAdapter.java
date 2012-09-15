/*---------------------------------------------------------------------------*
  | (C) 2006 Christopher Kohlhaas                                            |
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
package org.rapla.components.util.xml;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

final public class XMLReaderAdapter {
    /** Here you can set the xml-reader implementation that should be
        used.  The default implementation is the aelfred-parser from
        the saxon project: net.sf.saxon.aelfred.SAXDriver.
    */
    //public static String XML_READER_IMPL ="net.sf.saxon.aelfred.SAXDriver";
    public static String XML_READER_IMPL ="java.Saxon";

    private XMLReaderAdapter()
    {
    }
    
    private static ClassLoader getClassLoader() {
        return XMLReaderAdapter.class.getClassLoader();
    }

    public static void checkXMLSupport() throws ClassNotFoundException {
        try {
            getClassLoader().loadClass("javax.xml.parsers.SAXParserFactory");
        } catch (ClassNotFoundException ex) {
            throw new ClassNotFoundException
                ("Couldn't find SAX-XML-PARSER API: javax.xml.parsers"
                 + " You need java 1.4 or higher. For java-versions below 1.4 please download"
                 + " the saxon.jar from rapla.sourceforge.net"
                 + " and put it into the lib directory.");
        }
    }

    public static XMLReader createXMLReader(boolean validating) throws SAXException {
      try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(validating);
            return spf.newSAXParser().getXMLReader();
      
        } catch (Exception ex2) {
            throw new SAXException("Couldn't create XMLReader '"
                                   + XML_READER_IMPL +"' : " + ex2.getMessage());
        }
    }
}
