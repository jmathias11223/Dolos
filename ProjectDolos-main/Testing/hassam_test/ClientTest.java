import java.net.*;
import java.io.*;
import java.util.*;
  
public class ClientTest
{
    public static void main(String[] args) throws IOException {
        // don't need to specify a hostname, it will be the current machine
        ServerSocket ss = new ServerSocket(7777);
        System.out.println("ServerSocket awaiting connections...");
        Socket socket = ss.accept(); // blocking call, this will wait until a connection is attempted on this port.
        System.out.println("Connection from " + socket + "!");
        try{

        
        // get the input stream from the connected socket
        InputStream inputStream = socket.getInputStream();
        // create a DataInputStream so we can read data from it.
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        // read the message from the socket
        String message = dataInputStream.readUTF();
        System.out.println("The message sent from the socket was: " + message);

        System.out.println("Closing sockets.");
        ss.close();
        socket.close();
        }catch(Exception e ){
            System.out.println("end");
        }
    }
}

