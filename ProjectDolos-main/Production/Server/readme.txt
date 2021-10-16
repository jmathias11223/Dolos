DOLOS v1.5 REMOTE ACCESS TOOL - SERVER
Copyright (c) 2021
All Rights Reserved
-------------------------------------------------------------------------------
OVERVIEW:

The Dolos Remote Access Tool (RAT) allows for covert reconnaissance and 
offensive control of target machines. This software permits running of Dolos
servers for both Operators and Clients to communicate with one another.

INVOCATION:

Executing `java -jar Server.jar` in a terminal will start the Server script; by
default, the Server listens for Operator connections on port 20000 and Client
connections on port 10000. 

RUNTIME:

The Server will spawn a new thread to handle each new connection; the maximum
number of connections can be limited within the code (if necessary). Each 
Operator and Client have respective log files which will report the time, IP,
and action taken. These files can be used for debugging or security purposes, 
and are located within the Logs/ directory.

All commands from the Operator(s) and results/data from the Client(s) are stored
in the "database.db" SQLite database. This is automatically managed by the Server
code, however contents can be viewed and modified externally if need be.

All communications between Clients and Operators are encrypted using an AES-256
algorithm.

-------------------------------------------------------------------------------
