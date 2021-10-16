#!/bin/bash

javac -cp "../src/" -d "../bin" ../src/ServerDriver.java

cp -rf ../bin/* .
cp -rf ../Data/ .

touch manifest.mf
echo "Main-Class: ServerDriver" > manifest.mf

touch Server.jar
jar cfm Server.jar manifest.mf *

rm -rf AES/
rm -rf DB/
rm -rf Data/
rm -f *.class
rm -f manifest.mf

mv Server.jar ../../Production/Server/
printf ' [!] JAR File Repackaged Successfully\n'

