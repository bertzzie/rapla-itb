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

import java.util.*;
import java.io.*;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.XMLFilter;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.logger.LogEnabled;

import org.rapla.components.util.Assert;
import org.rapla.components.util.xml.XMLReaderAdapter;
import org.rapla.components.util.xml.XMLTransformerAdapter;
import org.rapla.framework.RaplaException;

class RaplaSAXPipeline implements LogEnabled {
    Vector<XMLFilter> filters = new Vector<XMLFilter>();
    String factoryName;
    XMLFilter mainFilter;
    RaplaErrorHandler errorHandler;
    String xmlParser;

    SAXTransformerFactory stf;
    XMLReader stylesheetReader;

    Logger logger = null;

    RaplaSAXPipeline() {
        mainFilter = new XMLFilterImpl();
        errorHandler = new RaplaErrorHandler();
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
        errorHandler.enableLogging(logger);
    }

    private Transformer createTransformer(InputSource in) throws RaplaException,SAXException {
        SAXTransformerFactory stf = XMLTransformerAdapter.getTransformerFactory();
        try {
            if (stylesheetReader == null) {
                stylesheetReader = XMLReaderAdapter.createXMLReader(false);
            }
            return stf.newTransformer(new SAXSource(stylesheetReader,in));
        } catch (TransformerConfigurationException ex) {
            throw new RaplaException(ex);
        }
    }

    public void addTransformer(URL file)
        throws RaplaException
               ,IOException
               ,SAXException
    {
        addTransformer(file,new String[0][2]);
    }

    public void addTransformer(URL file,String[][] parameter)
        throws RaplaException
               ,IOException
               ,SAXException
    {
        if (logger != null && logger.isInfoEnabled())
            logger.info("Creating new transformer with stylesheet '" + file + "'");
        Transformer transformer = createTransformer(new InputSource(file.toString()));
        for (int i=0;i<parameter.length;i++) {
            transformer.setParameter(parameter[i][0],parameter[i][1]);
        }
        XMLFilter f = new TransformerFilter(transformer);
        filters.add(f);
        if (logger != null && logger.isDebugEnabled())
            logger.debug("adding transformer '" + file + "'");
    }

    public void parse(ContentHandler handler,InputSource source)
        throws RaplaException
               ,IOException
               ,SAXException
    {
        XMLReader reader = XMLReaderAdapter.createXMLReader(false);
        Iterator<XMLFilter> it = filters.iterator();

        // filter1 will use the SAX parser as it's reader.
        XMLReader lastFilter = reader;
        while (it.hasNext()) {
            XMLFilter filter = (XMLFilter)it.next();
            filter.setParent(lastFilter);
            lastFilter = filter;
        }
        

        mainFilter.setParent(lastFilter);
        mainFilter.setContentHandler(handler);
        mainFilter.setErrorHandler(errorHandler);

        // Now, when you call the MainFilter to parse, it will set
        // itself as the ContentHandler for the previous filter, and
        // call the parse method on this filter, which will set itself as the
        // content handler for its previos filter, ...
        // The first filter will set itself as the content listener for the
        // SAX parser, and call parser.parse(new InputSource(foo_xml)).
        mainFilter.parse(source);
    }
    
}


class TransformerFilter extends XMLFilterImpl {
    Transformer transformer;
    
    public TransformerFilter(Transformer transformer) {
        this.transformer = transformer;
    }

    public void parse (InputSource input) throws IOException, SAXException {
        XMLReader parser = getParent();
        Assert.notNull(parser,"Must call setParent first");
        SAXSource source = new SAXSource();
        source.setInputSource(input);
        source.setXMLReader(parser);
        SAXResult result = new SAXResult();
        result.setHandler(getContentHandler());
        try {
            transformer.transform(source, result);
        } catch (TransformerException err) {
            Throwable cause = err.getException();
            if (cause != null && cause instanceof SAXException) {
                throw (SAXException)cause;
            } else if (cause != null && cause instanceof IOException) {
                throw (IOException)cause;
            } else {
                throw new SAXException(err);
            }
        }
    }
}

/*
class CopyXMLFilter extends XMLFilterImpl {
    XMLFilterImpl copy;
    String ident = "";
    
    CopyXMLFilter() {
        copy = new XMLFilterImpl();
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
        System.out.println();
        System.out.print( ident + "<" + qName );
        for (int i=0; i< atts.getLength(); i++)
            System.out.print( " " + atts.getURI(i)+ ":"+ atts.getLocalName(i) + "=\"" + atts.getValue(i) + "\"");
        System.out.println(">");
        ident = ident + "  ";
        super.startElement(uri,localName,qName,atts);
        copy.startElement(uri,localName,qName,atts);
    }

    public void endElement(String uri,String localName,String qName) throws SAXException {
        ident = ident.substring(1);
        System.out.print( ident + "</" + qName + ">" );
        super.endElement(uri,localName,qName);
        copy.endElement(uri,localName,qName);
    }

    public void startDocument() throws SAXException {
        System.out.println("Start document");
        super.startDocument();
        copy.startDocument();
    }
    public void endDocument() throws SAXException {
        super.endDocument();
        copy.endDocument();
    }
    public void startPrefixMapping(String prefix,String uri) throws SAXException {
        super.startPrefixMapping(prefix,uri);
        copy. startPrefixMapping(prefix,uri);
    }
    public void endPrefixMapping(String prefix) throws SAXException {
        super.endPrefixMapping(prefix);
        copy.endPrefixMapping(prefix);
    }
    public void characters(char[] ch,int start,int length) throws SAXException {
        super.characters(ch,start,length);
        copy.characters(ch,start,length);
    }
    public void ignorableWhitespace(char[] ch,int start,int length) throws SAXException {
        super.ignorableWhitespace(ch,start,length);
        copy.ignorableWhitespace(ch,start,length);
    }
    public void processingInstruction(String target,String data) throws SAXException {
        super.processingInstruction(target,data);
        copy.processingInstruction(target,data);
    }
    public void skippedEntity(String name) throws SAXException {
        super.skippedEntity(name);
        copy.skippedEntity(name);
    }
    public void setDocumentLocator(Locator locator) {
        super.setDocumentLocator(locator);
        copy.setDocumentLocator(locator);
    }

    public void setErrorHandler(ErrorHandler handler) {
        super.setErrorHandler(handler);
        copy.setErrorHandler(handler);
    }

    public XMLFilter getCopy() {
        return copy;
    }
}
*/
class RaplaErrorHandler implements ErrorHandler,LogEnabled {
    Logger logger = null;
    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void warning(SAXParseException exception) throws SAXException {
        if (logger != null)
            logger.error("Warning: " + getString(exception));
    }

     public String getString(SAXParseException exception)  {
        //       return "Line " + exception.getLineNumber()
        //      +    "\t Col  " + exception.getColumnNumber()
        //      +    "\t " +
        return exception.getMessage();
    }
}
