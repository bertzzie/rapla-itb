package org.rapla.components.mail;

public interface MailInterface {
    String ROLE = MailInterface.class.getName();
    /* Sends the mail.
       Callers should check if the parameters are all valid
       according to the SMTP RFC at http://www.ietf.org/rfc/rfc821.txt
       because the implementing classes may not check for validity
     */
    public void sendMail
        (
         String senderMail
         ,String recipient
         ,String subject
         ,String mailBody
         )
        throws MailException;
}
