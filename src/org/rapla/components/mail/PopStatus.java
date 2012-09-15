package org.rapla.components.mail;
/*
 * PopStatus.java
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

/**Class that holds the information returned from a pop request.
 * <p>Get the latest version of this and other classes on
 * <a href="http://www.geocities.com/SunsetStrip/Studio/4994/java.html">
 * Stefano Locati's Java page.</a>
 *
 *
 * This is required so that the POP class can be thread safe.
 *
 * @author <b>Original author:</b> John Thomas
 *     <a href="mailto:jthomas@cruzio.com">jthomas@cruzio.com</a>
 * @author <b>Current maintainer:</b> Stefano Locati
 *  <a href="mailto:slocati@geocities.com">slocati@geocities.com</a> or
 *  <a href="mailto:stefano.locati@usa.net">stefano.locati@usa.net</a>
 * @version $Revision: 713 $ $Date: 2004-11-16 21:37:01 +0700 (Sel, 16 Nop 2004) $
 *
 */

public class PopStatus   {


        boolean _OK=false;  // True if last command returned +OK

        String  _Response;      // Set to initial response from server

        String[] _Responses= new String[0]; // Set to last multiline response.


//----------------------------------------------------------
/**
 *  Returns the multi-line output from a command.
 *  @return  a multi-line response in an array of String.
 */
public String[] Responses() {
        return _Responses;
}

//----------------------------------------------------------
/**
 *  Returns the initial status line output from a command
 *  @return  the initial status line output from a command.
 */
public String Response() {
        return _Response;
}

//----------------------------------------------------------
/**
 *  Returns the completion status (<code>+OK</code> true or
 *  <code>-ERR</code> false) from the last command issued
 *  to the server.
 *  @return true in case of success (<code>+OK</code>),
 *     false otherwise (<code>-ERR</code>).
 */
public boolean OK() {
        return _OK;
}

//-------------------------------------------------------
} // end of Class PopStatus
