package org.rapla.plugin.export2ical;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Properties;

import javax.swing.*;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.layout.TableLayout;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.RaplaGUIComponent;

/*******************************************************************************
 * This is the admin-option panel
 * 
 * @author Twardon
 * 
 */
public class Export2iCalAdminOption extends DefaultPluginOption implements ActionListener {

	private JSpinner spiDaysBefore;
	private JSpinner spiDaysAfter;
	private JRadioButton optGlobalInterval;
	private JRadioButton optUserInterval;
	private JComboBox cboTimezone;
	private JLabel lblLastModifiedInterval;
	private JSpinner spiLastModifiedInterval;
	private JCheckBox chkUseLastModifiedIntervall;
	private JCheckBox chkExportAttendees;
	private JTextArea txtEMailRessourceAttribute;
    private JComboBox cbDefaultParticipationsStatusRessourceAttribute;

    public Export2iCalAdminOption(RaplaContext sm) throws RaplaException {
		super(sm);
	}

	protected JPanel createPanel() throws RaplaException {
		spiLastModifiedInterval = new JSpinner(new SpinnerNumberModel(5, 0, 365, 1));
		chkUseLastModifiedIntervall = new JCheckBox("do not deliver new calendar");
		chkExportAttendees = new JCheckBox("export attendees of vevent");
        txtEMailRessourceAttribute = new JTextArea(Export2iCalPlugin.DEFAULT_attendee_resource_attribute);
        RaplaGUIComponent copyPasteWrapper = new RaplaGUIComponent( getContext());
        copyPasteWrapper.addCopyPaste(txtEMailRessourceAttribute);
        txtEMailRessourceAttribute.setToolTipText("Define the key of the attribute containing the email address");
        cbDefaultParticipationsStatusRessourceAttribute = new JComboBox(new Object [] {
                "ACCEPTED", "TENTATIVE"

        });
        cbDefaultParticipationsStatusRessourceAttribute.setSelectedItem(Export2iCalPlugin.DEFAULT_attendee_participation_status);
        cbDefaultParticipationsStatusRessourceAttribute.setToolTipText("Define the default value for participation status");


		spiDaysBefore = new JSpinner(new SpinnerNumberModel(Export2iCalPlugin.DEFAULT_daysBefore, 0, 365, 1));
		spiDaysAfter = new JSpinner(new SpinnerNumberModel(Export2iCalPlugin.DEFAULT_daysAfter, 0, 365, 1));
		optGlobalInterval = new JRadioButton("global interval setting");
		optUserInterval = new JRadioButton("user interval settings");
		lblLastModifiedInterval = new JLabel("interval for delivery in days");
		String[] timeZoneIDs = getTimeZonesFromResource();
		cboTimezone = new JComboBox(timeZoneIDs);
		cboTimezone.setEditable(false);
		ButtonGroup group = new ButtonGroup();
		group.add(optGlobalInterval);
		group.add(optUserInterval);
		JPanel panel = super.createPanel();
		JPanel content = new JPanel();
		double[][] sizes = new double[][] {
				{       5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5 },
				{       TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
						TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5  } };
		TableLayout tableLayout = new TableLayout(sizes);
		content.setLayout(tableLayout);
		content.add(optGlobalInterval, "1,4");
		content.add(optUserInterval, "3,4");
		content.add(new JLabel("previous days:"), "1,6");
		content.add(spiDaysBefore, "3,6");
		content.add(new JLabel("subsequent days:"), "1,8");
		content.add(spiDaysAfter, "3,8");
		content.add(chkUseLastModifiedIntervall, "1,12");
		content.add(lblLastModifiedInterval, "1,14");
		content.add(spiLastModifiedInterval, "3,14");
		content.add(new JLabel("timezone:"), "1,10");
		content.add(cboTimezone, "3,10");
        content.add(chkExportAttendees, "1,16");
        content.add(new JLabel("attribute key in person-type:"), "1,18");
        content.add(txtEMailRessourceAttribute, "3,18");
        content.add(new JLabel("participation status:"), "1,20");
        content.add(cbDefaultParticipationsStatusRessourceAttribute, "3,20");

        panel.add(content, BorderLayout.CENTER);
		optUserInterval.addActionListener(this);
		optGlobalInterval.addActionListener(this);
		chkUseLastModifiedIntervall.addActionListener(this);
        chkExportAttendees.addActionListener(this);
		cboTimezone.addActionListener(this);
		return panel;
	}

	protected void addChildren(DefaultConfiguration newConfig) {

		try {
			newConfig.getMutableChild(Export2iCalPlugin.TIMEZONE, true).setValue(String.valueOf(cboTimezone.getSelectedItem()));
			newConfig.getMutableChild(Export2iCalPlugin.DAYS_BEFORE, true).setValue(Integer.parseInt(spiDaysBefore.getValue().toString()));
			newConfig.getMutableChild(Export2iCalPlugin.DAYS_AFTER, true).setValue(Integer.parseInt(spiDaysAfter.getValue().toString()));
			newConfig.getMutableChild(Export2iCalPlugin.GLOBAL_INTERVAL, true).setValue(optGlobalInterval.isSelected());

			String lastModIntervall = chkUseLastModifiedIntervall.isSelected() ? new String("-1") : spiLastModifiedInterval.getValue().toString();
			newConfig.getMutableChild(Export2iCalPlugin.LAST_MODIFIED_INTERVALL, true).setValue(lastModIntervall);

            newConfig.getMutableChild(Export2iCalPlugin.EXPORT_ATTENDEES, true).setValue(chkExportAttendees.isSelected());
            newConfig.getMutableChild(Export2iCalPlugin.EXPORT_ATTENDEES_EMAIL_ATTRIBUTE, true).setValue(txtEMailRessourceAttribute.getText());
            newConfig.getMutableChild(Export2iCalPlugin.EXPORT_ATTENDEES_PARTICIPATION_STATUS, true).setValue(cbDefaultParticipationsStatusRessourceAttribute.getSelectedItem().toString());

		} catch (ConfigurationException e) {
			getLogger().error("An error has occured saving the Export2iCal Configuration " + e.getMessage());
		}

	}

	protected void readConfig(Configuration config) {
		String TimezoneID = config.getChild(Export2iCalPlugin.TIMEZONE).getValue(Export2iCalPlugin.DEFAULT_timezone);

		int daysBefore = config.getChild(Export2iCalPlugin.DAYS_BEFORE).getValueAsInteger(Export2iCalPlugin.DEFAULT_daysBefore);
		int daysAfter = config.getChild(Export2iCalPlugin.DAYS_AFTER).getValueAsInteger(Export2iCalPlugin.DEFAULT_daysAfter);
		int lastModifiedIntervall = config.getChild(Export2iCalPlugin.LAST_MODIFIED_INTERVALL).getValueAsInteger(Export2iCalPlugin.DEFAULT_lastModifiedIntervall);

		boolean global_interval = config.getChild(Export2iCalPlugin.GLOBAL_INTERVAL).getValueAsBoolean(Export2iCalPlugin.DEFAULT_globalIntervall);

        boolean exportAttendees = config.getChild(Export2iCalPlugin.EXPORT_ATTENDEES).getValueAsBoolean(Export2iCalPlugin.DEFAULT_exportAttendees);
		String exportAttendeeDefaultEmailAttribute = config.getChild(Export2iCalPlugin.EXPORT_ATTENDEES_EMAIL_ATTRIBUTE).getValue(Export2iCalPlugin.DEFAULT_attendee_resource_attribute);
        String exportAttendeeParticipationStatus = config.getChild(Export2iCalPlugin.EXPORT_ATTENDEES_PARTICIPATION_STATUS).getValue(Export2iCalPlugin.DEFAULT_attendee_participation_status);

	
		if (lastModifiedIntervall == -1) {
			spiLastModifiedInterval.setEnabled(false);
			lblLastModifiedInterval.setEnabled(false);
			chkUseLastModifiedIntervall.setSelected(true);
		} else {
			spiLastModifiedInterval.setValue(new Integer(lastModifiedIntervall));
			lblLastModifiedInterval.setEnabled(true);
			chkUseLastModifiedIntervall.setSelected(false);
		}

		optGlobalInterval.setSelected(global_interval);
		optUserInterval.setSelected(!global_interval);

		spiDaysBefore.setValue(new Integer(daysBefore));
		spiDaysAfter.setValue(new Integer(daysAfter));

		cboTimezone.setSelectedItem(TimezoneID);

        chkExportAttendees.setSelected(exportAttendees);

        txtEMailRessourceAttribute.setText(exportAttendeeDefaultEmailAttribute);
        txtEMailRessourceAttribute.setEnabled(chkExportAttendees.isSelected());

        cbDefaultParticipationsStatusRessourceAttribute.setSelectedItem(exportAttendeeParticipationStatus);
        cbDefaultParticipationsStatusRessourceAttribute.setEnabled(chkExportAttendees.isSelected());
		//this.setTextFieldInput();
	}

	public String getDescriptorClassName() {
		return Export2iCalPlugin.class.getName();
	}

	public String getName(Locale locale) {
		return "Export2iCal";
	}

	/*
	 //This is now not needed anymore
	private void setTextFieldInput() {
		this.spiDaysBefore.setEnabled(optGlobalInterval.isSelected());
		this.spiDaysAfter.setEnabled(optGlobalInterval.isSelected());
	}*/

	/**
	 * Gets all the iCal4J supported TimeZones from the Resource File They are
	 * generated by trial-and error in the BUILD event.
	 * 
	 * @return String[] of the TimeZones for direct use in the ComboBox
	 */
	private String[] getTimeZonesFromResource() {

		Properties prop = new Properties();
		try {
			prop.load(Export2iCalPlugin.class.getResourceAsStream("/org/rapla/plugin/export2ical/AvailableTimeZones.properties"));
		} catch (Exception e) {
			getLogger().error("Error reading Resource TimeZone List!");
		}
		String[] ary = (String[]) prop.keySet().toArray((new String[prop.keySet().size()]));
		java.util.Arrays.sort(ary);
		return ary;

	}

	public void actionPerformed(ActionEvent e) {
		//this.setTextFieldInput();
		if (e.getSource() == chkUseLastModifiedIntervall) {
			spiLastModifiedInterval.setEnabled(!chkUseLastModifiedIntervall.isSelected());
			lblLastModifiedInterval.setEnabled(!chkUseLastModifiedIntervall.isSelected());
		}

        if (e.getSource() == chkExportAttendees) {
            txtEMailRessourceAttribute.setEnabled(chkExportAttendees.isSelected());
            cbDefaultParticipationsStatusRessourceAttribute.setEnabled(chkExportAttendees.isSelected());
        }
	}
}
