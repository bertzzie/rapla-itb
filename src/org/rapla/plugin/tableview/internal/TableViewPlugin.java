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
package org.rapla.plugin.tableview.internal;

import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.util.xml.XMLWriter;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DateCellRenderer;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;
import org.rapla.plugin.tableview.AppointmentTableColumn;
import org.rapla.plugin.tableview.ReservationTableColumn;

public class TableViewPlugin extends RaplaGUIComponent implements PluginDescriptor
{

	public TableViewPlugin(RaplaContext context) throws RaplaException {
		super(context);
	}

	 public static final String PLUGIN_CLASS = TableViewPlugin.class.getName();

	static boolean ENABLE_BY_DEFAULT = true;

    public String toString()
    {
        return "Table View";
    }

    public void provideServices(final Container container, Configuration config)
    {
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponent( RaplaExtensionPoints.CLIENT_EXTENSION, CSVExportInititalizer.class.getName(), PLUGIN_CLASS, config);
        
        container.addContainerProvidedComponent
        (
         RaplaExtensionPoints.CALENDAR_VIEW_EXTENSION
         ,ReservationTableViewFactory.class.getName()
         ,ReservationTableViewFactory.TABLE_VIEW
         ,null
         );
        
        container.addContainerProvidedComponent
        (
         RaplaExtensionPoints.CALENDAR_VIEW_EXTENSION
         ,AppointmentTableViewFactory.class.getName()
         ,AppointmentTableViewFactory.TABLE_VIEW
         ,null
         );
        
        
		addReservationTableColumns(container);
        addAppointmentTableColumns(container);

        //Summary rows
        container.addContainerProvidedComponent(RaplaExtensionPoints.RESERVATION_TABLE_SUMMARY, EventCounter.class.getName());
		container.addContainerProvidedComponent(RaplaExtensionPoints.APPOINTMENT_TABLE_SUMMARY, EventCounter.class.getName());
        
        
    }

	protected void addAppointmentTableColumns(final Container container) {
		container.addContainerProvidedComponentInstance(
                RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN, new AppointmentNameColumn()
                );

        
        container.addContainerProvidedComponentInstance(
                RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN, new AppointmentTableColumn() {
       			
       			public void init(TableColumn column) {
       		        column.setCellRenderer( new DateCellRenderer( getRaplaLocale()));
       		        column.setMaxWidth( 175 );
       		        column.setPreferredWidth( 175 );
       			}
       			
       			public Object getValue(AppointmentBlock block) {
       				return new Date(block.getStart());
       			}
       			
       			public String getColumnName() {
       				return getString("start_date");
       			}
       			
       			public Class<?> getColumnClass() {
       				return Date.class;
       			}

				public String getHtmlValue(AppointmentBlock block) 
				{
					RaplaLocale raplaLocale = getRaplaLocale();
					final Date date = new Date(block.getStart());
					String dateString= raplaLocale.formatDateLong(date) +  " " + raplaLocale.formatTime( date);
					return dateString;
				}
               }
        		);

        container.addContainerProvidedComponentInstance(
                RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN, new AppointmentTableColumn() {
       			
       			public void init(TableColumn column) {
       		        column.setCellRenderer( new DateCellRenderer( getRaplaLocale()));
       		        column.setMaxWidth( 175 );
       		        column.setPreferredWidth( 175 );
       			}
       			
       			public Object getValue(AppointmentBlock block) {
       				return new Date(block.getEnd());
       			}
       			
       			public String getColumnName() {
       				return getString("end_date");
       			}
       			
       			public Class<?> getColumnClass() {
       				return Date.class;
       			}

				public String getHtmlValue(AppointmentBlock block) 
				{
					RaplaLocale raplaLocale = getRaplaLocale();
					final Date date = new Date(block.getEnd());
					String dateString= raplaLocale.formatDateLong(date) +  " " + raplaLocale.formatTime( date);
					return dateString;
				}
               }
        		);


        
        container.addContainerProvidedComponentInstance(
                RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN, new AllocatableListColumn()
                {
                	@Override
                	protected boolean contains(Allocatable alloc)
                	{
                		return !alloc.isPerson();
                	}
                	
                	public String getColumnName() 
                	{
            			return getString("resources");
            		}
                }
                );


        container.addContainerProvidedComponentInstance(
                RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN, new AllocatableListColumn()
                {
                	@Override
                	protected boolean contains(Allocatable alloc)
                	{
                		return alloc.isPerson();
                	}
                	
                	public String getColumnName() 
                	{
            			return getString("persons");
            		}
                }
                );
	}

	protected void addReservationTableColumns(final Container container) {
		container.addContainerProvidedComponentInstance(
         RaplaExtensionPoints.RESERVATION_TABLE_COLUMN, new ReservationNameColumn()
         );
        
        container.addContainerProvidedComponentInstance(
                RaplaExtensionPoints.RESERVATION_TABLE_COLUMN, new ReservationTableColumn() {
       			
       			public void init(TableColumn column) {
       				column.setCellRenderer( new DateCellRenderer( getRaplaLocale()));
       		        column.setMaxWidth( 130 );
       		        column.setPreferredWidth( 130 );
       		        
       			}
       			
       			public Object getValue(Reservation reservation) {
       				return reservation.getFirstDate();
       			}
       			
       			public String getColumnName() {
       				return getString("start_date");
       			}
       			
       			public Class<?> getColumnClass() {
       				return Date.class;
       			}

				public String getHtmlValue(Reservation reservation) 
				{
					RaplaLocale raplaLocale = getRaplaLocale();
					final Date firstDate = reservation.getFirstDate();
					String string= raplaLocale.formatDateLong(firstDate) + " " + raplaLocale.formatTime( firstDate);
					return string;
				}
                }
               
                );

        container.addContainerProvidedComponentInstance(
                RaplaExtensionPoints.RESERVATION_TABLE_COLUMN, new ReservationTableColumn() {
       			
       			public void init(TableColumn column) {
       		        column.setCellRenderer( new DateCellRenderer( getRaplaLocale()));
       		        column.setMaxWidth( 130 );
       		        column.setPreferredWidth( 130 );
       			}
       			
       			public Object getValue(Reservation reservation) {
       				return reservation.getLastChangeTime();
       			}
       			
       			public String getColumnName() {
       				return getString("last_changed");
       			}
       			
       			public Class<?> getColumnClass() {
       				return Date.class;
       			}

				public String getHtmlValue(Reservation reservation) 
				{
					RaplaLocale raplaLocale = getRaplaLocale();
					final Date lastChangeTime = reservation.getLastChangeTime();
					String lastChanged= raplaLocale.formatDateLong(lastChangeTime);
					return lastChanged;
				}
               }
                );
	}

    public Object getPluginMetaInfos( String key )
    {
        if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return new Boolean( ENABLE_BY_DEFAULT );
        }
        return null;
    }


	static public final class EventCounter extends RaplaComponent implements SummaryExtension {
		public EventCounter(RaplaContext context) throws RaplaException {
			super(context);
		}

		public void init(final JTable table, JPanel summaryRow) {
			final JLabel counter = new JLabel();
			summaryRow.add( counter);
			table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				
				public void valueChanged(ListSelectionEvent arg0) 
				{
					int count = table.getSelectedRows().length;
					counter.setText( count+ " " + (count == 1 ? getString("reservation") : getString("reservations")));
				}
			});
		}
	}


	private class AllocatableListColumn implements AppointmentTableColumn {
		public void init(TableColumn column) {
		    column.setMaxWidth( 130 );
		    column.setPreferredWidth( 130 );
		}

		public Object getValue(AppointmentBlock block) 
		{
			Appointment appointment = block.getAppointment();
			Reservation reservation = appointment.getReservation();
			Allocatable[] allocatablesFor = reservation.getAllocatablesFor(appointment);
			StringBuilder buf = new StringBuilder();
			boolean first = true;
			for (Allocatable alloc: allocatablesFor)
			{
				if ( !contains( alloc))
				{
					continue;
				}
				if (!first)
				{
					buf.append(", ");
				}
				first = false;
				String name = alloc.getName( getLocale());
				buf.append( name);
				
			}
			return buf.toString();
		}

		protected boolean contains(Allocatable alloc) 
		{
			return true;
		}

		public String getColumnName() {
			return getString("resources");
		}

		public Class<?> getColumnClass() {
			return String.class;
		}

		public String getHtmlValue(AppointmentBlock block) 
		{
			String names = getValue(block).toString();
			return XMLWriter.encode(names);
		}
	}


	private final class ReservationNameColumn implements ReservationTableColumn{
		public void init(TableColumn column) {
		
		}

		public Object getValue(Reservation reservation) 
		{
		//	getLocale().
			return reservation.getName(getLocale());
		}
		
	
		public String getColumnName() {
			return getString("name");
		}

		public Class<?> getColumnClass() {
			return String.class;
		}

		public String getHtmlValue(Reservation event) {
			String value = getValue(event).toString();
			return XMLWriter.encode(value);		       

		}
		
		
	}
	
	private final class AppointmentNameColumn implements AppointmentTableColumn {
		public void init(TableColumn column) {
		
		}

		public Object getValue(AppointmentBlock block) 
		{
			//	getLocale().
			Appointment appointment = block.getAppointment();
			Reservation reservation = appointment.getReservation();
			return reservation.getName(getLocale());
		}

		public String getColumnName() {
			return getString("name");
		}

		public Class<?> getColumnClass() {
			return String.class;
		}

		public String getHtmlValue(AppointmentBlock block) {
			String value = getValue(block).toString();
			return XMLWriter.encode(value);		       

		}
	}

}

