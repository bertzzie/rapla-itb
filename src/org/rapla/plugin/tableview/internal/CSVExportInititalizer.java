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

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.rapla.components.iolayer.IOInterface;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.MenuExtensionPoint;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.tableview.RaplaTableColumn;

public class CSVExportInititalizer extends RaplaGUIComponent
{
	public CSVExportInititalizer( RaplaContext context ) throws RaplaException
    {
        super( context );
        MenuExtensionPoint export = (MenuExtensionPoint) getService( RaplaExtensionPoints.EXPORT_MENU_EXTENSION_POINT);
        export.insert(createExportMenu() );
    }

	private JMenuItem createExportMenu( )  {
	    JMenuItem item = new JMenuItem( "csv");
	    item.setIcon( getIcon("icon.export") );
	    item.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) {
	                try {
	                	CalendarSelectionModel model = getService(CalendarSelectionModel.class);
	                    export( model, getMainComponent());
	                } catch (Exception ex) {
	                    showException( ex, getMainComponent() );
	                }
	            }
	    });
	    return item;
	}
	
	private static final String LINE_BREAK = "\n"; 
	private static final String CELL_BREAK = ";"; 
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void export(final CalendarSelectionModel model,final Component parentComponent) throws Exception
	{
	    // generates a text file from all filtered events;
	    StringBuffer buf = new StringBuffer();
	    
	    Collection< ? extends RaplaTableColumn<?>> columns;
	    List<Object> objects = new ArrayList<Object>();
	    if (model.getViewId().equals(ReservationTableViewFactory.TABLE_VIEW))
	    {
	    	Map<?,?> map2 = getContainer().lookupServicesFor(RaplaExtensionPoints.RESERVATION_TABLE_COLUMN);
		    columns = (Collection<? extends RaplaTableColumn<?>>) map2.values();
		    objects.addAll(Arrays.asList( model.getReservations())); 
	    }
	    else
	    {
	    	Map<?,?> map2 = getContainer().lookupServicesFor(RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN);
		    columns = (Collection<? extends RaplaTableColumn<?>>) map2.values();
		    objects.addAll( model.getBlocks()); 
	    }
	    for (RaplaTableColumn column: columns)
    	{
	    	buf.append( column.getColumnName());
	    	buf.append(CELL_BREAK);
    	}
	    for (Object row: objects)
	    {
	    	buf.append(LINE_BREAK);
	    	for (RaplaTableColumn column: columns)
	    	{
	    		Object value = column.getValue( row);
	    		Class columnClass = column.getColumnClass();
	    		boolean isDate = columnClass.isAssignableFrom( java.util.Date.class);
	    		String formated;
				if ( isDate)
				{ 
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					format.setTimeZone( getRaplaLocale().getTimeZone());
					String timestamp = format.format(   (java.util.Date)value);
					formated = timestamp;
				}
				else
				{
					String escaped = escape(value);
					formated = escaped;
				}
				buf.append( formated );
	    	   	buf.append(CELL_BREAK);
	    	}
	    }
        byte[] bytes = buf.toString().getBytes();
		if (saveFile( bytes, "events.csv","csv"))
		{
			confirmPrint(getMainComponent());
		}
	}
	
	
	 protected boolean confirmPrint(Component topLevel) {
			try {
				DialogUI dlg = DialogUI.create(
	                    		 getContext()
	                    		,topLevel
	                            ,true
	                            ,getString("export")
	                            ,getString("file_saved")
	                            ,new String[] { getString("ok")}
	                            );
				dlg.setIcon(getIcon("icon.export"));
	            dlg.setDefault(0);
	            dlg.start();
	            return (dlg.getSelectedIndex() == 0);
			} catch (RaplaException e) {
				return true;
			}

	    }

	private String escape(Object cell) { 
		return cell.toString().replace(LINE_BREAK, " ").replace(CELL_BREAK, " "); 
	}
	
	public boolean saveFile(byte[] content,String filename, String extension) throws RaplaException {
		final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
		IOInterface io = (IOInterface) getService( IOInterface.class);
		try 
		{
			String file = io.saveFile( frame, null, new String[] {extension}, filename, content);
			return file != null;
		} 
		catch (IOException e) 
		{
			throw new RaplaException(e.getMessage(), e);
	    }
	}
}

