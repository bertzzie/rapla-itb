package org.rapla.plugin.eventtimecalculator;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.tableview.AppointmentTableColumn;

/**
* User: kuestermann
* Date: 22.08.12
* Time: 09:30
*/
public final class DurationColumnAppoimentBlock extends DurationColumn implements AppointmentTableColumn {

    public DurationColumnAppoimentBlock(RaplaContext context, Configuration config) throws RaplaException {
        super(context, config);
    }

    public String getValue(AppointmentBlock block) {
        long totalDuration = EventTimeCalculatorFactory.calcDuration(config, block);
        String format2 = EventTimeCalculatorFactory.format(config, totalDuration);
        return format2;
    }

    public String getHtmlValue(AppointmentBlock block) {
        String dateString = getValue(block);
        return dateString;
    }
}
