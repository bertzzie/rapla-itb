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
package org.rapla.components.calendar.jdk14adapter;

import java.awt.*;
import java.awt.event.*;
import java.awt.Component;
import java.util.Observable;
import javax.swing.*;
/** This class will only compile with JDK >=1.4
    You can safely exclude it if you build with JDK <1.4
 */
class AWTAdapterFactoryImpl extends AWTAdapterFactory {
    public Observable createMouseWheelObservable(Component component) {
        return new MyWheelListener(component);
    }
    public FocusAdapter createFocusAdapter(Container container) {
        return new MyFocusAdapter(container);
    }
}

class MyWheelListener extends Observable implements MouseWheelListener {
    MyWheelListener(Component component) {
        component.addMouseWheelListener(this);
    }

    public void mouseWheelMoved(MouseWheelEvent evt) {
        setChanged();
        notifyObservers(new Long(evt.getWheelRotation()));
    }

}

class MyFocusAdapter implements FocusAdapter {
    Container c;
    MyFocusAdapter(Container container) {
        this.c = container;
    }
    public boolean isFocusOwner() {
        return c.isFocusOwner();
    }
    public boolean isFocusable() {
        return c.isFocusable();
    }
    public void setFocusCycleRoot(boolean bEnable) {
        c.setFocusCycleRoot(bEnable);
    }
    public void setFocusable(boolean bFocusable) {
        c.setFocusable(bFocusable);
    }
    public void transferFocusUpCycle() {
        c.transferFocusUpCycle();
    }
    public void transferFocusBackward() {
        c.transferFocusBackward();
    }
    public boolean requestFocusInWindow() {
        return c.requestFocusInWindow();
    }

    public void ignoreFocusComponents(FocusTester focusTester) {
        c.setFocusTraversalPolicy(new MyFocusTraversalPolicy(focusTester));
    }
}

class MyFocusTraversalPolicy extends LayoutFocusTraversalPolicy {
    private static final long serialVersionUID = 1L;

    FocusTester focusTester;
    MyFocusTraversalPolicy( FocusTester focusTester) {
        this.focusTester = focusTester;
    }

    protected boolean accept(Component component) {
        if (!focusTester.accept(component))
            return false;
        return super.accept(component);
    }
}

