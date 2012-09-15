package org.rapla.gui;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JComponent;

import org.rapla.framework.RaplaException;
import org.rapla.gui.toolkit.DialogUI;

public interface InfoFactory
{
    String ROLE = InfoFactory.class.getName();

    JComponent createInfoComponent( Object object ) throws RaplaException;

    /** same as getToolTip(obj, true) */
    String getToolTip( Object obj );

    /** @param wrapHtml wraps an html Page arround the tooltip */
    String getToolTip( Object obj, boolean wrapHtml );

    void showInfoDialog( Object object, Component owner ) throws RaplaException;

    void showInfoDialog( Object object, Component owner, Point point ) throws RaplaException;

    DialogUI createDeleteDialog( Object[] deletables, Component owner ) throws RaplaException;

}