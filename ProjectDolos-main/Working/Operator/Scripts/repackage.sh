#!/bin/bash

./compile_operator.sh

cp -rf ../bin/* .

touch manifest.mf
echo "Main-Class: Driver" > manifest.mf

touch Operator.jar
jar cfm Operator.jar manifest.mf AES/ *.class

rm -rf AES/
rm -f *.class
rm -f manifest.mf

mv Operator.jar ../../Production/Operator/
printf ' [!] JAR File Repackaged Successfully\n'

