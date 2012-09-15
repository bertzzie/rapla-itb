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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.avalon.framework.activity.Disposable;
import org.rapla.entities.Entity;
import org.rapla.entities.RaplaObject;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.EditComponent;
import org.rapla.gui.EditController;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.DisposingTool;

public class EditDialog extends RaplaGUIComponent implements ModificationListener,Disposable {
    DialogUI dlg;
    boolean bSaving = false;
    EditComponent ui;
    boolean modal;

    public EditDialog(RaplaContext sm,EditComponent ui,boolean modal) throws RaplaException {
        super( sm);
        this.ui = ui;
        this.modal = modal;
    }

    public EditDialog(RaplaContext sm,EditComponent ui) throws RaplaException {
        this(sm,ui,true);
    }

    final private EditControllerImpl getPrivateEditDialog() {
        return (EditControllerImpl) getService(EditController.ROLE);
    }

    public int start(Object editObj,String title,Component owner)
        throws
            RaplaException
    {
            getLogger().debug("Editing Object: " + editObj);
            ui.setObject(editObj);
            JComponent editComponent = ui.getComponent();
            JPanel panel = new JPanel();
            panel.setLayout( new BorderLayout());
            panel.add( editComponent, BorderLayout.CENTER);
            editComponent.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
            dlg = DialogUI.create(getContext(),owner,modal,panel,new String[] {
                getString("save")
                ,getString("cancel")
            });
            dlg.setAbortAction(new AbortAction());
            dlg.getButton(0).setAction(new SaveAction());
            dlg.getButton(1).setAction(new AbortAction());
            dlg.getButton(0).setIcon(getIcon("icon.save"));
            dlg.getButton(1).setIcon(getIcon("icon.cancel"));
            dlg.setTitle(getI18n().format("edit.format",title));
            getUpdateModule().addModificationListener(this);
            dlg.addWindowListener(new DisposingTool(this));
            dlg.start();
            if (modal) 
            {
                return dlg.getSelectedIndex();
            }
            else
            {
            	getPrivateEditDialog().addEditDialog( this );
            	return -1;
            }
    }

    protected boolean shouldCancelOnModification(ModificationEvent evt) {
        return evt.hasChanged((RaplaObject)ui.getObject());
    }

    public void dataChanged(ModificationEvent evt) throws RaplaException {
        if (bSaving || dlg == null || !dlg.isVisible() || ui == null)
            return;
        if (shouldCancelOnModification(evt)) {
            getLogger().warn("Object has been changed outside.");
            DialogUI warning =
                DialogUI.create(getContext()
                                ,ui.getComponent()
                                ,true
                                ,getString("warning")
                                ,getI18n().format("warning.update",ui.getObject())
                                );
            warning.start();
        	getPrivateEditDialog().removeEditDialog( this );
            dlg.close();
        }
    }

    class SaveAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent evt) {
            try {
                ui.mapToObject();
                bSaving = true;
                getModification().store( (Entity<?>) ui.getObject());
               	getPrivateEditDialog().removeEditDialog( EditDialog.this );
               	dlg.close();
            } catch (Exception ex) {
                showException(ex,dlg);
            }
        }
    }

    class AbortAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent evt) {
        	getPrivateEditDialog().removeEditDialog( EditDialog.this );
        	dlg.close();
        }
    }

    public void dispose() {
        getUpdateModule().removeModificationListener(this);
    }
}

