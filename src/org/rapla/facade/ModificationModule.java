/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
package org.rapla.facade;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.rapla.entities.Category;
import org.rapla.entities.Entity;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.configuration.CalendarModelConfiguration;
import org.rapla.entities.configuration.RaplaMap;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaException;
/** All methods that allow modifing the entity-objects.
*/

public interface ModificationModule {
    /** check if the reservation can be saved */
    void checkReservation(Reservation reservation) throws RaplaException;
    /** creates a new Rapla Map. Keep in mind that only RaplaObjects and Strings are allowed as entries for a RaplaMap!*/
    <T> RaplaMap<T> newRaplaMap( Map<String,T> map);
    /** creates an ordered RaplaMap with the entries of the collection as values and their position in the collection from 1..n as keys*/
    <T> RaplaMap<T> newRaplaMap( Collection<T> col);

    /** WARNING! API could change for this method
     * @param extensionMap can be null*/
    CalendarModelConfiguration newRaplaCalendarModel(RaplaMap<? extends RaplaObject> selected , ClassificationFilter[] allocatableFilter, ClassificationFilter[] eventFilter, String title, Date startDate,Date endDate, Date selectedDate, String view, RaplaMap<String> optionMap);

    Reservation newReservation() throws RaplaException;
    Appointment newAppointment(Date startDate,Date endDate) throws RaplaException;
    Appointment newAppointment(Date startDate,Date endDate, RepeatingType repeatingType, int repeatingDuration) throws RaplaException;
    Allocatable newResource() throws RaplaException;
    Allocatable newPerson() throws RaplaException;
    Period newPeriod() throws RaplaException;
    Category newCategory() throws RaplaException;
    Attribute newAttribute(AttributeType attributeType) throws RaplaException;
    DynamicType newDynamicType(String classificationType) throws RaplaException;
    User newUser() throws RaplaException;

    /** Clones an entity. The entities will get new identifier and
     won't be equal to the original. The resulting object is not persistant and therefore
     can be editet.
     */
    <T extends Entity<T>> T clone(Entity<T> obj) throws RaplaException;

    /** This call will be delegated to the {@link org.rapla.storage.StorageOperator}. It
     * returns an editable working copy of an object. Only objects return by this method and new objects are editable.
     * To get the persistant, non-editable version of a working copy use {@link #getPersistant} */
    <T extends Entity<T>> T edit(Entity<T> obj) throws RaplaException;

    /** Returns the persistant version of a working copy.
     * Throws an {@link org.rapla.entities.EntityNotFoundException} when the
     * object is not found
     * @see #edit
     * @see #clone
     */
    <T> T getPersistant(Entity<T> working) throws RaplaException;

    /** This call will be delegated to the {@link org.rapla.storage.StorageOperator} */
    void storeObjects(Entity<?>[] obj) throws RaplaException;
    /** @see #storeObjects(Entity[]) */
    void store(Entity<?> obj) throws RaplaException;
    /** This call will be delegated to the {@link org.rapla.storage.StorageOperator} */
    void removeObjects(Entity<?>[] obj) throws RaplaException;
    /** @see #removeObjects(Entity[]) */
    void remove(Entity<?> obj) throws RaplaException;

    /** stores and removes objects in the one transaction
     * @throws RaplaException */
    void storeAndRemove( Entity<?>[] storedObjects, Entity<?>[] removedObjects) throws RaplaException;


}





