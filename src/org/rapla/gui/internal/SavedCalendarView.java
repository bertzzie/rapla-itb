package org.rapla.gui.internal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.CalendarModelConfiguration;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaAction;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.common.CalendarModelImpl;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.InternMenus;
import org.rapla.gui.internal.common.MultiCalendarView;
import org.rapla.gui.internal.splitpanes.RaplaFiltrableSelectionPane;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.plugin.abstractcalendar.AbstractHTMLCalendarPage;
import org.rapla.plugin.autoexport.AutoExportPlugin;

public class SavedCalendarView extends RaplaGUIComponent {

    JComboBox selectionBox;
    
   private boolean listenersEnabled = true;
    List<String> filenames = new ArrayList<String>();
    final MultiCalendarView calendarContainer;
    final CalendarSelectionModel model;
    final RaplaFiltrableSelectionPane resourceSelection; 
    JToolBar toolbar = new JToolBar();
    class SaveAction extends RaplaAction
    {

        public SaveAction(RaplaContext sm) throws RaplaException {
            super(sm);
            final String name = getString("save") ;
            putValue(NAME,name);
            putValue(SHORT_DESCRIPTION,name);
            putValue(SMALL_ICON,getIcon("icon.save"));
        }

        public void actionPerformed(ActionEvent arg0) {
            save();
        }
    }
    
    class PublishAction extends RaplaAction
    {
        PublishDialog publishDialog;
        public PublishAction(RaplaContext sm) throws RaplaException {
            super(sm);
            final String name = getString("publish") ;
            putValue(NAME,name);
            putValue(SHORT_DESCRIPTION,name);
            putValue(SMALL_ICON,getIcon("icon.export"));
            publishDialog = new PublishDialog(getContext());
            
        }

        public void actionPerformed(ActionEvent arg0) {
            try 
            {   
				CalendarSelectionModel model = getService( CalendarSelectionModel.class);
				String filename = getSelectedFile();
                Component parentComponent = getMainComponent();
                publishDialog.export(model, parentComponent, filename);
            }
            catch (RaplaException ex) {
                showException( ex, getMainComponent());
            }
        }
        
        public boolean hasPublishActions()
        {
            return publishDialog.hasPublishActions();
        }
        
    }
    
    class DeleteAction extends RaplaAction
    {
        public DeleteAction(RaplaContext sm) throws RaplaException {
            super(sm);
            final String name = getString("delete");
            putValue(NAME,name);
            putValue(SHORT_DESCRIPTION,name);
            putValue(SMALL_ICON,getIcon("icon.delete"));
            
        }

        public void actionPerformed(ActionEvent arg0) {
            try 
            {
                String[] objects = new String[] { getSelectedFile()};
                DialogUI dlg = getInfoFactory().createDeleteDialog( objects, getMainComponent());
                dlg.start();
                if (dlg.getSelectedIndex() != 0)
                    return;
                delete();
            }
            catch (RaplaException ex) {
                showException( ex, getMainComponent());
            }
        }
        
       
        
    }
    
    final SaveAction saveAction;
    final PublishAction publishAction;
    final DeleteAction deleteAction;
    
    
    public SavedCalendarView(RaplaContext context, final MultiCalendarView calendarContainer, final RaplaFiltrableSelectionPane resourceSelection, final CalendarSelectionModel model) throws RaplaException {
        super(context);
        // I18nBundle i18n = getI18n();
        saveAction = new SaveAction(context);
        publishAction = new PublishAction(context);
        deleteAction = new DeleteAction( context);
        this.model = model;
        this.calendarContainer = calendarContainer;
        this.resourceSelection = resourceSelection;
        JButton save = new JButton();
        JButton publish = new JButton();
        JButton delete = new JButton();
        
       
        toolbar.setFloatable( false);
        selectionBox = new JComboBox();
        selectionBox.setToolTipText( getString("calendar"));
        selectionBox.setMinimumSize( new Dimension(120,30));
        selectionBox.setSize( new Dimension(150,30));
        // rku: updated, the next line prevented resizing the combobox when using the divider of the splitpane
        // especially, when having long filenames this is annoying

        //selectionBox.setMaximumSize( new Dimension(200,30));
        selectionBox.setPreferredSize( new Dimension(150,30));
        
        save.setAction( saveAction);
        publish.setAction(publishAction);
        RaplaMenu settingsMenu = (RaplaMenu)getService(InternMenus.CALENDAR_SETTINGS);
        settingsMenu.insertAfterId(new JMenuItem(saveAction), null);
        if ( publishAction.hasPublishActions())
        {
            settingsMenu.insertAfterId(new JMenuItem(publishAction), null);
        }
        settingsMenu.insertAfterId(new JMenuItem(deleteAction),null);
       // exportMenu.insertAfterId(new JSeparator(), InternMenus.NEW_MENU_ROLE);
        // exportMenu.add(publishAction);
        
        delete.setAction( deleteAction);
       // toolbar.add(new JLabel(getString("calendar")));
       // toolbar.add(new JToolBar.Separator());
        toolbar.add( selectionBox);
        toolbar.add(new JToolBar.Separator());
        
        toolbar.add(save);
        save.setText("");
        publish.setText("");
        delete.setText("");
        if ( publishAction.hasPublishActions())
        {
            toolbar.add(publish);
        }
        toolbar.add(delete);
        toolbar.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        update();
        
        selectionBox.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				Object selectedItem = selectionBox.getSelectedItem();
				if ( selectedItem != null )
				{
					selectionBox.setToolTipText(selectedItem.toString());
				}
			}
		});
        final int defaultIndex = ((DefaultComboBoxModel) selectionBox.getModel()).getIndexOf(getString("default"));
        if (defaultIndex != -1)
        	selectionBox.setSelectedIndex(defaultIndex); 
        else
        	selectionBox.setSelectedIndex(0); 
        selectionBox.addActionListener( new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                if ( !listenersEnabled)
                {
                    return;
                }
                try 
                {
                    changeSelection();
                }
                catch (RaplaException ex) {
                    showException( ex, getMainComponent());
                }
            }
        });
    }
 
    public JComponent getComponent() {
        return toolbar;
    }
    
    private void changeSelection() throws RaplaException
    {
        String selectedFile = getSelectedFile();

        // keep current date   in mind
        final Date tmpDate = model.getSelectedDate();
        // keep in mind if current model had saved date
      
        String tmpModelHasStoredCalenderDate = (String)model.getOption(AbstractHTMLCalendarPage.SAVE_SELECTED_DATE);
        if(tmpModelHasStoredCalenderDate == null)
        	tmpModelHasStoredCalenderDate = "false"; 
        // load sets stored date
        model.load(selectedFile);
        closeFilter();
        // check if new model had stored date
        String newModelHasStoredCalenderDate = (String)model.getOption(AbstractHTMLCalendarPage.SAVE_SELECTED_DATE);
        if(newModelHasStoredCalenderDate == null)
        	newModelHasStoredCalenderDate = "false"; 
        if ("false".equals(newModelHasStoredCalenderDate)) {

            if ("false".equals(tmpModelHasStoredCalenderDate))
                    // if we are switching from a model with saved date to a model without date we reset to current date
            {
               model.setSelectedDate(tmpDate);
            } else {
               model.setSelectedDate(new Date());
            }
        }
            
        updateActions();
        resourceSelection.dataChanged(null);
        calendarContainer.update();
    }

	public void closeFilter() {
		// CKO Not a good solution. FilterDialogs should close themselfs when model changes.
        // BJO 00000139
        if(resourceSelection.getFilterButton().isOpen())
        	resourceSelection.getFilterButton().doClick();
        if(calendarContainer.getFilterButton().isOpen())
        	calendarContainer.getFilterButton().doClick();
    
        // BJO 00000139
	}
   
    public void update() throws RaplaException
    {
        updateActions();
        try
        {
            listenersEnabled = false;
             final String item = getSelectedFile();
            final Preferences preferences = getQuery().getPreferences();
            Map<String,Object> exportMap= preferences.getEntry(AutoExportPlugin.PLUGIN_ENTRY);
            filenames.clear();
            final String defaultEntry = getString("default");
           
            filenames.add(defaultEntry);
            if ( exportMap != null) {
                for (Iterator<String> it= exportMap.keySet().iterator();it.hasNext();) {
                    String filename = it.next();
                    filenames.add( filename);
                }
            }
            // rku: sort entries by name
            Collections.sort(filenames);
            DefaultComboBoxModel model = new DefaultComboBoxModel(filenames.toArray());
            selectionBox.setModel(model);
           
            if ( item != null )
            {
                model.setSelectedItem( item );
            }
        }
        finally
        {
            listenersEnabled = true;
        }
    
    }

    private void updateActions() {
        String selectedFile = getSelectedFile();
        boolean isDefault = selectedFile == null || selectedFile.equals(getString("default"));
        final boolean modifyPreferencesAllowed = isModifyPreferencesAllowed();
        saveAction.setEnabled(modifyPreferencesAllowed );
        publishAction.setEnabled( modifyPreferencesAllowed);
        deleteAction.setEnabled( !isDefault && modifyPreferencesAllowed);
    }
    
    private void delete() throws RaplaException
    {
        final Preferences preferences =  newEditablePreferences();
        Map<String,CalendarModelConfiguration> exportMap= preferences.getEntry(AutoExportPlugin.PLUGIN_ENTRY);
        Map<String,CalendarModelConfiguration> newMap = new TreeMap<String,CalendarModelConfiguration>();
        final String selectedFile = getSelectedFile();
        for (Iterator<String> it= exportMap.keySet().iterator();it.hasNext();) {
            String filename = it.next();
            if (!filename.equals( selectedFile)) {
            	CalendarModelConfiguration entry = exportMap.get( filename );
				newMap.put( filename, entry);
            }
        }
        preferences.putEntry( AutoExportPlugin.PLUGIN_ENTRY, getModification().newRaplaMap( newMap ));
        getModification().store( preferences);
        final int defaultIndex = ((DefaultComboBoxModel) selectionBox.getModel()).getIndexOf(getString("default"));
        if (defaultIndex != -1)
            selectionBox.setSelectedIndex(defaultIndex);
        else
        	selectionBox.setSelectedIndex(0);
        //changeSelection();
    }

    private void save() {
        final String selectedFile = getSelectedFile();
        
        final Component parentComponent = getMainComponent();
        try {
            
   
        JPanel panel = new JPanel();
        final JTextField textField = new JTextField(20);
        addCopyPaste( textField);
        String dateString = getRaplaLocale().formatDate(model.getSelectedDate());
        final JCheckBox saveSelectedDateField = new JCheckBox(getI18n().format("including_date",dateString));
        
        panel.setPreferredSize( new Dimension(600,300));
        panel.setLayout(new TableLayout( new double[][] {{TableLayout.PREFERRED,5,TableLayout.FILL},{TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.FILL}}));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(new JLabel(getString("file.enter_name") +":"), "0,0");
        panel.add(textField, "2,0");
        addCopyPaste( textField);
        panel.add(saveSelectedDateField, "2,2");
        
        final String entry = model.getOption(AbstractHTMLCalendarPage.SAVE_SELECTED_DATE);
        if(entry != null)
        	saveSelectedDateField.setSelected(entry.equals("true")); 

        final JList list = new JList(filenames.toArray());

        panel.add( new JScrollPane(list), "0,4,2,1");
      
        textField.setText( selectedFile);
        list.addListSelectionListener( new ListSelectionListener() {

            public void valueChanged( ListSelectionEvent e )
            {
                String filename = (String) list.getSelectedValue();
                if ( filename != null) {
                    textField.setText( filename );
                    try {
                        final CalendarSelectionModel m = new CalendarModelImpl(getContext(),  getUser());
                        m.load(filename);
                        final String entry = m.getOption(AbstractHTMLCalendarPage.SAVE_SELECTED_DATE);
                        if( entry != null)
                        	saveSelectedDateField.setSelected(entry.equals("true"));
                    } catch (RaplaException ex) {
                           showException( ex, getMainComponent());
                    }
                }
              
            }

        });
        
        final DialogUI dlg = DialogUI.create(
                getContext(),
                                        parentComponent,true,panel,
                                       new String[] {
                                           getString("save")
                                           ,getString("cancel")
                                       });
        dlg.setTitle(getString("save") + " " +getString("calendar_settings"));
        dlg.getButton(0).setIcon(getIcon("icon.save"));
        dlg.getButton(1).setIcon(getIcon("icon.cancel"));
        dlg.getButton(0).setAction( new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                String filename = textField.getText().trim();
                if (filename.length() == 0)
                {
                    showWarning(getString("error.no_name"), parentComponent);
                    return;
                }
                dlg.close();
               
                
                try 
                {
                    String saveSelectedDate = saveSelectedDateField.isSelected() ? "true" : "false";
                    model.setOption( AbstractHTMLCalendarPage.SAVE_SELECTED_DATE, saveSelectedDate);
                    
                    final ComboBoxModel selectionModel = selectionBox.getModel();
                    
                    // We reset exports for newly created files
                    if ( !contains(selectionModel,filename))
                    {
                        model.resetExports();
                    }
                    // BJO 
                    // CKO Many want the title empty by default. 
//                    if(model.getTitle() == null || model.getTitle().length() == 0)
//                    	model.setTitle(filename);
                    // BJO
                    model.save(filename);
                    try
                    {
                        listenersEnabled = false;
                        selectionModel.setSelectedItem( filename);
                    }
                    finally
                    {
                        listenersEnabled = true;
                    }
                }
                catch (RaplaException ex) 
                {
                    showException( ex, parentComponent);
                }
                
            }

            private boolean contains(ComboBoxModel model2,String filename) 
            {
                for ( int i=0;i<model2.getSize();i++)
                {
                    final Object elementAt = model2.getElementAt( i);
                    if ( elementAt.equals(filename))
                    {
                        return true;
                    }
                }
                return false;
            }



        });
        dlg.start();
        } catch (RaplaException ex) {
            showException( ex, parentComponent);
        }

    }
    
    private String getSelectedFile() {
        return (String)selectionBox.getModel().getSelectedItem();
    }

    
}
