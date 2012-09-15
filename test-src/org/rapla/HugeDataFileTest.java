package org.rapla;

import java.util.Date;

import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaException;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.StorageOperator;

public class HugeDataFileTest extends RaplaTestCase
{

    public HugeDataFileTest( String name )
    {
        super( name );
    }

    public void testHuge() throws RaplaException, Exception
    {
        getFacade().login("homer","duffs".toCharArray());
        int RESERVATION_COUNT = 5000;
        Reservation[] events = new Reservation[RESERVATION_COUNT];
        
        for ( int i=0;i<RESERVATION_COUNT;i++)
        {
            Reservation event = getFacade().newReservation();
            Appointment app1 = getFacade().newAppointment( new Date(), new Date());
            Appointment app2 = getFacade().newAppointment( new Date(), new Date());
            event.addAppointment( app1);
            event.addAppointment( app2);
            event.getClassification().setValue("name", "Test-Event " + i);
            events[i] = event;
        }
        System.out.println("Starting store");
        
        getFacade().storeObjects( events );

        System.out.println("Stored");
       // Thread.sleep( 100000);
        StorageOperator operator = (StorageOperator)getContext().lookup( CachableStorageOperator.ROLE + "/file");
        operator.disconnect();
        operator.connect("homer", "duffs".toCharArray());
    }
    
    public static void main(String[] args)
    {
        HugeDataFileTest test = new HugeDataFileTest( HugeDataFileTest.class.getName());
        try
        {
            test.setUp();
            test.testHuge();
            test.tearDown();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
    }
}
