package org.rapla.components.mail;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;

/**
<p>Default Implementation of the MailInterface with smtp protocol
at http://www.ietf.org/rfc/rfc821.txt.
</p>
Sample Configuration for local Mail-server.
<pre>
    &lt;smtp-service id="default-smpt-client">
       &lt;smtp-host>localhost&lt;/smtp-host>
       &lt;smtp-port>25&lt;/smtp-port>
    &lt;/smtp-service>
</pre>

 Sample Configuration with pop3 before smpt enabled.
 Auhtentifies on the pop3 server before establashing a smtp connection.
<pre>
    &lt;smtp-service id="default-smpt-client">
       &lt;pop3-before-smtp>
         &lt;host>pop-server.your.mail.provider&lt;/host>
         &lt;user>your.username&lt;/user>
         &lt;password>your.password&lt;/password>
       &lt;/pop3-before-smtp>
       &lt;smtp-host>smpt-server.mail.provider&lt;/smtp-host>
       &lt;smtp-port>25&lt;/smtp-port>
    &lt;/smtp-service>
</pre>

*/
public class SmtpClient extends AbstractLogEnabled implements Configurable,MailInterface {
    //connection information
    boolean smtpAfterPop;
    String popHost;
    String popUser;
    String popPwd;
    String smtpHost;
    int smtpPort;
    String localHost;

    public SmtpClient(Logger logger, Configuration config) throws ConfigurationException{
        enableLogging( logger);
        configure( config);
    }
        
    public SmtpClient(){
        smtpAfterPop = false;
        smtpHost = "localhost";
        smtpPort = 25;
        localHost = "localhost";//msp.getLocalHost();
    }

    public void configure(Configuration config) throws ConfigurationException {
        // get the configuration entry text with the default-value "Welcome"
        setSmtpPort(config.getChild("smtp-port").getValueAsInteger(25));
        setSmtpHost(config.getChild("smtp-host").getValue());
        Configuration pop3Config = config.getChild("pop3-before-smtp",false);
        if (pop3Config != null) {
            setPopBeforeSmtp(true);
            setPopHost(pop3Config.getChild("host").getValue());
            setPopUser(pop3Config.getChild("username").getValue());
            setPopPwd(pop3Config.getChild("password").getValue());
        }

    }

    public void setPopHost(String s){
        popHost = s;
    }
    public void setPopUser(String s){
        popUser = s;
    }
    public void setPopPwd(String s){
        popPwd = s;
    }

    public void setPopBeforeSmtp(boolean enable){
        smtpAfterPop = enable;
    }

    public void setSmtpHost(String s){
        smtpHost = s;
    }
    public void setSmtpPort(int i){
        smtpPort = i;
    }

    public void setLocalHost(String s){
        localHost  = s;
    }
    public Pop3 popBeforeSmtp() throws MailException {
        Pop3 pop = new Pop3(popHost, popUser, popPwd);

        getLogger().debug("Try to connect to host " + popHost + " for user " + popUser + "....");

        PopStatus status = pop.connect();

        if ( status.OK() )
            getLogger().debug("Connection established...");
        else
            throw new MailException("Connection to pop server failed...");

        if ( status.OK() ) status = pop.login();

        if ( status.OK() )
            getLogger().debug("Login accepted!");

        if ( status.OK() )
            pop.get_TotalMsgs();

        if ( !status.OK() )
            throw new MailException("pop failed...");

        return pop;
    }



    private boolean isResponse(BufferedReader in, String expectedServerResponse) throws MailException,IOException {
        String responseLine = in.readLine();
        if (responseLine == null)
            return false;
        getLogger().debug("Server: " + responseLine);
        return responseLine.indexOf(expectedServerResponse)>=0;
    }

    public void sendMail(String senderMail,String recipient, String subject, String mailBody) throws MailException {
        Socket smtpSocket = null;       //socket object
        Writer out = null;     //outputstream
        BufferedReader reader = null;       //inputStream
        Pop3 pop = null;
        if (smtpAfterPop){
            pop = popBeforeSmtp();
            getLogger().debug("popBeforeSmtp() has been succesfully executed, now trying to establish Smtp-connection...");

        }
        try {
            smtpSocket = new Socket(smtpHost, smtpPort);
            out = new OutputStreamWriter(smtpSocket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
            if (smtpSocket != null && out != null && reader != null)
            {
                if (!isResponse(reader, "220"))
                    throw new MailException("no greeting, return failed...");


                getLogger().debug("Client: HELO " + localHost);
                out.write("HELO " + localHost + "\n");
                out.flush();
                if (!isResponse(reader, "250"))
                    throw new MailException("HELO return failed...");
                
                getLogger().debug("Client: MAIL FROM: " + senderMail + "");
                out.write("MAIL FROM: " + senderMail + "\n");
                out.flush();
                if (!isResponse(reader, "250"))
                    throw new MailException("MAIL FROM return failed...");


                getLogger().debug("Client: RCPT TO: <" + recipient + ">");
                out.write("RCPT TO: " + recipient + "\n");
                out.flush();
                if (!isResponse(reader, "250"))
                    throw new MailException("RCPT return failed...");

                getLogger().debug("Client: DATA");
                out.write("DATA\n");
                out.flush();
                if (!isResponse(reader, "354"))
                    throw new MailException("DATA return failed...");

                getLogger().debug("Client: SUBJECT: " + subject);
                out.write("Subject: " + subject);
                out.write(13);
                out.write(10);
                /*
                getLogger().debug("Client: FROM: " + senderName );}
                out.writeBytes("FROM: " + senderName);
                out.write(13);
                out.write(10);
                */

                getLogger().debug("Client: " + mailBody);
                out.write(mailBody);
                getLogger().debug("Client: newline . newline");
                out.write(13);
                out.write(10);
                out.write(46);
                out.write(13);
                out.write(10);
                out.flush();
             
                if (isResponse(reader, "250") == true) {
                    getLogger().debug("Client: QUIT");
                    out.write("QUIT" + "\n");
                    out.flush();
                    if (isResponse(reader, "221") == false)
                        throw new MailException("QUIT return failed...");
                }
                out.close();
                reader.close();
                smtpSocket.close();
            }
        } catch (UnknownHostException e) {
            throw new MailException("Don't know about host: " + smtpHost);
        } catch (IOException e) {
            throw new MailException("I/O Error while connecting to: " + smtpHost + ":" + smtpPort + " \n" + e.getMessage());
        } finally {
            if (pop != null)
                pop.quit();
        }
        return;


    }

}





