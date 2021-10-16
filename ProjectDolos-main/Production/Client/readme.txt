DOLOS v1.5 REMOTE ACCESS TOOL - CLIENT
Copyright (c) 2021
All Rights Reserved
-------------------------------------------------------------------------------
OVERVIEW:

The Dolos Remote Access Tool (RAT) allows for covert reconnaissance and 
offensive control of target machines. This software permits connection to Dolos
servers from which commands can be received and executed without detection. The
IP and listening socket of the Server has been integrated within the Client
source code and must be modified if used with different systems. The "brains" 
of the Client lie in the file "config" which is a precompiled binary of the 
Python source code; this is supplemented with several Shell scripts and a JSON 
file (init.json) which holds certain Client parameters.

INSTALLATION:

Prior to installing, run `rename.sh <name>`, replacing the brackets with a 
unique client name; this is critical for identifying the client from the 
Operator. Once a confirmation is displayed, run `install.sh` and wait for a
confirmation. Upon finishing installation, remove all Dolos files from the 
target machine (or otherwise all files in this directory).

Installation will create the directory /var/tmp/x86_64-linux-gnu/ and populate
it with the "config" executable, Shell scripts, and init.json file. A setup 
script will run which injects a Cron job in the users Crontab to allow running
at startup. This job is obfuscated by appending a Carriage Return and spaces
to hide it when displayed. Finally, a script will boot up the Client and begin
gathering information.

RUNTIME:

The Client is capable of executing a suite of commands with uses ranging from
information gathering to offensive file encryption. The complete list is as
follows:

    - Take a screenshot of the user's screen
    - Run a keylogger to capture keystrokes
    - Encrypt (and decrypt) files 
    - Upload files
    - Reboot / shutdown the system
    - View system, tasks, network, and user(s) information
    - View directories and files (singly or recursively)
    - Execute any shell command

During runtime, the Client executes two independent cycles:

    Wake -> Connect to Server -> Receive Commands -> Send Results -> Sleep

    Wake -> Check Time -> If Necessary, Uninstall -> Sleep

The first cycle is responsible for the pipeline of commands and results to and
from the Client, while the second cycle is responsible for removing the client
when appropriate. Both of these cycle times (as well as the Killtime) can be
modified by an Operator at any time.

The usage of a connection cycle minimizes the Server connection time (especially
with a longer sleep time) and therefore reduces the chance of being detected by
a user or administrator. We decided to make the two cycles independent of one
another for greater user customization - for example, it is probably preferred
to check the Killtime more frequently to make sure the Client does not stay
longer than expected.

All communications between the Client and Server are encrypted using an AES-256
algorithm to keep the network traffic more anonymous and less conspicuous.

DEBUGGING:

The Client has the ability to log any output (and errors) to a file named 
"debug.txt" found in the /var/tmp/x86_64-linux-gnu/ directory if desired.
During creation and testing, this was utilized; however, this would be 
turned off in Production.

UNINSTALLATION:

If necessary, the client can be manually uninstalled using `uninstall.sh`, 
however it will automatically remove itself upon reaching its killdate (or if 
killed from an Operator). Uninstallation will remove the stored Cron job,
delete the /var/tmp/x86_64-linux-gnu/ directory and all its files, and kill the
Client. Client result data will remain stored on the Server until deleted by an
Operator.

-------------------------------------------------------------------------------
