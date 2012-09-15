/*--------------------------------------------------------------------------*
  | Copyright (C) 2006 Christopher Kohlhaas                                  |
  |                                                                          |
  | This program is free software; you can redistribute it and/or modify     |
  | it under the terms of the GNU General Public License as published by the |
  | Free Software Foundation. A copy of the license has been included with   |
  | these distribution in the COPYING file, if not go to www.fsf.org .       |
  |                                                                          |
  | As a special exception, you are granted the permissions to link this     |
  | program with every library, which license fulfills the Open Source       |
  | Definition as published by the Open Source Initiative (OSI).             |
  *--------------------------------------------------------------------------*/

package org.rapla.storage.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.internal.AppointmentImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ReservationReader extends RaplaXMLReader {
    ReservationImpl reservation;
    private Map<String,Appointment> appointmentsOldSchema = new HashMap<String,Appointment>();
    private Object allocatableId = null;
    private AppointmentImpl appointment = null;
    private Repeating repeating = null;
    private Collection<Appointment> restrictionsOldSchema = new ArrayList<Appointment>();
   
    private DynAttReader dynAttHandler;

    public ReservationReader( RaplaContext context) throws RaplaException  {
        super( context);
        dynAttHandler = new DynAttReader( context);
        addChildHandler(dynAttHandler);
    }

    public void processElement(String namespaceURI,String localName,String qName,Attributes atts)
        throws SAXException
    {
        if (namespaceURI.equals(DYNATT_NS)) 
        {
            dynAttHandler.setClassifiable(reservation);
            delegateElement(dynAttHandler,namespaceURI,localName,qName,atts);
            return;
        }

        if (!namespaceURI.equals(RAPLA_NS))
            return;

        if ( localName.equals( "reservation" ) ) 
        {
            String createdAt = atts.getValue( "", "created-at");
            String lastChanged = atts.getValue( "", "last-changed");
            String lastChangedBy = atts.getValue( "", "last-changed-by");

            Date createTime = null;
            Date changeTime = createTime;
            if (createdAt != null)
                createTime = parseDate( createdAt, false );
            if (lastChanged != null)
                changeTime = parseDate( lastChanged, false );

            reservation = new ReservationImpl( createTime, changeTime );
            if ( lastChangedBy != null) 
            {
            	try 
                {
            		User user = (User)resolve(User.TYPE,lastChangedBy );
            		reservation.setLastChangedBy( user );
            	} 
            	catch (SAXParseException ex) 
            	{
            		getLogger().warn("Can't find user " + lastChangedBy + " at line " + ex.getLineNumber());
            	}
            }
            setId(reservation, atts);
            setVersionIfThere( reservation, atts);

            setOwner(reservation, atts);
            appointmentsOldSchema.clear();
        }

        if (localName.equals("appointment")) 
        {
            if (atts.getValue("idref") != null) 
            {
                Appointment appointment = appointmentsOldSchema.get(atts.getValue("idref"));
                restrictionsOldSchema.add( appointment );
            } 
            else 
            {
                String id = atts.getValue("id");
                String startDate=atts.getValue("start-date");
                String endDate= atts.getValue("end-date");
                if (endDate == null)
                    endDate = startDate;

                String startTime = atts.getValue("start-time");
                String endTime = atts.getValue("end-time");

                Date start;
                Date end;
                if (startTime != null && endTime != null) 
                {
                    start = parseDateTime(startDate,startTime);
                    end = parseDateTime(endDate,endTime);
                } 
                else 
                {
                    start = parseDate(startDate,false);
                    end = parseDate(endDate,true);
                }

                appointment= new AppointmentImpl(start,end);
                appointment.setWholeDays(startTime== null || endTime==null);
                if (atts.getValue( "id" )!=null)
                {
                    setId(appointment, atts);
                } 
                else 
                {
                    setNewId(appointment);
                }
                setVersionIfThere( appointment, atts);
                reservation.addAppointment(appointment);
                if (id != null)
                {
                    appointmentsOldSchema.put(id,appointment);
                }
            }
        }

        if (localName.equals("repeating")) {
            String type =atts.getValue("type");
            String interval =atts.getValue("interval");
            String enddate =atts.getValue("end-date");
            String number =atts.getValue("number");
            appointment.setRepeatingEnabled(true);
            repeating = appointment.getRepeating();
            repeating.setType( RepeatingType.findForString( type));
            if (interval != null)
            {
                repeating.setInterval(Integer.valueOf(interval).intValue());
            }
            if (enddate != null) 
            {
                repeating.setEnd(parseDate(enddate,true));
            } 
            else if (number != null) 
            {
                repeating.setNumber(Integer.valueOf(number).intValue());
            } 
            else 
            {
                repeating.setEnd(null);
            }
            /*
            if (getLogger().enabled(6))
                getLogger().log(6, "Repeating " + repeating.toString() );
            */
        }

        if (localName.equals("allocate")) {
            String id = getString( atts, "idref" );
            allocatableId = getId( Allocatable.TYPE, id);
            reservation.getReferenceHandler().addId( allocatableId );
            restrictionsOldSchema.clear();
            if ( appointment != null )
            {
                reservation.addRestrictionForId( allocatableId, appointment);
            }
        }


        if (localName.equals("exception")) {
        }

        if (localName.equals("date")) {
            String dateString =atts.getValue("date");
            if (dateString != null && repeating != null)
                repeating.addException(parseDate(dateString,false));
        }
    }

    public void processEnd(String namespaceURI,String localName,String qName)
        throws SAXException
    {
        if (!namespaceURI.equals(RAPLA_NS))
            return;

        if (localName.equals("appointment") && appointment != null )
        {
            add(appointment);
            appointment = null;
        }
        if (localName.equals("reservation"))
        {
            add(reservation);
        }
        if (localName.equals("allocate"))
        {
            if (restrictionsOldSchema.size() > 0)
            {
                Appointment[] apps = 
                    restrictionsOldSchema.toArray( Appointment.EMPTY_ARRAY );
                reservation.setRestrictionForId( allocatableId, apps );
            }
        }
    }
}




