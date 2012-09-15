package org.rapla.plugin.mail;

import org.rapla.framework.RaplaException;

public interface MailToUserInterface
{
    String ROLE = MailToUserInterface.class.getName();
    
    void sendMail(String username,String subject, String body) throws RaplaException;
}
