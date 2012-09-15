package org.rapla.plugin.archiver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class ArchiverService extends RaplaComponent
{

    String REMOVE_OLDER_THAN_ENTRY = "remove-older-than";
    
    public ArchiverService( RaplaContext context, Configuration config ) throws RaplaException
    {
        super( context );
        String value = config.getChild( REMOVE_OLDER_THAN_ENTRY).getValue(null);
        if ( value != null)
        {
            Timer timer = new Timer(true);
            long period = Long.parseLong( value) * DateTools.MILLISECONDS_PER_DAY;
            RemoveTask removeTask = new RemoveTask( period);
            // Call it each hour
            timer.schedule(removeTask, new Date(), DateTools.MILLISECONDS_PER_HOUR); 
        }
    }
    
    class RemoveTask extends TimerTask
    {
        long period;
        public RemoveTask( long period )
        {
            this.period = period;
        }

        public void run()
        {   
            Date endDate = new Date(getClientFacade().today().getTime() - period);
            try
            {
                Reservation[] events = getClientFacade().getReservations((User) null, null, endDate, null); //ClassificationFilter.CLASSIFICATIONFILTER_ARRAY );
                List<Reservation> toRemove = new ArrayList<Reservation>();
                for ( int i=0;i< events.length;i++)
                {
                    Reservation event = events[i];
                    if ( isOlderThan( event, endDate))
                    {
                        toRemove.add(event);
                    }
                }
                if ( toRemove.size() > 0)
                {
                    getLogger().info("Removing " + toRemove.size() + " old events.");
                    Reservation[] eventsToRemove = (Reservation[])toRemove.toArray( Reservation.RESERVATION_ARRAY);
                    int STEP_SIZE = 100;
                    for ( int i=0;i< eventsToRemove.length;i+=STEP_SIZE)
                    {
                        int blockSize = Math.min( eventsToRemove.length- i, STEP_SIZE);
                        Reservation[] eventBlock = new Reservation[blockSize];
                        System.arraycopy( eventsToRemove,i, eventBlock,0, blockSize);
                        getClientFacade().removeObjects( eventBlock);
                    }
                }
            }
            catch (RaplaException e)
            {
                getLogger().error("Could not remove old events ", e);
            }
        }
    }
    
    private boolean isOlderThan( Reservation event, Date maxAllowedDate )
    {
        Appointment[] appointments = event.getAppointments();
        for ( int i=0;i<appointments.length;i++)
        {
            Appointment appointment = appointments[i];
            Date start = appointment.getStart();
            Date end = appointment.getMaxEnd();
            if ( start == null || end == null )
            {
                return false;
            }
            if ( end.after( maxAllowedDate))
            {
                return false;
            }
            if ( start.after( maxAllowedDate))
            {
                return false;
            }
        }
        return true;
    }


}
