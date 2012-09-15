package org.rapla.plugin.eventtimecalculator;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.tableview.ReservationTableColumn;

/**
* User: kuestermann
* Date: 22.08.12
* Time: 09:29
*/
public final class DurationColumnReservation extends DurationColumn implements ReservationTableColumn {

     public DurationColumnReservation(RaplaContext context, Configuration config) throws RaplaException {
         super(context, config);
     }

     public String getValue(Reservation event) {
         long totalDuration = EventTimeCalculatorFactory.calcDuration(config, event);
         String format2 = EventTimeCalculatorFactory.format(config, totalDuration);
         return format2;
     }


     public String getHtmlValue(Reservation object) {
         String dateString= getValue(object);
         return dateString;
     }
 }
