package org.rapla.components.mail;

/*
 * Pop3.java
 * Copyright (c) 1996 John Thomas  jthomas@cruzio.com
 *      All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software and
 * its documentation for commercial or non-commercial purposes
 * is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * LIMITATION OF LIABILITY.  UNDER NO CIRCUMSTANCES AND UNDER NO
 * LEGAL THEORY, SHALL THE AUTHOR OF THIS CLASS BE LIABLE TO YOU
 * OR ANY OTHER PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL OR
 * CONSEQUENTIAL DAMAGES OF ANY KIND.
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;

/** Interface to a Pop3 mail server. Can be used to check, fetch and delete mail messages.
 * <p>Get the latest version of this and other classes in
 * <a href="http://www.geocities.com/SunsetStrip/Studio/4994/java.html">
 * Stefano Locati's Java page.</a>
 *
 * Based on the
 * <a href="http://ds.internic.net/rfc/rfc1939.txt">rfc1939.txt</a>
 * definition for the Post Office Protocol - Version 3 (obsoletes
 * RFC 1725).
 * <b>If mailhost, user, password are not supplied to the
 * constructor, then they must be specified on the connect and
 * login calls.</b>
 *
 * <p>The <code>Apop</code> command is not supported by the
 * <code>Pop3</code> class. But there is an <code>Apop</code> class
 * that extends <code>Pop3</code> to add <code>Apop</code> support.
 * It can be used just like the pop class, just create an Apop object
 * instead a pop object. The Apop class still works even if the
 * <code>Pop3</code> server doesn't support <code>Apop</code> login.
 *
 * <p>Simple Usage Example to display the size of each message.
 * <pre>
 *   Pop3 pop = new Pop3(host, user, password);
 *   PopStatus status = pop.connect();
 *   if ( status.OK() )
 *      status = pop.login();
 *   if ( status.OK() ) {
 *       status = pop.list();
 *       String[] responses = status.Responses();
 *       for(int i=0; i< responses.length; i++) {
 *         System.out.println("Message[" + i + "]='" + responses[i] + "'");
 *       }
 *       status = pop.quit();
 *   }
 * </pre>
 *
 * <p>The following methods try to closely implement the corresponding
 *    Pop3 server commands.  See RFC1939.
 *
 * <pre>
 *   PopStatus     stat()
 *   PopStatus     list()
 *   PopStatus     list(msgnum)
 *   PopStatus     retr(msgnum)
 *   PopStatus     dele(msgnum)
 *   PopStatus     noop()
 *   PopStatus     quit()
 *   PopStatus     top(msgnum,numlines)
 *   PopStatus     uidl(msgnum)
 * </pre>
 *
 *  <ul><li>The indicated methods have additional multiline output
 *    that can be retrieved with the get_Responses method for
 *    the PopStatus object.  i.e.
 *  <pre>
 *       PopStatus status = mypopserver.list()
 *       String[] list = status.get_Responses()
 *  </pre>
 *
 *  <li>The following methods are convenience functions for the client
 *  <pre>
 *       PopStatus  appendFile(filename,msgnum)
 *
 *       int get_TotalMsgs() Number of mail messages on server
 *       int get_TotalSize() Total size of all mail messages
 *                           _TotalSize and _TotalMsgs are set during
 *                           login() by an internal stat() command
 *  </pre>
 *
 *  <li>The status of a POP request is returned in an instance of
 *    the PopStatus class.
 *    PopStatus has the following methods to extract the returned info.
 *  <pre>
 *       boolean    OK()        True if request had no errors
 *       String     Response()  The initial line returned by POP server
 *                              that starts with either +OK or -ERR
 *       String[]   Responses() If command returns multiple lines of
 *                              data (RETR, TOP, LIST) then this method
 *                              will return the lines in an array.
 *  </pre>
 *
 *  <li>Public debugging Methods.
 *  <pre>
 *       void     setDebugOn(boolean)      turn on debug output
 *       void     set_DebugFile(filename)  Set filename for debug output
 *       void     debug(String DebugInfo)  Display string on stdout
 *  </pre>
 *  </ul>
 *
 * @author <b>Original author:</b> John Thomas
 *     <a href="mailto:jthomas@cruzio.com">jthomas@cruzio.com</a>
 * @author <b>Current maintainer:</b> Stefano Locati
 *  <a href="mailto:slocati@geocities.com">slocati@geocities.com</a> or
 *  <a href="mailto:stefano.locati@usa.net">stefano.locati@usa.net</a>
 * @version $Revision: 2967 $ $Date: 2012-07-13 15:08:02 +0700 (Jumat, 13 Jul 2012) $
 *
 *
 */

public
class Pop3   {

    /** Authorization State */
    protected final int AUTHORIZATION = 1;

    /** Transaction State */
    protected final int TRANSACTION = 2;

    /** Update State */
    protected final int UPDATE = 3;


    /** Number of mail messages on server */
    protected int _TotalMsgs = 0;

    /** Total size of all messages on server */
    protected int _TotalSize = 0;

    /** status used by Send/Recv */
    protected boolean _StatusOK = false;

    /** Session State */
    protected int State = 0;

    /** The last Pop3 command sent to the server */
    protected String LastCmd;

    /** Pop3 server host name */
    protected String Host = null;

    /** Port on which the Pop3 server listens to */
    protected int Port = 110;

    /** Mailbox user name */
    protected String User = null;

    /** Mailbox password */
    protected String Password = null;

    /** Socket connected to the server  */
    protected Socket server;

    /** Input stream connected to the server socket */
    protected BufferedReader serverInputStream;

    /** Output stream connected to the server socket */
    protected DataOutputStream serverOutputStream;

    /** debug On switch */
    private boolean debugOn=false;


    /**
     *  Creates the object. No work is done.
     *  @param host      a Pop3 server host name
     *  @param user      a mailbox user name
     *  @param password  a mailbox password
     */
    public Pop3(String host, String user, String password) {
        Host = host;
        User = user;
        Password = password;
    }


    /**
     *  Creates the object. No work is done
     *  You will have to supply host, user and password through
     *  connect() and login() methods.
     *  @see #connect(java.lang.String)
     *  @see #login(java.lang.String, java.lang.String)
     */
    public Pop3() {
    }


    /**
     *  Makes a socket connection to the specified
     *  host (port 110).
     *  @param host        a Pop3 server host name
     *  @return PopStatus:  result of this operation
     */
    public PopStatus connect(String host) {
        // If method specifies the host name then save it and
        // call the default connect method
        Host = host;
        return this.connect();
    }

    /**
     *  Makes a socket connection to the specified
     *  host and port.
     *  @param host        Pop3 server host name
     *  @param port        TCP port to connect to
     *  @return PopStatus:  result of this operation
     */
    public PopStatus connect(String host, int port) {
        // If both host and port are specified then save them
        // and then call the default connect method
        Host = host;
        Port = port;     // Normally this would be 110 (RFC 1725)
        return this.connect();
    }

    /**
     *  Makes a socket connection to the host specified
     *  in the constructor (port 110).
     *  @return PopStatus:  result of this operation
     */
    public synchronized PopStatus connect() {
        PopStatus status = new PopStatus();
        debug("Connecting to " + Host + " at port " + Port);
        if (Host == null) {
            status._Response = "-ERR Host not specified";
            status._OK = false;
            return status;
        }

        try {
            server = new Socket(Host,Port);
            if (server == null) {  // a failure with no exception????
                debug("-ERR Error while connecting to Pop3 server");
                status._OK = false;
                status._Response = "-ERR Error while connecting to Pop3 server";
            } else {
                debug("Connected");
                // get the input stream that we will use to read from the server
                serverInputStream = new BufferedReader(
                    new InputStreamReader(server.getInputStream()));
                if (serverInputStream == null) {
                    debug("Failed to setup an input stream.");
                    status._OK = false;
                    status._Response = "-ERR Error setting up input stream";
                    server = null;
                }
                serverOutputStream = new DataOutputStream(
                    server.getOutputStream() );
                if (serverOutputStream == null) {
                    debug("Failed to setup an output stream.");
                    status._OK = false;
                    status._Response = "-ERR Error setting up output stream";
                    server = null;
                }
            }
        }
        catch (Exception e) {
            String msg = "Exception! " + e.toString();
            debug(msg);
            status._OK = false;
            status._Response = msg;
            server = null;
        }

        if (server != null) {
            status._OK = true;
            // POP protocol requires server to send a response on the
            // connect.  We will now get that response and parse it
            _StatusOK = true;    // Fake doing send() before recv()
            status._Response = recv();
            Parse(status,2);
            debug("Response=" + status._Response);
        }

        if (status._OK)
          State = AUTHORIZATION;

        return status;
    }

    /** Login the specified user with the specified password.
     *  If the login is successful, a STAT command is issued
     *  to get the current number of messages.
     *  @param user        a mailbox user name
     *  @param password    a mailbox password
     *  @return PopStatus: result of this operation
     */
    public PopStatus
    login(String user, String password) {
        User = user;
        Password = password;
        return login();
    }

   /** Login with the user and password specified in the constructor.
    *  If the login is successful, a <code>STAT</code>
    *  command is issued to get the current number of
    *  messages.
    *  @return PopStatus: result of this operation
    */
    public synchronized PopStatus login() {
        PopStatus status = new PopStatus();

        if (User == null || Password == null) {
            status._Response = "-ERR Userid or Password not specified";
            return status;
        }
        if ( server != null )  {
            send("USER " + User);
            status._Response = recv();
            Parse(status,1);
            if (status._OK) {
                send("PASS " + Password);
                status._Response = recv();
                Parse(status,1);
                if (status._OK) {
                    State = TRANSACTION;
                    // Now we will do an internal STAT function
                    stat();
                }
            }
        }
        return status;
    }

    /** Closes the socket connection.
     *  Use <code>quit</code> for a normal termination.
     *  @see #quit()
     */
    public synchronized void close() {
        debug("Closing socket");
        try {
            server.close();
            State = 0;
        } catch (IOException e) {
            debug("Failure in server.close()");
        }
    }

    /** Gets the number of messages and their total size from the server.
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus stat() {
        PopStatus status = new PopStatus();
        if (State != TRANSACTION) {
            status._Response = "-ERR Server not in transaction mode";
            return status;
        }
        send("STAT");        // Issue the STAT command
        status._Response = recv();     // read the response
        String[] tokens = Parse(status, 4);

        if (status._OK) {
            _TotalMsgs = Convert.toInt(tokens[1]);
            _TotalSize = Convert.toInt(tokens[2]);
        }

        return status;
    }

    /**
     *  Quits the session with the Pop3 server.
     *  After receiving a goodbye message from the server,
     *  the socket is closed.
     *  @return PopStatus
     */
    public synchronized PopStatus quit() {
        PopStatus status = new PopStatus();
        send("QUIT");        // Issue the STAT command
        State = UPDATE;
        status._Response = recv();     // read the response
        Parse(status,2);
        close();

        return status;
    }

    /**
     *  Gets the size of the specified mail message.
     *  @param msgnum      message number
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus list(int msgnum) {
        PopStatus status = new PopStatus();
        
        send("LIST " + msgnum);        // Issue the LIST n command
        status._Response = recv();     // read the response
        Parse(status,2);
        return status;
    }

    /** Gets a list of messages and the size of each one.
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus list() {

        PopStatus status = new PopStatus();
        send("LIST");        // Issue the LIST command

        recvN(status);     // read the response
        Parse(status,2);

        return status;
    }

    /** Gets the uidl of the specified mail msg.
     *  @param  msgnum     message number
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus uidl(int msgnum) {
        PopStatus status = new PopStatus();
        send("UIDL " + msgnum);     // Issue the UIDL msgnum command
        status._Response = recv();  // read the response
        Parse(status,2);

        return status;
    }

    /** Gets a list of messages and the <code>UIDL</code> of
     *  each one.
     *  <code>UIDL</code> is a message identifier that is
     *  garanteed to remain the same even across different
     *  sessions.
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus uidl() {

        PopStatus status = new PopStatus();
        send("UIDL");      // Issue the UIDL command

        recvN(status);     // read the responses
        Parse(status,2);

        return status;
    }

    /**  Gets the contents of a mail message.
     *  The array of strings obtained are the lines of the
     *  specified mail message.
     *  The lines have <code>CR/LF</code> stripped, any leading
     *  <code>"."</code> fixed up and the ending <code>"."</code>
     *  removed.<br>
     *  The array can be retrieved with the status.Responses() method.
     *  The <code>+OK</code> or <code>-ERR</code> status line is
     *  returned.
     *  @param msgnum      message number
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus retr(int msgnum) {

        PopStatus status = new PopStatus();
        send("RETR " + msgnum);        // Issue the RETR n command

        // This may produce more than one response so we call the
        // recvN method and save an array of strings in status._Responses.
        recvN(status);     // read the response
        // The initial string that contains the status is in the
        // status._Response state variable.
        Parse(status,2);

        return status;
    }


    /** Gets the top n lines of a mail message.
     *  The array of strings obtained are the lines of the
     *  mail headers and the top N lines of the indicated mail msg.
     *  The lines have <code>CR/LF</code> striped, any leading
     *  <code>"."</code> fixed up and the ending <code>"."</code>
     *  removed. <br>
     *  The array can be retrieved with status.Responses() method.
     *  The <code>+OK</code> or <code>-ERR</code> status line is
     *  returned.
     *  @param  msgnum     the message number
     *  @param  n          how many body lines should be retrieved.
     *     If <code>n=0</code>, you'll get just the headers,
     *     unfortunately I've bumped into a buggy Pop3 server that
     *     didn't like zeroes, so I suggest to use <code>n=1</code>
     *     if you want just headers.
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus top(int msgnum, int n) {

        PopStatus status = new PopStatus();
        send("TOP " + msgnum + " " + n); // Issue the TOP msgnum n command

        // This may produce more than one response so we call the
        // recvN method and set multiline output into _Responses
        recvN(status);     // read the response

        Parse(status,2);

        return status;
    }


    /** Marks the mail message for deletion
     *  This mail message will be deleted when QUIT is issued.
     *  If you lose the connection the message is not deleted.
     *  @param msgnum      a message number
     *  @return PopStatus: result of this operation
     *  @see  #rset()
     */
    public synchronized PopStatus dele(int msgnum) {
        PopStatus status = new PopStatus();
        send("DELE " + msgnum);        // Issue the DELE n command

        status._Response = recv();     // read the response
        Parse(status,2);

        return status;
    }

    /** Resets the mail messages that have been marked for deletion.
     *  Nothing will be deleted if QUIT is issued next.
     *  @return PopStatus: result of this operation
     *  @see  #dele(int)
     */
    public synchronized PopStatus rset() {
        PopStatus status = new PopStatus();
        send("RSET");        // Issue the RSET command

        status._Response = recv();     // read the response
        Parse(status,2);

        return status;
    }

    /** Does not do anything but it will keep the server active.
     *  @return PopStatus: result of this operation
     */
    public synchronized PopStatus noop() {

        PopStatus status = new PopStatus();
        send("NOOP");        // Issue the NOOP command

        status._Response = recv();     // read the response
        Parse(status,2);

        return status;
    }

    /** Returns the number of messages on the server.
     *  This value is set by an internal <code>STAT</code>
     *  issued at login.
     *  @return the number of messages on this server.
     *  @see    #get_TotalSize()
     */
    public int get_TotalMsgs() {
        return _TotalMsgs;
    }

    /** Returns the size of messages on the server.
     *  This value is set by an internal <code>STAT</code>
     *  issued at login.
     *  @return the total size of messages on this server.
     *  @see    #get_TotalMsgs()
     */
    public int get_TotalSize() {
        return _TotalSize;
    }

    /** Returns the contents of a mail message and append it to the
     *  specified mail file.
     *  It will internally call <code>RETR</code> and then write
     *  the results to the specified file.
     *  @param  filename   the name of the file to be extended
     *  @param  msgnum     a message number
     *  @return PopStatus: result of this operation
     */
    public synchronized  PopStatus appendFile(String filename, int msgnum) {
        PopStatus status = new PopStatus();

        String[] contents;

        send("RETR " + msgnum);         // RETR n will return the contents
        // of message n

        recvN(status);     // read the response
        Parse(status,2);
        if (status._OK) {
            RandomAccessFile openfile;
            try {
                openfile = new RandomAccessFile(filename,"rw");
            } catch (IOException e) {
                status._OK = false;
                status._Response = "-ERR File open failed";
                return status;
            }
            Date datestamp = new Date();
            contents = status.Responses();
            try {
                openfile.seek(openfile.length());
                openfile.writeBytes("From - " + datestamp.toString() + "\r\n");
                for(int i=0; i<contents.length;i++) {

                    openfile.writeBytes(contents[i]+"\r\n");
                    //openfile.writeByte((int)'\r');  // add CR LF
                    //openfile.writeByte((int)'\n');
                }
                openfile.close();

            } catch (IOException e) {
                status._OK = false;
                status._Response = "-ERR File write failed";
                return status;
            }
        }
        status._OK = true;
        return status;
    }

    /** Parses the response to a previously sent command from the server.
     * It will set boolean status._OK true if it returned <code>+OK</code>
     * and return an array of strings each representing a white space
     * delimited token. The remainder of the response after
     * <code>maxToParse</code> is returned as a single String.
     */
    String[] Parse(PopStatus status, int maxToParse) {
        String[] tokens = null;

        status._OK = false;
        String response = status._Response;
        if (response != null) {
            int i=0;
            int max;
            if (response.trim().startsWith("+OK"))
                status._OK = true;
            else
                debug(response);
            // This will break the line into a set of tokens.
            StringTokenizer st = new StringTokenizer(response);
            //tokens = new String[st.countTokens()];
            if (maxToParse == -1)
                max = st.countTokens();
            else
                max = maxToParse;
            tokens = new String[max+1];
            while (st.hasMoreTokens() && i < max) {
                tokens[i] = new String(st.nextToken());
                //debug("Token " + i + "= '" + tokens[i] + "'");
                i++;
            }
            // Now get any remaining tokens as a single string
            if (st.hasMoreTokens()) {
                StringBuffer rest = new StringBuffer(st.nextToken());
                while (st.hasMoreTokens() )
                    rest.append(" " + st.nextToken());
                tokens[max] = new String(rest);
                //debug("Token " + max + "= '" + tokens[max] + "'");
            }
        }
        return tokens;
    }

    /** Sends the passed command to the server.
     */
    void send (String cmdline) {
        debug(">> " + cmdline);
        LastCmd = cmdline;    // Save command for error msg

        try {
            // Write string as a set of bytes
            serverOutputStream.writeBytes(cmdline + "\r\n");
            _StatusOK = true;
        } catch (IOException i){
            System.err.println("Caught exception while sending command to server");
            _StatusOK = false;

        } catch (Exception e) {
            System.err.println("Send: Unexpected exception: " + e.toString());
            _StatusOK = false;
        }
    }

    /** Gets the next response to a previously sent command from the server.
     */
    String recv() {
        //debug("entered recv");

        String line = "";
        if ( ! _StatusOK  ) {
            line = "-ERR Failed sending command to server";
            return line;
        }
        // send() has written a command to the
        // server so now we will try to read the result
        try {
            line = serverInputStream.readLine();
            debug("<<" + line);
        } catch (IOException i){
            System.err.println("Caught exception while reading");
            line = "-ERR Caught IOException while reading from server";
        } catch (Exception e) {
            System.err.println("Unexpected exception: " + e.toString());
            line = "-ERR Unexpected exception while reading from server";
        }
        if (line == null)       {       // prevent crash if reading a null line
            debug("Read a null line from server");
            line = "-ERR <NULL>";
        }
        if (line.trim().startsWith("-ERR")) {
            debug("Result from server has error!");
            debug("Sent:     '" + LastCmd + "'");
            debug("Received: '" + line + "'");
            return line;
        } else {
            if (line.trim().startsWith("+OK")) {
                return line;
            } else {
                debug("Received strange response");
                debug("'" + line + "'");
                line = "-ERR Invalid response";
                return line;
            }
        }
    }

    /** Gets the responses to a previously sent command from the server.
     * This is used when more than one line is expected.
     * The last line of output should be <code>".\r\n"</code>
     */
    void recvN(PopStatus status) {
        debug("entered recvN");
        Vector<String> v = new Vector<String>(100,100);
        String line = "";
        
        // send() has written a command to the
        // server so now we will try to read the result
        try {
            boolean done = false;
            int linenum=0;
            while (!done) {
                line = serverInputStream.readLine();
                linenum++;
                debug("<<" + line.length() + " '" + line +"'");
                if (linenum == 1) { // process the initial line
                    if (line.trim().startsWith("-ERR ")) {
                        debug("Result from server has error!");
                        debug("Sent:     '" + LastCmd + "'");
                        debug("Received: '" + line + "'");
                        done = true;
                        status._Response = line;
                    } else {
                        if (line.trim().startsWith("+OK")) {
                            //Everything looks OK
                            status._Response = line;
                        } else {
                            debug("Received strange response");
                            debug("'" + line + "'");
                            done = true;
                            status._Response = "-ERR Invalid response";
                        }
                    }
                } else {
                    // process line 2 - n
                    if (line.startsWith(".")) {
                        if (line.length() == 1)
                            done = true;
                        else
                            v.addElement(line.substring(1));
                    } else
                        v.addElement(line);
                }

            }   // end of while(!done)
        } catch (IOException i){
            System.err.println("Caught exception while reading");
            status._Response = "-ERR Caught IOException while reading from server";
        } catch (Exception e) {
            System.err.println("Unexpected exception: " + e.toString());
            status._Response = "-ERR Unexpected exception while reading from server";
        }

        status._Responses = new String[v.size()];
        v.copyInto(status._Responses);
        return;
    }

    /** Sets debug on or off.
     *  Debug messages are written to standard error.
     *  @param OnOff  true to set on debugging, false to
     *      shut it up.
     */
    public void setDebugOn(boolean OnOff) {
        debugOn = OnOff;
    }

    /** If debugOn switch is set, display debug info.
     *  @param   debugstr  a debug message
     */
    public void debug(String debugstr) {
        if (debugOn) {
            System.err.println(debugstr);
        }
    }

    //-------------------------------------------------------

} // end of Class Pop3
