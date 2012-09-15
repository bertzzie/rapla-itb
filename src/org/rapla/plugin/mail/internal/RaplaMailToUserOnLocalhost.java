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
package org.rapla.plugin.mail.internal;

import org.rapla.components.mail.MailException;
import org.rapla.components.mail.MailInterface;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.mail.MailPlugin;
import org.rapla.plugin.mail.MailToUserInterface;

public class RaplaMailToUserOnLocalhost extends RaplaComponent implements MailToUserInterface
{

        public RaplaMailToUserOnLocalhost( RaplaContext context) throws RaplaException {
            super( context );
        }
        
        public void sendMail(String userName,String subject, String body) throws RaplaException {
            User recipientUser = getQuery().getUser( userName );
            // O.K. We need to generate the mail
            String recipientEmail = recipientUser.getEmail();
            if (recipientEmail == null || recipientEmail.trim().length() == 0) {
                getLogger().warn("No email address specified for user "  
                                 + recipientUser.getUsername()
                                 + " Can't send mail.");
                return;
            }


            final MailInterface mail = (MailInterface)getContext().lookup(MailInterface.ROLE);
            ClientFacade facade = (ClientFacade)  getContext().lookup(ClientFacade.ROLE);
            Preferences prefs = facade.getPreferences( null);
            final String defaultSender = prefs.getEntryAsString( MailPlugin.DEFAULT_SENDER_ENTRY, "");
            try {
                mail.sendMail( defaultSender, recipientEmail,subject, body);
            } catch (MailException ex) {
                throw new RaplaException( ex );
            }
            getLogger().info("Email send to user " + userName);
        }

		
  
}

