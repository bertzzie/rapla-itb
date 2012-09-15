/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org .       |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, of which license fullfill the Open Source    |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.storage.dbsql;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.Assert;
import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.internal.PreferencesImpl;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.domain.internal.AppointmentImpl;
import org.rapla.entities.domain.internal.PeriodImpl;
import org.rapla.entities.domain.internal.PermissionImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.internal.AttributeImpl;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.entities.internal.UserImpl;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.storage.xml.CategoryReader;
import org.rapla.storage.xml.PreferenceReader;
import org.rapla.storage.xml.RaplaXMLReader;
import org.rapla.storage.xml.RaplaXMLWriter;
import org.xml.sax.SAXException;

class RaplaSQL {
    private final List<Storage> stores = new ArrayList<Storage>();
    private final Logger logger;
    
    RaplaSQL( RaplaContext context, boolean oldResourceTable, boolean newResourceSchema) throws RaplaException{
        logger = ( Logger) context.lookup( Logger.class.getName());
        // The order is important. e.g. appointments can only be loaded if the reservation they are refering to are already loaded.
	    stores.add(new CategoryStorage( context));
	    stores.add(new DynamicTypeStorage( context));
		stores.add(new UserStorage( context));
	    stores.add(new AllocatableStorage( context, oldResourceTable,newResourceSchema));
	    stores.add(new PreferenceStorage( context));
		stores.add(new PeriodStorage( context));
	    stores.add(new ReservationStorage( context));
		stores.add(new AppointmentStorage( context));
	}


    protected Logger getLogger() {
    	return logger;
    }

/***************************************************
 *   Create everything                             *
 ***************************************************/
    synchronized public void createAll(Connection con)
        throws SQLException,RaplaException
    {
		getLogger().info("Inserting Data in Database");
		Iterator<Storage> it = stores.iterator();
		while (it.hasNext()) {
		    Storage storage =  it.next();
		    storage.setConnection(con);
		    ((RaplaTypeStorage) storage).insertAll();
		}
    }

    synchronized public void removeAll(Connection con)
    throws SQLException,RaplaException
    {
        getLogger().info("Deleting all Data in Database");
        ListIterator<Storage> listIt = stores.listIterator(stores.size());
        while (listIt.hasPrevious()) {
            Storage storage =  listIt.previous();
            storage.setConnection(con);
            storage.deleteAll();
        }
    }

    synchronized public void loadAll(Connection con) throws SQLException,RaplaException {
		Iterator<Storage> it = stores.iterator();
		while (it.hasNext()) {
		    Storage storage = it.next();
		    storage.setConnection(con);
		    storage.loadAll();
		}
    }

    synchronized public void remove(Connection con,RefEntity<?> entity) throws SQLException,RaplaException {
    	if ( Attribute.TYPE.equals(  entity.getRaplaType() ))
			return;
    	Iterator<Storage> it = stores.iterator();
		while (it.hasNext()) {
		    Storage storage = it.next();
		    if (((RaplaTypeStorage)storage).canStore(entity)) {
		    	storage.setConnection(con);
		    	storage.delete(entity);
		    	return;
		    }
		}
		throw new RaplaException("No Storage-Sublass matches this object: " + entity.getClass());
    }

    synchronized public void store(Connection con, RaplaObject entity) throws SQLException,RaplaException {
        if ( Attribute.TYPE.equals(  entity.getRaplaType() ))
            return;
    	Iterator<Storage> it = stores.iterator();
		while (it.hasNext()) {
		    Storage storage = it.next();
		    if (((RaplaTypeStorage)storage).canStore(entity)) {
		    	storage.setConnection(con);
		    	storage.save((RefEntity<?>)entity);
		    	return;
		    }
		}
		throw new RaplaException("No Storage-Sublass matches this object: " + entity.getClass());
    }
}

abstract class RaplaTypeStorage extends EntityStorage {
	RaplaType raplaType;

	RaplaTypeStorage( RaplaContext context, RaplaType raplaType, String tableName, String[] entries) throws RaplaException {
		super( context,tableName, entries );
		this.raplaType = raplaType;
	}
    boolean canStore(RaplaObject entity) {
    	return entity.getRaplaType().is(raplaType);
    }
    void insertAll() throws SQLException,RaplaException {
        insert(cache.getCollection( raplaType ));
    }

    protected String getXML(RaplaObject type) throws RaplaException {
		RaplaXMLWriter dynamicTypeWriter = getWriterFor( type.getRaplaType());
		StringWriter stringWriter = new StringWriter();
	    BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
	    dynamicTypeWriter.setWriter( bufferedWriter );
	    dynamicTypeWriter.setSQL( true );
	    try {
	        dynamicTypeWriter.writeObject(type);
	        bufferedWriter.flush();
	    } catch (IOException ex) {
	        throw new RaplaException( ex);
	    }
	    return stringWriter.getBuffer().toString();

	}

	protected RaplaXMLReader processXML(RaplaType type, String xml) throws RaplaException {
	    RaplaXMLReader contentHandler = getReaderFor( type);
	    if ( xml== null ||  xml.trim().length() <= 10) {
	        throw new RaplaException("Can't load " + type);
	    }
	    try {
	        getReader().readWithNamespaces( xml, contentHandler);
	    } catch (IOException ex) {
	        throw new RaplaException( ex );
	    }
	    return contentHandler;
	}

}


class PeriodStorage extends RaplaTypeStorage {
    public PeriodStorage(RaplaContext context) throws RaplaException {
    	super(context, Period.TYPE,"PERIOD",new String[] {"ID","NAME","PERIOD_START","PERIOD_END"});
    }

    protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException {
		Period s = (Period) entity;
		if ( getLogger().isDebugEnabled())
		    getLogger().debug("Inserting Period " + s.getName());
		stmt.setInt(1,getId(entity));
		stmt.setString(2,s.getName());
		stmt.setTimestamp(3,new java.sql.Timestamp(s.getStart().getTime()));
		stmt.setTimestamp(4,new java.sql.Timestamp(s.getEnd().getTime()));
		stmt.executeUpdate();
    }

    protected void load(ResultSet rset) throws SQLException {
		SimpleIdentifier id = new SimpleIdentifier(Period.TYPE, rset.getInt(1));
		String name = getString(rset,2);
		java.util.Date von  = new java.util.Date(rset.getTimestamp(3).getTime());
		java.util.Date bis  = new java.util.Date(rset.getTimestamp(4).getTime());
		PeriodImpl period = new PeriodImpl(von,bis);
		period.setName( name );
		period.setId( id );
		put( period );
		if ( getLogger().isDebugEnabled()) {
		    getLogger().debug("  " + name );
		    getLogger().debug("    start: " + von);
		    getLogger().debug("    end: " + bis);
		}
    }
}

class CategoryStorage extends RaplaTypeStorage {
	Map<Category,Integer> orderMap =  new HashMap<Category,Integer>();
    Map<Category,Object> categoriesWithoutParent = new TreeMap<Category,Object>(new Comparator<Category>()
        {
            public int compare( Category o1, Category o2 )
            {
                if ( o1.equals( o2))
                {
                    return 0;
                }
                int ordering1 = ( orderMap.get( o1 )).intValue();
                int ordering2 = (orderMap.get( o2 )).intValue();
                if ( ordering1 < ordering2)
                {
                    return -1;
                }
                if ( ordering1 > ordering2)
                {
                    return 1;
                }
                if (o1.hashCode() > o2.hashCode())
                {
                    return -1;
                }
                else
                {
                    return 1;
                }
            }
        
        }    
    );

    public CategoryStorage(RaplaContext context) throws RaplaException {
    	super(context,Category.TYPE, "CATEGORY",new String[] {"ID","PARENT_ID","CATEGORY_KEY","LABEL","DEFINITION", "PARENT_ORDER"});
    }

    protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException, RaplaException {
        Category root = getSuperCategory();
        if ( entity.equals( root ))
            return;
		Category category = (Category) entity;
		String name = category.getName( getLocale() );
		int id = getId(entity);
		int parentId = getId((RefEntity<?>)category.getParent());
		if ( getLogger().isDebugEnabled())
		    getLogger().debug("Inserting Category " + name);
		stmt.setInt(1, id);
		if ( root.equals( category.getParent())) 
        {
			stmt.setObject(2, null, Types.INTEGER);
		} 
        else 
        {
		    stmt.setInt(2, parentId);
		}
        int order = getOrder( category);
        String xml = getXML( category );
        stmt.setString(3, category.getKey());
		stmt.setString(4, name );
		stmt.setString(5, xml);
        stmt.setInt( 6, order);
		stmt.executeUpdate();
    }

    

    private int getOrder( Category category )
    {
        Category parent = category.getParent();
        if ( parent == null)
        {
            return 0;
        }
        Category[] childs = parent.getCategories();;
        for ( int i=0;i<childs.length;i++)
        {
            if ( childs[i].equals( category))
            {
                return i;
            }
        }
        getLogger().error("Category not found in parent");
        return 0;
    }

    public RaplaXMLReader getReaderFor( RaplaType type) throws RaplaException {
        RaplaXMLReader reader = super.getReaderFor( type );
        if ( type.equals( Category.TYPE ) ) {
            ((CategoryReader) reader).setReadOnlyThisCategory( true);
        }
        return reader;
    }

    protected void load(ResultSet rset) throws SQLException, RaplaException {
        int idInt = rset.getInt(1);
    	String key = getString( rset, 3 );
		String name = getString( rset, 4 );
        String xml = getString( rset, 5 );
    	Object id = new SimpleIdentifier(Category.TYPE, idInt);
    	int order = rset.getInt( 6 );
        CategoryImpl category;
    	if ( xml != null && xml.length() > 10 )
    	{
    	    category = ((CategoryReader)processXML( Category.TYPE, xml )).getCurrentCategory();
            //cache.remove( category );
            category.setId( id );
    	}
    	else
    	{
    		// for compatibility with version prior to 1.0rc1
    		category = new CategoryImpl();
			category.setId( id);
	    	category.setKey( key );
	    	category.getName().setName( getLocale().getLanguage(), name);
    	}
      
        put( category );

    	int parentIdInt = rset.getInt(2);
        orderMap.put( category, new Integer( order));
    	if ( !rset.wasNull() ) 
        {
            categoriesWithoutParent.put( category, new SimpleIdentifier(Category.TYPE, parentIdInt));
    	} 
        else 
        {
    	    categoriesWithoutParent.put( category, null );
    	}
    }

    public void loadAll() throws RaplaException, SQLException {
    	categoriesWithoutParent.clear();
    	super.loadAll();
    	// then we rebuild the hirarchy
    	Iterator<Map.Entry<Category,Object>> it = categoriesWithoutParent.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry<Category,Object> entry = it.next();
    		Object parentId = entry.getValue();
    		Category category =  entry.getKey();
    		Category parent;
            Assert.notNull( category );
    		if ( parentId != null) {
    		    parent = (Category) resolve( parentId );
                Assert.notNull( parent );
            } else {
    		    parent = getSuperCategory();
                Assert.notNull( parent );
            }
    		
            parent.addCategory( category );
    	}
    }
}

class AllocatableStorage extends RaplaTypeStorage {
    Map<Integer,Classification> classificationMap = new HashMap<Integer,Classification>();
    Map<Integer,Allocatable> allocatableMap = new HashMap<Integer,Allocatable>();
    AttributeValueStorage resourceAttributeStorage;
    PermissionStorage permissionStorage;
    boolean newResourceSchema;

    public AllocatableStorage(RaplaContext context, boolean oldResourceTable, boolean newResourceSchema ) throws RaplaException {
        super(context,Allocatable.TYPE
           , oldResourceTable ? 
             "RESOURCE" 
           : "RAPLA_RESOURCE"
           , newResourceSchema ? 
             new String [] 
             	{"ID","TYPE_KEY","IGNORE_CONFLICTS","OWNER_ID","CREATION_TIME","LAST_CHANGED","LAST_CHANGED_BY"} 
           : new String [] 
              	{"ID","TYPE_KEY","IS_PERSON","IGNORE_CONFLICTS"});
        resourceAttributeStorage = new AttributeValueStorage(context,"RESOURCE_ATTRIBUTE_VALUE", "RESOURCE_ID",classificationMap);
        permissionStorage = new PermissionStorage( context, allocatableMap);
        this.newResourceSchema = newResourceSchema;
        addSubStorage(resourceAttributeStorage);
        addSubStorage(permissionStorage );
    }

    protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException,RaplaException {
      	AllocatableImpl allocatable = (AllocatableImpl) entity;
        if ( getLogger().isDebugEnabled())
      	    getLogger().debug("Inserting Allocatable " + allocatable.getName(getLocale()));
      	int id = getId( entity );
      	String typeKey = allocatable.getClassification().getType().getElementKey();
		stmt.setInt(1, id );
      	stmt.setString(2, typeKey );
      	if (newResourceSchema)
      	{
      	  stmt.setInt(3, allocatable.isHoldBackConflicts()? 1:0);
          org.rapla.entities.Timestamp timestamp = allocatable;
          Date creationTime = timestamp.getCreateTime();
          Date lastModified = timestamp.getLastChangeTime();
      	  final RefEntity<?> owner = (RefEntity<?>) allocatable.getOwner();
      	  if ( owner != null)
      	  {
          	  int userId = getId( owner );
          	  stmt.setInt(4, userId );
      	  }
      	  else
      	  {
      	      stmt.setObject(4, null, Types.INTEGER);
      	  }
          if ( creationTime != null) 
          {
              stmt.setTimestamp( 5, new java.sql.Timestamp( creationTime.getTime() ));
          }
          else
          {
              stmt.setObject(5, null, Types.TIMESTAMP);
          }
          if ( lastModified != null) 
          {
              stmt.setTimestamp( 6, new java.sql.Timestamp( lastModified.getTime() ));
          }
          else
          {
              stmt.setObject(6, null, Types.TIMESTAMP);
          }
          User lastChangedBy = timestamp.getLastChangedBy();
          if ( lastChangedBy != null) {
              int lastChangedById = getId( (RefEntity<?>) lastChangedBy );
              stmt.setInt( 7, lastChangedById);
          } else {
              stmt.setObject(7, null, Types.INTEGER);
          }
      	}
      	else
      	{
      	  stmt.setInt(3, allocatable.isPerson()? 1:0);
          stmt.setInt(4, allocatable.isHoldBackConflicts()? 1:0);
            
      	}
        
      	stmt.executeUpdate();
    }

    protected void load(ResultSet rset) throws SQLException {
        int idInt = rset.getInt(1);
    	String typeKey = rset.getString(2);

        final Date createDate; 
        final Date lastChanged;
		boolean ignoreConflicts;
        if ( newResourceSchema)
    	{
            ignoreConflicts = rset.getInt( 3 ) == 1;
            java.sql.Timestamp creationTime = rset.getTimestamp( 5 );
            java.sql.Timestamp lastModified = rset.getTimestamp( 6 );
            createDate = creationTime != null ? new Date( creationTime.getTime()) : null;
            lastChanged = lastModified != null ? new Date( lastModified.getTime()) : null;
     	}
        else
        {
            ignoreConflicts = rset.getInt( 4 ) == 1;
            createDate = null;
            lastChanged = null;
        }
    	AllocatableImpl allocatable = new AllocatableImpl(createDate, lastChanged);
    	allocatable.setId( new SimpleIdentifier( allocatable.getRaplaType() , idInt));
    	allocatable.setHoldBackConflicts( ignoreConflicts );
    	DynamicType type = getDynamicType(typeKey );
        if ( type == null)
        {
            getLogger().error("Allocatable with id " + idInt + " has an unknown type " + typeKey + ". Try ignoring it");
            return;
        }
        if ( newResourceSchema)
        {
			int userInt = rset.getInt( 4 );
			if ( !rset.wasNull()) {
			    User user = (User)get( new SimpleIdentifier(User.TYPE, userInt));
			    allocatable.setOwner( user );
			}
            int lastModfiedByIdInt = rset.getInt( 7);
            if ( !rset.wasNull()) {
                User lastModifiedBy = (User)get( new SimpleIdentifier(User.TYPE, lastModfiedByIdInt));
                if ( lastModifiedBy != null)
                {
                    allocatable.setLastChangedBy( lastModifiedBy );
                }
            
            }
        }
    	Classification classification = type.newClassification();
    	allocatable.setClassification( classification );
    	classificationMap.put( new Integer(idInt), classification );
    	allocatableMap.put(  new Integer(idInt), allocatable);
    	put( allocatable );
    }

   

    public void loadAll() throws RaplaException, SQLException {
    	classificationMap.clear();
    	super.loadAll();
    }
}

class ReservationStorage extends RaplaTypeStorage {
    Map<Integer,Classification> classificationMap = new HashMap<Integer,Classification>();
    Map<Integer,Reservation> reservationMap = new HashMap<Integer,Reservation>();
    AttributeValueStorage attributeValueStorage;
    public ReservationStorage(RaplaContext context) throws RaplaException {
        super(context,Reservation.TYPE, "EVENT",new String [] {"ID","TYPE_KEY","OWNER_ID","CREATION_TIME","LAST_CHANGED","LAST_CHANGED_BY"});
        attributeValueStorage = new AttributeValueStorage(context,"EVENT_ATTRIBUTE_VALUE","EVENT_ID", classificationMap);
        addSubStorage(attributeValueStorage);
    }

    protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException,RaplaException {
      	Reservation event = (Reservation) entity;
      	if ( getLogger().isDebugEnabled())
      	    getLogger().debug("Storing Reservation " + event.getName(getLocale()));
      	int id = getId( entity );
      	String typeKey = event.getClassification().getType().getElementKey();
      	int userId = getId( (RefEntity<?>) event.getOwner() );
      	stmt.setInt(1, id );
      	stmt.setString(2, typeKey );
    	stmt.setInt(3, userId );
    	org.rapla.entities.Timestamp timestamp = event;
    	Date creationTime = timestamp.getCreateTime();
        Date lastModified = timestamp.getLastChangeTime();
        if ( creationTime != null) 
        {
            stmt.setTimestamp( 4, new java.sql.Timestamp( creationTime.getTime() ));
        }
        else
        {
            stmt.setObject(4, null, Types.TIMESTAMP);
        }
        if ( lastModified != null) 
        {
            stmt.setTimestamp( 5, new java.sql.Timestamp( lastModified.getTime() ));
        }
        else
        {
            stmt.setObject(5, null, Types.TIMESTAMP);
        }
        User lastChangedBy = timestamp.getLastChangedBy();
    	if ( lastChangedBy != null) {
            int lastChangedById = getId( (RefEntity<?>) lastChangedBy );
            stmt.setInt( 6, lastChangedById);
        } else {
			stmt.setObject(6, null, Types.INTEGER);
        }
        stmt.executeUpdate();
    }

    protected void load(ResultSet rset) throws SQLException {
        int idInt = rset.getInt(1);
    	String typeKey = rset.getString(2);
    	int userInt = rset.getInt(3);
    	java.sql.Timestamp creationTime = rset.getTimestamp( 4 );
    	java.sql.Timestamp lastModified = rset.getTimestamp( 5 );
    	final Date createDate = creationTime != null ? new Date( creationTime.getTime()) : null;
        final Date lastChanged = lastModified != null ? new Date( lastModified.getTime()) : null;
        ReservationImpl event = new ReservationImpl(createDate, lastChanged);
    	event.setId( new SimpleIdentifier(Reservation.TYPE, idInt));
    	DynamicType type = getDynamicType(typeKey );
        User user = (User)get( new SimpleIdentifier(User.TYPE, userInt));
        if ( user == null || type == null)
        {
            getLogger().warn("Reservation with id " + idInt + " has no type or owner. It will be ignored");
            return;
        }
    	event.setOwner( user );
        int lastModfiedByIdInt = rset.getInt( 6);
        if ( !rset.wasNull()) {
            User lastModifiedBy = (User)get( new SimpleIdentifier(User.TYPE, lastModfiedByIdInt));
            if ( lastModifiedBy != null)
            {
                event.setLastChangedBy( lastModifiedBy );
            }
        }

    	Classification classification = type.newClassification();
    	event.setClassification( classification );
    	classificationMap.put( new Integer(idInt), classification );
    	reservationMap.put( new Integer(idInt), event );
    	put( event );
    }

    public void loadAll() throws RaplaException, SQLException {
    	classificationMap.clear();
    	super.loadAll();
    }
}

/** This class should only be used within the ResourceStorage class*/
class AttributeValueStorage extends EntityStorage {
    Map<Integer,Classification> classificationMap;
    final String foreignKeyName;
    public AttributeValueStorage(RaplaContext context,String tablename, String foreignKeyName, Map<Integer,Classification> classificationMap) throws RaplaException {
	// FIXME: DB field with name 'VALUE' is not allowed in MS-Access.
	// But rename of field makes old versions incompatible
    	super(context, tablename, new String[]{foreignKeyName, "ATTRIBUTE_KEY", "VALUE"});
        this.foreignKeyName = foreignKeyName;
        this.classificationMap = classificationMap;
    }

    protected void write(PreparedStatement stmt,RefEntity<?> classifiable) throws EntityNotFoundException, SQLException {
        int id = getId(classifiable);
        Classification classification = ((Classifiable) classifiable).getClassification();;
        Attribute[] attributes = classification.getAttributes();
        for (int i=0;i<attributes.length;i++) {
            Attribute attribute = attributes[i];
            Object value = classification.getValue( attribute );
            if ( value == null) {
                continue;
            }
            
            String valueAsString = AttributeImpl.attributeValueToString( attribute, value, true);
            if ( valueAsString == null)
            {
                continue;
            }
            
            stmt.setInt(1, id);
            stmt.setString(2, attribute.getKey());
         	stmt.setString(3, valueAsString);
         	stmt.execute();
        }
    }

    public void save( RefEntity<?> entity ) throws RaplaException, SQLException{
        delete( entity );
        insert( entity );
    }

    protected void load(ResultSet rset) throws SQLException, RaplaException {
        int classifiableIdInt = rset.getInt( 1);
        String attributekey = rset.getString( 2 );
        Classification classification = (Classification) classificationMap.get(new Integer(classifiableIdInt));
        if ( classification == null) {
            getLogger().warn("No resource or reservation found for the id" + classifiableIdInt  + " ignoring.");
            return;
        }
    	Attribute attribute = classification.getType().getAttribute( attributekey );
    	if ( attribute == null) {
    		throw new EntityNotFoundException("DynamicType '" +classification.getType() +"' doesnt have an attribute with the key " + attributekey + " Current Allocatable Id " + classifiableIdInt);
    	}
    	String valueAsString = rset.getString( 3);
	    Object value = null;
    	try {
    	    if ( valueAsString != null)
            {
    	        value = AttributeImpl.parseAttributeValue(attribute,valueAsString, getResolver()) ;
                classification.setValue( attributekey, value);
            }
    	} catch (ParseException ex) {
    	    throw new RaplaException( "Error parsing attribute value: " +ex.getMessage(), ex );
    	}
    }

 

    public void delete( RefEntity<?> entity) throws SQLException {
        int classifiableId = getId( entity );
        executeBatchedStatement(con, "DELETE FROM " + tableName + " WHERE " + foreignKeyName + " = " + classifiableId);
    }
}

class PermissionStorage extends EntityStorage  {
    Map<Integer,Allocatable> allocatableMap;
    public PermissionStorage(RaplaContext context,Map<Integer,Allocatable> allocatableMap) throws RaplaException {
        super(context,"PERMISSION",new String[] {"RESOURCE_ID","USER_ID","GROUP_ID","ACCESS_LEVEL","MIN_ADVANCE","MAX_ADVANCE","START_DATE","END_DATE"});
        this.allocatableMap = allocatableMap;
    }

    protected void write(PreparedStatement stmt, RefEntity<?> allocatable) throws SQLException, RaplaException {
        int resourceId = getId(  allocatable);
        delete( allocatable );
        Permission[] permissions = ((Allocatable)allocatable).getPermissions();
        for (int i=0;i<permissions.length;i++) {
            Permission s = permissions[i];
		    Category group = s.getGroup();
			User user = s.getUser();
			Date start = s.getStart();
			Date end = s.getEnd();
			Long minAdvance = s.getMinAdvance();
			Long maxAdvance = s.getMaxAdvance();
			stmt.setInt(1, resourceId);
			if ( user != null ) {
			    int userId = getId( (RefEntity<?>) user );
			    stmt.setInt( 2, userId );
			} else {
				stmt.setObject(2, null, Types.INTEGER);
			}
			if ( group != null) {
			    int groupId = getId( (RefEntity<?>) group);
			    stmt.setInt( 3, groupId );
			} else {
				stmt.setObject(3, null, Types.INTEGER);
			}
			int accessLevel = s.getAccessLevel();
			stmt.setInt(4, accessLevel );
			if ( minAdvance != null) {
			    stmt.setInt( 5, minAdvance.intValue());
			} else {
				stmt.setObject(5, null, Types.INTEGER);
			}
			if ( maxAdvance != null) {
			    stmt.setInt( 6, maxAdvance.intValue());
			} else {
				stmt.setObject(6, null, Types.INTEGER);
			}
			if ( start != null) {
			    stmt.setTimestamp( 7, new java.sql.Timestamp( start.getTime() ));
			} else {
				stmt.setObject(7, null, Types.TIMESTAMP);
			}
			if ( end != null) {
			    stmt.setTimestamp( 8, new java.sql.Timestamp( end.getTime() ));
			} else {
				stmt.setObject(8, null, Types.TIMESTAMP);
			}
			stmt.executeUpdate();
		}
    }

    public void save( RefEntity<?> entity ) throws RaplaException, SQLException{
        delete( entity );
        insert( entity );
    }

    protected void load(ResultSet rset) throws SQLException, RaplaException {
        int allocatableIdInt = rset.getInt( 1);
        Allocatable allocatable = allocatableMap.get(new Integer(allocatableIdInt));
        PermissionImpl permission = new PermissionImpl();
        allocatable.addPermission( permission );
        int userIdInt = rset.getInt( 2);
        if ( !rset.wasNull()) {
            permission.getReferenceHandler().putId("user", new SimpleIdentifier( User.TYPE,userIdInt));
        }
        int groupIdInt = rset.getInt( 3);
        if ( !rset.wasNull()) {
            permission.getReferenceHandler().putId("group", new SimpleIdentifier( Category.TYPE,groupIdInt));
        }
        int accessLevel = rset.getInt( 4);
        permission.setAccessLevel( accessLevel );
        int minAdvance = rset.getInt( 5 );
        if ( !rset.wasNull()) {
            permission.setMinAdvance( new Long( minAdvance ));
        }
        int maxAdvance = rset.getInt( 6 );
        if ( !rset.wasNull()) {
            permission.setMaxAdvance( new Long( maxAdvance ));
        }
        Timestamp startDate = rset.getTimestamp( 7 );
        if ( !rset.wasNull()) {
            permission.setStart( new Date(startDate.getTime()));
        }
        Timestamp endDate = rset.getTimestamp( 8 );
        if ( !rset.wasNull()) {
            permission.setEnd( new Date(endDate.getTime()));
        }
    }

    public void delete( RefEntity<?> entity) throws SQLException {
        int resourceId = getId( entity ) ;
        executeBatchedStatement(con, "DELETE FROM " + tableName + " WHERE RESOURCE_ID = " + resourceId);
    }

}


class AppointmentStorage extends RaplaTypeStorage {
    AppointmentExceptionStorage appointmentExceptionStorage;
    AllocationStorage allocationStorage;
    public AppointmentStorage(RaplaContext context) throws RaplaException {
        super(context, Appointment.TYPE,"APPOINTMENT",new String [] {"ID","EVENT_ID","APPOINTMENT_START","APPOINTMENT_END","REPETITION_TYPE","REPETITION_NUMBER", "REPETITION_END", "REPETITION_INTERVAL"});
        appointmentExceptionStorage = new AppointmentExceptionStorage(context);
        allocationStorage = new AllocationStorage( context);
        addSubStorage(appointmentExceptionStorage);
        addSubStorage(allocationStorage);
    }

    protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException,RaplaException {
        Appointment appointment = (Appointment) entity;
      	int id = getId( entity );
      	Timestamp start = new Timestamp( appointment.getStart().getTime() );
      	Timestamp end = new Timestamp( appointment.getEnd().getTime() );
      	stmt.setInt(1, id );
      	int parentId = getId( (RefEntity<?>)appointment.getReservation() );
      	stmt.setInt(2, parentId );
      	stmt.setTimestamp(3, start );
      	stmt.setTimestamp(4, end );
      	Repeating repeating = appointment.getRepeating();
      	if ( repeating == null) {
			stmt.setObject(5, null, Types.VARCHAR);
			stmt.setObject(6, null, Types.INTEGER);
			stmt.setObject(7, null, Types.TIMESTAMP);
			stmt.setObject(8, null, Types.INTEGER);
      	} else {
      	    stmt.setString(5, repeating.getType().toString());
      	    int number = repeating.getNumber();
      	    if ( number >= 0) {
      	        stmt.setInt(6, number);
      	    }  else {
				stmt.setObject(6, null, Types.INTEGER);
      	    }
      	    Date repeatingEnd = repeating.getEnd();
      	    if ( repeatingEnd != null) {
      	        stmt.setObject(7, new Timestamp( repeatingEnd.getTime()));
      	    } else
      	    {
				stmt.setObject(7, null, Types.TIMESTAMP);
      	    }
      	    int interval = repeating.getInterval();
      	    stmt.setInt(8, interval);
      	}
      	stmt.executeUpdate();
    }

    protected void load(ResultSet rset) throws SQLException, EntityNotFoundException {
        int idInt = rset.getInt(1);
        int parentId = rset.getInt( 2 );
        Reservation event;
        try {
            event = (Reservation) resolve( new SimpleIdentifier( Reservation.TYPE, parentId));
        } 
        catch ( EntityNotFoundException ex)
        {
            getLogger().warn("Could not find reservation object with id "+ parentId + " for appointment with id " + idInt );
            return;
        }
        Date start = new Date(rset.getTimestamp(3).getTime());
        Date end = new Date(rset.getTimestamp(4).getTime());
        boolean wholeDayAppointment = start.getTime() == DateTools.cutDate( start.getTime()) && end.getTime() == DateTools.cutDate( end.getTime());
    	AppointmentImpl appointment = new AppointmentImpl(start, end);
    	appointment.setId( new SimpleIdentifier(Appointment.TYPE, idInt));
    	appointment.setWholeDays( wholeDayAppointment);
    	event.addAppointment( appointment );
    	String repeatingType = rset.getString( 5 );
    	if ( !rset.wasNull() ) {
    	    appointment.setRepeatingEnabled( true );
    	    Repeating repeating = appointment.getRepeating();
    	    repeating.setType( RepeatingType.findForString( repeatingType ) );

	        java.sql.Timestamp repeatingEnd = rset.getTimestamp( 7 );
	        if ( !rset.wasNull() ) {
	            repeating.setEnd( new Date(repeatingEnd.getTime()));
	        } else {
	        	int number  = rset.getInt( 6);
	        	if ( !rset.wasNull()) {
	        		repeating.setNumber( number);
	        	} else {
	                repeating.setEnd( null );
	        	}
	        }

	        int interval = rset.getInt( 8);
    	    if ( !rset.wasNull())
    	        repeating.setInterval( interval);
    	}
    	put( appointment );
    }
}


class AllocationStorage extends EntityStorage  {

    public AllocationStorage(RaplaContext context) throws RaplaException  {
        super(context,"ALLOCATION",new String [] {"APPOINTMENT_ID", "RESOURCE_ID"});
    }

    protected void write(PreparedStatement stmt, RefEntity<?> entity) throws SQLException, RaplaException {
        int appointmentId = getId( entity);
        Appointment appointment = (Appointment) entity;
        Reservation event = appointment.getReservation();
        Allocatable[] allocatables = event.getAllocatables();
        for (int j=0;j<allocatables.length;j++) {
            Allocatable allocatable  =  allocatables[j];
            if ( !event.hasAllocated( allocatable, appointment)) {
                continue;
            }
            int allocatableId = getId( (RefEntity<?>)allocatable);
    		stmt.setInt(1, appointmentId);
            stmt.setInt(2, allocatableId);
    		stmt.executeUpdate();
        }
    }
    public void save( RefEntity<?> entity ) throws RaplaException, SQLException{
        delete( entity );
        insert( entity );
    }

    protected void load(ResultSet rset) throws SQLException, RaplaException {
        int appointmentId = rset.getInt( 1 );
        int resourceId = rset.getInt( 2 );
        Appointment appointment;
        try {
            appointment = (Appointment)resolve( new SimpleIdentifier( Appointment.TYPE,appointmentId));
        }
        catch ( EntityNotFoundException ex)
        {
            getLogger().warn("Could not find appointment with id "+ appointmentId + " for the allocation resource " + resourceId + ". Ignoring." );
            return;
        }
        Reservation event = appointment.getReservation();
        Allocatable allocatable = (Allocatable)resolve( new SimpleIdentifier( Allocatable.TYPE,resourceId));
        if ( !event.hasAllocated( allocatable ) ) {
            event.addAllocatable( allocatable );
        }
        Appointment[] appointments = event.getRestriction( allocatable );
        Appointment[] newAppointments = new Appointment[ appointments.length+ 1];
        System.arraycopy(appointments,0, newAppointments, 0, appointments.length );
        newAppointments[ appointments.length] = appointment;
        if (event.getAppointments().length > newAppointments.length ) {
            event.setRestriction( allocatable, newAppointments );
        } else {
            event.setRestriction( allocatable, new Appointment[] {} );
        }
    }

    public void delete( RefEntity<?> entity) throws SQLException {
        int appointmentId =  getId( entity ) ;
        executeBatchedStatement(con, "DELETE FROM " + tableName + " WHERE APPOINTMENT_ID = " + appointmentId);
    }


 }

class AppointmentExceptionStorage extends EntityStorage  {
    public AppointmentExceptionStorage(RaplaContext context) throws RaplaException {
        super(context,"APPOINTMENT_EXCEPTION",new String [] {"APPOINTMENT_ID","EXCEPTION_DATE"});
    }


    protected void write(PreparedStatement stmt, RefEntity<?> entity) throws SQLException, RaplaException {
        int appointmentId = getId( entity);
        Appointment appointment = (Appointment) entity;
        Repeating repeating = appointment.getRepeating();
        if ( repeating == null) {
            return;
        }
        Date[] exceptions = repeating.getExceptions();
	    for ( int i=0;i< exceptions.length;i++) {
	        java.sql.Timestamp exception = new java.sql.Timestamp( exceptions[i].getTime());
	        stmt.setInt( 1, appointmentId );
	        stmt.setTimestamp( 2, exception );
	        stmt.executeUpdate();
	    }
	}

    protected void load(ResultSet rset) throws SQLException, RaplaException {
        int appointmentId = rset.getInt( 1);
        Appointment appointment;
        try {
            appointment = (Appointment)resolve( new SimpleIdentifier( Appointment.TYPE,appointmentId));
        }
        catch ( EntityNotFoundException ex)
        {
            getLogger().warn("Could not find appointment with id "+ appointmentId + " for the specified exception. Ignoring." );
            return;
        }
                
        Repeating repeating = appointment.getRepeating();
        if ( repeating != null) {
            Date date = new Date( rset.getTimestamp( 2 ).getTime());
            repeating.addException( date );
        }
    }

    

    public void delete( RefEntity<?> entity) throws SQLException {
        int appointmentId = getId( entity);
        executeBatchedStatement(con, "DELETE FROM " + tableName + " WHERE APPOINTMENT_ID = " + appointmentId);
    }

}

class DynamicTypeStorage extends RaplaTypeStorage {

    public DynamicTypeStorage(RaplaContext context) throws RaplaException {
        super(context, DynamicType.TYPE,"DYNAMIC_TYPE",
                new String [] {"ID","TYPE_KEY","DEFINITION"});
    }

	protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException, RaplaException {
        stmt.setInt(1,getId(entity));
        DynamicType type = (DynamicType) entity;
        stmt.setString(2, type.getElementKey());
        stmt.setString(3,  getXML( type) );
        stmt.executeUpdate();
    }

	protected void load(ResultSet rset) throws SQLException,RaplaException {
    	String xml = getString(rset,3);
    	processXML( DynamicType.TYPE, xml );
	}

}


class PreferenceStorage extends RaplaTypeStorage {

    public PreferenceStorage(RaplaContext context) throws RaplaException {
        super(context,Preferences.TYPE,"PREFERENCE",
	    new String [] {"USER_ID","ROLE","STRING_VALUE","XML_VALUE"});
    }

    protected void write(PreparedStatement stmt, RefEntity<?> entity) throws SQLException, RaplaException {
        PreferencesImpl preferences = (PreferencesImpl) entity;
        User user = preferences.getOwner();
        if ( user == null) {
			stmt.setObject(1, null, Types.INTEGER);
        } else {
            stmt.setInt(1,getId( (RefEntity<?>) user));
        }
        Iterator<String> it = preferences.getPreferenceEntries();
        while (it.hasNext()) {
            String role =  it.next();
            Object entry = preferences.getEntry(role);
            stmt.setString( 2, role);
            if ( entry instanceof String) {
            	stmt.setString( 3, (String) entry);
            	stmt.setString( 4, null);
            } else {
            	//System.out.println("Role " + role + " CHILDREN " + conf.getChildren().length);
            	String xml = getXML( (RaplaObject)entry);
            	stmt.setString( 3, null);
            	stmt.setString( 4, xml);
            }
            stmt.executeUpdate();
        }
    }

    public void save(RefEntity<?> entity) throws SQLException,RaplaException {
        delete( entity );
        insert( entity );
    }

    protected void load(ResultSet rset) throws SQLException, RaplaException {
    	//findPreferences
    	//check if value set
    	//  yes read value
    	//  no read xml

        int userIdAsInt = rset.getInt(1);
        User owner = null;
        Object preferenceId;
        if ( !rset.wasNull() ){
            Object userId = new SimpleIdentifier( User.TYPE, userIdAsInt );
            owner = (User) get( userId );
            preferenceId = new SimpleIdentifier( Preferences.TYPE, userIdAsInt );
        } else {
        	preferenceId = new SimpleIdentifier( Preferences.TYPE, 0 );
        }
        PreferencesImpl preferences = (PreferencesImpl) get( preferenceId );
        if ( preferences == null) {
        	preferences = new PreferencesImpl();
        	preferences.setId(preferenceId);
        	preferences.setOwner(owner);
        	put( preferences );
        }
        String configRole = getString( rset, 2);
        String value = rset.getString( 3 );
        if ( !rset.wasNull()) {
            preferences.putEntry(configRole, value);
        } else {
	        String xml = rset.getString( 4 );

	        PreferenceReader contentHandler = (PreferenceReader) processXML( Preferences.TYPE, xml );
	        try {
	            RaplaObject type = contentHandler.getChildType();
	            preferences.putEntry(configRole, type);
	        } catch (SAXException ex) {
	            throw new RaplaException (ex);
	        }
        }
    }

 

    public void delete( RefEntity<?> entity) throws SQLException {
        PreferencesImpl preferences = (PreferencesImpl) entity;
        User user = preferences.getOwner();
        if ( user != null) {
        	int userId = getId( (RefEntity<?>) user ) ;
        	executeBatchedStatement(con, "DELETE FROM " + tableName + " WHERE USER_ID = " + userId);
        } else {
        	executeBatchedStatement(con, "DELETE FROM " + tableName + " WHERE USER_ID = null");
        }
    }

 }

class UserStorage extends RaplaTypeStorage {
    UserGroupStorage groupStorage;
    public UserStorage(RaplaContext context) throws RaplaException {
        super( context,User.TYPE, "RAPLA_USER",
	    new String [] {"ID","USERNAME","PASSWORD","NAME","EMAIL","ISADMIN"});
        groupStorage = new UserGroupStorage( context );
        addSubStorage( groupStorage );
    }

    protected void write(PreparedStatement stmt,RefEntity<?> entity) throws SQLException, RaplaException {
        User user = (User) entity;
        if ( getLogger().isDebugEnabled())
            getLogger().debug("Inserting User " + user.getUsername());
        stmt.setInt(1,getId(entity));
       stmt.setString(2,user.getUsername());
       String password = cache.getPassword(entity.getId());
       stmt.setString(3,password);
       stmt.setString(4,user.getName());
       stmt.setString(5,user.getEmail());
       stmt.setInt(6,user.isAdmin()?1:0);
       stmt.executeUpdate();
    }

    protected void load(ResultSet rset) throws SQLException, RaplaException {
        int idAsInt = rset.getInt(1);
        String username = getString(rset,2);
        String name = getString(rset,4);
        String email = getString(rset,5);
        boolean isAdmin = rset.getInt(6) == 1;
        UserImpl user = new UserImpl();
        Object userId = new SimpleIdentifier(User.TYPE, idAsInt );
        user.setId( userId );
        user.setUsername( username );
        user.setName( name );
        user.setEmail( email );
        user.setAdmin( isAdmin );
        String password = getString(rset,3);
        if ( !rset.wasNull()) {
            putPassword(userId,password);
        }
        put(user);
   }

   
}

class UserGroupStorage extends EntityStorage {
    public UserGroupStorage(RaplaContext context) throws RaplaException {
        super(context,"RAPLA_USER_GROUP", new String [] {"USER_ID","CATEGORY_ID"});
    }

    public void save(RefEntity<?> entity) throws SQLException,RaplaException {
        delete( entity );
        insert( entity );
    }

    protected void write(PreparedStatement stmt, RefEntity<?> entity) throws SQLException, RaplaException {
        int userId = getId( entity);
        User user = (User) entity;
        stmt.setInt(1, userId);
        Category[] categories = user.getGroups();
        for (int i=0;i<categories.length;i++) {
            stmt.setInt( 2, getId( (RefEntity<?>)categories[i]));
            stmt.executeUpdate();
	    }
    }

    protected void load(ResultSet rset) throws SQLException, RaplaException {
        int userId = rset.getInt( 1);
        int categoryId = rset.getInt( 2);
        User user = (User)resolve( new SimpleIdentifier( User.TYPE,userId));
        Category category = (Category)resolve( new SimpleIdentifier( Category.TYPE,categoryId));
        user.addGroup( category);
    }

    public void delete( RefEntity<?> entity) throws SQLException {
    	int userId =  getId( entity ) ;
    	executeBatchedStatement(con, "DELETE FROM " + tableName + " WHERE USER_ID = " + userId);
    }
}


