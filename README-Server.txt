This README is for running rapla as server. You will find more information on our
documentation pages on 

http://rapla.sourceforge.net/documentation.html


The rapla server:
---------------------------

Since version 0.10.2 rapla server installation is very easy:
Download the binary-distribution or build one with "build dist-bin"

You can start it with:

raplaserver.bat (Windows)
call raplaserver.bat (NT)
raplaserver.sh run (Unix)

If you want to install Rapla-Server as a SERVICE on Win NT/2000/XP/Vista/7
use the scripts in the service folder to test/install/uninstall rapla as a service.


With a running server point your browser to
http://localhost:8051

You should see the rapla welcome screen. Click on start with webstart 
and the webstart client should launch. If not launched automatically, 
you should choose the webstart program from your 
java installation (e.g. C:\Programme\Java\jre1.5.0_06\bin\webstart.exe under Windows). 

You can login with username "admin" and empty password

Thats it.

Installing into another servlet container
-----------------------------------------

If you want to use another servlet container (e.g. tomcat) you can
copy the webapp (without s) folder to the webapps (with s) folder of
your servlet container and rename webapp (without s) to rapla.
See the documentation of your servelt-container for more details

Installing in Apache or IIS
----------------------------

You can install Jetty with apache or IIS by uncommenting
the AJP13 listener in jetty/conf/main.xml.

See http://jetty.mortbay.org/ for more information

Troubleshooting:
----------------
1. The most commen error will be long stacktrace with
  "Caused by: java.net.BindException: Could not bind to port 8051"
  somewhere hidden between the lines. This means that either another
  rapla server is running (so stop or kill this process) or that this
  port is blocked by another application.  You need to configure
  another the port in jetty/jetty.xml

<Call name="addConnector">
      <Arg>
         <New class="org.eclipse.jetty.server.nio.SelectChannelConnector">
            <Set name="port"><SystemProperty name="jetty.port" default="8051"/></Set>

2. look in the troubleshooting faq under http://code.google.com/p/rapla/

3. Post a question to rapla-developers@lists.sourceforge.net


Running Multiple Rapla servers:
-------------------------------

There is no Problem running multiple rapla servers on one computer. You just need to copy the webapp folder and rename it e.g. webapp2
and add a new webapplication to jetty/jetty.xml
 
  <New class="org.eclipse.jetty.webapp.WebAppContext">
      <Arg><Ref id="contexts"/></Arg>
      <Arg><SystemProperty name="jetty.home" default="."/>/webapp2</Arg>
      <Arg>/rapla2</Arg>
      <Set name="defaultsDescriptor"><SystemProperty name="jetty.home" default="."/>/jetty/webdefault.xml</Set>
      <Set name="resourceBase"><SystemProperty name="jetty.home" default="."/>/webapp2</Set>
    </New>
  
  Of course you need to change the context of the first webapp from / to /rapla
  
    <New class="org.eclipse.jetty.webapp.WebAppContext">
      <Arg><Ref id="contexts"/></Arg>
      <Arg><SystemProperty name="jetty.home" default="."/>/webapp</Arg>
      <Arg>/rapla</Arg>
      <Set name="defaultsDescriptor"><SystemProperty name="jetty.home" default="."/>/jetty/webdefault.xml</Set>
      <Set name="resourceBase"><SystemProperty name="jetty.home" default="."/>/webapp</Set>
    </New>
  
  
  
    
Now you should have two applications running
http://localhost:8051/rapla/ 
http://localhost:8051/rapla2/ 
  

Acknowlegdement
-----------------

Rapla includes free software developed by the Apache Software Foundation
(http://www.apache.org/) and other organizations
For more Information see legal/LIBRARIES-FAQ


