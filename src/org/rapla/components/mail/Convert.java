package org.rapla.components.mail;
/*
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for commercial or non-commercial purposes
 * is hereby granted provided that this copyright notice appears
 * in all copies.
 *
 * LIMITATION OF LIABILITY.  UNDER NO CIRCUMSTANCES AND UNDER NO
 * LEGAL THEORY, SHALL THE AUTHOR OF THIS CLASS BE LIABLE TO YOU
 * OR ANY OTHER PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL OR
 * CONSEQUENTIAL DAMAGES OF ANY KIND.
 *
 */

/** Class with a few static methods used by <code>pop3</code> and <code>apop</code> classes in this package.
 * <p>Get the latest version of this and other classes on
 * <a href="http://www.geocities.com/SunsetStrip/Studio/4994/java.html">
 * Stefano Locati's Java page.</a>
 *
 *
 * @author Stefano Locati
 *  <a href="mailto:slocati@geocities.com">slocati@geocities.com</a> or
 *  <a href="mailto:stefano.locati@usa.net">stefano.locati@usa.net</a>
 * @version $Revision: 713 $ $Date: 2004-11-16 21:37:01 +0700 (Sel, 16 Nop 2004) $
 */
public class Convert
{

//---------------------------------------------------------------------------
   /** This class is a library of static methods so can't be
    *  istantiated.
    */
   private Convert()
   {
   }

//---------------------------------------------------------------------------
   /**
    * Convert a String into an int, giving 0 as default value.
    * @param  s  a String to be converted
    * @return int value represented by the argument, or 0 if the argument
    *    isn't a valid number.
    */
   public static final int toInt(String s)
   {
       // or try: return Integer.parseInt(s);
       try {
           // return (Integer.valueOf(s)).intValue();
           return Integer.parseInt(s);
       } catch (NumberFormatException e) {
           return 0;
       }
   }

//---------------------------------------------------------------------------
   /**
    * Convert a byte into an hexadecimal number.
    * The output is always lowercase, two digit long in the range
    * <code>00-ff</code>.
    * <p><b>Example:</b> <code>12</code> gives <code>0c</code>,
    *   <code>18</code> gives <code>12</code>,
    *   <code>255</code> gives <code>ff</code>, ...
    * @param  n a byte value to be converted.
    * @return a two digit hexadecimal number.
    */
   public static final String toHexString(byte n)
   {
       // note: & 0xff is used to reset high bits
       if ( (n >= 0) && (n <= 15) )
           return "0" + Integer.toHexString( n & 0xff );
       else return Integer.toHexString( n & 0xff );
   }

//---------------------------------------------------------------------------
   /**
    * Convert a number represented by the given byte array into a
    * hexadecimal number String.
    * <p><b>Example:</b> <code>{ 12, 22, 16 }</code> gives
    * <code>0c1610</code>. That means <code>12</code> is <code>0c</code>,
    * <code>22</code> is <code>16</code>,
    * <code>16</code> is <code>10</code> or, if you prefer all together,<br>
    * <code>12 * 256^2 + 22 * 256^1 + 16 * 256 = 792080</code>
    * gives <code>c1610</code>.
    * @param  n the byte array to be converted.
    * @return a hexadecimal String with an even number of digits.
    */
   public static final String toHexString(byte[] n)
   {
       StringBuffer hex = new StringBuffer(2*n.length);
       for (int i = 0; i < n.length; i++) {
           hex.append(toHexString(n[i]));
       }
       return hex.toString();
   }

}
