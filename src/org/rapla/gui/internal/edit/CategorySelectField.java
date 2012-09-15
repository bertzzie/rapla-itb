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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.rapla.entities.Category;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RecursiveNode;

public class CategorySelectField extends AbstractEditField
{
    private RaplaButton selectButton = new RaplaButton(RaplaButton.SMALL);
    private Category selectedCategory;
    Category rootCategory;
    Category defaultCategory;

    private boolean useDefaultCategory = true;
   
    private boolean useNullCategory = true;

  
    public RaplaButton getButton() {
        return selectButton;
    }

    public CategorySelectField(RaplaContext sm,String fieldName,Category rootCategory) throws RaplaException {
       this( sm, fieldName, rootCategory, null);
    }
    
    public CategorySelectField(RaplaContext sm,String fieldName,Category rootCategory, Category defaultCategory) throws RaplaException {
        super( sm);
        useDefaultCategory = defaultCategory != null;
        setFieldName( fieldName );
        this.rootCategory = rootCategory;
        selectButton.setAction(new SelectionAction());
        selectButton.setHorizontalAlignment(RaplaButton.LEFT);
        selectButton.setText(getString("select"));
        selectButton.setIcon(getIcon("icon.tree"));
        this.defaultCategory = defaultCategory;
    }
    
   
    public boolean isUseNullCategory() {
        return useNullCategory;
    }

    public void setUseNullCategory(boolean useNullCategory) {
        this.useNullCategory = useNullCategory;
    }


  
    public class SelectionAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

      
        public void actionPerformed(ActionEvent evt) {
            try {
                showDialog(selectButton);
            } catch (RaplaException ex) {
                showException(ex,selectButton);
            }
        }

       
    }

    public Object getValue() {
        return selectedCategory;
    }

    final private TreeFactory getTreeFactory() {
        return getService(TreeFactory.class);
    }


    public void setValue(Object object) {
        String text;
        selectedCategory = (Category) object;
        if (selectedCategory != null) {
            text = selectedCategory.getPath(rootCategory,getI18n().getLocale());
        } else {
            text = getString("nothing_selected");
        }
        selectButton.setText(text);
    }
   
    public void showDialog(JComponent parent) throws RaplaException {
        final DialogUI dialog;
        final JTree tree;
        tree = new JTree();
        tree.setCellRenderer(getTreeFactory().createRenderer());
        //tree.setVisibleRowCount(15);
        tree.setRootVisible( false );
        tree.setShowsRootHandles(true);
        tree.setModel(getTreeFactory().createModel(rootCategory));
       

        selectCategory(tree,selectedCategory);
        JPanel panel = new JPanel();
        panel.setLayout( new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setMinimumSize(new Dimension(300, 200));
        scrollPane.setPreferredSize(new Dimension(400, 260));
        panel.add(scrollPane, BorderLayout.PAGE_START);
        
        if (useDefaultCategory)
        {
            JButton defaultButton = new JButton(getString("defaultselection"));
            panel.add( defaultButton,  BorderLayout.CENTER);
            defaultButton.setPreferredSize(new Dimension(100, 20));
            defaultButton.addActionListener( new ActionListener() {
                
                
                public void actionPerformed(ActionEvent arg0) 
                {
                    selectCategory( tree, defaultCategory);
                    
                }
                
            });
        }
        
        if (useNullCategory)
        {
            JButton emptyButton = new JButton(getString("nothing_selected"));
            panel.add( emptyButton, BorderLayout.PAGE_END);
            emptyButton.setPreferredSize(new Dimension(100, 20));
            emptyButton.addActionListener( new ActionListener() {
                
                
                public void actionPerformed(ActionEvent arg0) 
                {
                    selectCategory(tree, null );
                    
                }
                
            });
        }

        dialog = DialogUI.create(getContext()
                ,parent
                                 ,true
                                 ,panel
                                 ,new String[] { getString("apply"),getString("cancel")});
        
        tree.addMouseListener(new MouseAdapter() {
            // End dialog when a leaf is double clicked
            public void mousePressed(MouseEvent e) {
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selPath != null && e.getClickCount() == 2) {
                    RecursiveNode node = (RecursiveNode)selPath.getLastPathComponent();
                    if (node.isLeaf()) {
                        dialog.getButton(0).doClick();
                    }
                }
            }
        });
        dialog.setTitle(getName());
        dialog.start();
        if (dialog.getSelectedIndex() == 0) {
            Object newValue = null;
            TreePath path = tree.getSelectionPath();
            if (path != null) {
                newValue = ((RecursiveNode)path.getLastPathComponent()).getUserObject();
            }
            if (( newValue == null && selectedCategory != null)
                || ( newValue !=null && !newValue.equals(selectedCategory))
                ) {
                setValue( newValue );
                fireContentChanged();
            }
        }
    }

    private void selectCategory(JTree tree, Category category) {
        if ( category == null)
        {
            tree.setSelectionPath(null);
        }
        ArrayList<Category> path = new ArrayList<Category>();
        while (true) {
            if (category == null)
                return;
            if (category.equals(rootCategory))
                break;
            path.add(category);
            category = category.getParent();
        }
        RecursiveNode.selectUserObjects(tree,path.toArray());
       
    }
    public Category getRootCategory() 
    {
        return rootCategory;
    }

    public void setRootCategory(Category rootCategory) 
    {
        this.rootCategory = rootCategory;
    }

    public JComponent getComponent() {
        return selectButton;
    }

}


