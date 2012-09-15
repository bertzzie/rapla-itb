package org.rapla.gui.toolkit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.apache.avalon.framework.container.ContainerUtil;

/** Disposes an object on window close. Must be added as a WindowListener
 to the target window*/
final public class DisposingTool extends WindowAdapter {
    Object m_objectToDispose;
    public DisposingTool(Object objectToDispose) {
        m_objectToDispose = objectToDispose;
    }
    public void windowClosed(WindowEvent e) {
        ContainerUtil.dispose(m_objectToDispose);
    }
}
