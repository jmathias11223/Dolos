# Server Code

## Overview

This directory holds the code that is pushed to the EC2 Linux server. The two 
primary components are the `ClientServer`, which is responsible for serving and 
handling RAT client(s), and the `OperatorServer`, which is responsible for serving 
and handling operator/controller connections. These two components are run 
together using the `ServerDriver` script.  
  
Both Clients and Operators are required to validate themselves with an access key,
which can be found in `Data/accesskey.txt`. This key is also used for encryption and 
decryption using an AES256 program, which can be found in `src/AES`.  

## File Structure

The `src` directory contains the java sourcecode for the Server; the `bin` directory
holds pre-compiled class files. `Data` is used to hold the current commands and data
from the operators and clients (along with the key), and `Logs` contains sever logs 
as well as archived commands and data.

## Compilation

From the `src` directory, input the following command:  
  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`javac ServerDriver.java -d ../bin/`

## Invocation

From the `bin` directory, input the following command:  
  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`java ServerDriver <Client Port> <Operator Port> <Max # Clients> <Max # Operators>`
