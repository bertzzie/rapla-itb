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
package org.rapla.components.util.xml;

import java.io.IOException;
import java.io.BufferedWriter;

import org.xml.sax.Attributes;

/** Provides some basic functionality for xml-file creation. This 
 * is the SAX like alternative to the creation of a DOM Tree.*/
public class XMLWriter {
    BufferedWriter writer;
    boolean xmlSQL = false;
    public void setWriter(BufferedWriter writer) {
        this.writer = writer;
    }
    
    public BufferedWriter getWriter() {
        return this.writer;
    }

    int level = 0;
    private void indent() throws IOException {
    if( !xmlSQL) //BJO do not indent for sql db, XML_VALUE column will be too small
        for (int i = 0; i < level * 3; i++) writer.write(' ');
    }

    protected void increaseIndentLevel() {
        level ++;
    }

    protected void decreaseIndentLevel() {
        if (level > 0)
            level --;
    }

    public int getIndentLevel() {
        return level;
    }

    public void setIndentLevel(int level) {
        this.level = level;
    }

    public static String encode(String text) {
        boolean needsEncoding = false;
        int size = text.length();
        for ( int i= 0; i<size && !needsEncoding; i++) {
            char c = text.charAt(i);
            switch ( c) {
            case '<':
            case '>':
            case '&':
            case '"':
                needsEncoding = true;
                break;
            }
        }
        if ( !needsEncoding )
            return text;
        StringBuffer buf = new StringBuffer();
        for ( int i= 0; i<size; i++) {
            char c = text.charAt(i);
            switch ( c) {
            case '<':
                buf.append("&lt;");
                break;
            case '>':
                buf.append("&gt;");
                break;
            case '&':
                buf.append("&amp;");
                break;
            case '"':
                buf.append("&quot;");
                break;
            default:
                buf.append(c);
                break;
            } // end of switch ()
        } // end of for ()
        return buf.toString();
    }
    
    protected void printEncode(String text) throws IOException {
        if (text == null)
            return;
        writer.write( encode(text) );
    }

    protected void openTag(String start) throws IOException {
        indent();
        writer.write('<');
        writer.write(start);
        level++;
    }

    protected void openElement(String start) throws IOException {
        indent();
        writer.write('<');
        writer.write(start);
        writer.write('>');writer.newLine();
        level++;
    }

    protected void openElementOnLine(String start) throws IOException {
        indent();
        writer.write('<');
        writer.write(start);
        writer.write('>');
        level++;
    }

    protected void att(Attributes attr) throws IOException{
        for (int i=0;i<attr.getLength();i++)
        {
            att(attr.getQName(i),attr.getValue(i));
        }
    }

    protected void att(String attribute,String value)  throws IOException {
        writer.write(' ');
        writer.write(attribute);
        writer.write('=');
        writer.write('"');
        printEncode(value);
        writer.write('"');
    }

    protected void closeTag()  throws IOException {
        writer.write('>');writer.newLine();
    }

    protected void closeTagOnLine() throws IOException{
        writer.write('>');
    }

    protected void closeElementOnLine(String element) throws IOException {
        level--;
        writer.write('<');
        writer.write('/');
        writer.write(element);
        writer.write('>');
    }

    protected void closeElement(String element) throws IOException {
        level--;
        indent();
        writer.write('<');
        writer.write('/');
        writer.write(element);
        writer.write('>');writer.newLine();
    }

    protected void closeElementTag() throws IOException {
        level--;
        writer.write('/');
        writer.write('>');writer.newLine();
    }

    /** writes the line to the specified PrintWriter */
    public void println(String text) throws IOException {
        indent();
        writer.write(text);writer.newLine();
    }

    /** writes the text to the specified PrintWriter */
    public void print(String text) throws IOException {
        writer.write(text);
    }

    /** writes the line to the specified PrintWriter */
    public void println() throws IOException {
        writer.newLine();
    }

    public void setSQL(boolean sql) {
        this.xmlSQL = sql;
    }
}
