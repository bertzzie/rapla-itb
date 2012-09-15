package org.rapla.components.mail;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
Dummy Implementation of the MailInterface writes Mail to a file.
This can be used for testing the notification service without sending
an email.

Sample Configuration.
<pre>
    &lt;mail-to-file id="mail-to-file">
       &lt;dir>/tmp/rapla-mails/&lt;/dir>
    &lt;/mail-to-file>
</pre>


*/
public class MailToFile implements Configurable,MailInterface {
    String dirPath;

    public MailToFile( ) throws ConfigurationException{
    }

    public MailToFile( Configuration config) throws ConfigurationException{
        configure( config);
    }


    public void configure(Configuration config) throws ConfigurationException {
        // get the configuration entry text with the default-value "Welcome"
        dirPath = config.getChild("dir").getValue();
    }

    public void sendMail(String senderMail,String recipient, String subject, String mailBody) throws MailException {
    	File dir = null;
    	File file = null;
    	PrintWriter writer = null;
    	try {
    	    dir = new File(dirPath);
    	    if (!dir.exists())
    	        throw new MailException(dirPath + " doesn't exist.");
        } catch (SecurityException e) {
                throw new MailException("Can't access " + dirPath + " \n" + e.getMessage());
    	}

        try {
    	    int messageNumber = 1;
    	    while (file == null || file.exists()) {
        		file = new File(dir,"msg" + messageNumber);
        		messageNumber ++;
    	    }
    	    writer = new PrintWriter(new FileWriter(file));
    	    writer.println("From: " + senderMail);
    	    writer.println("To: " + recipient);
    	    writer.println("Subject: " + subject);
    	    writer.println("");
    	    writer.print(mailBody);
            } catch (SecurityException e) {
                throw new MailException("Can't access " + file + " \n" + e.getMessage());
            } catch (IOException e) {
                throw new MailException("I/O Error while writing file: "
    				    + file + " \n" + e.getMessage());
        } finally {
    	    if (writer != null)
    		writer.close();
    	}
        return;
    }

}





