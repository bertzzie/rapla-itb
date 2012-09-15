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
package org.rapla.storage.dbsql;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaDefaultContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.internal.ConfigTools;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.IOContext;
import org.rapla.storage.LocalCache;
import org.rapla.storage.UpdateEvent;
import org.rapla.storage.UpdateResult;
import org.rapla.storage.impl.AbstractCachableOperator;
import org.rapla.storage.impl.EntityStore;
import org.rapla.storage.xml.RaplaInput;
import org.rapla.storage.xml.RaplaMainWriter;

/** This Operator is used to store the data in a SQL-DBMS.*/
public class DBOperator extends AbstractCachableOperator
implements
Disposable
{
    private String driverClassname;
    protected String datasourceName;
    protected String user;
    protected String password;
    protected String dbURL;
    protected Driver dbDriver;
    protected boolean isConnected;
    Properties dbProperties = new Properties();
    boolean bSupportsTransactions = false;
    boolean hsqldb = false;
    private boolean oldResourceTableName;
    private boolean newResourceSchema =false;

    private String backupEncoding;
    private String backupFileName;

    
    public DBOperator(RaplaContext context, Configuration config) throws RaplaException,ConfigurationException {
        super( context );


        String backupFile = config.getChild("backup").getValue("");
        if (backupFile != null)
        	backupFileName = ConfigTools.resolveContext( backupFile, context);
       
        backupEncoding = config.getChild( "encoding" ).getValue( "utf-8" );
 
        datasourceName = config.getChild("datasource").getValue(null);
        // dont use datasource (we have to configure a driver )
        if ( datasourceName == null)
        {
	        try 
	        {
	            driverClassname = config.getChild("driver").getValue();
	            dbURL = ConfigTools.resolveContext( config.getChild("url").getValue(), serviceManager);
                getLogger().info("Data:" + dbURL);
	        } 
	        catch (ConfigurationException e) 
	        {
	            throw new RaplaException( e );
	        }
	        dbProperties.setProperty("user", config.getChild("user").getValue("") );
	        dbProperties.setProperty("password", config.getChild("password").getValue("") );
	        hsqldb = config.getChild("hsqldb-shutdown").getValueAsBoolean( false );
	        try 
	        {
	            dbDriver = (Driver) getClass().getClassLoader().loadClass(driverClassname).newInstance();
	        } 
	        catch (ClassNotFoundException e) 
	        {
	            throw new RaplaException("DB-Driver not found: " + driverClassname +
	                                          "\nCheck classpath!");
	        } 
	        catch (Exception e) 
	        {
	            throw new RaplaException("Could not instantiate DB-Driver: " + driverClassname, e);
	        }
        }
    }

    public boolean supportsActiveMonitoring() {
        return false;
    }

    public Connection createConnection() throws RaplaException {
        try {
        	 Connection connection;
        	 //datasource lookup 
        	 if ( datasourceName != null)
        	 {
        		 Context cxt = new InitialContext();
        		 DataSource ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/" + datasourceName );
        		 if ( ds == null )
        		 {
        			 throw new RaplaDBException("Datasource not found"); 
        		 }
        		 connection = ds.getConnection();
        	 }
        	 // or driver initialization
        	 else
        	 {
        		 connection = dbDriver.connect(dbURL, dbProperties);
                 if (connection == null)
                 {
                     throw new RaplaDBException("No driver found for: " + dbURL + "\nCheck url!");
                 }
        	 }
             
             bSupportsTransactions = connection.getMetaData().supportsTransactions();
             if (bSupportsTransactions)
             {
                 connection.setAutoCommit( false );
             }
             else
             {
                 getLogger().warn("No Transaction support");
             }
             return connection;
        } catch (Throwable ex) {
             if ( ex instanceof RaplaDBException)
             {
                 throw (RaplaDBException) ex;
             }
             ex.printStackTrace();
             throw new RaplaDBException("DB-Connection aborted",ex);
        }
    }

    public void connect() throws RaplaException {
        if (isConnected())
        {
            return;
        }
        loadData();
        isConnected = true;
    }

    public void connect(String username,char[] password) throws RaplaException {
        connect();
    }

    public boolean isConnected() {
        return isConnected;
    }
    
    final public void refresh() throws RaplaException {
        getLogger().warn("Incremental refreshs are not supported");
    }

    public void refreshFull() throws RaplaException {
        try 
        {
            loadData();
        } 
        catch (Exception ex) 
        {
            cache.clearAll();
            disconnect();
            if (ex instanceof RaplaException)
            {
                throw (RaplaException)ex;
            }
            else
            {
                throw new RaplaException(ex);
            }
        }
    }

    public void forceDisconnect() {
        try 
        {
            disconnect();
        } 
        catch (Exception ex) 
        {
            getLogger().error("Error during disconnect ", ex);
        }
    }

    public void disconnect() throws RaplaException 
    {

    	if (!isConnected())
    		return;
        backupData();

        cache.clearAll();
        idTable.setCache( cache );
        // HSQLDB Special
        if ( hsqldb ) 
        {
            String sql  ="SHUTDOWN COMPACT";
            try 
            {
                Connection connection = createConnection();
                Statement statement = connection.createStatement();
                statement.executeQuery(sql);
            } 
            catch (SQLException ex) 
            {
                 throw new RaplaException( ex);
            }
        }
        isConnected = false;
        fireStorageDisconnected();
    }

    public void dispose() 
    {
        forceDisconnect();
    }

    final protected void loadData() throws RaplaException {
        Connection connection = createConnection();
        try {
        	// Upgrade db if neccessary
        	{
                ResultSet categoryTable = connection.prepareStatement("select * from CATEGORY" ).executeQuery();
                if ( categoryTable.getMetaData().getColumnCount() == 4) 
                {
                    getLogger().warn("Patching Database for table CATEGORY");
	            	try {
	            		connection.prepareStatement("ALTER TABLE CATEGORY ADD COLUMN DEFINITION TEXT").execute();
	            		connection.commit();
	            	} catch (SQLException ex ) {
                        getLogger().warn("Category patch failed. Trying HDBSQL Syntax");
	            		connection.prepareStatement("ALTER TABLE CATEGORY ADD COLUMN DEFINITION VARCHAR").execute();
	            		connection.commit();
	            	}
                    getLogger().warn("CATEGORY patched!");
	            }
                if ( categoryTable.getMetaData().getColumnCount() == 5) 
                {
                    getLogger().warn("Patching Database for table CATEGORY (Category Order)");
                    connection.prepareStatement("ALTER TABLE CATEGORY ADD COLUMN PARENT_ORDER INTEGER").execute();
                    getLogger().warn("CATEGORY patched!");
                }
	            ResultSet eventTable = connection.prepareStatement("select * from EVENT" ).executeQuery();
	            if ( eventTable.getMetaData().getColumnCount()  == 5) {
	                getLogger().warn("Patching Database for table EVENT");
	            	connection.prepareStatement("ALTER TABLE EVENT ADD COLUMN LAST_CHANGED_BY INTEGER").execute();
	            	connection.commit();
                    getLogger().warn("EVENT patched");
	            }
                checkForOldResourceTable( connection );
                
        	}
        	ResultSet set = connection.prepareStatement("select * from DYNAMIC_TYPE").executeQuery();
  	        if ( !set.next() ) {
                getLogger().warn("No content in database! Creating new database");
                CachableStorageOperator sourceOperator = ( CachableStorageOperator) serviceManager.lookup(CachableStorageOperator.ROLE + "/file");
                sourceOperator.connect();
                saveData(sourceOperator.getCache());
                getLogger().warn("Database created!");
            } 
	         cache.clearAll();
	         idTable.setCache(cache);
	         readEverythingIntoCache( connection );
	         idTable.setCache(cache);
	         
	         if ( getLogger().isDebugEnabled())
	             getLogger().debug("Entities contextualized");
	
	         if ( getLogger().isDebugEnabled())
	             getLogger().debug("All ConfigurationReferences resolved");
        } 
        catch (RaplaException ex) 
        {
            throw ex;
        } 
        catch (Exception ex) 
        {
            throw new RaplaException( ex);
        } 
        finally 
        {
            close ( connection );
        }
    }

    private void checkForOldResourceTable( Connection connection ) throws SQLException
    {
        try 
        {
            ResultSet oldResourceTable = connection.getMetaData().getTables(null, null,"RESOURCE" , null);
            while ( oldResourceTable.next())
            {
                oldResourceTableName = true;
            }
            // If there is also a new Table use the new Table
            ResultSet newResourceTable = connection.getMetaData().getTables(null, null,"rapla_resource" , null);
            while ( newResourceTable.next())
            {
                oldResourceTableName = false;
            }
        } 
        catch (SQLException ex)
        {
            oldResourceTableName = false;
            getLogger().warn("Can't determine table schema for table, assuming new schema. Please upgrade database schema if neccessary.");
        }
        if ( oldResourceTableName )
        {
            getLogger().warn("Using old resource table name resource. Please rename to rapla_resource");
        }
        newResourceSchema = true;
        if ( !oldResourceTableName)
        {
            try 
            {
                ResultSet lastChangedColumn = connection.getMetaData().getColumns(null, null,"rapla_resource" , "is_person");
                while ( true)
                {
                    final boolean next = lastChangedColumn.next();
                    if(!next)
                    {
                        break;
                    }
                    newResourceSchema = false;
                }
            } 
            catch (SQLException ex)
            {
                getLogger().warn("Can't determine table schema for table, assuming new schema. Please upgrade database schema if neccessary.");
            }
            if ( !newResourceSchema )
            {
                getLogger().warn("Using old resource table schema without timestamp. Please upgrade to new schema.");
            }
        }
    }
    public Object createIdentifier(RaplaType raplaType) throws RaplaException {
        return idTable.createId(raplaType);
    }

    public void dispatch(UpdateEvent evt) throws RaplaException {
        evt = createClosure( evt  );
        check(evt);
        Connection connection = createConnection();
        try {
             executeEvent(connection,evt);
             if (bSupportsTransactions) {
                 getLogger().debug("Commiting");
                 connection.commit();
             }
         } catch (Exception ex) {
             try {
                 if (bSupportsTransactions) {
                     connection.rollback();
                     getLogger().error("Doing rollback");
                     throw new RaplaDBException(getI18n().getString("error.rollback"),ex);
                 } else {
                     String message = getI18n().getString("error.no_rollback");
                     getLogger().fatalError(message);
                     forceDisconnect();
                     throw new RaplaDBException(message,ex);
                 }
             } catch (SQLException sqlEx) {
                 String message = "Unrecoverable error while storing";
                 getLogger().fatalError(message, sqlEx);
                 forceDisconnect();
                 throw new RaplaDBException(message,sqlEx);
             }
        } finally {
            close( connection );
        }
        UpdateResult result = super.update(evt, true);
        fireStorageUpdated(result);
        
    }

    /**
    * @param evt
    * @throws RaplaException
    */
    protected void executeEvent(Connection connection,UpdateEvent evt) throws RaplaException, SQLException {
        // create the writer
        RaplaSQL raplaSQL =  new RaplaSQL(createOutputContext(cache), oldResourceTableName, newResourceSchema);
        // execute updates
        Iterator<RefEntity<?>> it = evt.getStoreObjects().iterator();
        while (it.hasNext()) {
            RefEntity<?> entity =  it.next();
            raplaSQL.store( connection, entity);
        }

        // execute removes
        it = evt.getRemoveObjects().iterator();
        while (it.hasNext()) {
             Object id = it.next().getId();
             RefEntity<?> entity = cache.get(id);
             if (entity != null)
                 raplaSQL.remove( connection, entity);
        }

    }

    public void removeAll() throws RaplaException {
        Connection connection = createConnection();
        try {
            checkForOldResourceTable( connection );
            RaplaSQL raplaSQL =  new RaplaSQL(createOutputContext(cache), oldResourceTableName, newResourceSchema);
             if (!isConnected())
                 createConnection();

             raplaSQL.removeAll( connection );
             connection.commit();
             // do something here
             getLogger().info("DB cleared");
        } 
        catch (SQLException ex) 
        {
            throw new RaplaException(ex);
        }
        finally
        {
            close( connection );
        }
    }
    
    public void saveData() throws RaplaException 
    {
    	saveData( cache);
    }
    
    public void saveData(LocalCache cache) throws RaplaException {
        Connection connection = createConnection();
        try {
            checkForOldResourceTable( connection );
            RaplaSQL raplaSQL =  new RaplaSQL(createOutputContext(cache), oldResourceTableName, newResourceSchema);
            getLogger().info("Creation of DB started");
             if (!isConnected())
                 createConnection();

             raplaSQL.removeAll( connection );
             raplaSQL.createAll( connection );
             connection.commit();
             // do something here
             getLogger().info("DB Creation complete");
        } 
        catch (SQLException ex) 
        {
            throw new RaplaException(ex);
        }
        finally
        {
            close( connection );
        }
    }

    static private void close(Connection connection) throws RaplaException 
    {
        try 
        {
            connection.close();
        } 
        catch (SQLException e) 
        {
            throw new RaplaException("Can't close connection to database ", e);
        }
    }

    protected boolean readEverythingIntoCache(Connection connection) throws RaplaException, IOException, SQLException {
        EntityStore entityStore = new EntityStore(null, cache.getSuperCategory());
        
	    RaplaSQL raplaSQL = new RaplaSQL(createInputContext(entityStore), oldResourceTableName, newResourceSchema);
	    raplaSQL.loadAll( connection );
        resolveEntities( entityStore.getList().iterator(), entityStore );
        cache.putAll( entityStore.getList());
        for (Iterator<RefEntity<?>> it = cache.getIterator(User.TYPE);it.hasNext();)
        {
            RefEntity<?> user = it.next();
            String password = entityStore.getPassword( user.getId());
            cache.putPassword(user.getId(), password);
        }
	    return false;
	}

	private RaplaDefaultContext createInputContext(  EntityStore store) throws RaplaException {
        RaplaDefaultContext inputContext =  new IOContext().createInputContext(serviceManager, store,idTable);
        RaplaInput xmlAdapter = new RaplaInput(getLogger().getChildLogger("reading"));
        inputContext.put(RaplaInput.class.getName(),xmlAdapter);
        return inputContext;
        
    }
    
    private RaplaDefaultContext createOutputContext(LocalCache cache) throws RaplaException {
        RaplaDefaultContext outputContext =  new IOContext().createOutputContext(serviceManager, cache,true,false);
        return outputContext;
        
    }

//implement backup at disconnect 
    final public void backupData() throws RaplaException { 
        try {

            if (backupFileName.length()==0)
            	return;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            writeData(buffer);
            byte[] data = buffer.toByteArray();
            buffer.close();
            OutputStream out = new FileOutputStream(backupFileName);
            out.write(data);
            out.close();
            getLogger().info("Backup data to: " + backupFileName);
        } catch (IOException e) {
            getLogger().error("Backup error: " + e.getMessage());
            throw new RaplaException(e.getMessage());
        }
    }
   

    private void writeData( OutputStream out ) throws IOException, RaplaException
    {
    	
        RaplaContext outputContext = new IOContext().createOutputContext( serviceManager, cache, true, true );
        RaplaMainWriter writer = new RaplaMainWriter( outputContext );
        writer.setEncoding(backupEncoding);
        writer.write( out );
    }
}
