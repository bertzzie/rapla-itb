package org.rapla.plugin.appointmentmarker;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.rapla.components.util.DateTools;
import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class AppointmentMarker extends RaplaComponent 
{
    static public String ROLE = AppointmentMarker.class.getName();

    public AppointmentMarker( RaplaContext context ) throws RaplaException
    {
        super( context );
    }

    /** returns if the event has the appointmentmarker attribute*/
    public boolean canBeMarked(Reservation event)
    {
        Attribute att = event.getClassification().getType().getAttribute( AppointmentMarkerPlugin.MARKER_ATTRIBUTE_KEY);
        return att!= null;
    }
    
    /** returns all the marked Dates as an Set (of Date object(*/
    public Set<Date> getMarkedDates(Reservation event) {
        final Set<Date> markedDates = new TreeSet<Date>();
        Attribute att = event.getClassification().getType().getAttribute( AppointmentMarkerPlugin.MARKER_ATTRIBUTE_KEY);
        
        Object value = event.getClassification().getValue( att );
        final SerializableDateTimeFormat format = new SerializableDateTimeFormat();
        
        // Parse the marked dates
        if ( value != null)
        {
        	if ( !(value instanceof String))
        	{
        		throw new RuntimeException("appointmentmarker attribute needs to be of type string");
        	}
            String stringValue = (String) value;
			StringTokenizer tokenizer = new StringTokenizer(stringValue,";");
            while ( tokenizer.hasMoreTokens())
            {
                String dateString=tokenizer.nextToken();
                try
                {
                    Date markedDate = format.parseDate( dateString, false);
                    markedDates.add(markedDate);
                }
                catch (ParseException e)
                {
                    getLogger().error("Couldnt parse date " + dateString + " for appointment marking. Ignoring");
                }
            }
        }
        return markedDates;
    }
        
    /** returns if the event is marked on the passed dates*/
    public boolean isMarked( Reservation event, Date date)
    {
        return isMarked( date, getMarkedDates( event));
    }
    
    /** same as isMarked but you can pass the markedDates*/
    static public boolean isMarked(  Date date, Collection<Date> markedDates)
    {
        Date normalizedDate = DateTools.cutDate(  date);
        return markedDates.contains( normalizedDate );
    }
    
    /** marks/unmarks an event on the passed date*/
    public void setMarked( Reservation event, Date dateToMark, boolean mark)
    {
        final SerializableDateTimeFormat format = new SerializableDateTimeFormat();
        Set<Date> markedDates = getMarkedDates( event );
        Date currentSelectedDate = DateTools.cutDate( dateToMark );
        if  ( mark )
        {
            markedDates.add( currentSelectedDate );
        }
        else
        {
            markedDates.remove( currentSelectedDate );
        }
        String newValue = "";
        for (Iterator<Date> it = markedDates.iterator();it.hasNext();)
        {
           Date markedDate = it.next();
           newValue+=format.formatDate( markedDate) + ";";
        }
        event.getClassification().setValue(AppointmentMarkerPlugin.MARKER_ATTRIBUTE_KEY, newValue);
    }
    
    /** returns null if the event contains an appointment that repeats forever*/
    public List<AppointmentBlock> getAllBlocks(Reservation event) {
        List<AppointmentBlock> blocks = new ArrayList<AppointmentBlock>();
        Appointment[] appointments = event.getAppointments();
        for (int i = 0; i<appointments.length; i++) {
            Appointment appointment = appointments[i];
            Repeating repeating = appointment.getRepeating();
            if ( repeating == null ) {
                appointment.createBlocks( appointment.getStart(), appointment.getEnd(), blocks);
                continue;
            }
            if ( repeating.getEnd() == null ){ // Repeats foreever ?
                return null;
            }
            appointment.createBlocks( appointment.getStart(), repeating.getEnd(), blocks);
        }
        Collections.sort(blocks);
        return blocks;
    }
    
}
