package org.rapla.bootstrap;


public class RaplaStandaloneLoader
{
    public static void main(String[] args)
    {
        String baseDir = "./webapp/WEB-INF";
        String dirList = "lib";
        String classname = "org.rapla.Main";
        RaplaLoader.start( baseDir,dirList, classname, args );
    }
}
