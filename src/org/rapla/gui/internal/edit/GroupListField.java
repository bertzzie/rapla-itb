package org.rapla.gui.internal.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.toolkit.RaplaButton;

public class GroupListField extends AbstractEditField implements ChangeListener, ActionListener {
    DefaultListModel model = new DefaultListModel();

    JPanel panel = new JPanel();
    JToolBar toolbar = new JToolBar();

    CategorySelectField newCategory;
    RaplaButton removeButton = new RaplaButton(RaplaButton.SMALL);
    RaplaButton newButton  = new RaplaButton(RaplaButton.SMALL);
    JList list = new JList();

    /**
     * @param sm
     * @throws RaplaException
     */
    public GroupListField(RaplaContext sm) throws RaplaException {
        super(sm);
    	final Category rootCategory = getQuery().getUserGroupsCategory();
        if ( rootCategory == null )
            return;
        newCategory = new CategorySelectField(sm,"group", rootCategory );
        newCategory.setUseNullCategory( false);
        toolbar.add( newButton  );
        toolbar.add( removeButton );
        toolbar.setFloatable( false );
        panel.setLayout( new BorderLayout() );
        panel.add( toolbar, BorderLayout.NORTH );
        final JScrollPane jScrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setPreferredSize( new Dimension( 300, 150 ) );
        panel.add( jScrollPane, BorderLayout.CENTER );
        newButton.setText( getString( "group" ) + " " + getString( "add" ) );
        removeButton.setText( getString( "group" ) + " " + getString( "remove" ) );
        newButton.setIcon( getIcon( "icon.new" ) );
        removeButton.setIcon( getIcon( "icon.remove" ) );
        newCategory.addChangeListener( this );
        newButton.addActionListener( this );
        removeButton.addActionListener( this );

        list.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus)
                {
                    if ( value != null ) {
                        Category category = (Category) value;
                        value = category.getPath( rootCategory
                                                  , getI18n().getLocale());
                    }
                    return super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
                }
            }
            );
    }

    public JComponent getComponent() {
        return panel;
    }

    public boolean isBlock() {
        return true;
    }

    public boolean isVariableSized() {
        return false;
    }

    public void mapFrom(Object o) throws RaplaException {
        Category[] groups = ((User)o).getGroups();
        mapFromList(Arrays.asList(groups));
    }

    public void mapToList(Collection<Category> groups) throws RaplaException {
    	groups.clear();
    	@SuppressWarnings("unchecked")
		Enumeration<Category> it = (Enumeration<Category>) model.elements();
    	while (it.hasMoreElements())
    	{
    		Category cat= it.nextElement();
    		groups.add( cat);
    	}
    }
    
	public void mapFromList(Collection<Category> groups) {
		model.clear();
        for ( Category cat:groups) {
            model.addElement( cat );
        }
        list.setModel(model);
	}

    protected Object getValue() {
        return null;
    }

    protected void setValue(Object object) {
    }

    public void mapTo(Object o) throws RaplaException {
    	User user = (User) o;
    	for (Category cat : user.getGroups())
    	{
    		if (!model.contains( cat))
    		{
    			user.removeGroup( cat);
    		}
    	}
    	@SuppressWarnings("unchecked")
		Enumeration<Category> it = (Enumeration<Category>) model.elements();
    	while (it.hasMoreElements())
    	{
    		Category cat= it.nextElement();
    		if ( !user.belongsTo( cat))
    		{
    			user.addGroup( cat);
    		}
    	}
    }
    
    
    public void actionPerformed(ActionEvent evt) {
        if ( evt.getSource() ==  newButton)
        {
            try {
                newCategory.showDialog(newButton);
            } catch (RaplaException ex) {
                showException(ex,newButton);
            }
        }
        if ( evt.getSource() ==  removeButton)
        {
            for ( Object value: list.getSelectedValues())
            {
                Category group = (Category) value;
                if (group != null) {
                    model.removeElement( group );
                    fireContentChanged();
                }
            }
        }
        
    }

    
	public void stateChanged(ChangeEvent evt) {
        Category newGroup = (Category) newCategory.getValue();
        if ( ! model.contains( newGroup ) ) {
            model.addElement( newGroup );
            fireContentChanged();
        }
    }


}