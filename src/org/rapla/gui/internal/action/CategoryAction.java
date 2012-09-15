/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas, Bettina Lademann                |
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
package org.rapla.gui.internal.action;

import java.awt.Component;
import java.awt.Point;

import org.rapla.entities.Category;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.toolkit.DialogUI;

public class CategoryAction extends  RaplaObjectAction{
    public CategoryAction(RaplaContext sm,Component parent,Point p) throws RaplaException {
        super(sm,parent,p);
    }

    protected void delete() throws RaplaException {
         if (object == null)
             return;
         Object[] objects = new Object[] {object};
         DialogUI dlg = getInfoFactory().createDeleteDialog( objects, parent);
         dlg.start();
         if (dlg.getSelectedIndex() != 0)
             return;
         Category category = (Category) object;
         Category parentClone = (Category) getModification().edit( category.getParent() );
         parentClone.removeCategory( parentClone.findCategory( category) );
         getModification().store( parentClone );
    }

}
