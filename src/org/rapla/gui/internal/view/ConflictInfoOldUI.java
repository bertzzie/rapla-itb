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
package org.rapla.gui.internal.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.rapla.gui.toolkit.RaplaWidget;

public class ConflictInfoOldUI
    implements RaplaWidget
{
    JPanel content = new JPanel();
    JTable jTable1 = new JTable();
    private Action editAction = null;

    public ConflictInfoOldUI() {
        content.setLayout(new BorderLayout());
        jTable1.setPreferredScrollableViewportSize(new Dimension(400, 70));
        JScrollPane scrollPane = new JScrollPane(jTable1);
        content.add(scrollPane,BorderLayout.CENTER);

        jTable1.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    if(event.getClickCount() == 2 && editAction!=null) {
                        editAction.actionPerformed(new ActionEvent(ConflictInfoOldUI.this, ActionEvent.ACTION_PERFORMED, ""));
                    }
                }
            });
    }

    public JTable getTable() {
        return jTable1;
    }

    public void setEditAction(Action action) {
        editAction = action;
    }

    public JComponent getComponent() {
        return content;
    }

}



