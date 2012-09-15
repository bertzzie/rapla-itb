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

import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

class UserInfoUI extends ClassificationInfoUI {
    public UserInfoUI(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    protected String createHTMLAndFillLinks(Object object,LinkController controller) {
        User user = (User) object;
        StringBuffer buf = new StringBuffer();
        if (user.isAdmin()) {
            highlight(getString("admin"),buf);
        }
        Collection<Row> att = new ArrayList<Row>();
        att.add(new Row(getString("username"), strong( encode( user.getUsername() ) ) ) );
        
        final Allocatable person = user.getPerson();
        if ( person == null)
        {
            att.add(new Row(getString("name"), encode(user.getName())));
            att.add(new Row(getString("email"), encode(user.getEmail())));
        }
        else
        {
            att.addAll(getClassificationAttributes(person, false));
        }
        createTable(att,buf,false);
        
        Category userGroupsCategory;
		try {
			userGroupsCategory = getQuery().getUserGroupsCategory();
		} catch (RaplaException e) {
			// Should not happen, but null doesnt harm anyway
			userGroupsCategory = null;
		}
        Category[] groups = user.getGroups();
        if ( groups.length > 0 ) {
            buf.append(getString("groups") + ":");
            buf.append("<ul>");
            for ( int i = 0; i < groups.length; i++ ) {
                buf.append("<li>");
                String groupName = groups[i].getPath( userGroupsCategory , getI18n().getLocale());
                encode ( groupName , buf);
                buf.append("</li>\n");
            }
            buf.append("</ul>");
        }
        return buf.toString();
    }
    
    public String getTooltip(Object object) {
        return createHTMLAndFillLinks( object, null );
    }
}

