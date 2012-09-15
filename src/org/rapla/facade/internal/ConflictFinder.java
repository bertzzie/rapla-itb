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
package org.rapla.facade.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.AppointmentBlockEndComparator;
import org.rapla.entities.domain.AppointmentBlockStartComparator;
import org.rapla.entities.domain.AppointmentStartComparator;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.AllocationChangeEvent;
import org.rapla.facade.AllocationChangeEvent.Type;
import org.rapla.facade.Conflict;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaException;
import org.rapla.storage.LocalCache;
import org.rapla.storage.StorageOperator;

public class ConflictFinder {
    StorageOperator operator;
    
    Map<Allocatable,SortedSet<Appointment>> appointmentMap;
    Map<Allocatable,Set<Conflict>> conflictMap;

    public ConflictFinder( StorageOperator operator)  {
        this.operator = operator;
    }

	private void initIfNotNull() throws RaplaException 
	{
		if ( appointmentMap != null)
		{
			return;
		}
		appointmentMap = new HashMap<Allocatable, SortedSet<Appointment>>();
        conflictMap = new HashMap<Allocatable, Set<Conflict>>();
        Date startDate = new Date();
		// Get all appointments since the startDate
		SortedSet<Appointment> allAppointments = operator.getAppointments(null, startDate, null);
		for ( Appointment app:allAppointments)
		{
			Reservation reservation = app.getReservation();
			Allocatable[] allocatables = reservation.getAllocatablesFor(app);
			for ( Allocatable alloc:allocatables)
			{
				Collection<Appointment> list = getAndCreateList(alloc);
				list.add( app);
			}
		}
		for (Map.Entry<Allocatable, SortedSet<Appointment>> entry:appointmentMap.entrySet())
		{
			Allocatable allocatable = entry.getKey();
			updateConflicts(allocatable, null);
		}
	}

	public SortedSet<Appointment> getAndCreateList(Allocatable alloc) {
		SortedSet<Appointment> set = appointmentMap.get( alloc);
		if ( set == null)
		{
			set = new TreeSet<Appointment>(new AppointmentStartComparator());
			appointmentMap.put(alloc, set);
		}
		return set;
	}

    public Conflict[] getConflicts(Reservation reservation) throws RaplaException {
    	initIfNotNull();
    	synchronized (operator.getLock()) {
            ArrayList<Conflict> conflictList = new ArrayList<Conflict>();
            Allocatable[] allocatables = reservation.getAllocatables();
            for ( Allocatable allocatable:allocatables)
            {
    			if ( allocatable.isHoldBackConflicts())
    			{
    				continue;
    			}

            	SortedSet<Appointment> allAppointments = getAndCreateList(allocatable);
            	Appointment[] appointments = reservation.getAppointmentsFor(allocatable);
            	for ( Appointment appointment: appointments)
            	{
            		for ( Appointment overlappingAppointment: allAppointments)
            		{
            			if ( appointment.overlaps( overlappingAppointment))
            			{
            				addConflicts(conflictList,appointment,overlappingAppointment,allocatable);
            			}
            		}
            	}
            }
            return (Conflict[]) conflictList.toArray(Conflict.CONFLICT_ARRAY);
        }
    }
        
	public void updateConflicts(AllocationChangeEvent[] changeEvents) 
    {
    	if ( appointmentMap == null)
    	{
    		return;
    	}
    	Date today = new Date();
    	Map<Allocatable,AllocationChange> toUpdate = new HashMap<Allocatable,AllocationChange>();
    	for ( AllocationChangeEvent evt: changeEvents)
		{
			Allocatable allocatable = evt.getAllocatable();
			AllocationChange updateSet = toUpdate.get( allocatable);
			if ( updateSet == null)
			{
				updateSet = new AllocationChange();
				toUpdate.put(allocatable, updateSet);
			}
			Collection<Appointment> appointmentSet = getAndCreateList(allocatable);
			Type type = evt.getType();
			if (type == AllocationChangeEvent.REMOVE)
			{
				Appointment appointment = evt.getOldAppointment();
				appointmentSet.remove(appointment);
				updateSet.toRemove.add( appointment);
			}
			else if (type == AllocationChangeEvent.ADD)
			{
				Appointment appointment = evt.getNewAppointment();
				Date maxEnd = appointment.getMaxEnd();
				if (maxEnd == null  || maxEnd.after( today));
				{
					appointmentSet.add(appointment);
					updateSet.toChange.add( appointment);
				}
			}
			else if (type == AllocationChangeEvent.CHANGE)
			{
				Appointment old = evt.getOldAppointment();
				appointmentSet.remove(old);
				updateSet.toRemove.add( old);
				Appointment appointment = evt.getNewAppointment();
				Date maxEnd = appointment.getMaxEnd();
				if (maxEnd == null  || maxEnd.after( today));
				{
					appointmentSet.add(appointment);
					updateSet.toChange.add( appointment);
				}
			}
			else
			{
				throw new IllegalStateException("AllocationChangeEventType " + type + " not supported");
			}
		}
    	for ( Map.Entry<Allocatable, AllocationChange> entry:toUpdate.entrySet())
    	{
    		Allocatable allocatable = entry.getKey();
    		AllocationChange changedAppointments = entry.getValue();
			
			updateConflicts( allocatable, changedAppointments);
    	}
	}
    class AllocationChange
    {
    	SortedSet<Appointment> toChange =  new TreeSet<Appointment>(new AppointmentStartComparator());
    	SortedSet<Appointment> toRemove=  new TreeSet<Appointment>(new AppointmentStartComparator());
    }
	
    private void updateConflicts(Allocatable allocatable,AllocationChange change) {
        
    	
		SortedSet<Appointment> allAppointments = appointmentMap.get(allocatable);
		SortedSet<Appointment> changedAppointments;
    	SortedSet<Appointment> removedAppointments;
		if ( change == null)
		{
			changedAppointments = allAppointments;
	    	removedAppointments = new TreeSet<Appointment>();

		}
		else
		{
			changedAppointments = change.toChange;
	    	removedAppointments = change.toRemove;
		}
		
		Set<Conflict> conflictList = conflictMap.get(allocatable);
		if ( conflictList == null)
		{
			conflictList= new LinkedHashSet<Conflict>();
			conflictMap.put( allocatable, conflictList);
		}
		else
		{
			removeConflicts(conflictList, removedAppointments);
			removeConflicts(conflictList, changedAppointments);
		}
		
		{
			SortedSet<AppointmentBlock> allAppointmentBlocks = createBlocks(allAppointments, new AppointmentBlockEndComparator());
			SortedSet<AppointmentBlock> appointmentBlocks = createBlocks(changedAppointments, new AppointmentBlockStartComparator());
            
			
			// Check the conflicts for each time block
			for (AppointmentBlock appBlock:appointmentBlocks)
			{
				final Appointment appointment1 = appBlock.getAppointment();
				
                long start = appBlock.getStart();
				/*
				 * Shrink the set of all time blocks down to those with a start date which is
				 * later than or equal to the start date of the block
				 */
				AppointmentBlock compareBlock = new AppointmentBlock(start, start, appointment1,false);
				final SortedSet<AppointmentBlock> tailSet = allAppointmentBlocks.tailSet(compareBlock);
            	
				// Check all time blocks which start after or at the same time as the block which is being checked
				for (AppointmentBlock appBlock2:tailSet)
				{
					// If the start date of the compared block is after the end date of the block, quit the loop
					if (appBlock2.getStart() > appBlock.getEnd())
					{
						break;
					}
					// Check if the corresponding appointments of both blocks overlap each other
                    final Appointment appointment2 = appBlock2.getAppointment();
                    if (appointment2.overlaps(appointment1))
					{
						// Add appointments to conflict list
                    	addConflicts(conflictList, appointment1, appointment2,  allocatable);
					}
				}
			}
		}
		
    }

	public void removeConflicts(Set<Conflict> conflictList,
			Set<Appointment> list) {
		for ( Iterator<Conflict> it = conflictList.iterator();it.hasNext();)
		{
			Conflict conflict = it.next();
			Appointment appointment1 = conflict.getAppointment1();
			Appointment appointment2 = conflict.getAppointment2();
			if ( list.contains( appointment1) || list.contains( appointment2))
			{
				it.remove();
			}
		}
	}

	public SortedSet<AppointmentBlock> createBlocks(
			SortedSet<Appointment> appointmentSet,final Comparator<AppointmentBlock> comparator) {
		
		// Create a new set of time blocks, ordered by their start dates
		SortedSet<AppointmentBlock> allAppointmentBlocks = new TreeSet<AppointmentBlock>(comparator);

		if ( appointmentSet.isEmpty())
		{
			return allAppointmentBlocks;
		}
		Appointment last = appointmentSet.last();
		
		// Get all time blocks of all appointments
		for (Appointment appointment:appointmentSet)
		{
			// Get the end date of the appointment (if repeating, end date of last occurence)
			Date end = appointment.getMaxEnd();
			
			// Check if the appointment is repeating forever
			if (end == null)
			{
				// If the repeating has no end, set the end to the start of the last appointment in the set + 100 weeks (~2 years)
				
				end = new Date(last.getStart().getTime() + DateTools.MILLISECONDS_PER_WEEK * 100);
			}
			
			/*
			 * If the appointment has a repeating, get all single time blocks of it. If it is no
			 * repeating, this will just create one block, which is equal to the appointment
			 * itself.
			 */
			appointment.createBlocks(appointment.getStart(), DateTools.fillDate(end), allAppointmentBlocks);
		}
		return allAppointmentBlocks;
	}


	/**
	 * Determines all conflicts which occur after a given start date.
	 * Note: This method is always called with <code>user = null</code>, see call hierarchy
	 * 
	 * Changed in: Rapla 1.6
	 * @param allocatables 
	 */
	public Collection<Conflict> getConflicts( Collection<Allocatable> allocatables, User user) throws RaplaException
	{
		initIfNotNull();
		Collection<Conflict> conflictList = new HashSet<Conflict>();
		for ( Allocatable allocatable: allocatables)
		{
			Set<Conflict> set = conflictMap.get( allocatable);
			if ( allocatable.isHoldBackConflicts())
			{
				continue;
			}
			if ( set != null)
			{
				for ( Conflict conflict: set)
				{
					
					Reservation reservation = conflict.getReservation1();
					Reservation overlappingReservation = conflict.getReservation2();
					if (user == null || RaplaComponent.canModify(reservation, user) || RaplaComponent.canModify(overlappingReservation, user))
					{
						conflictList.add(conflict);
					}
				}
			}
		}
		return conflictList;
	}


	public Set<Allocatable> getAllocatableBindings(Collection<Allocatable> allocatables,Appointment forAppointment) throws RaplaException {
        Set<Allocatable> allocatableSet = new HashSet<Allocatable>();
        initIfNotNull();
        for ( Map.Entry<Allocatable, SortedSet<Appointment>> entry:appointmentMap.entrySet())
        {
        	Allocatable allocatable = entry.getKey();
			if ( allocatable.isHoldBackConflicts())
			{
				continue;
			}
        	if ( !allocatables.contains( allocatable))
        	{
        		continue;
        	}
        	SortedSet<Appointment> appointments = entry.getValue();
        	Date start =forAppointment.getStart();
			Date end = forAppointment.getMaxEnd();
			SortedSet<Appointment> appointmentsToTest = LocalCache.getAppointments(appointments, null, start, end);	
        	for ( Appointment overlappingAppointment: appointmentsToTest)
        	{
        		if ( overlappingAppointment.overlaps( forAppointment) && !overlappingAppointment.equals( forAppointment))
        		{
        			allocatableSet.add( allocatable);
        		}
        		
        	}
        
        }
        return allocatableSet;
    }

	

    private Conflict createConflict(Appointment a1,
                                    Appointment a2,Allocatable allocatable) {

        return new ConflictImpl(a1.getReservation()
                            ,a1
                            ,allocatable
                            ,a2.getReservation()
                            ,a2.getReservation().getOwner()
                            ,a2);
    }


	private void addConflicts(Collection<Conflict> conflictList, Appointment appointment1, Appointment appointment2,   Allocatable allocatable)
	{
		Reservation reservation = appointment1.getReservation();
		Reservation overlappingReservation = appointment2.getReservation();
		if (reservation.equals(overlappingReservation))
			return;
        {
            final Conflict conflict = createConflict(appointment2, appointment1, allocatable);
            // Rapla 1.4: Don't add conflicts twice
            if (!conflictList.contains(conflict) )
            {
                conflictList.add(conflict);
            }
        }
	}
    

	


}
