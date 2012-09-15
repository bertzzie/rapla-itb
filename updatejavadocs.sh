#! /bin/sh
./build.sh javadocs
cd build 
tar -czf javadocs.tar.gz javadocs/
scp javadocs.tar.gz $rapladocs 
ssh $sf
#rsync --size-only -r -u -v build/javadocs website/doc/javadocs
