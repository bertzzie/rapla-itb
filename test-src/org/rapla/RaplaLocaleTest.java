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
package org.rapla;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;

public class RaplaLocaleTest extends TestCase {
    DefaultConfiguration config;

    public RaplaLocaleTest(String name) {
        super(name);
    }

    public static Test suite() {
	return new TestSuite(RaplaLocaleTest.class);
    }

    private DefaultConfiguration createConfig(String defaultLanguage,String countryString) {
	DefaultConfiguration config = new DefaultConfiguration("locale",this.toString());
	DefaultConfiguration country = new DefaultConfiguration("country",this.toString());
	country.setValue(countryString);
	config.addChild(country);
	DefaultConfiguration languages = new DefaultConfiguration("languages",this.toString());
	config.addChild(languages);
	languages.setAttribute("default",defaultLanguage);
	DefaultConfiguration language1 = new DefaultConfiguration("language",this.toString());
	language1.setValue("de");
	DefaultConfiguration language2 = new DefaultConfiguration("language",this.toString());
	language2.setValue("en");
	languages.addChild(language1);
	languages.addChild(language2);
	return config;
    }

    public void testDateFormat3() throws ParseException, RaplaException {
        RaplaLocale raplaLocale = new RaplaLocaleImpl(createConfig("de","DE"));
        String s = raplaLocale.formatDate(new SerializableDateTimeFormat().parseDate("2001-01-12",false));
        assertEquals( "12.01.01", s);
    }

    public void testTimeFormat4() throws ParseException, RaplaException {
        RaplaLocale raplaLocale= new RaplaLocaleImpl(createConfig("en","US"));
        Calendar cal = Calendar.getInstance(raplaLocale.getTimeZone()
					    ,Locale.US);
        cal.set(Calendar.HOUR_OF_DAY,21);
        cal.set(Calendar.MINUTE,0);
        String s = raplaLocale.formatTime(cal.getTime());
        assertEquals("9:00 PM", s);
    }



}





