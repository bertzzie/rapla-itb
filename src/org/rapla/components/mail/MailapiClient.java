package org.rapla.components.mail;


import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;



public class MailapiClient implements MailInterface
{
        
    String mailhost = "localhost";
    int port = 25;
    boolean ssl =false;
    String username;
    String password;
    
    public MailapiClient(Configuration config) throws ConfigurationException {
        // get the configuration entry text with the default-value "Welcome"
        setPort(config.getChild("smtp-port").getValueAsInteger(25));
        setSmtpHost(config.getChild("smtp-host").getValue());
        setUsername( config.getChild("username").getValue(null));
        setPassword( config.getChild("password").getValue(null));
        setSsl( config.getChild("ssl").getValueAsBoolean(false));
        
    }
    
    public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public MailapiClient()
    {
    }
    
           
    public void sendMail( String senderMail, String recipient, String subject, String mailBody ) throws MailException
    {
        Properties props = new Properties();
        props.put("mail.smtp.host", mailhost);
        props.put("mail.smtp.port", new Integer(port));
        javax.mail.Authenticator authenticator = null;
        if ( username != null)
        {
        	props.put("username", username);
        	if ( username.trim().length() > 0)
        	{
        		props.put("mail.smtp.auth","true");
        		authenticator = new javax.mail.Authenticator() {
                	   protected PasswordAuthentication getPasswordAuthentication() {
        					return new PasswordAuthentication(username,password);
        				}
                   };
                
        	}
        }
        if ( password != null)
        {
        	props.put("password", password);
        }
        if (ssl)
        {
        	props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");	
        	props.put("mail.smtp.socketFactory.port",  new Integer(port));
        }
        
        try {
        	Session session = Session.getInstance(props, authenticator);
        
   			Message message = new MimeMessage(session);
   			if ( senderMail != null && senderMail.trim().length() > 0)
   			{
   				message.setFrom(new InternetAddress(senderMail));
   			}
   			message.setRecipients(Message.RecipientType.TO,
   					InternetAddress.parse(recipient));
   			message.setSubject(subject);
   			message.setText(mailBody);
   			Transport.send(message);
        } catch (AddressException ex) {
            String message = ex.getMessage();
            throw new MailException( message, ex);
        } catch (MessagingException ex) {
        	String message = ex.getMessage();
        	throw new MailException( message, ex);
        }
    }



    public String getSmtpHost()
    {
        return mailhost;
    }


    public void setSmtpHost( String mailhost )
    {
        this.mailhost = mailhost;
    }


    public String getPassword()
    {
        return password;
    }


    public void setPassword( String password )
    {
        this.password = password;
    }


    public int getPort()
    {
        return port;
    }


    public void setPort( int port )
    {
        this.port = port;
    }


    public String getUsername()
    {
        return username;
    }


    public void setUsername( String username )
    {
        this.username = username;
    }

}