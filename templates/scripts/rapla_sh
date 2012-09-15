#!/bin/sh
# 
# Script for starting @doc.name@ version @doc.version@ under Unix
# Set either JAVA_HOME to point at your Java Development Kit installation.
# or PATH to point at the java command

# resolve links - $0 may be a softlink
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`

if [ -z "$JAVA_HOME" ] ;  then
  JAVA=`which java`
  if [ -z "$JAVA" ] ; then
    echo "Cannot find JAVA. You must set JAVA_HOME to point at your Java Development Kit installation."
    exit 1
  fi
  JAVA_BINDIR=`dirname $JAVA`
  JAVA_HOME=$JAVA_BINDIR/..
  echo "Guessing JAVA_HOME:" $JAVA_HOME
fi
if [ -z "$JAVA_OPTIONS" ] ;  then
  JAVA_OPTIONS="-Xmx128M"
  echo "Guessing JAVA_OPTIONS:" $JAVA_HOME
fi

if [ ! -x $PRGDIR/webapp ] ; then
    chmod +x $PRGDIR/webapp
fi
if [ ! -x $PRGDIR/webapp/WEB-INF ] ; then
    chmod +x $PRGDIR/webapp/WEB-INF
fi
if [ ! -x $PRGDIR/webapp/WEB-INF/lib ] ; then
    chmod +x $PRGDIR/webapp/WEB-INF/lib
fi

echo "PROGDIR" $PRGDIR
cd $PRGDIR
$JAVA_HOME/bin/java $JAVA_OPTIONS -jar raplabootstrap.jar $1 $2 $3 $4
