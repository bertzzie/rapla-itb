/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.entities.tests;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.rapla.RaplaTestCase;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.configuration.CalendarModelConfiguration;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaMap;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.ClassificationFilterRule;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationModule;
import org.rapla.facade.QueryModule;
import org.rapla.facade.UpdateModule;

public class ReservationFilterTest extends RaplaTestCase {
    ModificationModule modificationMod;
    QueryModule queryMod;
    UpdateModule updateMod;

    public ReservationFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ReservationFilterTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        ClientFacade facade = getFacade();
        queryMod = facade;
        modificationMod = facade;
        updateMod = facade;
    }

    public void testStore() throws Exception {
        // select from event where (name contains 'planting' or name contains 'owl') or (description contains 'friends');
        DynamicType dynamicType = queryMod.getDynamicType("event");
        ClassificationFilter classificationFilter = dynamicType.newClassificationFilter();
        classificationFilter.setRule(0
                                     ,dynamicType.getAttribute("name")
                                     ,new Object[][] {
                                         {"contains","planting"}
                                         ,{"contains","owl"}
                                     }
                                     );
        classificationFilter.setRule(1
                                     ,dynamicType.getAttribute("description")
                                     ,new Object[][] {
                                         {"contains","friends"}
                                     }
                                     );
        /*
        modificationMod.newRaplaCalendarModel( )
        ReservationFilter filter  = modificationMod.newReservationFilter(, null, ReservationFilter.PARTICULAR_PERIOD, queryMod.getPeriods()[1], null, null );
        //  filter.setPeriod();
        //assertEquals("bowling",queryMod.getReservations(filter)[0].getClassification().getValue("name"));
        assertTrue(((EntityReferencer)filter).isRefering((RefEntity)dynamicType));
*/
        ClassificationFilter[] filter = new ClassificationFilter[] {classificationFilter};

        Map<String, RaplaObject> emptyMap = Collections.emptyMap();
		RaplaMap<RaplaObject> selected = modificationMod.newRaplaMap( emptyMap );
        CalendarModelConfiguration conf = modificationMod.newRaplaCalendarModel(selected, null,filter,null, null, null, queryMod.today(), "week", null );
        Preferences prefs = (Preferences) modificationMod.edit( queryMod.getPreferences());
        prefs.putEntry( "org.rapla.TestConf", conf);
        modificationMod.store( prefs );

        DynamicType newDynamicType = (DynamicType) modificationMod.edit( dynamicType );
        newDynamicType.removeAttribute(dynamicType.getAttribute("description"));
        modificationMod.store( newDynamicType );

        CalendarModelConfiguration configuration = (CalendarModelConfiguration)queryMod.getPreferences().getEntry("org.rapla.TestConf");
        filter =  configuration.getFilter();
        Iterator<? extends ClassificationFilterRule> it = filter[0].ruleIterator();
        it.next();
        assertTrue("second rule should be removed." , !it.hasNext());

    }


}
