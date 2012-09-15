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
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;

import org.rapla.entities.Category;
import org.rapla.entities.Entity;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.ModificationModule;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaAction;
import org.rapla.gui.toolkit.DialogUI;

public class RaplaObjectAction extends RaplaAction {
    public final static int DELETE = 1;
    public final static int COPY = 2;
    public final static int PASTE = 3;
    public final static int CUT = 4;
    public final static int NEW = 5;
    public final static int EDIT = 6;
    public final static int VIEW = 7;
    public final static int DELETE_SELECTION = 8;
    protected Component parent;
    Point point;
    protected int type;
    boolean isPerson;
    protected RaplaObject object;
    Collection<RaplaObject> objectList;
    protected RaplaType raplaType;

    public RaplaObjectAction(RaplaContext sm) throws RaplaException {
        this(sm,null);
    }

    public RaplaObjectAction(RaplaContext sm,Component parent) throws RaplaException {
        this(sm,parent,null);
    }

    public RaplaObjectAction(RaplaContext sm,Component parent,Point point) throws RaplaException {
        super( sm);
        this.parent = parent;
        this.point = point;
    }

    public RaplaObjectAction setNew(RaplaType raplaType) {
        this.raplaType = raplaType;
        this.type = NEW;
        putValue(NAME, getString("new"));
        putValue(SMALL_ICON, getIcon("icon.new"));
        update();
        return this;
    }

    public RaplaObjectAction setDelete(RaplaObject object) {
        this.type = DELETE;
        putValue(NAME, getString("delete"));
        putValue(SMALL_ICON, getIcon("icon.delete"));
        changeObject(object);
        return this;
    }

    public RaplaObjectAction setDeleteSelection(Collection<RaplaObject> selection) {
        this.type = DELETE_SELECTION;
        putValue(NAME, getString("delete_selection"));
        putValue(SMALL_ICON, getIcon("icon.delete"));
        changeSelection( selection );
        return this;
    }

    public RaplaObjectAction setView(RaplaObject object) {
        this.type = VIEW;
        putValue(NAME, getString("view"));
        putValue(SMALL_ICON, getIcon("icon.help"));
        changeObject(object);
        return this;
    }

    public RaplaObjectAction setEdit(RaplaObject object) {
        this.type = EDIT;
        putValue(NAME, getString("edit"));
        putValue(SMALL_ICON, getIcon("icon.edit"));
        changeObject(object);
        return this;
    }

    public void changeObject(RaplaObject object) {
        this.object = object;
        if (type == DELETE) {
            if (object == null)
                putValue(NAME, getString("delete"));
            else
                putValue(NAME, getI18n().format("delete.format",getName(object)));
        }
        update();
    }

    public void changeSelection(Collection<RaplaObject> objectList) {
        this.objectList = objectList;
        update();
    }

    protected void update() {
        boolean enabled = true;
        if (type == EDIT || type == DELETE) {
            enabled = canModify(object);

        } else if (type == NEW ) {
            enabled = (raplaType != null && raplaType.is(Allocatable.TYPE) && isRegisterer()) || isAdmin();
        } else if (type == DELETE_SELECTION) {
            if (objectList != null && objectList.size() > 0 ) {
                Iterator<RaplaObject> it = objectList.iterator();
                while (it.hasNext()) {
                    if (!canModify(it.next())){
                        enabled = false;
                        break;
                    }
                }
            } else {
                enabled = false;
            }
        }
        setEnabled(enabled);
    }


    public void actionPerformed(ActionEvent evt) {
        try {
            switch (type) {
            case DELETE: delete();break;
            case DELETE_SELECTION: deleteSelection();break;
            case EDIT: edit();break;
            case NEW: newEntity();break;
            case VIEW: view();break;
            }
        } catch (RaplaException ex) {
            showException(ex,parent);
        } // end of try-catch
    }

    public void view() throws RaplaException {
        getInfoFactory().showInfoDialog(object,parent);
    }


    protected Entity<? extends Entity<?>> newEntity(RaplaType raplaType) throws RaplaException {
        ModificationModule m = getModification();
        if ( Reservation.TYPE.is( raplaType ))
            return m.newReservation();
        if ( Allocatable.TYPE.is( raplaType ))
            return (isPerson) ?m.newPerson(): m.newResource() ;
       if ( Category.TYPE.is( raplaType ))
            return m.newCategory(); //will probably never happen
       if ( User.TYPE.is( raplaType ))
                    return m.newUser();
       if ( Period.TYPE.is( raplaType ))
             return m.newPeriod();

       throw new RaplaException("Can't create Entity for " + raplaType + "!");
    }


    /** guesses the DynamicType for the new object depending on the selected element.
        <li>If the selected element is a DynamicType-Folder the DynamicType will be returned.
        <li>If the selected element has a Classification the appropriatie DynamicType will be returned.
        <li>else the first matching DynamicType for the passed classificationType will be returned.
        <li>null if none of the above criterias matched.
    */
    public DynamicType guessType() throws RaplaException {
        DynamicType[] types = guessTypes();
        if ( types.length > 0)
        {
            return types[0];
        }
        else
        {
            return null;
        }
    }
    
    public DynamicType[] guessTypes() throws RaplaException {
        DynamicType dynamicType = null;
        getLogger().debug("Guessing DynamicType from " + object);
        if (object instanceof DynamicType)
            dynamicType = (DynamicType) object;

        if (object instanceof Classifiable) {
            Classification classification= ((Classifiable) object).getClassification();
            dynamicType = classification.getType();
        }
        if (dynamicType != null)
        {
            return new DynamicType[] {dynamicType};
        }
        String classificationType = null;
        if ( Reservation.TYPE.is( raplaType )) {
            classificationType = DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION;
        } else  if ( Allocatable.TYPE.is( raplaType )) {
            if ( isPerson ) {
                classificationType = DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION;
            } else {
                classificationType = DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION;
            }
        }
        DynamicType[] dynamicTypes = getQuery().getDynamicTypes( classificationType );
        return dynamicTypes;
      
    }

    @SuppressWarnings("unchecked")
	protected  void newEntity() throws RaplaException {
    	if ( Category.TYPE.is( raplaType )) {
        	getEditController().edit((Category)object, parent);
        } else {
	    	@SuppressWarnings("rawtypes")
			Entity<? extends Entity> obj = newEntity(raplaType);
	        if (obj instanceof Classifiable) {
	            DynamicType type = guessType();
	            final Classification newClassification = type.newClassification();
                ((Classifiable)obj).setClassification(newClassification);
	        }
        	getEditController().edit(obj, parent);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	protected void edit() throws RaplaException {
    	if ( object instanceof Entity)
    	{
    		getEditController().edit((Entity)object, parent);
    	}
    }

    protected void delete() throws RaplaException {
        if (object == null)
            return;
        if (!( object instanceof Entity<?>))
        {
        	return;
        }
        Entity<?>[] objects = new Entity[] { (Entity<?>) object};
        DialogUI dlg = getInfoFactory().createDeleteDialog( objects, parent);
        dlg.start();
        if (dlg.getSelectedIndex() != 0)
            return;
        getModification().removeObjects(objects);
    }

    protected void deleteSelection() throws RaplaException {
        if (objectList == null || objectList.size() == 0)
            return;
        DialogUI dlg = getInfoFactory().createDeleteDialog(objectList.toArray(), parent);
        dlg.start();
        if (dlg.getSelectedIndex() != 0)
            return;
        getModification().removeObjects((Entity[])objectList.toArray( Entity.ENTITY_ARRAY));
    }

	public void setPerson(boolean b) {
		isPerson = b;
	}

}

