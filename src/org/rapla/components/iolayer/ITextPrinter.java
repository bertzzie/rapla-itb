package org.rapla.components.iolayer;

import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTable.PrintMode;
import javax.swing.table.DefaultTableModel;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

public class ITextPrinter {

	static public void createPdf( Printable printable, java.io.OutputStream  fileOutputStream, PageFormat format) {
		float width = (float)format.getWidth();
		float height = (float)format.getHeight();
		Rectangle pageSize = new Rectangle(width, height);
		Document document = new Document(pageSize);
		
	   try {
	      PdfWriter writer;
	 	writer = PdfWriter.getInstance(document,
	            fileOutputStream);
	      document.open();
	      PdfContentByte cb = writer.getDirectContent();
    
    	   
		   
	      for  (int page = 0;page<5000;page++)
	      {
	    	  Graphics2D g2;
	    	  PdfTemplate tp = cb.createTemplate(width, height);
	    	  g2 = tp.createGraphics(width, height);		
		      int status = printable.print(g2, format, page);
		      g2.dispose();
		      if ( status == Printable.NO_SUCH_PAGE)
		      {
		    	  break;
		      }
		      if ( status != Printable.PAGE_EXISTS)
		      {
		    	  break;
		      }
		      cb.addTemplate(tp, 0, 0);
		        document.newPage();  
		     
	      }
	   } catch (Exception e) {
	      System.err.println(e.getMessage());
	   }
	   document.close();
	}
	
	public static void main(String[] args)
	{
	// Table Tester
		JTable table = new JTable();
		int size = 50;
		DefaultTableModel model = new DefaultTableModel(size+1,1);
		table.setModel( model );
		for ( int i = 0;i<size;i++)
		{
			table.getModel().setValueAt("Test " + i, i, 0);
		}
		JFrame test = new JFrame();
		test.add( new JScrollPane(table));
		test.setSize(400, 300);
		test.setVisible(true);
		Printable printable = table.getPrintable(PrintMode.NORMAL, null, null);
		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream("my_jtable_fonts.pdf");
			PageFormat format = new PageFormat();
			createPdf( printable, fileOutputStream, format);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  	
		System.exit( 0);
	}
}
