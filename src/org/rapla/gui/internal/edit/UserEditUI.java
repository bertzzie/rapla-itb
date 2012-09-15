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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RaplaTree;

/****************************************************************
 * This is the controller-class for the User-Edit-Panel         *
 ****************************************************************/

/*User
  1. username, string
  2. name,string
  3. email,string,
  4. isadmin,boolean
*/

class UserEditUI  extends AbstractEditUI {
	
    /**
     * @param sm
     * @throws RaplaException
     */
    public UserEditUI(RaplaContext sm) throws RaplaException {
        super(sm);
        EditField[] fields = new EditField[] {
            new TextField(sm,"username")
        //    ,new BooleanField(sm,"maptoperson")
            ,new PersonSelectField(sm)
            ,new TextField(sm,"name")
            ,new TextField(sm,"email")
            ,new BooleanField(sm,"admin")
            ,new GroupListField(sm)
        };
        setFields(fields);
    }

    class PersonSelectField extends AbstractEditField implements ChangeListener, ActionListener {
        User user;
       
        JPanel panel = new JPanel();
        JToolBar toolbar = new JToolBar();

        RaplaButton newButton  = new RaplaButton(RaplaButton.SMALL);
        RaplaButton removeButton  = new RaplaButton(RaplaButton.SMALL);
        
        /**
         * @param sm
         * @throws RaplaException
         */
        public PersonSelectField(RaplaContext sm) throws RaplaException {
            super(sm);
            final Category rootCategory = getQuery().getUserGroupsCategory();
            if ( rootCategory == null )
                return;
            toolbar.add( newButton  );
            toolbar.add( removeButton  );
            toolbar.setFloatable( false );
            panel.setLayout( new BorderLayout() );
            panel.add( toolbar, BorderLayout.NORTH );
            newButton.addActionListener( this );
            removeButton.addActionListener( this );
            removeButton.setText( getString("remove") );
            removeButton.setIcon( getIcon( "icon.remove" ) );
            newButton.setText( getString("add") );
            newButton.setIcon( getIcon( "icon.new" ) );

        }

        private void updateButton() {
            final boolean personSet = user != null && user.getPerson() != null;
            removeButton.setEnabled( personSet) ;
            newButton.setEnabled( !personSet) ;
            
            fields[2].getComponent().setEnabled( !personSet);
            fields[3].getComponent().setEnabled( !personSet);
      
        }

        public JComponent getComponent() {
            return panel;
        }

        public boolean isBlock() {
            return false;
        }

        public boolean isVariableSized() {
            return false;
        }

        public String getName()
        {
            return getString("bind_with_person");
        }
        public void mapFrom(Object o) throws RaplaException {
            user = (User) o;
            updateButton();
            
        }

        protected Object getValue() {
            return null;
        }

        protected void setValue(Object object) {
        }

        public void mapTo(Object o) throws RaplaException {
        }

        public void actionPerformed(ActionEvent evt) {
            if ( evt.getSource() ==  newButton)
            {
                    try {
                        showAddDialog();
                    } catch (RaplaException ex) {
                        showException(ex,newButton);
                    }
            }
            
            if ( evt.getSource() ==  removeButton)
            {
                user.setPerson( null );
                user.setEmail( null );
                user.setName(null);
                try {
					fields[2].mapFrom( user);
					fields[3].mapFrom( user);
				} catch (RaplaException e) {
					getLogger().error(e.getMessage(), e);
				}
            
                updateButton();
                
            }
               
        }

        public void stateChanged(ChangeEvent evt) {
        }
        
        private void showAddDialog() throws RaplaException {
            final DialogUI dialog;
            RaplaTree treeSelection = new RaplaTree();
            treeSelection.setMultiSelect(true);
            final TreeFactory treeFactory = getTreeFactory();
            treeSelection.getTree().setCellRenderer(treeFactory.createRenderer());

            final DynamicType[] personTypes = getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION);
            List<ClassificationFilter> filters = new ArrayList<ClassificationFilter>();
            for (DynamicType personType: personTypes)
            {
                if ( personType.getAttribute("email") != null)
                {
                    final ClassificationFilter filter = personType.newClassificationFilter();
                    filters.add( filter);
                }
            }
            final Allocatable[] allocatables = getQuery().getAllocatables(filters.toArray(ClassificationFilter.CLASSIFICATIONFILTER_ARRAY));
            List<Classifiable> allocatablesWithEmail = new ArrayList<Classifiable>();
            for ( Allocatable a: allocatables)
            {
                final Classification classification = a.getClassification();
                final Attribute attribute = classification.getAttribute("email");
                if ( attribute != null)
                {
	                final String email = (String)classification.getValue(attribute);
	                if (email != null && email.length() > 0)
	                {
	                    allocatablesWithEmail.add( a );
	                }
                }
            }
            final Classifiable[] allocatableArray = allocatablesWithEmail.toArray(Classifiable.CLASSIFIABLE_ARRAY);
            treeSelection.exchangeTreeModel(treeFactory.createClassifiableModel(allocatableArray));
            treeSelection.setMinimumSize(new java.awt.Dimension(300, 200));
            treeSelection.setPreferredSize(new java.awt.Dimension(400, 260));
            
           
            dialog = DialogUI.create(
                    getContext()
                    ,getComponent()
                    ,true
                    ,treeSelection
                    ,new String[] { getString("apply"),getString("cancel")});
            final JTree tree = treeSelection.getTree();
            tree.addMouseListener(new MouseAdapter() {
                // End dialog when a leaf is double clicked
                public void mousePressed(MouseEvent e) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    if (selPath != null && e.getClickCount() == 2) {
                        final Object lastPathComponent = selPath.getLastPathComponent();
                        if (((TreeNode) lastPathComponent).isLeaf() )
                            dialog.getButton(0).doClick();
                        }
                    else if (selPath != null && e.getClickCount() == 1) {
                        final Object lastPathComponent = selPath.getLastPathComponent();
                        if (((TreeNode) lastPathComponent).isLeaf() ) 
                            dialog.getButton(0).setEnabled(true);
                        else
                            dialog.getButton(0).setEnabled(false);                       
                    }
                }
            });
            dialog.setTitle(getName());
            dialog.start();
            if (dialog.getSelectedIndex() == 0) {
                Iterator<?> it = treeSelection.getSelectedElements().iterator();
                while (it.hasNext()) {
                    user.setPerson((Allocatable) it.next());
                    fields[2].mapFrom( user);
                    fields[3].mapFrom( user);
                    updateButton();
                }
            }
        }
    }
    
    final private TreeFactory getTreeFactory() {
        return getService(TreeFactory.class);
    }

}
