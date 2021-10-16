DOLOS v1.5 REMOTE ACCESS TOOL
Copyright (c) 2021
All Rights Reserved
-------------------------------------------------------------------------------
OVERVIEW:

The Dolos Remote Access Tool (RAT) allows for covert reconnaissance and 
offensive control of target machines. It is structured as three distinct
parts: the Client, which runs on the target machine and executes commands
received from a server; the Operator, which can send commands and manipulate
one or more clients; and the Server, which facilitates communication between
Operators and Clients as a third party. Dolos currently targets only Linux
machines, however plans for Windows and OSX are in the future.

FILE STRUCTURE

After unpacking these files (if applicable), source code and executables can 
be found between the Production, Working, and Testing directories. Production
contains prepackaged code which is shippable (to an extent) for the three parts
of the system; each subdirectory contains a readme.txt which goes into further
detail of how to use each part. Working contains all source code (both released
and unreleased) also split into the three system parts. Testing is used solely
for new features and debugging, and is divided among the four developers (Raul,
Justin, Aidan, and Hassam).

The file "README.md" is primarily used for the github repository, while this 
file (readme.txt) is more user-friendly outside of web browsers.

CLIENT

The Client is coded primarily in Python and packaged using PyInstaller to
create a binary executable which can be run absent of a Python installation.
Since Dolos runs on Linux machines, the installation/uninstallation and other
overhead scripts are written using Shell. In Production, the Client is bundled
as a tar file that is installed with a script. To install, a user must have
direct access to the target machine with the Client files downloaded (whether
through the internet, flashdrive, etc), and after installation all files can
be removed and deleted. Further Client details can be found in the Production
folder.

OPERATOR

The Operator is coded in Java, allowing it to be run on any system with the JRE
installed. In Production, all bytecode is packaged as a JAR file which can be 
run on a double-click. Alongside that, a file (accesskey.txt) must contain the
proper key to access the server, else it will be denied access. Any screenshots
or results files retrieved by the Operator can be found in their respective
subdirectories. Further Operator details can be found in the Production folder.

SERVER

The Server is also coded in Java and utilizes a SQLite database to organize and
manage commands from the Operator(s) and results from the Client(s). In testing,
an AWS EC2 instance was used to run the Server, however it should be able to 
migrate to any type of server. While running, logs are kept that keep track of
all Operator and Client activity and can be accessed at any time. Further 
Server details can be found in the Production folder.

-------------------------------------------------------------------------------
