package org.rapla.plugin.eventtimecalculator;

import java.awt.BorderLayout;
import java.util.Locale;
import javax.swing.*;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.OptionPanel;
import org.rapla.plugin.eventtimecalculator.EventTimeCalculatorPlugin;

/**
 * ****************************************************************************
 * This is the admin-option panel.
 *
 * @author Tobias Bertram
 */
public class EventTimeCalculatorAdminOption extends DefaultPluginOption implements OptionPanel {
    private I18nBundle i18n;

    private RaplaNumber intervalNumber;
    private RaplaNumber breakNumber;
    //private RaplaNumber lunchbreakNumber;
    private RaplaNumber timeUnit;
    private JTextField timeFormat;

    public EventTimeCalculatorAdminOption(RaplaContext sm, Configuration config) throws RaplaException {
        super(sm);
        i18n = (I18nBundle) sm.lookup(I18nBundle.ROLE + "/" + EventTimeCalculatorPlugin.RESOURCE_FILE);
        setChildBundleName(EventTimeCalculatorPlugin.RESOURCE_FILE);
        this.config = config;
    }

    /**
     * creates the panel shown in the admin option dialog.
     */
    protected JPanel createPanel() throws RaplaException {
        JPanel panel = super.createPanel();
        JPanel content = new JPanel();
        double[][] sizes = new double[][]{
                {5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5},
                        {TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                        TableLayout.PREFERRED, 5,
                                TableLayout.PREFERRED, 5,
                                TableLayout.PREFERRED, 5
                }};
        TableLayout tableLayout = new TableLayout(sizes);
        content.setLayout(tableLayout);
        content.add(new JLabel(i18n.getString("time_till_break") + ":"), "1,0");
        content.add(new JLabel(i18n.getString("break_duration") + ":"), "1,2");
      //  content.add(new JLabel(i18n.getString("lunch_break_duration") + ":"), "1,4");
        content.add(new JLabel(i18n.getString("time_unit") + ":"), "1,6");
        content.add(new JLabel(i18n.getString("time_format") + ":"), "1,8");

        intervalNumber = new RaplaNumber(EventTimeCalculatorPlugin.DEFAULT_intervalNumber, new Integer(0), null, false);
        content.add(intervalNumber, "3,0");
        content.add(new JLabel(i18n.getString("minutes")), "5,0");

        breakNumber = new RaplaNumber(EventTimeCalculatorPlugin.DEFAULT_breakNumber, new Integer(0), null, false);
        content.add(breakNumber, "3,2");
        content.add(new JLabel(i18n.getString("minutes")), "5,2");

//        lunchbreakNumber = new RaplaNumber(EventTimeCalculatorPlugin.DEFAULT_lunchbreakNumber, new Integer(1), null, false);
//        content.add(lunchbreakNumber, "3,4");
//        content.add(new JLabel(i18n.getString("minutes")), "5,4");

        timeUnit = new RaplaNumber(EventTimeCalculatorPlugin.DEFAULT_timeUnit, new Integer(1), null, false);
        content.add(timeUnit, "3,6");
        content.add(new JLabel(i18n.getString("minutes")), "5,6");

        timeFormat = new JTextField();
        content.add(timeFormat, "3,8");


        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /**
     * adds new configuration to the children to overwrite the default configuration.
     */
    protected void addChildren(DefaultConfiguration newConfig) {
        try {
            newConfig.getMutableChild(EventTimeCalculatorPlugin.INTERVAL_NUMBER, true).setValue(intervalNumber.getNumber().intValue());
            newConfig.getMutableChild(EventTimeCalculatorPlugin.BREAK_NUMBER, true).setValue(breakNumber.getNumber().intValue());
//            newConfig.getMutableChild(EventTimeCalculatorPlugin.LUNCHBREAK_NUMBER, true).setValue(lunchbreakNumber.getNumber().intValue());
            newConfig.getMutableChild(EventTimeCalculatorPlugin.TIME_UNIT, true).setValue(timeUnit.getNumber().intValue());
            newConfig.getMutableChild(EventTimeCalculatorPlugin.TIME_FORMAT, true).setValue(timeFormat.getText());
        } catch (ConfigurationException e) {
            getLogger().error("An error has occured saving the EventTimeCalculator Configuration " + e.getMessage());
        }
    }

    /**
     * reads children out of the configuration and shows them in the admin option panel.
     */
    protected void readConfig(Configuration config) {
        int intervalNumberInt = config.getChild(EventTimeCalculatorPlugin.INTERVAL_NUMBER).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_intervalNumber);
        intervalNumber.setNumber(new Integer(intervalNumberInt));
        int breakNumberInt = config.getChild(EventTimeCalculatorPlugin.BREAK_NUMBER).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_breakNumber);
        breakNumber.setNumber(new Integer(breakNumberInt));
//        int lunchbreakNumberInt = config.getChild(EventTimeCalculatorPlugin.LUNCHBREAK_NUMBER).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_lunchbreakNumber);
//        lunchbreakNumber.setNumber(new Integer(lunchbreakNumberInt));
        int timeUnitInt = config.getChild(EventTimeCalculatorPlugin.TIME_UNIT).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_timeUnit);
        timeUnit.setNumber(new Integer(timeUnitInt));
        String timeFormatString = config.getChild(EventTimeCalculatorPlugin.TIME_FORMAT).getValue(EventTimeCalculatorPlugin.DEFAULT_timeFormat);
        timeFormat.setText(timeFormatString);
    }

    /**
     * called when the option panel is selected for displaying.
     */
    public void show() throws RaplaException {
        super.show();
    }

    /**
     * commits the changes in the option dialog.
     */
    public void commit() throws RaplaException {
        super.commit();
    }

    /**
     * returns a string with the name of the class EventTimeCalculatorPlugin.
     */
    public String getDescriptorClassName() {
        return EventTimeCalculatorPlugin.class.getName();
    }

    /**
     * returns a string with the name of the plugin.
     */
    public String getName(Locale locale) {
        return "Event Time Calculator Plugin";
    }

}

