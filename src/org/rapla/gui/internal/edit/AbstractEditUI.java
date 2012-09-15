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
package org.rapla.gui.internal.edit;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComponent;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.EditComponent;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.RaplaWidget;
import org.rapla.components.layout.TableLayout;

/** 
 */
public abstract class AbstractEditUI extends RaplaGUIComponent
implements
    EditComponent
    ,ChangeListener
    ,RaplaWidget
{

    protected JPanel editPanel = new JPanel();
    protected Object o;
    protected EditField[] fields = new EditField[0];

    public AbstractEditUI(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    protected void setFields(EditField[] fields) {
        for (int i=0;i<fields.length;i++) {
            fields[i].removeChangeListener(this);
        }
        this.fields = fields;
        for (int i=0;i<fields.length;i++) {
            fields[i].addChangeListener(this);
        }
        editPanel.removeAll();
        layout();
        editPanel.revalidate();
    }

    protected void layout() {
        TableLayout tableLayout = new TableLayout();
        editPanel.setLayout(tableLayout);
        tableLayout.insertColumn(0,5);
        tableLayout.insertColumn(1,TableLayout.PREFERRED);
        tableLayout.insertColumn(2,5);
        tableLayout.insertColumn(3,TableLayout.FILL);
        tableLayout.insertColumn(4,5);
        int variableSizedBlocks = 0;
        for (int i=0;i<fields.length;i++)
            if (fields[i].isVariableSized())
                variableSizedBlocks ++;

        int row = 0;
        for (int i=0;i<fields.length;i++) {
            tableLayout.insertRow(row,5);
            row ++;
            if (fields[i].isVariableSized())
                tableLayout.insertRow(row,0.99 / ((double) variableSizedBlocks));
            else
                tableLayout.insertRow(row,TableLayout.PREFERRED);
            if (fields[i].isBlock()) {
                editPanel.add("1," + row + ",3," + row+",l", fields[i].getComponent());
            } else {
                editPanel.add("1," + row +",l,c", new JLabel(fields[i].getName() + ":"));
                editPanel.add("3," + row +",l,c", fields[i].getComponent());
            }
            row ++;
        }
        if (variableSizedBlocks == 0) {
            tableLayout.insertRow(row,TableLayout.FILL);
            editPanel.add("0," + row + ",4," + row ,new JLabel(""));
        }
    }

    public void setObject(Object o) throws RaplaException {
        this.o = o;
        for (int i=0;i<fields.length;i++) {
            fields[i].mapFrom(o);
        }
    }

    public Object getObject() {
        return o;
    }

    public boolean isBlock() {
        return false;
    }

    public JComponent getComponent() {
        return editPanel;
    }

    public void mapToObject() throws RaplaException {
        if (o == null)
            return;
        for (int i=0;i<fields.length;i++) {
            fields[i].mapTo(o);
        }
    }

    public void stateChanged(ChangeEvent evt) {
    }

}

