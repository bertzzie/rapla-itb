package org.rapla.plugin.eventtimecalculator;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

import javax.swing.table.TableColumn;

/**
* User: kuestermann
* Date: 22.08.12
* Time: 09:30
*/
public class DurationColumn extends RaplaComponent {
    protected Configuration config;

    public DurationColumn(RaplaContext context, Configuration config)
            throws RaplaException {
        super(context);
        this.config = config;
    }


    public void init(TableColumn column) {
        column.setMaxWidth(90);
        column.setPreferredWidth(90);
    }


    public String getColumnName() {
        I18nBundle i18n = (I18nBundle) getService(I18nBundle.ROLE + "/" + EventTimeCalculatorPlugin.RESOURCE_FILE);

        return i18n.getString("total_duration");
    }


    public Class<?> getColumnClass() {
        return String.class;
    }


}
