import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import AES.AESInput;
import AES.Driver;
import DB.Database;


/**
 * Code to listen and connect to Operators over a specified port. Runs on 
 * threads to allow for multiple concurrent operators. 
 * 
 * @author Aidan Sprague
 * @version 07.12.2021
 */
public class OperatorServer {

    // OperatorServer data fields:
    private static RandomAccessFile log;        
    private static SimpleDateFormat formatter;  // [yyyy-MM-dd HH:mm:ss]
    private static String key;                  
    private static Database db;   
    private static final String FILEPATH = "/home/admin/Server/";   // location of server files

    /**
     * Starts the operator server.
     */
    public static void main(String[] args) {

        // Validate command line arguments
        if (args.length != 2) {
            System.out.println("Invocation: java OperatorServer <Port #> "
                + "<Maximum # of Connections>");
            System.exit(1);
        }

        // Extract the arguments
        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);

        // Start up the server
        try { new OperatorServer(port, threads); }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Creates a new OperatorServer object which will run indefinitely, 
     * accepting connections on a specified port and allowing a maximum of
     * numThreads connections.
     * 
     * @param port          the port number to listen for connections
     * @param numThreads    the maximum number of connections
     * 
     * @throws IOException              if a connection issue occurs
     * @throws FileNotFoundException    if a file cannot be accessed
     */
    public OperatorServer(int port, int numThreads) throws IOException, 
        FileNotFoundException {

        // Open relevant files
        log = new RandomAccessFile(FILEPATH + "Logs/operator-log.txt", "rws");
        log.seek(log.length());
        db = new Database(FILEPATH + "Data/clients.db");
        
        formatter = new SimpleDateFormat("'['yyyy-MM-dd HH:mm:ss']'");

        // Open and store the key
        RandomAccessFile keyFile = new RandomAccessFile(FILEPATH + "Data/accesskey_2.txt", "r");
        key = keyFile.readLine();
        keyFile.close();

        // Open a connection on the specified port for listening
        @SuppressWarnings("resource")
        ServerSocket serverSocket = new ServerSocket(port);
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        // Log starting information
        log.seek(log.length());
        log.writeBytes(getTime() + " Connection Opened (port " + port + ")\n");

        // Add shutdown conditions
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    log.seek(log.length());
                    log.writeBytes(getTime() + " Connection Closed\n");
                    log.writeBytes("----------------------------------------\n");
                    log.close();
                } 
                catch (Exception e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        });

        // Listen for connections indefinitely
        while (true) 
            pool.execute(new ServerThread(serverSocket.accept()));
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

    // --------------------------------------------------------------------- //

    /**
     * Private inner class to act as the individual server threads.
     */
    private static class ServerThread implements Runnable {

        // ServerThread data fields:
        private Socket socket;
        private BufferedReader operator_in;
        private PrintWriter operator_out;
        
        /**
         * Creates a new ServerThread object using a supplied socket object as
         * its connection. 
         * 
         * @param socket    the socket to communicate with
         */
        ServerThread(Socket socket) { this.socket = socket; }

        /**
         * This method is called when a new thread is created. It contains the
         * logic of how the server handles the operator.
         */
        @Override
        public void run() {
            try {
                // Log who connected
                log.seek(log.length());
                log.writeBytes(getTime() + "\tConnected: " + socket + "\n");

                // Create I/O buffers
                operator_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                operator_out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                // Send connection confirmation
                sendEncrypted("200 Connected");

                // Validate the key from the operator
                String response = receiveDecrypted();
                if (response == null || !response.equals(key)) {

                    // Send negative verification result
                    sendEncrypted("400 ERROR: invalid key");
                    socket.close();

                    // Log result
                    log.seek(log.length());
                    log.writeBytes(getTime() + "\t\tSocket sent invalid key\n");
                    log.writeBytes(getTime() + "\tClosed: " + socket + "\n");

                    return;
                }

                // Send positive verification result
                sendEncrypted("200 Key Accepted");

                // Log result
                log.seek(log.length());
                log.writeBytes(getTime() + "\t\tKey Accepted\n");

                // External Variable declarations
                ArrayList<String> client_ids = null;

                // Start Menu
                while (true) {
                    boolean leaveNext = false;
                    response = receiveDecrypted();
                    if (response == null) {
                        sendEncrypted("475 ERROR: null decision");
                        log.writeBytes(getTime() + "\t\tOperator send null decision\n");
                        continue;
                    }

                    switch (response) {
                        case "0":
                            // Quit
                            socket.close();

                            break;
                        case "1":
                            // Select client(s)
                            client_ids = getClients();
                            if (client_ids == null) 
                                continue;
                            
                            sendEncrypted("200 Client ID Accepted");
                            log.writeBytes(getTime() + "\t\tClient(s) " + client_ids.toString() + " selected\n");
                            leaveNext = true;

                            break;
                        case "2":
                            // Remove client(s)
                            ArrayList<String> removals = getClients();
                            if (removals == null)
                                continue;

                            for (String removal : removals)
                                db.removeClient(removal);

                            sendEncrypted("200 Clients Removed");
                            log.writeBytes(getTime() + "\t\tClient(s) " + removals.toString() + " removed\n");

                            break;
                        default:
                            // Invalid
                            sendEncrypted("400 Invalid Choice");
                    }

                    if (leaveNext) { break; }
                }

                // Loop unil operator quits
                int decision = -1;
                ArrayList<String> data;
                ArrayList<String> cmds;
                ArrayList<String> rpts;
                while (true) {

                    // Ensure operator sent something
                    response = receiveDecrypted();
                    if (response == null) {

                        // Send error message
                        sendEncrypted("405 ERROR: null response");
                        socket.close();

                        // Log results
                        log.seek(log.length());
                        log.writeBytes(getTime() + "\t\tSocket sent null response\n");
                        log.writeBytes(getTime() + "\tClosed: " + socket + "\n");

                        return;
                    }

                    // Ensure operator sent number
                    try { decision = Integer.parseInt(response); }
                    catch (NumberFormatException e) { 
                         
                        // Send error message
                        sendEncrypted("410 ERROR: invalid decision");

                        // Log results
                        log.seek(log.length());
                        log.writeBytes(getTime() + "\t\tSocket sent invalid decision\n");

                        continue;
                    }

                    // Ensure operator sent VALID number
                    if (decision < -1 || decision > 9) {

                        // Send error message
                        sendEncrypted("415 ERROR: invalid decision");

                        // Log results
                        log.seek(log.length());
                        log.writeBytes(getTime() + "\t\tSocket sent invalid decision\n");

                        continue;
                    }

                    // Handle the operator's decision
                    switch (decision) {
                        case -1:
                            // List encrypted files
                            sendEncrypted("sync");
                            String clientUsed = receiveDecrypted();

                            String files = db.listEncryptedFiles(clientUsed).toString().replaceAll("[\\[\\]\\|(){}]","");
                            if (files == null || files.length() == 0)
                                files = "[No Files]";
                                
                            sendEncrypted(files);
                            log.seek(log.length());
                            log.writeBytes(getTime() + "\t\tEncrypted files sent for " + clientUsed + "\n");

                            break;
                        case 0:
                            // Exit
                            socket.close();

                            break;
                        case 1:
                            // Send new commands
                            sendEncrypted("200 Ready to receive");
                            response = receiveDecrypted();

                            /*
                            response = operator_in.readLine();
                            if (response.contains("|")) {
                                String[] commands = response.split("\\|");
                                response = Driver.run(new AESInput(commands[0], key, false));
                                for ()
                            }
                            */

                            if (response == null || response.equals("")) {
                                sendEncrypted("300 No commands received");
                                log.seek(log.length());
                                log.writeBytes(getTime() + "\t\tNo commands received\n");
                            }
                            else {
                                sendEncrypted("200 Commands received");
                                String[] coms = response.substring(0, response.length() - 1).split("\\|");

                                for (String client_id : client_ids) {
                                    for (String cmd : coms) {
                                        db.addCommand(client_id, cmd);

                                        String[] split_cmd = cmd.split("\\s+");
                                        if (split_cmd[0].equals("encrypt")) {
                                            db.addEncryptedFile(client_id, split_cmd[1], key);
                                        }
                                        else if (split_cmd[0].equals("decrypt")) {
                                            db.removeEncryptedFile(client_id, split_cmd[1]);
                                        }
                                    }
                                }
                                
                                log.seek(log.length());
                                log.writeBytes(getTime() + "\t\tCommands received for " + client_ids.toString() + "\n");
                            }
                            
                            break;
                        case 2:
                            // See backlog of commands, if any
                            for (String client_id : client_ids) {
                                cmds = db.getCommands(client_id);
                                if (cmds == null || cmds.size() == 0) {
                                    sendEncrypted("[No commands in queue]");
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tNo commands to send from " + client_id + " queue\n");
                                }
                                else {
                                    sendEncrypted(cmds.toString());
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tCommands sent from " + client_id + " queue\n");
                                }
                                receiveDecrypted();
                            }

                            break;
                        case 3:
                            // Clear commands
                            for (String client_id : client_ids) {
                                cmds = db.getCommands(client_id);
                                if (cmds.size() == 0) {
                                    sendEncrypted("205 Queue Empty");
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tQueue already empty for " + client_id + "\n");
                                }
                                else {
                                    db.removeAllCommands(client_id);
                                    sendEncrypted("200 Commands Cleared");
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tCommands cleared for " + client_id + "\n");
                                }
                                receiveDecrypted();
                            }
                            
                            break;
                        case 4:
                            // Add repeat commands
                            sendEncrypted("200 Ready to receive");
                            response = receiveDecrypted();

                            if (response == null || response.equals("")) {
                                sendEncrypted("300 No commands received");
                                log.seek(log.length());
                                log.writeBytes(getTime() + "\t\tNo commands received\n");
                            }
                            else {
                                sendEncrypted("200 Commands received");
                                String[] reps = response.substring(0, response.length() - 1).split("\\|");

                                for (String client_id : client_ids) {
                                    for (String rpt : reps) 
                                        db.addRepeat(client_id, rpt);
                                }

                                log.seek(log.length());
                                log.writeBytes(getTime() + "\t\tRepeat commands received for " + client_ids.toString() + "\n");
                            }

                            break;
                        case 5:
                            // View repeat commands
                            for (String client_id : client_ids) {
                                rpts = db.getRepeats(client_id);
                                if (rpts.size() == 0) {
                                    sendEncrypted("[No repeat commands]");
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tNo repeat commands to send from " + client_id + "\n");
                                }
                                else {
                                    sendEncrypted(rpts.toString());
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tRepeat commands sent from " + client_id + "\n");
                                }
                                receiveDecrypted();
                            }

                            break;
                        case 6:
                            // Delete repeat commands
                            for (String client_id : client_ids) {
                                rpts = db.getRepeats(client_id);
                                if (rpts.size() == 0) {
                                    sendEncrypted("205 No Repeat Commands");
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tRepeat commands already empty for " + client_id + "\n");
                                }
                                else {
                                    db.removeAllRepeats(client_id);
                                    sendEncrypted("200 Commands Cleared");
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tRepeat commands cleared for " + client_id + "\n");
                                }
                                receiveDecrypted();
                            }

                            break;
                        case 7:
                            // Results Menu
                            sendEncrypted("sync");
                            while (true) {
                                boolean leaveNext = false;
                                response = receiveDecrypted();
                                switch (response) {
                                    case "0":
                                        // Exit
                                        leaveNext = true;
                                        sendEncrypted("200 Exiting");

                                        break;
                                    case "1":
                                        // Results info
                                        for (String client_id : client_ids) {
                                            String res_data = db.getClientResultsInfo(client_id);
                                            if (res_data == null) {
                                                sendEncrypted("[No results]");
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tNo results info to send for " + client_id + "\n");
                                            }
                                            else {
                                                sendEncrypted(res_data);
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tResults info sent for " + client_id + "\n");
                                            }
                                            receiveDecrypted();
                                        }

                                        break;
                                    case "2":
                                        // View last result
                                        for (String client_id : client_ids) {
                                            String last_data = db.getClientLastResult(client_id);
                                            if (last_data == null) {
                                                operator_out.println("[No results]");
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tNo results to send from " + client_id + " backlog\n");
                                            }
                                            else {
                                                operator_out.println(last_data);
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tCurrent Results sent for " + client_id + "\n");
                                            }
                                            receiveDecrypted();
                                        }

                                        break;
                                    case "3":
                                    case "5":
                                        // View range of results, Download range of results
                                        sendEncrypted("sync");
                                        response = receiveDecrypted();
                                        String[] dates = response.split("\\|");

                                        for (String client_id : client_ids) {
                                            data = db.getClientResultRange(client_id, Double.parseDouble(dates[0]), Double.parseDouble(dates[1]));
                                            if (data == null || data.size() == 0) {
                                                operator_out.println("[No results]");
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tNo results to send from " + client_id + " range\n");
                                            }
                                            else {
                                                for (String d : data) {
                                                    operator_out.println(d);
                                                    receiveDecrypted();
                                                }
                                                operator_out.println("END OF RESULTS");
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tRange Results sent from " + client_id + "\n");
                                            }
                                            receiveDecrypted();
                                        }
                                        
                                        break;
                                    case "4":
                                    case "6":
                                        // View all results, Download all results
                                        for (String client_id : client_ids) {
                                            data = db.getAllClientResults(client_id);
                                            if (data == null || data.size() == 0) {
                                                operator_out.println("[No past results]");
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tNo results to send from " + client_id + " archive\n");
                                            }
                                            else {
                                                for (String d : data) {
                                                    operator_out.println(d);
                                                    receiveDecrypted();
                                                }
                                                operator_out.println("END OF RESULTS");
                                                log.seek(log.length());
                                                log.writeBytes(getTime() + "\t\tAll Results sent from " + client_id + "\n");
                                            }
                                            receiveDecrypted();
                                        }

                                        break;
                                    default:
                                        // Invalid
                                        sendEncrypted("400 Invalid Choice");
                                }

                                if (leaveNext) { break; }
                            }

                            break;
                        case 8:
                            // Client info
                            for (String client_id : client_ids) {
                                ArrayList<String> client_info = db.getClientInfo(client_id);
                                if (client_info == null || client_info.size() == 0) {
                                    sendEncrypted("490 Client Info Not Found");
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tClient info not found for " + client_id + "\n");
                                }
                                else {
                                    String formattedClients = client_info.toString().replaceAll("[\\[\\](){}]","");
                                    sendEncrypted(formattedClients);
                                    log.seek(log.length());
                                    log.writeBytes(getTime() + "\t\tClient info sent for " + client_id + "\n");
                                }
                                receiveDecrypted();
                            }

                            break;
                        case 9:
                            // Start menu
                            log.writeBytes(getTime() + "\t\tChanging clients...\n");
                            sendEncrypted("sync");

                            while (true) {
                                boolean leaveNext = false;
                                response = receiveDecrypted();
                                if (response == null) {
                                    sendEncrypted("475 ERROR: null decision");
                                    log.writeBytes(getTime() + "\t\tOperator send null decision\n");
                                    continue;
                                }

                                switch (response) {
                                    case "0":
                                        // Quit
                                        socket.close();

                                        break;
                                    case "1":
                                        // Select client(s)
                                        client_ids = getClients();
                                        if (client_ids == null) 
                                            continue;
                                        
                                        sendEncrypted("200 Client ID Accepted");
                                        log.writeBytes(getTime() + "\t\tClient(s) " + client_ids.toString() + " selected\n");
                                        leaveNext = true;

                                        break;
                                    case "2":
                                        // Remove client(s)
                                        ArrayList<String> removals = getClients();
                                        if (removals == null)
                                            continue;

                                        for (String removal : removals)
                                            db.removeClient(removal);

                                        sendEncrypted("200 Clients Removed");
                                        log.writeBytes(getTime() + "\t\tClient(s) " + removals.toString() + " removed\n");

                                        break;
                                    default:
                                        // Invalid
                                        sendEncrypted("400 Invalid Choice");
                                }

                                if (leaveNext) { break; }
                            }

                            break;
                        default:
                            // Invaid decision
                            sendEncrypted("420 ERROR: invalid decision");
                            log.seek(log.length());
                            log.writeBytes(getTime() + "\t\tSocket sent invalid decision\n");
                    }
                }
            }
            catch (SocketException e) { 
                try { 
                    log.seek(log.length());
                    log.writeBytes(getTime() + "\t\tOperator terminated connection\n");
                    log.writeBytes(getTime() + "\tClosed: " + socket + "\n"); 
                } catch (Exception f) { f.printStackTrace(); }
            } 
            catch (Exception e) { e.printStackTrace(); }
        }

        /**
         * Encrypts plaintext to ciphertext.
         */
        private String encrypt(String str) {
            AESInput input = new AESInput(str, key, true);
            return Driver.run(input);
        }

        /**
         * Decrypts ciphertext to plaintext.
         */
        private String decrypt(String str) {
            AESInput input = new AESInput(str, key, false);
            return Driver.run(input);
        }

        /**
         * Encrypts input before being sent to the client. Currently not in use
         * until Client.c has the capability to decrypt messages.
         * 
         * @param str   the string to encrypt and send
         */
        private void sendEncrypted(String str) { 
            operator_out.println(encrypt(str)); 
        }

        /**
         * Receives and decrypts a message from the client. Currently not in
         * use until Client.c has the capability to encrypt messages.
         * 
         * @return a decrypted string from the client
         */
        private String receiveDecrypted() throws Exception { 
            String rec = operator_in.readLine();
            if (rec == null)
                return null;

            return decrypt(rec); 
        }

        /**
         * Sends the list of available clients to the operator and receive
         * their selection.
         * 
         * @return an ArrayList of selected clients
         */
        private ArrayList<String> getClients() throws Exception {

            ArrayList<String> clients = db.getClientList();
            if (clients == null || clients.size() == 0) {
                sendEncrypted("No Known Clients.");
                log.writeBytes(getTime() + "\t\tNo known clients\n");
                return null;
            }

            String formattedClients = clients.toString().replaceAll("[\\[\\](){}]","");
            sendEncrypted(formattedClients);

            // Receive user choice
            while (true) {
                boolean failed = false;
                String response = receiveDecrypted();
                if (response == null) {
                    sendEncrypted("481 ERROR: null client id");
                    log.writeBytes(getTime() + "\t\tOperator send null client id\n");
                    return null;
                }
                if (response.equalsIgnoreCase("null")) {
                    sendEncrypted("sync");
                    log.writeBytes(getTime() + "\t\tOperator cancelled request\n");
                    return null;
                }

                ArrayList<String> client_ids = new ArrayList<>(Arrays.asList(response.split(",")));
                for (String client_id : client_ids) {
                    if (!clients.contains(client_id)) {
                        sendEncrypted("480|" + client_id);
                        log.writeBytes(getTime() + "\t\tOperator send invalid client id (" + client_id + ")\n");
                        failed = true;
                        break;
                    }
                }
                if (failed) { continue; }

                return client_ids;
            }
        }

    }
}
