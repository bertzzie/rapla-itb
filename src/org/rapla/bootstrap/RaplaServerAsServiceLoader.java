package org.rapla.bootstrap;


public class RaplaServerAsServiceLoader
{
    public static void main(String[] args)
    {
        String baseDir = "../../jetty";
        String dirList = "lib";
        String classname = "org.eclipse.jetty.xml.XmlConfiguration";
        System.setProperty( "java.awt.headless", "true" );
        String[] arguments = new String[] {"../../jetty/jetty.xml"};
        RaplaLoader.start( baseDir,dirList, classname, arguments );
    }
}
