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
package org.rapla.gui;
import java.awt.Component;
import java.awt.Point;
import java.util.Collection;
import java.util.Collections;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaDefaultContext;

public class MenuContext extends RaplaDefaultContext
{
    public static String PARENT_COMPONENT = "parent_component";
    public static String POPUP_POINT = "popup_point";
    public static String FOCUSED_OBJECT = "menu_object";
    /** a collection of selected objects */
    public static String SELECTED_OBJECTS = "selected_objects";

    public MenuContext(RaplaContext parentContext, Object focusedObject) {
        this( parentContext, focusedObject, null, null );
    }

    public MenuContext(RaplaContext parentContext,  Object focusedObject, Component parent,Point p) {
        super( parentContext);
        put( FOCUSED_OBJECT, focusedObject );
        put( PARENT_COMPONENT, parent );
        put( POPUP_POINT, p );
        put( SELECTED_OBJECTS, Collections.EMPTY_LIST );
    }

    public void setSelectedObjects(Collection<?> selectedObjects) {
        put ( SELECTED_OBJECTS, selectedObjects);
    }

    public Collection<?> getSelectedObjects() {
        return (Collection<?>) getUnsave( SELECTED_OBJECTS);
    }

    public Point getPoint() {
        return (Point) getUnsave( POPUP_POINT );

    }

    public Component getComponent() {
        return (Component) getUnsave( PARENT_COMPONENT );
    }

    public Object getFocusedObject() {
        return  getUnsave( FOCUSED_OBJECT );
    }


}









