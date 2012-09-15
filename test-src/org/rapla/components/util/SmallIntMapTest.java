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
package org.rapla.components.util;
import java.util.*;

import org.rapla.components.util.SmallIntMap;

import junit.framework.*;

public class SmallIntMapTest extends TestCase {

    public SmallIntMapTest(String name) {
        super(name);
    }

    public static Test suite() {
	return new TestSuite(SmallIntMapTest.class);
    }

    protected void setUp() {
    }

    public void test1() {
	SmallIntMap map = new SmallIntMap(new int[] {1,10,5},new String[] {"1","10","5"});
	assertEquals("1",map.get(1));
	assertEquals("5",map.get(5));
	assertTrue(map.keySet().contains(new Integer(5)));
	Collection<?> col = map.values();
	assertTrue(col.contains("1"));
	assertTrue(col.contains("10"));
	assertTrue(col.contains("5"));
    }

    public void test2() {
	SmallIntMap map = new SmallIntMap();
	map.put(5,"A");
	map.put(1,"B");
	map.put(10,"C");
	assertEquals("B",map.get(1));
	assertTrue(map.keySet().contains(new Integer(5)));
	assertEquals(1, map.findMatchingKeys("B")[0]);
	map.remove(1);
	assertEquals(null, map.get(1));
	assertEquals(0, map.findMatchingKeys("B").length);
	map.put(1,"D");
	assertEquals("D",map.get(1));
	map.put(4,"D");
	int key1 = map.findMatchingKeys("D")[0];
	int key2 = map.findMatchingKeys("D")[1];
	assertTrue((key1 == 4 && key2 == 1) || (key1 ==1 && key2 == 4));
	Collection<?> col = map.values();
	assertTrue(col.contains("A"));
	assertTrue(col.contains("C"));
	assertTrue(col.contains("D"));
	assertTrue(!col.contains("B"));
    }

    public void test3() {
	SmallIntMap map = new SmallIntMap();
	Map<Object,Object> map1 = new HashMap<Object,Object>();
	Map<Object,Object> map2 = new HashMap<Object,Object>();
	Map<Object,Object> map3 = new HashMap<Object,Object>();
	map.put(TEST1,map1);
	map.put(TEST2,map2);
	map.put(TEST3,map3);
	map2.put("1","hallo");
	map3.put("1",new ArrayList<Object>());
	Collection<?> col = map.values();
	assertTrue(col.contains(map1));
	assertTrue(col.contains(map2));
	assertTrue(col.contains(map3));
    }
    final static int TEST1 = 1;
    final static int TEST2 = 10;
    final static int TEST3 = 5;
}





