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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.storage.LocalCache;
import org.rapla.storage.impl.EntityStore;
import org.rapla.storage.xml.PreferenceReader;
import org.rapla.storage.xml.PreferenceWriter;
import org.rapla.storage.xml.RaplaInput;
import org.rapla.storage.xml.RaplaXMLReader;
import org.rapla.storage.xml.RaplaXMLWriter;

abstract class EntityStorage implements Storage {
    Random random = new Random();

    String insertSql;
    String updateSql;
    String deleteSql;
    String selectSql;
    String deleteAllSql;
    String searchForIdSql;

    RaplaContext sm;
    LocalCache cache;
    private EntityStore entityStore;
    private RaplaLocale raplaLocale;

    Collection<Storage> subStores = new ArrayList<Storage>();
    Connection con;
    int lastParameterIndex; /** first paramter is 1 */
    final String tableName;

    Logger logger;

    protected EntityStorage( RaplaContext context, String table,String[] entries) throws RaplaException {
        this.sm = context;
        if ( context.has( EntityStore.class.getName()))
        {
            this.entityStore = (EntityStore) context.lookup( EntityStore.class.getName()); 
        }
        if ( context.has( LocalCache.class.getName()))
        {
            this.cache = (LocalCache) context.lookup( LocalCache.class.getName()); 
        }
        this.raplaLocale = (RaplaLocale) sm.lookup(RaplaLocale.ROLE);
        logger = (Logger) context.lookup( Logger.class.getName());
        lastParameterIndex = entries.length;
        tableName = table;
    	createSQL(table,entries);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(insertSql);
            getLogger().debug(updateSql);
            getLogger().debug(deleteSql);
            getLogger().debug(selectSql);
            getLogger().debug(deleteAllSql);
        }
    }

    protected Logger getLogger() {
        return logger;
    }


    protected void createSQL(String table,String[] entries) {
        String idString = entries[0];
		selectSql = "select " + getEntryList(entries) + " from " + table ;
		deleteSql = "delete from " + table + " where " + idString + "= ?";
		insertSql = "insert into " + table + " (" + getEntryList(entries) + ") values (" + getMarkerList(entries.length) + ")";
		updateSql = "update " + table + " set " + getUpdateList(entries) + " where " + idString + "= ?";
		deleteAllSql = "delete from " + table;
		searchForIdSql = "select id from " + table + " where id = ?";
	}

    protected void addSubStorage(Storage subStore) {
    	subStores.add(subStore);
    }

    public void setConnection(Connection con) {
		this.con= con;
		Iterator<Storage> it = subStores.iterator();
		while (it.hasNext()) {
		    (it.next()).setConnection(con);
		}
    }

    public Locale getLocale() {
    	return raplaLocale.getLocale();
    }
    public java.sql.Date getSQLDate(Calendar cal) {
        return new java.sql.Date(cal.getTime().getTime());
    }

    public java.sql.Time getSQLTime(Calendar cal) {
        return new java.sql.Time(cal.getTime().getTime());
    }

    private String getEntryList(String[] entries) {
		StringBuffer buf = new StringBuffer();
		for (int i=0;i<entries.length; i++) {
		    buf.append(entries[i]);
		    if (i < entries.length - 1 )
			buf.append(", ");
		}
		return buf.toString();
    }
    private String getMarkerList(int length) {
		StringBuffer buf = new StringBuffer();
		for (int i=0;i<length; i++) {
		    buf.append('?');
		    if (i < length - 1)
			buf.append(',');
		}
		return buf.toString();
    }
    private String getUpdateList(String[] entries) {
		StringBuffer buf = new StringBuffer();
		for (int i=0;i<entries.length; i++) {
		    buf.append(entries[i]);
		    buf.append("=? ");
		    if (i < entries.length -1)
			buf.append(", ");
		}
		return buf.toString();
    }

    public static void executeBatchedStatement(Connection con,String sql) throws SQLException {
        Statement stmt = null;
        try {
		    stmt = con.createStatement();
		    StringTokenizer tokenizer = new StringTokenizer(sql,";");
		    while (tokenizer.hasMoreTokens())
		        stmt.executeUpdate(tokenizer.nextToken());
        } finally {
            if (stmt!=null)
                stmt.close();
        }
    }

    String getString(ResultSet rset,int index) throws SQLException {
		String str = rset.getString(index);
		return (str !=null) ? str:"";
    }

    public static int getId(RefEntity<?> entity) {
    	return ((SimpleIdentifier) entity.getId()).getKey();
    }

    private boolean isInDatabase(RefEntity<?> entity) throws SQLException {
        ResultSet rset = null;
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(searchForIdSql);
            stmt.setInt(1, getId(entity));
            rset = stmt.executeQuery();
            return rset.next();
        } finally {
            if (rset!=null)
                rset.close();
            if (stmt!=null)
                stmt.close();
        }
    }

    public void loadAll() throws SQLException,RaplaException {
	    Statement stmt = null;
        ResultSet rset = null;
        try {
            stmt = con.createStatement();
            rset = stmt.executeQuery(selectSql);
            while (rset.next ()) {
            	load(rset);
            }
        } finally {
            if (rset != null)
                rset.close();
            if (stmt!=null)
                stmt.close();
        }
        Iterator<Storage> it = subStores.iterator();
        while (it.hasNext()) {
        	( it.next()).loadAll();
        }
    }


    public void insert(Collection<? extends RaplaObject> entities) throws SQLException,RaplaException {
        Iterator<? extends RaplaObject> it2 = entities.iterator();
        while (it2.hasNext()) {
        	RefEntity<?> entity =  (RefEntity<?>)it2.next();
        	insert ( entity);
        }
    }

    public void insert(RefEntity<?> entity ) throws SQLException,RaplaException {
        Iterator<Storage> it = subStores.iterator();
		while (it.hasNext()) {
		    ( it.next()).insert( entity);
		}
		PreparedStatement stmt = null;
    	try {
    	    stmt = con.prepareStatement(insertSql);
    	    write(stmt, entity);
    	} finally {
    		if (stmt!=null)
    			stmt.close();
    	}
    }

    public void update(RefEntity<?> entity ) throws SQLException,RaplaException {
        Iterator<Storage> it = subStores.iterator();
		while (it.hasNext()) {
            Storage storage = it.next();
            storage.delete( entity );
		    storage.insert( entity);
		}
		PreparedStatement stmt = null;
        try {
    	    stmt = con.prepareStatement( updateSql);
    	    int id = getId( entity );
    	    stmt.setInt( lastParameterIndex + 1,id );
    		write(stmt, entity);
    	} finally {
    		if (stmt!=null)
    			stmt.close();
    	}
    }

    public void save(RefEntity<?> entity) throws SQLException,RaplaException {
        if (isInDatabase(entity)) {
		    update( entity );
		} else {
		    insert( entity );
		}
    }


	public void delete(RefEntity<?> entity) throws SQLException, RaplaException {
    	Iterator<Storage> it = subStores.iterator();
		while (it.hasNext()) {
		    ((Storage) it.next()).delete( entity);
		}

        PreparedStatement stmt = null;
        try {
	        stmt = con.prepareStatement(deleteSql);
	        stmt.setInt(1,getId( entity));
	        stmt.executeUpdate();
        } finally {
            if (stmt!=null)
                stmt.close();
        }
    }

    public void deleteAll() throws SQLException {
		Iterator<Storage> it = subStores.iterator();
		while (it.hasNext()) {
		    ( it.next()).deleteAll();
		}
		executeBatchedStatement(con,deleteAllSql);
    }
    abstract protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException,RaplaException;
    abstract protected void load(ResultSet rs) throws SQLException,RaplaException;

    public RaplaInput getReader() throws RaplaException {
        return (RaplaInput) lookup( RaplaInput.class.getName());

    }

    public RaplaXMLReader getReaderFor( RaplaType type) throws RaplaException {
        @SuppressWarnings("unchecked")
		Map<Object,RaplaXMLReader> readerMap = (Map<Object,RaplaXMLReader>) lookup( PreferenceReader.READERMAP);
        return readerMap.get( type);
    }

    public RaplaXMLWriter getWriterFor( RaplaType type) throws RaplaException {
        @SuppressWarnings("unchecked")
		Map<RaplaType,RaplaXMLWriter> writerMap = (Map<RaplaType,RaplaXMLWriter>) lookup( PreferenceWriter.WRITERMAP );
        return writerMap.get( type);
    }

    protected Object lookup(String role) throws RaplaException {
        try {
            return sm.lookup( role);
        } catch (RaplaContextException e) {
            throw new RaplaException( e);
        }

    }
    protected void put( RefEntity<?> entity)
    {
       entityStore.put( entity);
        
    }
    
    protected EntityResolver getResolver()
    {
        return entityStore;
    }
    
    protected void putPassword( Object userId, String password )
    {
        entityStore.putPassword( userId, password);
    }

    protected DynamicType getDynamicType( String typeKey )
    {
        return entityStore.getDynamicType( typeKey);
    }

    protected RefEntity<?> resolve( Object id) throws EntityNotFoundException
    {
        return entityStore.resolve( id);
    }

    protected Category getSuperCategory()
    {
        if ( cache != null)
        {
            return cache.getSuperCategory();
        }
        return entityStore.getSuperCategory();
    }
    
    protected RefEntity<?> get( Object id )
    {
        return entityStore.get( id);  
    }
}




