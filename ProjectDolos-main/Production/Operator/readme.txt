DOLOS v1.5 REMOTE ACCESS TOOL - OPERATOR
Copyright (c) 2021
All Rights Reserved
-------------------------------------------------------------------------------
OVERVIEW:

The Dolos Remote Access Tool (RAT) allows for covert reconnaissance and 
offensive control of target machines. This software permits connection to Dolos
servers from which commands can be queued and results can be downloaded. 
The IP and listening socket of the Server has been integrated within the
Operator source code and must be modified if used with different systems.

INVOCATION:

Double-clicking the Operator.jar file should open a terminal window with Dolos
running; if this fails, it can be manually run using `java -jar Operator.jar`
within a terminal window. From there, users can interact with the Operator
through a Command Line Interface. All communications with the Server are 
encrypted using an AES-256 algorithm.

Additional information about the Operator's functions and available commands 
can be found in several help menus inside the Operator.

FILE ORGANIZATION:

All result files will be saved as .txt files within the Results directory, and
all screenshots will be saved as .jpg files within the Screenshots directory.
The file "accesskey.txt" MUST contain the proper key to access the server, 
otherwise a connection will not be established.

-------------------------------------------------------------------------------
