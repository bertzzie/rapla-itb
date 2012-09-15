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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Permission;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.toolkit.EmptyLineBorder;
/**
 *  @author Christopher Kohlhaas
 */
public class PermissionListField extends AbstractEditField
{
    JList permissionList = new JList();
    JPanel jPanel = new JPanel();
    PermissionField permissionField;
    private RaplaListEdit listEdit;
    Listener listener = new Listener();
    Allocatable allocatable;
    DefaultListModel model = new DefaultListModel();

    public PermissionListField( RaplaContext sm,String fieldName ) throws RaplaException {
        super( sm);
        permissionField = new PermissionField(sm,"permission");
        super.setFieldName(fieldName );
        jPanel.setLayout(new BorderLayout());
        listEdit = new RaplaListEdit( getI18n(), permissionField.getComponent(), listener);
        jPanel.add(listEdit.getComponent(),BorderLayout.CENTER);

        jPanel.setBorder(BorderFactory.createTitledBorder( new EmptyLineBorder(), getString("permissions")) );
        permissionField.addChangeListener( listener );
    }

    public JComponent getComponent() {
        return jPanel;
    }


    public boolean isBlock() {
        return true;
    }

    public boolean isVariableSized() {
        return false;
    }

    protected Object getValue() {
        return null;
    }

    protected void setValue(Object object) {
    }

    public void mapTo(Object o) throws RaplaException {
    }

    public void mapFrom(Object o) throws RaplaException {
        model.clear();
        allocatable = (Allocatable) o;
        Permission[] permissions = allocatable.getPermissions();;
        for (int i = 0; i < permissions.length; i++ ) {
            model.addElement(permissions[i]);
        }
        listEdit.setListDimension(new Dimension(210,90));
        listEdit.setMoveButtonVisible(false);
        listEdit.getList().setModel(model);
        listEdit.getList().setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

                public Component getListCellRendererComponent(JList list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected,
                                                              boolean cellHasFocus) {
                    Permission p = (Permission) value;
                    if (p.getUser() != null) {
                        value = getString("user") + " "
                            + p.getUser().getUsername();
                    } else if (p.getGroup() != null){
                        value = getString("group") + " "
                            + p.getGroup().getName(getI18n().getLocale());
                    } else {
                        value = getString("all_users");
                    }
                    value = (index + 1) +") " + value;
                    return super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
                }
            });
    }

    private void removePermission() {
        Object[] objects = listEdit.getList().getSelectedValues();
        for (int i=0;i<objects.length;i++) {
            Permission permission = (Permission) objects[i];
            if ( allocatable.getPermissions().length > 1 ) {
                model.removeElement(permission);
                allocatable.removePermission(permission);
            }
        }
        listEdit.getList().requestFocus();
    }

    private void createPermission() {
        Permission permission = allocatable.newPermission();
        allocatable.addPermission( permission );
        model.addElement(permission);
    }

    class Listener implements ActionListener, ChangeListener {
        public void actionPerformed(ActionEvent evt) {
            try {
                if (evt.getActionCommand().equals("remove")) {
                    removePermission();
                } else if (evt.getActionCommand().equals("new")) {
                    createPermission();
                } else if (evt.getActionCommand().equals("edit")) {
                    permissionField.setValue( listEdit.getList().getSelectedValue() );
                }
            } catch (RaplaException ex) {
                showException(ex,getComponent());
            }
        }

        public void stateChanged(ChangeEvent evt) {
            Object permission = permissionField.getValue();
            model.set( model.indexOf( permission ),permission );
        }

    }

}


