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
package org.rapla.gui.internal;
import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.Conflict;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.MenuContext;
import org.rapla.gui.MenuFactory;
import org.rapla.gui.ObjectMenuFactory;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.ReservationWizard;
import org.rapla.gui.internal.action.DynamicTypeAction;
import org.rapla.gui.internal.action.RaplaObjectAction;
import org.rapla.gui.internal.action.ReservationAction;
import org.rapla.gui.internal.action.user.PasswordChangeAction;
import org.rapla.gui.internal.action.user.UserAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.toolkit.MenuInterface;
import org.rapla.gui.toolkit.RaplaMenuItem;
import org.rapla.gui.toolkit.RaplaSeparator;
import org.rapla.plugin.RaplaExtensionPoints;

public class MenuFactoryImpl extends RaplaGUIComponent implements MenuFactory
{
    public MenuInterface addReservationWizards( MenuInterface menu, MenuContext context, String afterId ) throws RaplaException
    {
        Component parent = context.getComponent();
        Object focusedObjectUncasted = context.getFocusedObject();
        if ( !(focusedObjectUncasted instanceof RaplaObject))
        {
        	focusedObjectUncasted = null;
        }
        RaplaObject focusedObject = (RaplaObject) focusedObjectUncasted;
        Point p = context.getPoint();
        @SuppressWarnings("unchecked")
		Iterator<ReservationWizard> it = ((Collection<ReservationWizard>)getContainer().lookupServicesFor( RaplaExtensionPoints.RESERVATION_WIZARD_EXTENSION).values()).iterator();
        while (it.hasNext())
        {
            ReservationWizard wizard =  it.next();
            addReservationAction(menu,parent,p, afterId).setNew(wizard).changeObject( focusedObject );
        }
        return menu;
    }


    public MenuFactoryImpl(RaplaContext sm) throws RaplaException {
       super(sm);
    }

    public MenuInterface addNew( MenuInterface menu, MenuContext context,String afterId ) throws RaplaException
    {
        // Do nothing if the user can't allocate anything
        if (!canUserAllocateSomething( getUser()) )
            return menu;

        addReservationWizards(menu, context, afterId);

        Component parent = context.getComponent();
        Object focusedObject = context.getFocusedObject();
	    Point p = context.getPoint();
        
        boolean allocatableType = false;
        boolean reservationType = false;
        if ( focusedObject instanceof DynamicType)
        {
            DynamicType type = (DynamicType) focusedObject;
            String classificationType = type.getAnnotation( DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE );
            allocatableType = classificationType.equals(  DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION ) || classificationType.equals(  DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION );
            reservationType = classificationType.equals(  DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION );
        }
        boolean allocatableNodeContext = allocatableType || focusedObject instanceof Allocatable  || focusedObject == CalendarSelectionModel.ALLOCATABLES_ROOT;
        if ( isRegisterer() || isAdmin()) {
            if ( allocatableNodeContext)
            {
                menu.addSeparator();
                addAllocatableMenuNew( menu, parent,p, focusedObject);                
            }
        }
        if ( isAdmin()  )
        {
            boolean reservationNodeContext =  reservationType || (focusedObject!= null && focusedObject.equals( getString("reservation_type" )));
            boolean userNodeContext = focusedObject instanceof User || (focusedObject  != null &&  focusedObject.equals( getString("users")));
            boolean periodNodeContext = focusedObject instanceof Period || (focusedObject  != null &&  focusedObject.equals( getString("periods")));
            boolean categoryNodeContext = focusedObject instanceof Category || (focusedObject  != null &&  focusedObject.equals( getString("categories")));
            if (userNodeContext || allocatableNodeContext || reservationNodeContext || periodNodeContext || categoryNodeContext )
            {
                menu.addSeparator();
            }
            if ( userNodeContext)
            {
                addUserMenuNew( menu , parent, p, focusedObject);
            }
           
            if (allocatableNodeContext)
            {
                addTypeMenuNew(menu, DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION
                        ,parent, p, focusedObject);
                addTypeMenuNew(menu, DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION
                        ,parent, p, focusedObject);

            }
            if ( periodNodeContext)
            {
                addPeriodMenuNew(  menu , parent, p );
            }
            if ( categoryNodeContext )
            {
                Collection<?> list = context.getSelectedObjects();
                addCategoryMenuNew(  menu , parent, p, focusedObject, list );
            }
            if ( reservationNodeContext)
            {
                addTypeMenuNew(menu, DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION
                               ,parent, p, focusedObject);
            }
            /*
             */
        }
        return menu;
    }

    public MenuInterface addObjectMenu( MenuInterface menu, MenuContext context) throws RaplaException 
    {
        return addObjectMenu( menu, context, "EDIT_BEGIN");
    }

    public MenuInterface addObjectMenu( MenuInterface menu, MenuContext context, String afterId ) throws RaplaException
    {
        Component parent = context.getComponent();
        Object focusedObject = context.getFocusedObject();
        Point p = context.getPoint();
        
        Collection<RaplaObject> list = new ArrayList<RaplaObject>();
        for ( Object obj:  context.getSelectedObjects())
        {
    	   if ( obj instanceof RaplaObject)
    	   {
    		   list.add( (RaplaObject) obj);
    	   }
       	}
        if ( focusedObject == null || !(focusedObject instanceof RaplaObject)) 
        {
            addAction(menu,parent,p, afterId).setDeleteSelection(getDeletableObjects(list));
        }
        else
        {
	        RaplaObject obj = (RaplaObject) focusedObject;
	        RaplaType type = obj.getRaplaType();
	        if ( type.equals(Allocatable.TYPE))
	        {
	            addAllocatableMenuEdit(menu,  parent, p, (Allocatable)obj, list, afterId);
	        }
	        else if ( type.equals(Reservation.TYPE) )
	        {
	            addReservationMenuEdit(menu, parent, p, (Reservation)obj, list, getStartDateForNewReservations(), afterId);
	        }
	        else if ( type.equals(User.TYPE) )
	        {
	            addUserMenuEdit( menu , parent, p, obj, list, afterId);
	        }
	        else if ( type.equals(Period.TYPE) )
	        {
	            addPeriodMenuEdit(  menu , parent, p, obj, list, afterId);
	        }
	        else if ( type.equals(Category.TYPE) )
	        {
	            addCategoryMenu(  menu , parent, p, obj, list, afterId);
	        }
	        else if ( type.equals(Conflict.TYPE) )
	        {
	            addConflictMenu(  menu , parent, p, obj, list, afterId);
	        }
	        if ( type.equals(DynamicType.TYPE))
	        {
	            if ( isAdmin())
	            {
	                addAction(menu,parent,p, afterId).setDeleteSelection( list );
	                addAction(menu,parent,p,afterId).setView(obj);
	                addAction(menu,parent,p, afterId).setEdit(obj);
	            }
	        }
        }

        @SuppressWarnings("unchecked")
		Iterator<ObjectMenuFactory> it = ((Collection<ObjectMenuFactory>)getContainer().lookupServicesFor( RaplaExtensionPoints.OBJECT_MENU_EXTENSION).values()).iterator();
        while (it.hasNext())
        {
            ObjectMenuFactory objectMenuFact = (ObjectMenuFactory) it.next();
            RaplaObject obj = focusedObject instanceof RaplaObject ? (RaplaObject) focusedObject : null;
  	        RaplaMenuItem[] items = objectMenuFact.create( context, obj);
            for ( int i =0;i<items.length;i++)
            {
                RaplaMenuItem item =  items[i];
                menu.insertAfterId( item, afterId);
            }
        }
        

        return menu;
    }

    private Date getStartDateForNewReservations() 
    {
        Date startDate = getModel().getSelectedDate();
        if ( startDate != null ) {
            Date time = new Date (DateTools.MILLISECONDS_PER_HOUR * getCalendarOptions().getWorktimeStart());
            startDate = getRaplaLocale().toDate(startDate,time);
        }
        return startDate;
    }

    private CalendarSelectionModel getModel() 
    {
        return getService( CalendarSelectionModel.class);
    }

    private void addAllocatableMenuEdit(MenuInterface menu,Component parent,Point p,Allocatable obj,Collection<?> selection, String id) throws RaplaException {
        addAction(menu,parent,p, id).setView(obj);
        addAction(menu,parent,p, id).setDeleteSelection(getDeletableObjects(selection));
        addAction(menu,parent,p, id).setEdit(obj);
    }

    private void addAllocatableMenuNew(MenuInterface menu,Component parent,Point p,Object focusedObj) throws RaplaException {
    	RaplaObjectAction newResource = addAction(menu,parent,p).setNew( Allocatable.TYPE );
    	if (focusedObj != CalendarSelectionModel.ALLOCATABLES_ROOT) 
    	{
	        if (focusedObj instanceof DynamicType) 
	        {
	        	if (((DynamicType) focusedObj).getAnnotation(DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE).equals(DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION)) 
	        	{
	        		newResource.setPerson(true);
	        	}
	        	newResource.changeObject( (DynamicType)focusedObj );
	        } 
	        if (focusedObj instanceof Allocatable) 
	        {
	        	if (((Allocatable) focusedObj).isPerson()) 
	        	{
	        		newResource.setPerson(true);
	        	}
	         	newResource.changeObject( (Allocatable)focusedObj );
	     	   
	        }
	        DynamicType[] types = newResource.guessTypes();
	        if (types.length == 1)	//user has clicked on a resource/person type
	        {
	            DynamicType type = types[0];
	            newResource.putValue(Action.NAME,type.getName( getLocale() ));
	            return;
	        }
    	} 
    	else 
    	{
	        //user has clicked on top "resources" folder : 
	        //add an entry to create a new resource and another to create a new person
	        DynamicType[] resourceType= getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION );
	        if ( resourceType.length == 1)
	        {
	            newResource.putValue(Action.NAME,resourceType[0].getName( getLocale() ));            
	        }
	        else
	        {
	            newResource.putValue(Action.NAME,getString("resource"));   
	        }
	
	        RaplaObjectAction newPerson = addAction(menu,parent,p).setNew( Allocatable.TYPE );
	        newPerson.setPerson( true );
	        DynamicType[] personType= getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION );
	        if ( personType.length == 1)
	        {
	            newPerson.putValue(Action.NAME,personType[0].getName( getLocale()));
	        }
	        else
	        {
	            newPerson.putValue(Action.NAME,getString("person"));
	        }
    	}
    }


    private void addReservationMenuEdit(MenuInterface menu,Component parent,Point p,Reservation obj,Collection<?> selection,Date startDate, String afterId) throws RaplaException
    {
        addAction(menu,parent,p, afterId).setDeleteSelection(getDeletableObjects(selection));
        addReservationAction(menu,parent,p, afterId).setView(obj);
        addReservationAction(menu,parent,p, afterId).setEdit(obj);
    }


    private void addTypeMenuNew(MenuInterface menu,String classificationType,Component parent,Point p,Object obj) throws RaplaException {
            DynamicTypeAction newReservationType = newDynamicTypeAction(parent,p);
            menu.add(new JMenuItem(newReservationType));
            newReservationType.setNewClassificationType(classificationType);
            newReservationType.putValue(Action.NAME,getString(classificationType + "_type"));
    }



    private void addUserMenuEdit(MenuInterface menu,Component parent,Point p,RaplaObject obj,Collection<?> selection, String afterId) throws RaplaException {
        addAction(menu,parent,p, afterId).setView(obj);
        addAction(menu,parent,p, afterId).setDeleteSelection(getDeletableObjects(selection));
        addAction(menu,parent,p, afterId).setEdit(obj);
        menu.insertAfterId( new RaplaSeparator("sep1"), afterId);
        menu.insertAfterId( new RaplaSeparator("sep2"), afterId);
        PasswordChangeAction passwordChangeAction = new PasswordChangeAction(getContext(),parent,p);
        passwordChangeAction.changeObject( obj );
        menu.insertAfterId( new JMenuItem( passwordChangeAction ), "sep2");

        UserAction switchUserAction = newUserAction(parent,p);
        switchUserAction.setSwitchToUser();
        switchUserAction.changeObject( obj );
        menu.insertAfterId( new JMenuItem( switchUserAction ), "sep2");
    }

    private void addUserMenuNew(MenuInterface menu,Component parent,Point p,Object obj) throws RaplaException {
        UserAction newUserAction = newUserAction(parent,p);
        newUserAction.setNew();
        menu.add( new JMenuItem( newUserAction ));
    }

    private void addCategoryMenu(MenuInterface menu,Component parent,Point p,RaplaObject obj,Collection<?> selection, String afterId) throws RaplaException {
        addAction(menu,parent,p, afterId).setEdit(obj);
        addAction(menu,parent,p, afterId).setDeleteSelection(getDeletableObjects(selection));
    }
    
    private void addCategoryMenuNew(MenuInterface menu, Component parent, Point p, Object obj, Collection<?> selection) throws RaplaException {
    	RaplaObjectAction newAction = addAction(menu,parent,p).setNew( Category.TYPE );
    	if ( obj instanceof Category)
    	{
    		newAction.changeObject((Category)obj);
    	}
    	else if ( obj != null &&  obj.equals( getString("categories")))
    	{
    		newAction.changeObject(getQuery().getSuperCategory());
    	}
      	newAction.putValue(Action.NAME,getString("category"));
    }

    private void addConflictMenu(MenuInterface menu,Component parent,Point p,RaplaObject obj,Collection<?> selection, String afterId) throws RaplaException {
        addReservationAction(menu,parent,p, afterId).setEdit( ((Conflict)obj).getReservation1());
        addAction(menu,parent,p, afterId).setView(obj);
    }

    private void addPeriodMenuEdit(MenuInterface menu, Component parent, Point p, RaplaObject obj, Collection<?> selection, String afterId) throws RaplaException {
        addAction(menu,parent,p, afterId).setEdit(obj);
        addAction(menu,parent,p, afterId).setView(obj);
        addAction(menu,parent,p, afterId).setDeleteSelection(getDeletableObjects(selection));
    }

    private void addPeriodMenuNew(MenuInterface menu, Component parent, Point p) throws RaplaException {
        Action newAction = addAction(menu,parent,p).setNew( Period.TYPE );
        newAction.putValue(Action.NAME,getString("period"));

    }

    private RaplaObjectAction addAction(MenuInterface menu, Component parent,Point p) throws RaplaException {
        RaplaObjectAction action = newObjectAction(parent,p);
        menu.add(new JMenuItem(action));
        return action;
    }

    private RaplaObjectAction addAction(MenuInterface menu, Component parent,Point p,String id) throws RaplaException {
        RaplaObjectAction action = newObjectAction(parent,p);
        menu.insertAfterId( new JMenuItem(action), id);
        return action;
    }


    private ReservationAction addReservationAction(MenuInterface menu, Component parent, Point p,String afterId) throws RaplaException {
        ReservationAction action = new ReservationAction(getContext(),parent,p);
        menu.insertAfterId(new JMenuItem(action), afterId);
        return action;
    }

    private RaplaObjectAction newObjectAction(Component parent,Point point) throws RaplaException {
        RaplaObjectAction action = new RaplaObjectAction(getContext(),parent);
        return action;
    }


    private DynamicTypeAction newDynamicTypeAction(Component parent,Point point) throws RaplaException {
        DynamicTypeAction action = new DynamicTypeAction(getContext(),parent,point);
        return action;
    }



    private UserAction newUserAction(Component parent,Point point) throws RaplaException {
        UserAction action = new UserAction(getContext(),parent,point);
        return action;
    }


    // This will exclude DynamicTypes and non editable Objects from the list
    private List<RaplaObject> getDeletableObjects(Collection<?> list) {
        Iterator<?> it = list.iterator();
        ArrayList<RaplaObject> editableObjects = new ArrayList<RaplaObject>();
        while (it.hasNext()) {
            Object o = it.next();
            if (canModify(o) )
                editableObjects.add((RaplaObject)o);
        }
        return editableObjects;
    }


}









