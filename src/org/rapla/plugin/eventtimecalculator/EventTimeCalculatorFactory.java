package org.rapla.plugin.eventtimecalculator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.util.DateTools;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.AppointmentStatusFactory;
import org.rapla.gui.ReservationEdit;
import org.rapla.gui.toolkit.RaplaWidget;

public class EventTimeCalculatorFactory implements AppointmentStatusFactory {


    static protected long calcDuration(Configuration config, Reservation event) {
        Collection<AppointmentBlock> blocks = new ArrayList<AppointmentBlock>();
        long totalDuration = 0;
        for (Appointment app : event.getAppointments()) {
            Date start = app.getStart();
            Date end = app.getMaxEnd();
            if (end == null) {
                totalDuration = -1;
                break;
            } else {
                app.createBlocks(start, end, blocks);
            }
        }
        for (AppointmentBlock block : blocks) {
            long duration = calcDuration(config, block);
            if (totalDuration >= 0) {
                totalDuration += duration;
            }
        }
        return totalDuration;
    }

    static protected String format(Configuration config, long totalDuration) {
        if (totalDuration < 0) {
            return "";
        }
        final String format = config.getChild(EventTimeCalculatorPlugin.TIME_FORMAT).getValue(EventTimeCalculatorPlugin.DEFAULT_timeFormat);
        final int timeUnit = config.getChild(EventTimeCalculatorPlugin.TIME_UNIT).getValueAsInteger(EventTimeCalculatorPlugin.DEFAULT_timeUnit);
        String format2 = MessageFormat.format(format, totalDuration / timeUnit, totalDuration % timeUnit);
        return format2;
    }



    static protected long calcDuration(Configuration config, AppointmentBlock block) {
        long duration = DateTools.countMinutes(block.getStart(), block.getEnd());
        final long TIME_TILL_BREAK_DURATION = config.getChild(EventTimeCalculatorPlugin.INTERVAL_NUMBER).getValueAsLong(EventTimeCalculatorPlugin.DEFAULT_timeUnit);
        final long BREAK_DURATION = config.getChild(EventTimeCalculatorPlugin.BREAK_NUMBER).getValueAsLong(EventTimeCalculatorPlugin.DEFAULT_breakNumber);
        long totalDuration = calculateActualDuration(duration, TIME_TILL_BREAK_DURATION, BREAK_DURATION);
        return totalDuration;
    }

    public RaplaWidget createStatus(RaplaContext context, ReservationEdit reservationEdit) throws RaplaException {
        return new EventTimeCalculatorStatusWidget(context, reservationEdit);
    }


    /**
    * calculates the actual duration in minutes of an appointment.
    *
    * @param duration is the total duration of an appointment as long.
    * @return returns the actual duration of an appointment as long.
    */
   public static long calculateActualDuration(long duration,final long TIME_TILL_BREAK_DURATION,final long BREAK_DURATION) {
      long actualDuration = 0;
      
       if (duration > (TIME_TILL_BREAK_DURATION + BREAK_DURATION)) {
           long counter =  (duration / (TIME_TILL_BREAK_DURATION + BREAK_DURATION));
           actualDuration = duration - (counter * BREAK_DURATION);
       } else {
           actualDuration = duration;
       }
       return actualDuration;
   }


}

