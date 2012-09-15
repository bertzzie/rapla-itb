package org.rapla.components.mail;


import junit.framework.TestCase;

import org.apache.avalon.framework.logger.ConsoleLogger;

public class MailTest extends TestCase
{
    
    public void testMailSend() throws MailException
    {
        SmtpClient client = new SmtpClient();
        client.setSmtpHost("localhost");
        client.setSmtpPort( 5023);
        client.enableLogging( new ConsoleLogger( ConsoleLogger.LEVEL_DEBUG));
        
       
        MockMailServer mailServer = new MockMailServer();
        mailServer.setPort( 5023);
        mailServer.startMailer( true);
        String sender = "zimt@gmx.de";
        String recipient = "zimt@gmx.de";
        client.sendMail(sender,recipient,"HALLO", "Test body");
        assertEquals( sender.trim().toLowerCase(), mailServer.getSenderMail().trim().toLowerCase());
        assertEquals( recipient.trim().toLowerCase(), mailServer.getRecipient().trim().toLowerCase());
        
    }
}
