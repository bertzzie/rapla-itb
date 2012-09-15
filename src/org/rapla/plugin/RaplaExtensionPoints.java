package org.rapla.plugin;

import org.rapla.framework.RaplaContext;
import org.rapla.gui.AppointmentStatusFactory;
import org.rapla.gui.MenuExtensionPoint;
import org.rapla.gui.ObjectMenuFactory;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.ReservationWizard;
import org.rapla.gui.ViewFactory;
import org.rapla.plugin.tableview.AppointmentTableColumn;
import org.rapla.plugin.tableview.ReservationTableColumn;
import org.rapla.plugin.tableview.internal.SummaryExtension;
import org.rapla.servletpages.HTMLMenuExtensionPoint;




/** Constant Pool of all basic extension points of the Rapla system.
 * You can add your extension  in the provideService Method of your PluginDescriptor
 * <pre>
 * container.addContainerProvidedComponent( REPLACE_WITH_EXTENSION_POINT_NAME, REPLACE_WITH_CLASS_IMPLEMENTING_EXTENSION, config);
 * </pre>
 * @see org.rapla.framework.PluginDescriptor
*/

public interface RaplaExtensionPoints
{
    /** add your own views to Rapla, by providing a org.rapla.gui.ViewFactory 
     * @see ViewFactory
     * */
    String CALENDAR_VIEW_EXTENSION = "org.rapla.gui.CalendarView";

    /** A client extension is started automaticaly when a user has successfully login into the Rapla system. A class added as service doesn't need to implement a specific interface and is instanciated automaticaly after client login. You can add a RaplaContext parameter to your constructor to get access to the services of rapla. 
     @see RaplaContext
     */
    String CLIENT_EXTENSION = "org.rapla.clientPlugin";
    /** A server extension is started automaticaly when the server is up and running and connected to a data store. A class added as service doesn't need to implement a specific interface and is instanciated automaticaly after server start. You can add a RaplaContext parameter to your constructor to get access to the services of rapla.
     @see RaplaContext
     * */
    String SERVER_EXTENSION = "org.rapla.serverPlugin";

    /**
     * You can add arbitrary serlvet pages to your rapla webapp.
     *
     * Example that adds a page with the name "my-page-name" and the class
     * "org.rapla.plugin.myplugin.MyPageGenerator". You can call this page with <code>rapla?page=my-page-name</code>
     * <p/>
     * In the provideService Method of your PluginDescriptor do the following
     <pre>
     container.addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, "org.rapla.plugin.myplugin.MyPageGenerator", "my-page-name", config);
     </pre>

    *@see org.rapla.servletpages.RaplaPageGenerator
     m*/
    String SERVLET_PAGE_EXTENSION = "org.rapla.serverPage";

    /** You can add a specific configuration panel for your plugin.
     * @see org.rapla.entities.configuration.Preferences
     * @see OptionPanel
     * */
    String PLUGIN_OPTION_PANEL_EXTENSION = "org.rapla.plugin.Option";
    /** You can add additional option panels for editing the user preference.
     * @see org.rapla.entities.configuration.Preferences
     * @see OptionPanel
     * */
    String USER_OPTION_PANEL_EXTENSION = "org.rapla.UserOptions";
    /** You can add additional option panels for the editing the system preferences
    * @see org.rapla.entities.configuration.Preferences
    * @see OptionPanel
    * */
    String SYSTEM_OPTION_PANEL_EXTENSION = "org.rapla.SystemOptions";

    /** add your own wizard to create events. See ReservationWizard class. Your plugin should provid a ReservationWizard 
     *@see ReservationWizard 
     **/
    String RESERVATION_WIZARD_EXTENSION = "org.rapla.gui.Reservationwizard";


    /** add your own menu entries in the context menu of an object. To do this provide
      an ObjectMenuFactory under this entry.
      @see ObjectMenuFactory
      */
    String OBJECT_MENU_EXTENSION = "org.rapla.gui.ObjectMenuFactory";

    /** add a footer for summary of appointments in edit window
     * provide an AppointmentStatusFactory to add your own footer to the appointment edit 
       @see AppointmentStatusFactory 
     * */
    String APPOINTMENT_STATUS = "org.rapla.gui.edit.reservation.AppointmentStatus";
    
    /** add a column for the reservation table 
     * 
     @see ReservationTableColumn
     */
    String RESERVATION_TABLE_COLUMN = "org.rapla.plugin.tableview.reservationcolumn";
    
    
    /** add a summary footer for the reservation table 
    @see SummaryExtension
    * */
    String RESERVATION_TABLE_SUMMARY = "org.rapla.plugin.tableview.reservationsummary";
    
    /** add a summary footer for the appointment table 
     @see SummaryExtension
    * */
    String APPOINTMENT_TABLE_SUMMARY = "org.rapla.plugin.tableview.appointmentsummary";
    
    /** add a column for the appointment table 
     @see AppointmentTableColumn 
     * */
    String APPOINTMENT_TABLE_COLUMN = "org.rapla.plugin.tableview.appointmentcolumn";
  
    /** add your own submenus to the admin menu. Get the MenuExtensionPoint via the lookup method and add the menu.
     @see MenuExtensionPoint
     */
    String ADMIN_MENU_EXTENSION_POINT ="org.rapla.gui.AdminMenuInsert";
    /** add your own import-menu submenus
     @see MenuExtensionPoint
     */
    String IMPORT_MENU_EXTENSION_POINT ="org.rapla.gui.ImportMenuInsert";
    /** add your own export-menu submenus    
      @see MenuExtensionPoint
     */
    String EXPORT_MENU_EXTENSION_POINT ="org.rapla.gui.ExportMenuInsert";
    /** add your own view-menu submenus    
     @see MenuExtensionPoint
     */
    String VIEW_MENU_EXTENSION_POINT ="org.rapla.gui.ViewMenuInsert";
    /** add your own edit-menu submenus    
     @see MenuExtensionPoint
     */ 
    String EDIT_MENU_EXTENSION_POINT = "org.rapla.gui.EditMenuInsert";
    /** add your own help-menu submenus    
     @see MenuExtensionPoint
     */
    String HELP_MENU_EXTENSION_POINT = "org.rapla.gui.ExtraMenuInsert";

    /** you can add your own entries on the index page Just add a HTMLMenuEntry to the list
     @see HTMLMenuExtensionPoint
     * */
    String HTML_MAIN_MENU_EXTENSION_POINT = "org.rapla.servletpages";

    /** you can add servlet pre processer to manipulate request and response before standard processing is
     * done by rapla
     */
    String SERVLET_REQUEST_RESPONSE_PREPROCESSING_POINT = "org.rapla.servlet.preprocessing";
}
