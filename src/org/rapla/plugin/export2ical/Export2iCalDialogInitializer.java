package org.rapla.plugin.export2ical;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.rapla.components.iolayer.IOInterface;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.MenuExtensionPoint;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.plugin.RaplaExtensionPoints;

public class Export2iCalDialogInitializer extends RaplaGUIComponent {

	
	
	public Export2iCalDialogInitializer(RaplaContext sm) throws RaplaException {
		super(sm);

		

		if (!isModifyPreferencesAllowed()) {
			return;
		}

		setChildBundleName(Export2iCalPlugin.RESOURCE_FILE);

		MenuExtensionPoint exportExtension = (MenuExtensionPoint) getService(RaplaExtensionPoints.EXPORT_MENU_EXTENSION_POINT);
		
		final StartupEnvironment startupEnvironment = getService(StartupEnvironment.class);
        final int startupMode = startupEnvironment.getStartupMode();
        boolean hasserver = getUpdateModule().isClientForServer();
        if ( hasserver && startupMode != StartupEnvironment.APPLET)
        {
    		exportExtension.insert(exportiCalToFile());
        }
		getLogger().info("Export2iCal plugin added");
	}

	private JMenuItem exportiCalToFile() {

		JMenuItem item = new JMenuItem(getString("export_file_text"));
		item.setIcon(getIcon("icon.export"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				getCalendarOptions();
				try {
					CalendarModel raplaCal = getService(CalendarModel.class);
				    StringBuffer buf = new StringBuffer();
				    for (Reservation reservation:raplaCal.getReservations())
				    {
				        boolean first = buf.length() == 0;
				        if ( !first)
				            buf.append(",");
				        final String idString = ((RefEntity<?>) reservation).getId().toString().split("_")[1];
                        buf.append( idString );
				    }
				    String ids = buf.toString();
				    String result = getWebservice( ICalExport.class).export(ids);
				    if ( result.trim().length() == 0)
				    {
				        JOptionPane.showMessageDialog(null, getString("no_dates_text"), "Export2iCal", JOptionPane.INFORMATION_MESSAGE);
				    }
				    else
				    {
				        String nonEmptyTitle = raplaCal.getNonEmptyTitle();
				        if ( nonEmptyTitle.length() == 0)
				        {
				            nonEmptyTitle = "rapla_calendar";
				        }
                        String name =nonEmptyTitle +".ics";
				        export(result, name);
				    }
				} catch (Exception ex) {
					showException(ex, getMainComponent());
				}
			}
		});
		return item;
	}

	private void export(String result, String name) throws RaplaException {
		final byte[] bytes = result.getBytes();
        saveFile(bytes, name , new String[] {"ics","ical"});
//	    JFileChooser fileChooser = new JFileChooser();
//		fileChooser.setFileFilter(new iCalFileFilter());
//
//		if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
//			String path = fileChooser.getSelectedFile().getPath();
//			File file = null;
//			if (path.substring(path.length() - 4, path.length()).equals(".ics")) {
//				file = new File(path);
//			} else {
//				file = new File(path + ".ics");
//			}
//			try {
//				FileWriter fileWriter = new FileWriter(file);
//				BufferedWriter writer = new BufferedWriter( fileWriter);
//				writer.write( result);
//				//calOutputter.output(ical, fileWriter);
//				JOptionPane.showMessageDialog(null, getString("export_file_succesfull_text"), "Export2iCal", JOptionPane.INFORMATION_MESSAGE);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
	}

//	private class iCalFileFilter extends FileFilter {
//		public boolean accept(File file) {
//			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".ics"));
//		}
//
//		public String getDescription() {
//			return "iCal Files";
//		}
//	}

	public void saveFile(byte[] content, String filename, String[] extensions) throws RaplaException {
		final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
		IOInterface io = getService(IOInterface.class);
		try {
			io.saveFile(frame, null, extensions, filename, content);
		} catch (IOException e) {
			throw new RaplaException("Can't export file!", e);
		}
	}
}
