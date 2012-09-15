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

public interface FocusAdapter {
    boolean isFocusOwner();
    boolean isFocusable();
    void setFocusable(boolean bFocusable);
    void transferFocusUpCycle();
    void transferFocusBackward();
    boolean requestFocusInWindow();
    void ignoreFocusComponents(FocusTester focusTester);
    void setFocusCycleRoot(boolean bEnable);
}
