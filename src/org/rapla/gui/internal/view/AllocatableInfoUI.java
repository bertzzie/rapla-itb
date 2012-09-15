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
package org.rapla.gui.internal.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Permission;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class AllocatableInfoUI extends ClassificationInfoUI {
    public AllocatableInfoUI(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    void insertPermissions( Allocatable allocatable, StringBuffer buf ) {
        User user;
        Date today;
        try {
            user = getUser();
            today = getQuery().today();
        } catch (Exception ex) {
            return;
        }
        boolean firstPermission = true;
        if ( user.isAdmin() ) {
            return;
        }
        Permission[] permissions = allocatable.getPermissions();
        boolean everytime = false;
        for ( int i = 0; i < permissions.length; i++ ) {
            Permission permission = permissions[i];
            if ( permission.affectsUser ( user ) 
                    && permission.getMinAllowed( today ) == null
                    && permission.getMaxAllowed( today ) == null ) {
                everytime = true;
                break;
            }
        }
        
        for ( int i = 0; i < permissions.length; i++ ) {
            Permission permission = permissions[i];
            if ( permission.affectsUser( user ) ) {
                if ( firstPermission ) {
                    firstPermission = false;
                    buf.append( "<strong>" );
                    buf.append( getString( "allocatable_in_timeframe" ) );
                    buf.append( ":</strong>" );
                    buf.append("<br>");
                    if ( everytime ) {
                        buf.append( getString("everytime") );
                        break;
                    }
                }
                
                if ( permission.getMinAllowed( today ) != null ) {
                    Date date = permission.getMinAllowed( today );
                    buf.append( getRaplaLocale().formatDate( date ) );
                } else {
                    buf.append(getString("open"));
                }
                buf.append(" - ");
                if ( permission.getMaxAllowed( today ) != null ) {
                    Date date = permission.getMaxAllowed( today );
                    buf.append( getRaplaLocale().formatDate( date ) );
                } else {
                    buf.append(getString("open"));
                }
                buf.append("<br>");
            }
        }
    }
    
    protected String createHTMLAndFillLinks(Object object,LinkController controller) {
        Allocatable allocatable = (Allocatable) object;
        StringBuffer buf = new StringBuffer();
        insertModificationRow( allocatable, buf );
        insertClassificationTitle( allocatable, buf );
        createTable( getAttributes( allocatable, controller, false),buf,false);
        return buf.toString();
    }
    
    public List<Row> getAttributes(Allocatable allocatable,LinkController controller,  boolean excludeAdditionalInfos) {
        ArrayList<Row> att = new ArrayList<Row>();
        att.addAll( super.getClassificationAttributes( allocatable, excludeAdditionalInfos ));
        final Locale locale = getLocale();
        User owner = allocatable.getOwner();
        User lastChangeBy = allocatable.getLastChangedBy();
        if ( owner != null)
        {
            final String ownerName = owner.getName(locale);
            String ownerText = encode(ownerName);
            if (controller != null)
                ownerText = controller.createLink(owner,ownerName);
            
            att.add( new Row(getString("resource.owner"), ownerText));
        }
        if ( lastChangeBy != null && (owner == null || !lastChangeBy.equals(owner))) {
            final String lastChangedName = lastChangeBy.getName(locale);
            String lastChangeByText = encode(lastChangedName);
            if (controller != null)
                lastChangeByText = controller.createLink(lastChangeBy,lastChangedName);
            att.add( new Row(getString("last_changed_by"), lastChangeByText));
            
        }
       
        return att;
    }

    public String getTooltip(Object object) {
        Allocatable allocatable = (Allocatable) object;
        StringBuffer buf = new StringBuffer();
        insertClassificationTitle( allocatable, buf );
        insertModificationRow( allocatable, buf );
        Collection<Row> att = new ArrayList<Row>();
        att.addAll(getAttributes(allocatable,  null,  true));
        createTable(att,buf);
        insertPermissions( allocatable, buf );
        return buf.toString();
    }



}

