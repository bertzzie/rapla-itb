package org.rapla.plugin.eventtimecalculator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;

/*******************************************************************************
 * This is the user-option panel.
 * @author Tobias Bertram
 * 
 */
public class EventTimeCalculatorUserOption extends RaplaGUIComponent implements OptionPanel {
	private Preferences preferences;
	private Configuration config;
	private JPanel panel = new JPanel();

    private RaplaNumber intervalNumber;
    private int intervalNumberUser;
    private int intervalNumberAdmin;
    
    private RaplaNumber breakNumber;
    private int breakNumberUser;
    private int breakNumberAdmin;
    
//    private RaplaNumber lunchbreakNumber;
//    private int lunchbreakNumberUser;
//    private int lunchbreakNumberAdmin;

    I18nBundle i18n;

    public EventTimeCalculatorUserOption(RaplaContext sm, Configuration config) throws RaplaException {
		super(sm);
		i18n = (I18nBundle) sm.lookup(I18nBundle.ROLE + "/org.rapla.plugin.eventtimecalculator.EventTimeCalculatorResources");
	    setChildBundleName(EventTimeCalculatorPlugin.RESOURCE_FILE);
		this.config = config;
	}
    
    /**
     * returns the panel, created in method createList().
     */
	public JComponent getComponent() {
		return panel;
	}
	
	/**
	 * returns a string with the name of the plugin.
	 */
	public String getName(Locale locale) {
		return "Event Time Calculator Plugin";
	}
	
	/**
     * creates the panel shown in the user option dialog.
     */
	public void createList() throws RaplaException {
		double[][] sizes = new double[][] {
				{       5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5 },
				{       TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5
                        } };
        TableLayout tableLayout = new TableLayout(sizes);
        panel.setLayout(tableLayout);
        panel.add(new JLabel(i18n.getString("user_settings")), "1,0");
        panel.add(new JLabel(i18n.getString("time_till_break")+":"), "1,2");
        panel.add(new JLabel(i18n.getString("break_duration")+":"), "1,4");
//        panel.add(new JLabel(i18n.getString("lunch_break_duration")+":"), "1,6");
        
        intervalNumber = new RaplaNumber(new Integer(90), new Integer(1), null, false);
        panel.add(intervalNumber, "3,2");
        intervalNumber.setNumber(new Integer(intervalNumberUser));
        
        breakNumber = new RaplaNumber(new Integer(15), new Integer(1), null, false);
        panel.add(breakNumber, "3,4");
        breakNumber.setNumber(new Integer(breakNumberUser));
        
  //      lunchbreakNumber = new RaplaNumber(new Integer(30), new Integer(1), null, false);
  //      panel.add(lunchbreakNumber, "3,6");
    //    lunchbreakNumber.setNumber(new Integer(lunchbreakNumberUser));
        
        panel.add(new JLabel(i18n.getString("minutes")), "5,2");
        panel.add(new JLabel(i18n.getString("minutes")), "5,4");
      //  panel.add(new JLabel(i18n.getString("minutes")), "5,6");
        
        JButton defaultButton = new JButton(i18n.getString("default_preferences"));
        panel.add(defaultButton, "1,8");
        defaultButton.addActionListener(new ActionListener()
        {
        	public void actionPerformed (ActionEvent e) {
        		try {
        			intervalNumber.setNumber(intervalNumberAdmin);
        			breakNumber.setNumber(breakNumberAdmin);
        		//	lunchbreakNumber.setNumber(lunchbreakNumberAdmin);
        		} catch (Exception ex) {
        			ex.printStackTrace();
        		}
        	}
        });
	}
	
	/**
     * called when the option panel is selected for displaying.
     */
	public void show() throws RaplaException {
        intervalNumberAdmin = config.getChild(EventTimeCalculatorPlugin.INTERVAL_NUMBER).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_intervalNumber);
        intervalNumberUser = preferences.getEntryAsInteger(EventTimeCalculatorPlugin.INTERVAL_NUMBER, intervalNumberAdmin);

        breakNumberAdmin = config.getChild(EventTimeCalculatorPlugin.BREAK_NUMBER).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_breakNumber);
        breakNumberUser = preferences.getEntryAsInteger(EventTimeCalculatorPlugin.BREAK_NUMBER, breakNumberAdmin);

    //    lunchbreakNumberAdmin = config.getChild(EventTimeCalculatorPlugin.LUNCHBREAK_NUMBER).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_lunchbreakNumber);
    //    lunchbreakNumberUser = preferences.getEntryAsInteger(EventTimeCalculatorPlugin.PREF_LUNCHBREAK_NUMBER, lunchbreakNumberAdmin);

		createList();
	}

	/**
	 * sets preferences.
	 */
	public void setPreferences(Preferences preferences) {
		this.preferences = preferences;
	}
	
	/**
     * commits the changes in the option dialog.
     */
	public void commit() {
		preferences.putEntry(EventTimeCalculatorPlugin.INTERVAL_NUMBER, ""+this.intervalNumber.getNumber().intValue());
		preferences.putEntry(EventTimeCalculatorPlugin.BREAK_NUMBER, ""+this.breakNumber.getNumber().intValue());
	//	preferences.putEntry(EventTimeCalculatorPlugin.PREF_LUNCHBREAK_NUMBER, ""+this.lunchbreakNumber.getNumber().intValue());
	}
}