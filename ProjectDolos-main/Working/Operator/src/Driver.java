import java.io.Console;
import java.awt.GraphicsEnvironment;

public class Driver {
    public static void main(String [] args) throws Exception {

        Console console = System.console();
        if (console == null && !GraphicsEnvironment.isHeadless()){

            String filename = Driver.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
            Runtime.getRuntime().exec(new String[]{"cmd","/c","start","cmd","/k","java -jar \"" + filename + "\""});
        }
        else{
            String[] args2 = {"23.23.136.140", "20000"};
            Operator.main(args2);
            System.out.println(" [!] Program has ended, type 'exit' to close the console.");
        }
    }
}