package org.rapla;

import java.util.Date;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.storage.dbrm.RemoteOperator;

public class CommunicatorTest extends ServletTestBase
{
    
    public CommunicatorTest( String name )
    {
        super( name );
    }

  
    public void testLargeform() throws Exception
    {
        ClientFacade facade = (ClientFacade)getContext().lookup(ClientFacade.ROLE + "/remote-facade-3");
        facade.login("homer","duffs".toCharArray());
        Allocatable alloc = facade.newResource();
        StringBuffer buf = new StringBuffer();
        int stringsize = 100000;
        for (int i=0;i< stringsize;i++)
        {
            buf.append( "xxxxxxxxxx");
        }
        String verylongname = buf.toString();
        alloc.getClassification().setValue("name", verylongname);
        facade.store( alloc);
    }
    
    
    public void testClient() throws Exception
    {
       ClientFacade facade = (ClientFacade)getContext().lookup(ClientFacade.ROLE + "/remote-facade-3");
       boolean success = facade.login("admin","test".toCharArray());
       assertFalse( "Login should fail",success ); 
       facade.login("homer","duffs".toCharArray());
       try 
       {
           Preferences preferences = facade.edit( facade.getPreferences( null));
           preferences.putEntry("test-entry", "test-value");
           
           facade.store( preferences);
           preferences = facade.edit( facade.getPreferences( null));
           preferences.putEntry("test-entry", "test-value");
           facade.store( preferences);

           Allocatable[] allocatables = facade.getAllocatables();
           assertTrue( allocatables.length > 0);
           Reservation[] events = facade.getReservations( new Allocatable[] {allocatables[0]}, null,null);
           assertTrue( events.length > 0);
           
           Reservation r = events[0];
           Reservation editable = facade.edit( r);
           facade.store( editable );
           
           Reservation newEvent = facade.newReservation();
           Appointment newApp = facade.newAppointment( new Date(), new Date());
           newEvent.addAppointment( newApp );
           newEvent.getClassification().setValue("name","Test Reservation");
           newEvent.addAllocatable( allocatables[0]);
           
           facade.store( newEvent );
           facade.remove( newEvent);
       }
       finally
       {
           facade.logout();
       }
    }

    public void testUmlaute() throws Exception
    {
        ClientFacade facade = (ClientFacade)getContext().lookup(ClientFacade.ROLE + "/remote-facade-3");
        facade.login("homer","duffs".toCharArray());
        Allocatable alloc = facade.newResource();
        String typeName = alloc.getClassification().getType().getElementKey();
        String nameWithUmlaute = "ÜÄÖüäöß";
        alloc.getClassification().setValue("name", nameWithUmlaute);
        int allocSizeBefore = facade.getAllocatables().length;
        facade.store( alloc);
        
        facade.logout();
        facade.login("homer","duffs".toCharArray());
        DynamicType type = facade.getDynamicType( typeName);
        ClassificationFilter filter = type.newClassificationFilter();
        filter.addEqualsRule("name", nameWithUmlaute);
        Allocatable[] allAllocs = facade.getAllocatables();
        assertEquals( allocSizeBefore + 1, allAllocs.length);
        Allocatable[] allocs = facade.getAllocatables( new ClassificationFilter[] {filter});
        assertEquals( 1, allocs.length);
        
    }
    public void testManyClients() throws Exception
    {
        RaplaContext context = getContext();
        int clientNum = 50;
        RemoteOperator [] opts = new RemoteOperator[ clientNum];
        DefaultConfiguration remoteConfig = new DefaultConfiguration("element");
        DefaultConfiguration serverParam = new DefaultConfiguration("server");
        serverParam.setValue("http://localhost:8051/");
        remoteConfig.addChild( serverParam );
        for ( int i=0;i<clientNum;i++)
        {
            RemoteOperator opt = new RemoteOperator(context,remoteConfig );
            opt.connect("homer","duffs".toCharArray());
            opts[i] = opt;
            System.out.println("Client " + i + " successfully subscribed");
        }
        testClient();
        
        for ( int i=0;i<clientNum;i++)
        {
            RemoteOperator opt = opts[i];
            opt.disconnect();
        }
    }
}
