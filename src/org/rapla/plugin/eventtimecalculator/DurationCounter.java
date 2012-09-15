package org.rapla.plugin.eventtimecalculator;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.tablesorter.TableSorter;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.tableview.internal.AppointmentTableModel;
import org.rapla.plugin.tableview.internal.ReservationTableModel;
import org.rapla.plugin.tableview.internal.SummaryExtension;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

/**
* User: kuestermann
* Date: 22.08.12
* Time: 09:29
*/
public final class DurationCounter extends RaplaComponent implements SummaryExtension {
    Configuration config;
    public DurationCounter(RaplaContext context, Configuration config) throws RaplaException {
         super(context);
         this.config = config;
     }

     public void init(final JTable table, JPanel summaryRow) {
         final JLabel counter = new JLabel();
         summaryRow.add( Box.createHorizontalStrut(30));

         summaryRow.add( counter);
         table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

             public void valueChanged(ListSelectionEvent arg0)
             {
                 int[] selectedRows = table.getSelectedRows();
                 TableModel model = table.getModel();
                 if ( model instanceof TableSorter)
                 {
                     model = ((TableSorter)model).getTableModel();
                 }
                 long totalduration = 0;
                 for ( int row:selectedRows)
                 {
                     if ( model instanceof AppointmentTableModel)
                     {
                         AppointmentBlock block = ((AppointmentTableModel) model).getAppointmentAt(row);
                         long duration = EventTimeCalculatorFactory.calcDuration(config, block);
                         totalduration+= duration;
                     }
                     if ( model instanceof ReservationTableModel)
                     {
                         Reservation block = ((ReservationTableModel) model).getReservationAt(row);
                         long duration = EventTimeCalculatorFactory.calcDuration(config, block);
                         if ( duration <0)
                         {
                             totalduration = -1;
                             break;
                         }
                         totalduration+= duration;
                     }
                 }
                 I18nBundle i18n = (I18nBundle) getService(I18nBundle.ROLE + "/" + EventTimeCalculatorPlugin.RESOURCE_FILE);
                 counter.setText( i18n.getString("total_duration") + " " + EventTimeCalculatorFactory.format(config, totalduration));
             }
         });
     }
 }
