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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.entities.domain.Permission;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class PermissionField extends AbstractEditField implements  ChangeListener, ActionListener {
    EditField groupSelect;
    EditField userSelect;

    JPanel panel = new JPanel();
    JPanel reservationPanel;
    Permission permission;

    JComboBox startSelection = new JComboBox();
    JComboBox endSelection = new JComboBox();
    DateField startDate;
    DateField endDate;
    LongField minAdvance;
    LongField maxAdvance;

    ListField accessField;

    public PermissionField(RaplaContext sm,String fieldName) throws RaplaException {
        super( sm);
        setFieldName( fieldName );

        panel.setBorder(BorderFactory.createEmptyBorder(5,8,5,8));

        double pre =TableLayout.PREFERRED;
        double fill =TableLayout.FILL;
        panel.setLayout( new TableLayout( new double[][]
            {{fill, 5},  // Columns
             {pre,5,pre,5,pre}} // Rows
                                          ));

        JPanel userPanel = new JPanel();
        panel.add( userPanel , "0,0,f,f" );
        userPanel.setLayout( new TableLayout( new double[][]
            {{pre, 10, fill, 5},  // Columns
             {pre,5,pre,5,pre}} // Rows
                                          ));

        userSelect = new UserListField( sm,"user" );
        userPanel.add( new JLabel(getString("user") + ":"), "0,0,l,f" );
        userPanel.add( userSelect.getComponent(),"2,0,l,f" );

        Category rootCategory =
            getQuery().getUserGroupsCategory();
        if ( rootCategory != null) {
            if (rootCategory.getDepth() > 2) {
                groupSelect = new CategorySelectField(getContext(),"group", rootCategory);
            } else {
                groupSelect = new CategoryListField(getContext(),"group", rootCategory);
            }
            userPanel.add( new JLabel(getString("group") + ":"), "0,2,l,f" );
            userPanel.add( groupSelect.getComponent(),"2,2,l,f" );
            groupSelect.addChangeListener( this );
        }


        reservationPanel = new JPanel();
        panel.add( reservationPanel , "0,2,f,f" );
        reservationPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), getString("allocatable_in_timeframe") + ":" ));
        reservationPanel.setLayout( new TableLayout( new double[][]
            {{pre,3, pre, 5, pre, 5},  // Columns
             {pre, 5, pre}} // Rows
                                                     ));

        reservationPanel.add( new JLabel( getString("start_date") + ":" ) , "0,0,l,f" );
        reservationPanel.add( startSelection , "2,0,l,f" );
        startSelection.setModel( createSelectionModel() );
        startSelection.setSelectedIndex( 0 );

        startDate = new DateField(sm,"start");
        reservationPanel.add( startDate.getComponent() , "4,0,l,f" );

        minAdvance = new LongField(sm,"minAdvance", new Long(0) );
        reservationPanel.add( minAdvance.getComponent() , "4,0,l,f" );

        reservationPanel.add( new JLabel( getString("end_date") + ":" ), "0,2,l,f" );
        reservationPanel.add( endSelection , "2,2,l,f" );
        endSelection.setModel( createSelectionModel() );
        endSelection.setSelectedIndex( 0 );

        endDate = new DateField(sm,"end");
        reservationPanel.add( endDate.getComponent() , "4,2,l,f" );

        maxAdvance = new LongField(sm,"maxAdvance", new Long(1) );
        reservationPanel.add( maxAdvance.getComponent() , "4,2,l,f" );

        userPanel.add( new JLabel(getString("permission.access") + ":"), "0,4,f,f" );
        Vector<Integer> vector = new Vector<Integer>();
        int[] accessLevels = Permission.ACCESS_LEVEL_TYPES;
        for (int i=0;i<accessLevels.length;i++) {
            vector.add(new Integer( accessLevels[i] ) );
        }
        accessField = new ListField(sm,"accessLevel", vector );
        accessField.setRenderer( new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value != null) {
                   String key = (String) Permission.ACCESS_LEVEL_NAMEMAP.get( ((Integer) value).intValue() );
                   value = getI18n().getString("permission." + key );
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus );
            }}
        );
        
        userPanel.add( accessField.getComponent(), "2,4,f,f" );
       
        toggleVisibility();
        userSelect.addChangeListener( this );

        startSelection.addActionListener(this);
        minAdvance.addChangeListener(this);
        startDate.addChangeListener(this);

        endSelection.addActionListener(this);
        maxAdvance.addChangeListener(this);
        endDate.addChangeListener(this);

        accessField.addChangeListener(this);
        panel.revalidate();
    }

    public JComponent getComponent() {
        return panel;
    }


    private DefaultComboBoxModel createSelectionModel() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(getString( "open" ) );
        model.addElement(getString( "fixed_date") );
        model.addElement(getString( "x_days_advance") );
        return model;
    }


    private void toggleVisibility() {
        int level = ((Integer)accessField.getValue()).intValue();
        reservationPanel.setVisible( level >= Permission.ALLOCATE && level < Permission.ADMIN);
        
        int i = startSelection.getSelectedIndex();
        startDate.getComponent().setVisible( i == 1 );
        minAdvance.getComponent().setVisible( i == 2 );

        int j = endSelection.getSelectedIndex();
        endDate.getComponent().setVisible( j == 1 );
        maxAdvance.getComponent().setVisible( j == 2 );
    }

    boolean listenersDisabled = false;
    public void setValue(Object value) throws RaplaException {
        try {
            listenersDisabled = true;

            permission = (Permission) value;
            if ( groupSelect != null )
                groupSelect.mapFrom( permission );
            userSelect.mapFrom( permission );
            accessField.mapFrom( permission );

            int startIndex = 0;
            if ( permission.getStart() != null )
                startIndex = 1;
            if ( permission.getMinAdvance() != null )
                startIndex = 2;
            startSelection.setSelectedIndex( startIndex );

            int endIndex = 0;
            if ( permission.getEnd() != null )
                endIndex = 1;
            if ( permission.getMaxAdvance() != null )
                endIndex = 2;
            endSelection.setSelectedIndex( endIndex );

            startDate.mapFrom( permission );
            minAdvance.mapFrom( permission );
            endDate.mapFrom( permission );
            maxAdvance.mapFrom( permission );
            toggleVisibility();
        } finally {
            listenersDisabled = false;
        }
    }


    public void actionPerformed(ActionEvent evt) {
        if ( listenersDisabled )
            return;

        try {
          if (evt.getSource() == startSelection) {
              int i = startSelection.getSelectedIndex();
              if ( i == 0 ) {
                  permission.setStart( null );
                  permission.setMinAdvance( null );
              }
              if ( i == 1 ) {
                  Date today = getQuery().today();
                  permission.setStart( today );
                  startDate.mapFrom ( permission );
              } if ( i == 2 ) {
                  permission.setMinAdvance( new Long(0) );
                  minAdvance.mapFrom ( permission );
              }
          }
          if (evt.getSource() == endSelection) {
              int i = endSelection.getSelectedIndex();
              if ( i == 0 ) {
                  permission.setEnd( null );
                  permission.setMaxAdvance( null );
              }
              if ( i == 1 ) {
                  Date today = getQuery().today();
                  permission.setEnd( today );
                  endDate.mapFrom ( permission );
              } if ( i == 2 ) {
                  permission.setMaxAdvance( new Long( 30 ) );
                  maxAdvance.mapFrom ( permission );
              }
          }
          toggleVisibility();
          fireContentChanged();
        } catch ( RaplaException ex ) {
            showException( ex, getComponent() );
        }
    }

    public Object getValue() {
        return permission;
    }

    public void stateChanged(ChangeEvent evt) {
        if ( listenersDisabled )
            return;

        try {
            if (evt.getSource() == groupSelect) {
                groupSelect.mapTo( permission );
                userSelect.mapFrom(permission);
            } else if (evt.getSource() == userSelect) {
                userSelect.mapTo( permission );
                if ( groupSelect != null )
                    groupSelect.mapFrom(permission);
            } else if (evt.getSource() == startDate) {
                startDate.mapTo( permission );
            } else if (evt.getSource() == minAdvance) {
                minAdvance.mapTo( permission );
            } else if (evt.getSource() == endDate) {
                endDate.mapTo( permission );
            } else if (evt.getSource() == maxAdvance) {
                maxAdvance.mapTo( permission );
            } else if (evt.getSource() == accessField ) {
                accessField.mapTo( permission );
                toggleVisibility();
            }
            fireContentChanged();
        } catch (RaplaException ex) {
            showException( ex, getComponent() );
        }
    }

    class UserListField extends ListField {

        public UserListField(RaplaContext sm,String fieldName) throws RaplaException{
            super(sm,fieldName);
            User[] obj = getQuery().getUsers();
            Vector<Object> list = new Vector<Object>();
            list.add(nothingSelected);
            for (int i=0;i<obj.length;i++)
                list.add(obj[i]);
            setVector(list);
        }
    }

}


