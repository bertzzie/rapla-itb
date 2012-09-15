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

package org.rapla.components.xmlbundle.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.Assert;
import org.rapla.components.util.IOUtil;
import org.rapla.components.util.xml.XMLReaderAdapter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** This class reads *Resources.xml files and generates
 the appropriate ResourceBundle java-files.
 <pre>
 Usage :
 org.rapla.components.xmlbundle.TranslationParser PATH_TO_SOURCES [DESTINATION_PATH]

 Note: a xml-parser must be on your classpath.

 Example usage under windows:
 java -classpath lib\saxon.jar;lib\fortress.jar;build\classes org.rapla.components.xmlbundle.TranslationParser src
 </pre>

 */
public class TranslationParser extends DefaultHandler
{
    RaplaDictionary dict;
    DictionaryEntry currentEntry = null;
    String currentLang = null;
    String defaultLang = null;
    String currentIconSrc = null;

    static Logger log = new ConsoleLogger( ConsoleLogger.LEVEL_INFO );
    int level = 0;

    // used to store the nested content in the translation element
    StringBuffer charBuffer;
    XMLReader xmlReader;
    /** The translation parser will add an extra identifer {$i18nbundle_parent$} to
     the translation table if a parentbundle is specified.
     */
    public final static String PARENT_BUNDLE_IDENTIFIER = "{$i18nbundle_parent$}";
    DefaultHandler handler = new DefaultHandler()
    {
        public InputSource resolveEntity( String publicId, String systemId ) throws SAXException
        {
            if ( systemId.endsWith( "resources.dtd" ) )
            {
                try
                {
                    URL resource = getClass().getResource( "/org/rapla/components/xmlbundle/resources.dtd" );
                    Assert.notNull( resource, "resources.dtd not found on classpath" );
                    return new InputSource( IOUtil.getInputStream( resource ) );
                }
                catch ( IOException ex )
                {
                    throw new SAXException( ex );
                }
            }
            else
            {
                // use the default behaviour
                try
                {
                    return super.resolveEntity( publicId, systemId );
                }
                catch ( SAXException ex )
                {
                    throw ex;
                }
                catch ( Exception ex )
                {
                    throw new SAXException( ex );
                }
            }
        }

        public void startElement( String uri, String name, String qName, Attributes atts ) throws SAXException
        {
            if ( log.isDebugEnabled() )
                log.debug( indent() + "Start element: " + qName + "(" + name + ")" );

            level:
            {
                if ( name.equals( "resources" ) )
                {
                    String defaultLang = atts.getValue( "", "default" );
                    String parentDict = atts.getValue( "", "parent" );
                    dict = new RaplaDictionary( defaultLang );
                    if ( parentDict != null && parentDict.trim().length() > 0 )
                    {
                        DictionaryEntry entry = new DictionaryEntry( PARENT_BUNDLE_IDENTIFIER );
                        entry.add( "en", parentDict.trim() );
                        try
                        {
                            dict.addEntry( entry );
                        }
                        catch ( UniqueKeyException ex )
                        {
                            //first entry must be unique
                        }
                    }
                    break level;
                }

                if ( name.equals( "entry" ) )
                {
                    String key = atts.getValue( "", "key" );
                    currentEntry = new DictionaryEntry( key );
                    break level;
                }

                if ( name.equals( "text" ) )
                {
                    currentLang = atts.getValue( "", "lang" );
                    if ( currentLang == null )
                        currentLang = dict.getDefaultLang();;
                    charBuffer = new StringBuffer();
                    break level;
                }

                if ( name.equals( "icon" ) )
                {
                    currentLang = atts.getValue( "", "lang" );
                    if ( currentLang == null )
                        currentLang = dict.getDefaultLang();
                    currentIconSrc = atts.getValue( "", "src" );
                    charBuffer = new StringBuffer();
                    break level;
                }

                // copy startag
                if ( charBuffer != null )
                {
                    copyStartTag( name, atts );
                }
            }
            level++;
        }

        public void endElement( String uri, String name, String qName ) throws SAXException
        {
            level--;
            if ( log.isDebugEnabled() )
                log.debug( indent() + "End element: " + qName + "(" + name + ")" );

            level:
            {
                if ( name.equals( "icon" ) )
                {
                    if ( currentIconSrc != null )
                        currentEntry.add( currentLang, currentIconSrc );
                    break level;
                }

                if ( name.equals( "text" ) )
                {
                    removeWhiteSpaces( charBuffer );
                    currentEntry.add( currentLang, charBuffer.toString() );
                    break level;
                }

                if ( name.equals( "entry" ) )
                {
                    try
                    {
                        dict.addEntry( currentEntry );
                    }
                    catch ( UniqueKeyException e )
                    {
                        throw new SAXException( e.getMessage() );
                    } // end of try-catch
                    currentEntry = null;
                    break level;
                }

                // copy endtag
                if ( charBuffer != null )
                {
                    copyEndTag( name );
                } // end of if ()
            }
        }

        public void characters( char ch[], int start, int length )
        {
            // copy nested content
            if ( charBuffer != null )
            {
                charBuffer.append( ch, start, length );
            } // end of if ()
        }
    };

    TranslationParser() throws ConfigurationException
    {
        super();
        try
        {
            xmlReader = XMLReaderAdapter.createXMLReader( false );
            xmlReader.setContentHandler( handler );
            xmlReader.setErrorHandler( handler );
            xmlReader.setDTDHandler( handler );
            xmlReader.setEntityResolver( handler );
        }
        catch ( SAXException ex )
        {
            if ( ex.getException() != null )
            {
                throw new ConfigurationException( "", ex.getException() );
            }
            else
            {
                throw new ConfigurationException( "", ex );
            } // end of else
        }
    }

    RaplaDictionary parse( InputStream in ) throws IOException, SAXException
    {
        dict = null;
        xmlReader.parse( new InputSource( in ) );
        checkDict();
        return dict;
    }

    RaplaDictionary parse( String systemID ) throws IOException, SAXException
    {
        dict = null;
        xmlReader.parse( systemID );
        checkDict();
        return dict;
    }

    private void checkDict() throws IOException
    {
        if ( dict == null )
        {
            throw new IOException( "Dictionary file empty " );
        }
    }

    private void copyStartTag( String name, Attributes atts )
    {
        charBuffer.append( '<' );
        charBuffer.append( name );
        for ( int i = 0; i < atts.getLength(); i++ )
        {
            charBuffer.append( ' ' );
            charBuffer.append( atts.getLocalName( i ) );
            charBuffer.append( '=' );
            charBuffer.append( '\"' );
            charBuffer.append( atts.getValue( i ) );
            charBuffer.append( '\"' );
        }
        charBuffer.append( '>' );
    }

    private void copyEndTag( String name )
    {
        if ( ( charBuffer != null )
                && ( charBuffer.length() > 0 )
                && ( charBuffer.charAt( charBuffer.length() - 1 ) == '>' ) )
        {
            // <some-tag></some-tag> --> <some-tag/>
            charBuffer.insert( charBuffer.length() - 1, "/" );
        }
        else
        {
            // </some-tag>
            charBuffer.append( "</" + name + ">" );
        } // end of else

    }

    private void removeWhiteSpaces( StringBuffer buf )
    {
        for ( int i = 1; i < buf.length(); i++ )
        {
            if ( ( buf.charAt( i ) == ' ' ) && ( buf.charAt( i - 1 ) == ' ' ) )
                buf.deleteCharAt( --i );
        } // end of for ()
    }

    private String indent()
    {
        StringBuffer buffer = new StringBuffer();
        for ( int i = 0; i <= level; i++ )
            buffer.append( ' ' );
        return buffer.toString();
    }

    public static final String USAGE = new String( "Usage : \n"
            + "PATH_TO_SOURCES [DESTINATION_PATH]\n"
            + "Note: a xml-parser must be on your classpath.\n"
            + "Example usage under windows:\n"
            + "java -classpath lib\\saxon.jar;lib\\fortress.jar;build\\classes "
            + "org.rapla.components.xmlbundle.TranslationParser "
            + "src \n" );

    public static void processDir( String srcDir, String destDir ) throws IOException, SAXException,
            ConfigurationException
    {
        TranslationParser parser = new TranslationParser();
        ResourceFileGenerator generator = new ResourceFileGenerator();
        Set<String> languages = new HashSet<String>();
        Stack<File> stack = new Stack<File>();
        File topDir = new File( srcDir );
        stack.push( topDir );
        while ( !stack.empty() )
        {
            File file = (File) stack.pop();
            if ( file.isDirectory() )
            {
                //              System.out.println("Checking Dir: " + file.getName());
                File[] files = file.listFiles();
                for ( int i = 0; i < files.length; i++ )
                    stack.push( files[i] );
            }
            else
            {
                //              System.out.println("Checking File: " + file.getName());
                if ( file.getName().endsWith( "Resources.xml" ) )
                {
                    String absolut = file.getAbsolutePath();
                    System.out.println( "Transforming source:" + file );
                    String relativePath = absolut.substring( topDir.getAbsolutePath().length() );
                    String prefix = file.getName().substring( 0, file.getName().length() - "Resources.xml".length() );
                    String pathName = relativePath.substring( 0, relativePath.indexOf( file.getName() ) );
                    RaplaDictionary dict = parser.parse( file.toURI().toURL().toExternalForm() );

                    File dir = new File( destDir, pathName );
                    System.out.println( "destination:" + dir );
                    dir.mkdirs();

                    String packageName = ResourceFileGenerator.toPackageName( pathName );
                    generator.transform( dict, packageName, prefix + "Resources", "en", dir );
                    String[] langs = dict.getAvailableLanguages();
                    for ( int i = 0; i < langs.length; i++ )
                        languages.add( langs[i] );
                }
            }
        }
    }

    public static void main( String[] args )
    {
        try
        {
            if ( args.length < 1 )
            {
                System.out.println( USAGE );
                return;
            } // end of if ()

            String sourceDir = args[0];
            String destDir = ( args.length > 1 ) ? args[1] : sourceDir;
            processDir( sourceDir, destDir );

        }
        catch ( SAXParseException ex )
        {
            log.error( "Line:" + ex.getLineNumber() + " Column:" + ex.getColumnNumber() + " " + ex.getMessage(), ex );
            System.exit( 1 );
        }
        catch ( Throwable e )
        {
            log.error( e.getMessage(), e );
            System.exit( 1 );
        }
    } // end of main ()

}
