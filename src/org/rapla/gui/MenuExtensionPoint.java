package org.rapla.gui;

import javax.swing.JMenuItem;
import javax.swing.JSeparator;

public interface MenuExtensionPoint
{
    public void insert(JMenuItem item);
    public void insert(JSeparator seperator);
}
