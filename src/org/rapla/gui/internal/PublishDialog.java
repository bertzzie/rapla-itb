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
package org.rapla.gui.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.layout.TableLayout;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.plugin.abstractcalendar.AbstractHTMLCalendarPage;
import org.rapla.plugin.autoexport.AutoExportPlugin;
import org.rapla.plugin.export2ical.Export2iCalPlugin;
import org.rapla.plugin.urlencryption.UrlEncryption;
import org.rapla.plugin.urlencryption.UrlEncryptionPlugin;

/**
 */

public class PublishDialog extends RaplaGUIComponent
{
    String text;
    I18nBundle pluginI18n;
    boolean icalEnabled;
    boolean htmlEnabled;
    private boolean encryptionEnabled = false;
    private boolean encryptionAllowed = false;

    /*        availableURL = config.getChild("serverurl").getValue(null);
if (availableURL != null)
  availableURL = ConfigTools.resolveContext(availableURL,context);*/
    public PublishDialog(RaplaContext sm) throws RaplaException {
        super(sm);
        if ( !isModifyPreferencesAllowed() ) {
        	return;
        }
        icalEnabled =this.getContext().has(Export2iCalPlugin.PLUGIN_CLASS);
        htmlEnabled =this.getContext().has(AutoExportPlugin.PLUGIN_CLASS);
        if ( htmlEnabled)
        {
        	setChildBundleName( AutoExportPlugin.RESOURCE_FILE);
        }
        encryptionAllowed = this.getContext().has(UrlEncryptionPlugin.PLUGIN_CLASS);
    }
    
    public boolean hasPublishActions()
    {
        return icalEnabled || htmlEnabled;
    }

    String getAddress(String filename, String generator) {
        if ( filename == null) 
        {
            return "";
        }
        try 
        {
            StartupEnvironment env = getService( StartupEnvironment.class );
            URL codeBase = env.getDownloadURL();
            filename =URLEncoder.encode( filename, "UTF-8" );

            String urlExtension = "";

            // In case of enabled and activated URL encryption:
            if(this.encryptionEnabled && this.encryptionAllowed)
                urlExtension = UrlEncryption.ENCRYPTED_PARAMETER_NAME+"="+getWebservice(UrlEncryption.class).encrypt("page="+generator+"&user=" + getUser().getUsername() + "&file=" + filename);
            else
                urlExtension = "page="+generator+"&user=" + getUser().getUsername() + "&file=" + filename;

            return new URL( codeBase,"rapla?" + urlExtension).toExternalForm();
           // return new URL( codeBase,"rapla?page="+generator+"&user=" + getUser().getUsername() + "&file=" + filename).toExternalForm();
        } 
        catch (Exception ex)
        {
            return "Not in webstart mode. Exportname is " + filename  ;
        }
    }

    public void export(final CalendarSelectionModel model,final Component parentComponent,final String filename) throws RaplaException
    {
        {
            final String entry = (String)model.getOption(UrlEncryptionPlugin.URL_ENCRYPTION);

            this.encryptionEnabled = entry != null && entry.equalsIgnoreCase("true");
        }

        JPanel panel = new JPanel();
        final JTextField titleField = new JTextField(20);
        addCopyPaste(titleField);
        final JCheckBox showNavField = new JCheckBox();
        final JCheckBox saveSelectedDateField = new JCheckBox();
        final JTextField htmlURL = new JTextField();
        final JTextField icalURL = new JTextField();
        final JPanel statusHtml = createStatus(filename,"calendar", htmlURL);
        final JPanel statusICal = createStatus(filename,"iCal", icalURL);


        panel.setPreferredSize( new Dimension(600,300));

        panel.setLayout(new TableLayout( new double[][] {{TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.FILL},
                {TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,
                        TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,
                        TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,10, TableLayout.PREFERRED,5, TableLayout.PREFERRED
                }}));

        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        final JCheckBox htmlCheck = new JCheckBox("HTML " + getString("publish"));
        if ( htmlEnabled)
        {
            panel.add(htmlCheck,"0,0");
            panel.add(new JLabel(getString("weekview.print.title_textfield") +":"),"2,2");
            panel.add( titleField, "4,2");
            panel.add(new JLabel(getString("show_navigation")),"2,4");
            panel.add( showNavField, "4,4");
            String dateString = getRaplaLocale().formatDate(model.getSelectedDate());
            panel.add(new JLabel(getI18n().format("including_date",dateString)),"2,6");
            panel.add( saveSelectedDateField, "4,6");
            panel.add( statusHtml, "2,8,4,1");
        }
        final JCheckBox icalCheck = new JCheckBox("ICAL " + getString("publish"));
        
        if ( icalEnabled)
        {   
            panel.add(icalCheck,"0,10");
            panel.add( statusICal, "2,12,4,1");
        }

        final JCheckBox encryptionCheck = new JCheckBox();
        if(this.encryptionAllowed)
        {
        	I18nBundle i18n = (I18nBundle)getService(I18nBundle.ROLE+"/" + UrlEncryptionPlugin.RESOURCE_FILE);
        	String encryption = i18n.getString("encryption");
            encryptionCheck.setSelected(this.encryptionEnabled);
            encryptionCheck.setText("URL " + encryption);
            final JLabel encryptionActivation = new JLabel();
            encryptionCheck.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    encryptionEnabled = encryptionCheck.isSelected();
                    htmlURL.setText(  getAddress( filename, "calendar"));
                    icalURL.setText(  getAddress( filename, "iCal"));

                }
            });

            panel.add(encryptionCheck, "0,26");
            panel.add(encryptionActivation, "0,28,4,1");
        }
        
        final String title = model.getTitle();
        titleField.setText(title);
        {
            final String entry = (String)model.getOption(AbstractHTMLCalendarPage.SHOW_NAVIGATION_ENTRY);
            showNavField.setSelected( entry == null || entry.equals("true"));
        }
        {
            final String entry = (String)model.getOption(AbstractHTMLCalendarPage.SAVE_SELECTED_DATE);
            if(entry != null)
            	saveSelectedDateField.setSelected( entry.equals("true"));
        }
        {
            final String entry = (String)model.getOption(AutoExportPlugin.HTML_EXPORT);
            htmlCheck.setSelected( entry != null && entry.equals("true"));
        }
        {
            final String entry = (String)model.getOption(Export2iCalPlugin.ICAL_EXPORT);
            icalCheck.setSelected( entry != null && entry.equals("true"));
        }
        final ActionListener checkListener = new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                boolean htmlEnabled = htmlCheck.isSelected() && htmlCheck.isEnabled();
                titleField.setEnabled( htmlEnabled);
                showNavField.setEnabled( htmlEnabled);
                saveSelectedDateField.setEnabled( htmlEnabled);
                statusHtml.setEnabled( htmlEnabled);
                boolean icalEnabled = icalCheck.isSelected() && icalCheck.isEnabled();
                statusICal.setEnabled( icalEnabled);
                encryptionEnabled = encryptionCheck.isSelected();
            }
        };
       
        checkListener.actionPerformed( null);
        htmlCheck.addActionListener( checkListener);
        icalCheck.addActionListener( checkListener);
        

        final DialogUI dlg = DialogUI.create(
                getContext(),
                                        parentComponent,false,panel,
                                       new String[] {
                                           getString("save")
                                           ,getString("cancel")
                                       });
        dlg.setTitle(getString("publish"));
        dlg.getButton(0).setIcon(getIcon("icon.save"));
        dlg.getButton(1).setIcon(getIcon("icon.cancel"));
        dlg.getButton(0).setAction( new AbstractAction() {
            private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
                dlg.close();
                
                try 
                {
                
                    
                    String title = titleField.getText().trim();
                    if ( title.length() > 0)
                        model.setTitle( title );
                    else
                        model.setTitle( null);
                    
                    
                    String showNavEntry = showNavField.isSelected() ? "true" : "false";
                    model.setOption( AbstractHTMLCalendarPage.SHOW_NAVIGATION_ENTRY, showNavEntry);
                    
                    String saveSelectedDate = saveSelectedDateField.isSelected() ? "true" : "false";
                    model.setOption( AbstractHTMLCalendarPage.SAVE_SELECTED_DATE, saveSelectedDate);
                    
                    final String icalSelected = icalCheck.isSelected() ? "true" : "false";
                    
                    model.setOption( Export2iCalPlugin.ICAL_EXPORT, icalSelected);
                    
                    final String htmlSelected = htmlCheck.isSelected() ? "true" : "false";
                    model.setOption( AutoExportPlugin.HTML_EXPORT, htmlSelected);

                    final String encryptionSelected = encryptionAllowed && encryptionCheck.isSelected() ? "true" : "false";
                    model.setOption(UrlEncryptionPlugin.URL_ENCRYPTION, encryptionSelected);

                    model.save( filename);
                } 
                catch (RaplaException ex) 
                {
                    showException( ex, parentComponent);
                }
            }
        });
        dlg.start();
    }

  
    private JPanel createStatus(String filename, String generator, final JTextField urlLabel) throws RaplaException {
        addCopyPaste(urlLabel);
        JPanel status = new JPanel()
        {
			private static final long serialVersionUID = 1L;
            public void setEnabled(boolean enabled)
            {
                super.setEnabled(enabled);
                urlLabel.setEnabled( enabled);
            }
        };
        status.setLayout( new BorderLayout());
        urlLabel.setText( "");
        urlLabel.setEditable( true );
        urlLabel.setFont( urlLabel.getFont().deriveFont( (float)10.0));
        status.add( new JLabel("URL: "), BorderLayout.WEST );
        status.add( urlLabel, BorderLayout.CENTER );
        
        final RaplaButton copyButton = new RaplaButton();
        copyButton.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        copyButton.setFocusable(false);
        copyButton.setRolloverEnabled(false);
        copyButton.setIcon(getIcon("icon.copy"));
        copyButton.setToolTipText(getString("copy_to_clipboard"));
        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	urlLabel.requestFocus();
            	urlLabel.selectAll();
                copy(urlLabel,e);
            }

        });
        status.add(copyButton, BorderLayout.EAST);
        urlLabel.setText(  getAddress( filename,generator));
        return status;
    }
}


