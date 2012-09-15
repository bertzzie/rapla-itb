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

import java.awt.Component;
import java.awt.Container;
import java.util.Observable;

/** This class functions as an adapter to JDK1.4 only objects.  Under
    JDK1.4 you can use the method AWTAdapterFactory.getFactory() to
    get an implementation of the AWTAdaptorFactory. Under JDK<1.4
    AWTAdapterFactory.getFactory() returns null.
 */
public class AWTAdapterFactory {
    // this must be the same as the package name for this class.
    private static String PACKAGEPREFIX = "org.rapla.components.calendar.jdk14adapter";

    /** Creates a MouseWheelObservable for the specified component. When
     the MouseWheel is moved all Observers will get notified. The value
    passed to the observers is the result of MouseWheelEvent.getWheelRotation().*/
    public Observable createMouseWheelObservable(Component component) {
        return null;
    };

    /** Creates a FocusAdapter for the specified component. */
    public FocusAdapter createFocusAdapter(Container container) {
        return null;
    }

    private static boolean bFactoryInitialized = false;
    private static AWTAdapterFactory factory = null;
    /** returns null if jdk <1.4 */
    public static AWTAdapterFactory getFactory() {
        try {
        // Wheel support only works since JDK 1.4
            if (factory == null && !bFactoryInitialized) {
                bFactoryInitialized = true;
                // Test if MouseWheelSupported
                Class.forName("java.awt.event.MouseWheelListener");
                Component.class.getMethod("isFocusable",new Class[] {});

                factory = (AWTAdapterFactory) Class.forName(PACKAGEPREFIX + ".AWTAdapterFactoryImpl").newInstance();
            }
            return factory;
        } catch (Exception ex) {
            return null;
        }
    }
}
