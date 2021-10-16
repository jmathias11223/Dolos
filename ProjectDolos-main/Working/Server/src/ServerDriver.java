/**
 * Runs both the ClientServer and the OperatorServer simultaneously. Disables 
 * console output for a smoother and faster run, however server logs are still
 * kept for both scripts.
 * 
 * Compilation: From the Server/src directory, input the following command:
 * 
 *      javac ServerDriver.java -d ../bin/
 * 
 * Invocation: From the Server/bin directory, input the following command:
 * 
 *      java ServerDriver <Client Port> <Operator Port> <Max # Clients> <Max # Operators>
 * 
 * @author Aidan Sprague
 * @version 07.12.2021
 */
public class ServerDriver {

    public static void main(String[] args) {

        // Validate command line arguments
        if (args.length != 4 && args.length != 0) {
            System.out.println("Invocation: java ServerDriver <Client Port> "
                + "<Operator Port> <Max # Clients> <Max # Operators");
            System.exit(1);
        }
        
        // Run both of the servers
        if (args.length == 4) {
            String[] args1 = {args[0], args[2]};
            String[] args2 = {args[1], args[3]};
            new Thread(new Runnable() {
                public void run() { ClientServer.main(args1); }
            }).start();

            OperatorServer.main(args2);
        }
        else {
            String[] args1 = {"10000", "10"};
            String[] args2 = {"20000", "10"};
            new Thread(new Runnable() {
                public void run() { ClientServer.main(args1); }
            }).start();

            OperatorServer.main(args2);
        }
    }
}
