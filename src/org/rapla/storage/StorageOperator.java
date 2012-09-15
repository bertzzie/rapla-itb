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
/** A Facade Interface for manipulating the stored data.
 *  This abstraction allows Rapla to store the data
 *  in many ways. <BR>
 *  Currently implemented are the storage in an XML-File
 *  ,the storage in an SQL-DBMS and storage over a
 *  network connection.
 *  @see org.rapla.storage.dbsql.DBOperator
 *  @see org.rapla.storage.dbfile.XMLOperator
 *  @see org.rapla.storage.dbrm.RemoteOperator
 *  @author Christopher Kohlhaas
 */
package org.rapla.storage;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaException;

public interface StorageOperator {
    String ROLE = StorageOperator.class.getName();

    void connect() throws RaplaException;
    void connect(String username,char[] password) throws RaplaException;
    boolean isConnected();
    /** Refreshes the data. This could be helpfull if the storage
     * operator uses a cache and does not support "Active Monitoring"
     * of the original data */
    void refresh() throws RaplaException;
    void disconnect() throws RaplaException;

    /** should return a clone of the object. <strong>Never</strong> edit the
        original, <strong>always</strong> edit the object returned by editObject.*/
    <T> RefEntity<T> editObject(RefEntity<T> obj, User user) throws RaplaException;

    <T> T getPersistant(RefEntity<T> entity) throws EntityNotFoundException;
    /** Stores and/or removes entities and specifies a user that is responsible for the changes.
     * Notifies  all registered StorageUpdateListeners after a successful
     storage.*/
    void storeAndRemove(Collection<RefEntity<?>> storeObjects,Collection<RefEntity<?>> removeObjects,RefEntity<User> user) throws RaplaException;

    Object createIdentifier(RaplaType raplaType) throws RaplaException;

    <T extends RaplaObject> Collection<T> getObjects(Class<T> raplaType) throws RaplaException;

    /** returns all the objects (except reservations)that are visible for the current user */
    List<RefEntity<?>> getVisibleEntities(User user) throws RaplaException;

    /** returns the user or null if a user with the given username was not found. */
    User getUser(String username) throws RaplaException;
    Preferences getPreferences(User user) throws RaplaException;


    /** returns the reservations of the specified user, sorted by name.*/
    List<Reservation> getReservations(User user,Date start,Date end) throws RaplaException;

    /** returns the appointments of the specified user in the specified period, sorted by start-date */
    SortedSet<Appointment> getAppointments(User user,Date start,Date end) throws RaplaException;

    Category getSuperCategory();

    /** changes the password and calls storeObjects(new Object[] {user}) */
    void changePassword(RefEntity<User> user,char[] oldPassword,char[] newPassword) throws RaplaException;

    boolean canChangePassword();

    void addStorageUpdateListener(StorageUpdateListener updateListener);
    void removeStorageUpdateListener(StorageUpdateListener updateListener);

    Object getLock();

    Date today();

    boolean supportsActiveMonitoring();

}