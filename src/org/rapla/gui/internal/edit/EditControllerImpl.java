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
package org.rapla.gui.internal.edit;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.rapla.entities.Category;
import org.rapla.entities.Entity;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.EditComponent;
import org.rapla.gui.EditController;
import org.rapla.gui.RaplaGUIComponent;

/**This class handles the edit-ui for all entities (except reservations).*/
public class EditControllerImpl extends RaplaGUIComponent implements EditController
{
	Collection<EditDialog> editWindowList = new ArrayList<EditDialog>();

    public EditControllerImpl(RaplaContext sm) throws RaplaException {
        super(sm);
    }
    
    void addEditDialog(EditDialog editWindow) {
        editWindowList.add(editWindow);
    }

    void removeEditDialog(EditDialog editWindow) {
        editWindowList.remove(editWindow);
    }


    /* (non-Javadoc)
     * @see org.rapla.gui.edit.IEditController#createUI(org.rapla.entities.RaplaPersistant)
     */
    public EditComponent createUI(RaplaObject obj) throws RaplaException {
        RaplaType type = obj.getRaplaType();
        EditComponent ui = null;
        if ( Allocatable.TYPE.equals( type )) {
            ui=new AllocatableEditUI(getContext());
        } else if ( DynamicType.TYPE.equals( type )) {
            ui=new DynamicTypeEditUI(getContext());
        } else if ( User.TYPE.equals( type )) {
            ui=new UserEditUI(getContext());
        } else if ( Period.TYPE.equals( type )) {
            ui=new PeriodEditUI(getContext());
        } else if ( Category.TYPE.equals( type )) {
            ui=new CategoryEditUI(getContext());
        } else if ( Preferences.TYPE.equals( type )) {
            ui=new PreferencesEditUI(getContext());
        }

        if ( ui == null) {
            throw new RuntimeException("Can't edit objects of type " + type.toString());
        }
        return ui;
    }

    protected String guessTitle(Object obj) {
        if (obj instanceof Entity)
        {
            String type = ((Entity<?>) obj).getRaplaType().getLocalName();
            return getString( type );
        }
        return "";
    }

    /* (non-Javadoc)
     * @see org.rapla.gui.edit.IEditController#edit(org.rapla.entities.Entity, java.awt.Component)
     */
    public <T extends Entity<T>> void edit(Entity<T> obj,Component owner) throws RaplaException {
        edit(obj, guessTitle(obj), owner);
    }

    /* (non-Javadoc)
     * @see org.rapla.gui.edit.IEditController#edit(org.rapla.entities.Entity, java.lang.String, java.awt.Component)
     */
	public <T extends Entity<T>> void  edit(Entity<T> obj,String title,Component owner) throws RaplaException {
    	  // Lookup if the reservation is already beeing edited
        EditDialog c = null;
        Iterator<EditDialog> it = editWindowList.iterator();
        while (it.hasNext()) {
            c = it.next();
            Object editObj = c.ui.getObject();
            if (editObj != null && editObj instanceof Entity && ((Entity<?>)editObj).isIdentical(obj))
            {
                break;
            }
            else
            {
                c = null;
            }
        }

        if (c != null) {
            c.dlg.requestFocus();
            c.dlg.toFront();
        } else {
            if ( obj.isPersistant()) 
            {
				obj =  getModification().edit(obj);
            }
            EditComponent ui = createUI( obj );
            EditDialog gui = new EditDialog(getContext(),ui, false);
            gui.start(obj,title,owner);
        }
        //return c;
    	
    }

}

