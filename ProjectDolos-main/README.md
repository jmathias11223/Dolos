# Dolos [Δόλος] - A Remote Access Tool

## Product Description

Dolos is a remote access tool (RAT) that performs stealthy reconnaisance
on target systems. Dolos is made up of three parts: operator, server, 
and client. The operator is the user's command center where the user can
monitor, send commands, and view information on one or more clients. The server 
stores all logs and data in a database and acts as an intermediary for the communication
between the operator and the client. Finally, the client is the part that receives 
commands from the operator, executes them on the target system, and sends back the 
results. All three parts work together to create a cohesive and versatile tool.

When the user queues commands on the operator, it will send those commands
to the server. The server will wait for the next time the client connects, 
send the queued commands, and receive any and all results from the client.
The server will then store those results along with the communication logs
from both the operator and the client so when the user wishes to view 
the results from the commands they sent, they can easily enter a command on 
the operator to do so.

## Usage

### Operator

NOTE: The operator will only run on a computer with Java installed

1. Navigate to the Production/Operator directory
2. Run the file titled Operator.jar
3. A command line window should open with the Dolos UI

### Server

No need to worry about the server! It handles things on its own.

### Client

NOTE: The client will only work on a Linux-based OS

1. Navigate to the Production/Client folder
2. Copy the contents of the folder to the system you want to infect
3. Run the rename.sh script and enter a name for the client
4. Run the install.sh script
5. To confirm that the client is installed and running, open terminal and run the command ps aux. There should be three processes within the folder /var/tmp/x86_64-linux-gnu

## List of Commands:

- directory [path]             
    - Gets directory contents; default is current directory
- keylogger <int>              
    - Starts a keylogger for int seconds
- logs                         
    - Collects information from the keylogger
- network                      
    - Gets connection info
- print <file>                 
    - Gets contents of a file
- privilege                    
    - Gets privilege status
- reboot [int]                 
    - Reboots the system (optional delay of int seconds)
- screencapture                
    - Takes a screenshot at the next connection
- shell [arg1] [arg2] ...      
    - Execute any shell command
- shutdown [int]               
    - Shuts down the system (optional delay of int seconds)
- system                       
    - Gets basic system info
- tasks                        
    - Gets running processes
- time                         
    - Gets client system's local time
- tree [path]                  
    - Gets a tree of directory contents; default is current directory
- upload [filename]            
    - Uploads file to client file system, only works for small .txt files as of now
    - File must be placed in Operator/Scripts directory
    - File is uploaded to /var/tmp/x86_64-linux-gnu on client system
- users                        
    - Gets active users

- encrypt [path] [path] ...    
    - Encrypts the file(s) specified
- decrypt [path] [path] ...    
    - Decrypts the file(s) specified
- decrypt -all                 
    - Decrypts all encrypted files on current client's file system
- list_encrypted               
    - Lists all files that are currently encrypted

- sleep_1 <int>                
    - Sets the client sleep time for server connections (total seconds)
- sleep_2 <int>                
    - Sets the client sleep time for checking killtime (total seconds)
- killtime                     
    - Sets the client kill date and time

## Versions:

### Dolos v1.6:
- Integrated screen capturing mechanism
- Cleaned up all files
- Added the upload command
    - Currently works with
        - .txt and related files
    - Not yet working with
        - Images
        - Tar files
        - Executables

### Dolos v1.5:
- Supports file encryption & decryption
    - Can encrypt/decrypt multiple files at once
    - Keeps track of files that have already been encrypted
- Bug fixes with encryption algorithms
- Implemented the keylogger
- Added commands:
    - decrypt [path] [path] ...
    - encrypt [path] [path] ...
    - decrypt -all
    - list_encrypted
- Changed name from RATatouille to Dolos
- Stored encrypted files list on database

### Dolos v1.4:
- More robust menu system
- Added more commands
- Able to run multiple clients
- Moved from files to a database for data storage

### Dolos v1.3:
- Added security for the encryption/decryption key
- Client and Operator are aware of the current system time
- Sets up client installation and uninstallation

### Dolos v1.2:
- Added end-to-end AES-256 encryption for all data
- Optimized runtime of encryption and decryption from a 3 minute runtime to <10 seconds
- Changed title from Client Connector to RATatouille
- Encrypted all stored data on the server
- Bug fixes

### Dolos v1.1:
- Added functionality to clear command queue
- Added command to download all results from commands
- Added more commands
- Added add/view/delete functionality for repeat commands
- Added a help menu
- Stored all collected data in files server-side

### Dolos v1.0:
- Added functionality to queue commands
- Added command to view all queued commands
- Full connection between client, server, and operator
- All commands could be run on tarGets system
- Completed basic UI

### Early Alpha
- Established basic connection between client, server, and operator
- Tested commands manually on tarGets system
