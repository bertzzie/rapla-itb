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
package org.rapla.plugin.mail;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.mail.MailInterface;
import org.rapla.components.mail.MailapiClient;
import org.rapla.components.mail.SmtpClient;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.mail.internal.MailOption;
import org.rapla.plugin.mail.internal.RaplaMailToUserOnLocalhost;
import org.rapla.plugin.mail.internal.RaplaMailToUserOnServer;
import org.rapla.server.ServerService;
/** Provides the MailToUserInterface and the MailInterface for sending mails.
 * The MailInterface can only be used on a machine that can connect to the mailserver.
 * While the MailToUserInterface can be called from a client, it redirects the mail request to
 * the server, which must be able to connect to the mailserver.
 *
 * Example 1:
 *
 * <code>
 *  MailToUserInterface mail = (MailToUserInterface) getContext().loopup( MailToUserInterface.ROLE );
 *  mail.sendMail( subject, body );
 * </code>
 *
 * Example 2:
 *
 * <code>
 *  MailInterface mail = (MailInterface) getContext().loopup( MailInterface.ROLE );
 *  mail.sendMail( senderMail, recipient, subject, body );
 * </code>

 * @see org.rapla.components.mail.MailInterface
 * @see org.rapla.plugin.mail.MailToUserInterface
 */
public class MailPlugin implements PluginDescriptor
{
    public static final String MAIL_ON_SERVER = MailPlugin.class.getPackage().getName() + ".MailOnServer";
    public static final String MAIL_ON_LOCALHOST = MailPlugin.class.getPackage().getName() + ".MailOnLocalhost";
    public static final String DEFAULT_SENDER_ENTRY = "org.rapla.plugin.mail.DefaultSender";

    
    public String toString() {
        return "Mail Service";
    }

    public void provideServices(Container container, Configuration config) {
        container.addContainerProvidedComponent( RaplaExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION,MailOption.class.getName(),MailPlugin.class.getName(), config);

        
        if ( !config.getAttributeAsBoolean("enabled", false) )
        	return;
        
        String mailClass =config.getChild("mailinterface").getValue( null);
        
        if ( mailClass == null)
        {
	        // Use the mail API
	        if ( config.getAttributeAsBoolean("usemailapi", true))
	        {
	            mailClass = MailapiClient.class.getName();
	        }
	        else
	        {
	        	mailClass = SmtpClient.class.getName();
	        }
        }
        
        container.addContainerProvidedComponent( MailInterface.ROLE, mailClass, mailClass , config);
        
        if ( container.getContext().has( ServerService.ROLE) ){
            // only add mail service on localhost
            container.addContainerProvidedComponent( MailToUserInterface.ROLE, RaplaMailToUserOnLocalhost.class.getName(), MAIL_ON_LOCALHOST , config);
        } else {
            // the following order is important for resolving,
            // first add the service on the server
            // then on localhost
            container.addContainerProvidedComponent( MailToUserInterface.ROLE, RaplaMailToUserOnServer.class.getName(),MAIL_ON_SERVER, config);
            container.addContainerProvidedComponent( MailToUserInterface.ROLE, RaplaMailToUserOnLocalhost.class.getName(), MAIL_ON_LOCALHOST , config);
        }
    }

    public Object getPluginMetaInfos( String key )
    {
        return null;
    }
}

