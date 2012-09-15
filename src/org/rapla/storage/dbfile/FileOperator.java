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
package org.rapla.storage.dbfile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.util.IOUtil;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.storage.IOContext;
import org.rapla.storage.LocalCache;
import org.rapla.storage.UpdateEvent;
import org.rapla.storage.UpdateResult;
import org.rapla.storage.impl.AbstractCachableOperator;
import org.rapla.storage.impl.EntityStore;
import org.rapla.storage.xml.RaplaInput;
import org.rapla.storage.xml.RaplaMainReader;
import org.rapla.storage.xml.RaplaMainWriter;

/** Use this Operator to keep the data stored in an XML-File.
 * <p>Sample configuration:
 <pre>
 &lt;file-storage id="file">
 &lt;file>data.xml&lt;/file>
 &lt;encoding>utf-8&lt;/encoding>
 &lt;validate>no&lt;/validate>
 &lt;/facade>
 </pre>
 * <ul>
 *   <li>The file entry contains the path of the data file.
 *   If the path is not an absolute path it will be resolved
 *   relative to the location of the configuration file
 *   </li>
 *   <li>The encoding entry specifies the encoding of the xml-file.
 *   Currently only UTF-8 is tested.
 *   </li>
 *   <li>The validate entry specifies if the xml-file should be checked
 *    against a schema-file that is located under org/rapla/storage/xml/rapla.rng
 *    (Default is no)
 *   </li>
 * </ul>
 * </p>
 * <p>Note: The xmloperator doesn't check passwords.</p>

 @see AbstractCachableOperator
 @see org.rapla.storage.StorageOperator
 */
final public class FileOperator extends AbstractCachableOperator
{
 
	private File storageFile;
    private URL loadingURL;

    private final String encoding;
    protected boolean isConnected = false;
    final boolean includeIds ;

    private final boolean validate;

    public FileOperator( RaplaContext context, Configuration config ) throws RaplaException
    {
        super( context );
        StartupEnvironment env = (StartupEnvironment) context.lookup( StartupEnvironment.class.getName() );

        URL contextRootURL = env.getContextRootURL();

        String fileName = config.getChild( "file" ).getValue( "data.xml" );
        try
        {
            File file = new File( fileName );
            if ( file.isAbsolute() )
            {
                storageFile = file;
                loadingURL = storageFile.getCanonicalFile().toURI().toURL();
            }
            else
            {
                int startupEnv = env.getStartupMode();
                if ( startupEnv == StartupEnvironment.WEBSTART || startupEnv == StartupEnvironment.APPLET )
                {
                    loadingURL = new URL( contextRootURL, fileName );
                }
                else
                {
                    File contextRootFile = IOUtil.getFileFrom( contextRootURL );
                    storageFile = new File( contextRootFile, fileName );
                    loadingURL = storageFile.getCanonicalFile().toURI().toURL();
                }
            }
            getLogger().info("Data:" + loadingURL);
        }
        catch ( MalformedURLException ex )
        {
            throw new RaplaException( fileName + " is not an valid path " );
        }
        catch ( IOException ex )
        {
            throw new RaplaException( "Can't read " + storageFile + " " + ex.getMessage() );
        }
        encoding = config.getChild( "encoding" ).getValue( "utf-8" );
        validate = config.getChild( "validate" ).getValueAsBoolean( false );
        includeIds = config.getChild( "includeIds" ).getValueAsBoolean( false );
    }

    public boolean supportsActiveMonitoring()
    {
        return false;
    }

    /** just calls connect. Username and password will be ignored.*/
    final public void connect( String username, char[] password ) throws RaplaException
    {
        connect();
    }

    /** Sets the isConnected-flag and calls loadData.*/
    final public void connect() throws RaplaException
    {
        if ( isConnected )
            return;
        loadData();
        isConnected = true;
    }

    final public boolean isConnected()
    {
        return isConnected;
    }

    /** this implementation does nothing. Once connected isConnected will always return true.*/
    final public void disconnect() throws RaplaException
    {
        isConnected = false;
        fireStorageDisconnected();
    }

    final public void refresh() throws RaplaException
    {
        getLogger().warn( "Incremental refreshs are not supported" );
    }

    final public void refreshFull() throws RaplaException
    {
        try
        {
            loadData();
            isConnected = true;
        }
        catch ( Exception ex )
        {
            cache.clearAll();
            isConnected = false;
            if ( ex instanceof RaplaException )
                throw (RaplaException) ex;
            else
                throw new RaplaException( ex );
        }
    }

    final protected void loadData() throws RaplaException
    {
        try
        {
            cache.clearAll();
            idTable.setCache( cache );
            if ( getLogger().isDebugEnabled() )
                getLogger().debug( "Reading data from file:" + loadingURL );

            RaplaInput xmlAdapter = new RaplaInput( getLogger().getChildLogger( "reading" ) );

            EntityStore entityStore = new EntityStore( null, cache.getSuperCategory() );
            RaplaContext context = new IOContext().createInputContext( serviceManager, entityStore, idTable );
            RaplaMainReader contentHandler = new RaplaMainReader( context );
            xmlAdapter.read( loadingURL, contentHandler, validate );
            resolveEntities( entityStore.getList().iterator(), entityStore );
            cache.putAll( entityStore.getList() );
            cache.getSuperCategory().setReadOnly(true);
            for ( Iterator<?> it = cache.getIterator( User.TYPE ); it.hasNext(); )
            {
                RefEntity<?> user = ( (RefEntity<?>) it.next() );
                String password = entityStore.getPassword( user.getId() );
                //System.out.println("Storing password in cache" + password);
                cache.putPassword( user.getId(), password );
            }
            // contextualize all Entities
            if ( getLogger().isDebugEnabled() )
                getLogger().debug( "Entities contextualized" );

            // save the converted file;
            if ( xmlAdapter.wasConverted() )
            {
                getLogger().info( "Storing the converted file" );
                saveData(cache);
            }
        }
        catch ( IOException ex )
        {
        	getLogger().info( "Loading error: " + loadingURL);
            throw new RaplaException( "Can't load file at " + loadingURL + ": " + ex.getMessage() );
        }
        catch ( RaplaException ex )
        {
            throw ex;
        }
        catch ( Exception ex )
        {
            throw new RaplaException( ex );
        }
    }

    
    public void dispatch( UpdateEvent evt ) throws RaplaException
    {
        evt = createClosure( evt );
        check( evt );
        // call of update must be first to update the cache.
        // then saveData() saves all the data in the cache
        UpdateResult result = update( evt, true );
        saveData(cache);
        fireStorageUpdated( result );
    }

    final public Object createIdentifier( RaplaType raplaType ) throws RaplaException
    {
        return idTable.createId( raplaType );
    }

    
    final public void setCache( LocalCache cache ) throws RaplaException
    {
        super.setCache( cache );
        idTable.setCache( cache );
    }

    private boolean bWarningDisplayed = false;

    private void showReadOnlyWarning() throws RaplaException
    {
        if ( bWarningDisplayed )
            return;
        javax.swing.JOptionPane.showMessageDialog( null, getI18n().getString( "warning.readonly_storage" ),
                                                   getI18n().getString( "warning" ),
                                                   javax.swing.JOptionPane.WARNING_MESSAGE );
        bWarningDisplayed = true;
    }

    final public void saveData(LocalCache cache) throws RaplaException
    {
        try
        {
            if ( storageFile == null )
            {
                showReadOnlyWarning();
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            writeData( buffer,cache );
            byte[] data = buffer.toByteArray();
            buffer.close();

            moveFile( storageFile, storageFile.getPath() + ".bak" );
            OutputStream out = new FileOutputStream( storageFile );
            out.write( data );
            out.close();
        }
        catch ( IOException e )
        {
            throw new RaplaException( e.getMessage() );
        }
    }

    private void writeData( OutputStream out, LocalCache cache ) throws IOException, RaplaException
    {
        final boolean includeVersions = false;
        RaplaContext outputContext = new IOContext().createOutputContext( serviceManager, cache, includeIds, includeVersions );
        RaplaMainWriter writer = new RaplaMainWriter( outputContext );
        writer.setEncoding( encoding );
        writer.write( out );
    }

    private void moveFile( File file, String newPath ) throws IOException
    {
        File backupFile = new File( newPath );
        backupFile.delete();
        file.renameTo( backupFile );
    }

}
