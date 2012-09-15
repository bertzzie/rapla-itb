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

import java.util.Date;

import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.domain.internal.PermissionImpl;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleEntity;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class AllocatableReader extends RaplaXMLReader
{
    DynAttReader dynAttHandler;
    AllocatableImpl allocatable;

    public AllocatableReader( RaplaContext context ) throws RaplaException
    {
        super( context );
        dynAttHandler = new DynAttReader( context );
        addChildHandler( dynAttHandler );
    }

    public void processElement(
        String namespaceURI,
        String localName,
        String qName,
        Attributes atts ) throws SAXException
    {
        if (namespaceURI.equals( DYNATT_NS ))
        {
            dynAttHandler.setClassifiable( allocatable );
            delegateElement(
                dynAttHandler,
                namespaceURI,
                localName,
                qName,
                atts );
            return;
        }

        if (!namespaceURI.equals( RAPLA_NS ))
            return;

        String holdBackString = getString( atts, "holdbackconflicts", "false" );
        boolean holdBackConflicts = Boolean.valueOf( holdBackString ).booleanValue();
        if (localName.equals( "resource" ) || localName.equals( "person" ))
        {
            String createdAt = atts.getValue( "", "created-at");
            String lastChanged = atts.getValue( "", "last-changed");
            String lastChangedBy = atts.getValue( "", "last-changed-by");

            Date createTime = null;
            Date changeTime = createTime;
            if (createdAt != null)
                createTime = parseDate( createdAt, false );
            if (lastChanged != null)
                changeTime = parseDate( lastChanged, false );

            allocatable = new AllocatableImpl(createTime, changeTime);
            if ( lastChangedBy != null) 
            {
                try 
                {
                    User user = (User)resolve(User.TYPE,lastChangedBy );
                    allocatable.setLastChangedBy( user );
                } 
                catch (SAXParseException ex) 
                {
                    getLogger().warn("Can't find user " + lastChangedBy + " at line " + ex.getLineNumber());
                }
            }
            allocatable.setHoldBackConflicts( holdBackConflicts );
            setId( (SimpleEntity<?>) allocatable, atts );
            setVersionIfThere( allocatable, atts);
            setOwner(allocatable, atts);
        }

        
        if (localName.equals( "permission" ))
        {
            PermissionImpl permission = new PermissionImpl();

            // process user
            String userString = atts.getValue( "user" );
            if (userString != null)
                permission.setUser( (User) resolve( User.TYPE, userString ) );

            // process group
            String groupId = atts.getValue( "groupidref" );
            if (groupId != null)
            {
                permission.setGroup( (Category) resolve(
                        Category.TYPE,
                        groupId ) );
            }
            else
            {
                String groupName = atts.getValue( "group" );
                if (groupName != null)
                {
                    Category group= getGroup( groupName);
                    permission.setGroup( group);
                }
            }

            String startDate = getString( atts, "start-date", null );
            if (startDate != null)
            {
                permission.setStart( parseDate( startDate, false ) );
            }

            String endDate = getString( atts, "end-date", null );
            if (endDate != null)
            {
                permission.setEnd( parseDate( endDate, false ) );
            }

            String minAdvance = getString( atts, "min-advance", null );
            if (minAdvance != null)
            {
                permission.setMinAdvance( parseLong( minAdvance ) );
            }

            String maxAdvance = getString( atts, "max-advance", null );
            if (maxAdvance != null)
            {
                permission.setMaxAdvance( parseLong( maxAdvance ) );
            }

            String accessLevel = getString(
                atts,
                "access",
                (String) Permission.ACCESS_LEVEL_NAMEMAP.get( Permission.ALLOCATE_CONFLICTS ) );
            int[] matchingLevel = Permission.ACCESS_LEVEL_NAMEMAP.findMatchingKeys( accessLevel );
            if (matchingLevel.length == 0)
            {
                throw createSAXParseException( "Unknown access level '" + accessLevel + "'" );
            }
            permission.setAccessLevel( matchingLevel[0] );
            allocatable.addPermission( permission );
        }
    }

    public void processEnd( String namespaceURI, String localName, String qName )
        throws SAXException
    {
        if (!namespaceURI.equals( RAPLA_NS ))
            return;

        if (localName.equals( "resource" ) || localName.equals( "person" ))
        {
            if (allocatable.getPermissions().length == 0)
                allocatable.addPermission( new PermissionImpl() );
            add( (RefEntity<?>) allocatable );
        }
    }
}
