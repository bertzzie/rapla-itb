package org.rapla.gui;

import java.awt.Component;

import org.rapla.entities.Entity;
import org.rapla.entities.RaplaObject;
import org.rapla.framework.RaplaException;

public interface EditController
{
    public final static String ROLE = EditController.class.getName();

    EditComponent createUI( RaplaObject obj ) throws RaplaException;

    <T extends Entity<T>> void edit( Entity<T> obj, Component owner ) throws RaplaException;
    <T extends Entity<T>> void edit( Entity<T> obj, String title, Component owner ) throws RaplaException;

}