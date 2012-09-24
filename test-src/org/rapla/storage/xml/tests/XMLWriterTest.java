package org.rapla.storage.xml.tests;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.rapla.components.util.xml.*;

import org.rapla.*;

public class XMLWriterTest extends RaplaTestCase {
	
	private final String TEST_FILE = "test-src/org/rapla/storage/xml/tests/xml-testing.txt";
	private final String TEST_RESULT = "test-src/org/rapla/storage/xml/tests/xml-testing-result.txt";
	
	private XMLWriter xmlWriter = new XMLWriter();
	private StringWriter strWrt = new StringWriter();
	
	public XMLWriterTest(String name) {
		super(name);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		
		xmlWriter.setWriter(new BufferedWriter(strWrt));
	}
	
	protected void tearDown() throws Exception {
	}
	
	public void testEncoding() {
		assertEquals("&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;", 
					 XMLWriter.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		
		assertEquals("&lt;string&gt;Hello&amp;Greetings&lt;/string&gt;", 
					 XMLWriter.encode("<string>Hello&Greetings</string>"));
		
		try {
			String fileResult = readFileHelper(TEST_FILE);
			
			assertEquals(readFileHelper(TEST_RESULT),
						 XMLWriter.encode(fileResult));
		}
		catch (IOException ioe) {
			AssertionError ae = new AssertionError("Required testing file not found!");
			ae.initCause(ioe);
			throw ae;
		}
	}
	
	public void testPrint() {
		try {
			xmlWriter.print("testing");
			xmlWriter.getWriter().flush();
			assertEquals("testing", strWrt.getBuffer().toString());
		}
		catch(IOException ioe) {
			AssertionError ae = new AssertionError("Some weird IO Error. Dunno?");
			ae.initCause(ioe);
			throw ae;
		}
	}
	
	public void testPrintlnWithParam() {
		try {
			xmlWriter.println("Test");
			xmlWriter.getWriter().flush();
			assertEquals("Test" + System.lineSeparator(), strWrt.getBuffer().toString());
		}
		catch(IOException ioe) {
			AssertionError ae = new AssertionError("Some weird IO Error. Dunno?");
			ae.initCause(ioe);
			throw ae;
		}
	}
	
	public void testPrintlnWithNoParam() {
		try {
			xmlWriter.println();
			xmlWriter.getWriter().flush();
			assertEquals(System.lineSeparator(), strWrt.getBuffer().toString());
		}
		catch(IOException ioe) {
			AssertionError ae = new AssertionError("Some weird IO Error. Dunno?");
			ae.initCause(ioe);
			throw ae;
		}
	}
	
	private String readFileHelper(String path) throws IOException {
		FileInputStream fstream = new FileInputStream(new File(path));
		try {
			FileChannel fc = fstream.getChannel();
			MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			return Charset.defaultCharset().decode(mbb).toString();
		}
		finally {
			fstream.close();
		}
	}
}
