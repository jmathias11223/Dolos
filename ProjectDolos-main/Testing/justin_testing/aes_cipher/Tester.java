
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;


public class Tester {
    public static void main(String[] args) throws IOException {

        System.out.println(
                  "                oo__                                                  \n"
                + "               <;___)------                                           \n"
                + "  _____        __\"_\"__    _              _ _ _             __   ___  \n"
                + " |  __ \\    /\\|__   __|  | |            (_| | |           /_ | |__ \\ \n"
                + " | |__) |  /  \\  | | __ _| |_ ___  _   _ _| | | ___  __   _| |    ) |\n"
                + " |  _  /  / /\\ \\ | |/ _` | __/ _ \\| | | | | | |/ _ \\ \\ \\ / | |   / / \n"
                + " | | \\ \\ / ____ \\| | (_| | || (_) | |_| | | | |  __/  \\ V /| |_ / /_ \n"
                + " |_|  \\_/_/    \\_|_|\\__,_|\\__\\___/ \\__,_|_|_|_|\\___|   \\_/ |_(_|____|\n");

        String list = '"bin" : "AAA", 
        "com" : "456",
        "exe" : "8FD",
        "jar" : "611",
        "py" : "FFF",
        "mp3" : "3CB",
        "mp4" : "499",
        "wav" : "999",
        "sh" : "911",
        "xls" : "DD6",
        "xlsx" : "FAC",
        "txt" : "F78",
        "doc" : "784",
        "docx" : "1DB",
        "pdf" : "C44",
        "csv" : "CDB",
        "tar" : "633",
        "rar" : "777",
        "gif" : "D43",
        "jpg" : "3FC",
        "jpeg" : "222",
        "png" : "691",
        "zip" : "8AC",
        "gzip" : "7DD"';
        String[] arr = list.split(",");
        for(String s : arr) {
            System.out.println(s.substring(9, 12) + " : " + s.substring(1, 4) + ",");
        }
    }


    /**
     * Takes in the user-input password and modifies it 
     * to make it untraceable
     * 
     * @param key
     * @return Encrypted key
     */
    public static String encryptKey(String key) {
        if(key.length() > 32) {
            key = key.substring(0, 32);
        }
        key = Driver.textToHex(key);
        while(key.length() < 64) {
            key = key + "00";
        }
        System.out.println(key);
        String encrypted = "";
        for(int runs = 0; runs < 2; runs++) {
            String curr = key.substring(runs * 32, runs * 32 + 32);
            // Run sbox
            String time = getTime();
            String[] arr = time.split("-");
            int day = Integer.parseInt(arr[2].substring(0, 2));
            while(day > 0) {
                curr = Driver.subBytes(curr);
                day--;
            }
            encrypted += curr;
        }
        key = "";
        for(int i = 0; i < 2; i++) {
            // Perform a logical XOR on a static hex value to get rid of any patterns
            key += Driver.addRoundKey(encrypted.substring(i * 32, i * 32 + 32), "30190dcc14585301f5bfc5b666c84775");
        }
        return key;
    }

    // Decrypts key
    public static String decryptKey(String key) {
        String decrypted = "";
        for(int i = 0; i < 2; i++) {
            decrypted += Driver.addRoundKey(key.substring(i * 32, i * 32 + 32), "30190dcc14585301f5bfc5b666c84775");
        }
        key = decrypted;
        decrypted = "";
        for(int runs = 0; runs < 2; runs++) {
            // Run sbox
            String time = getTime();
            String[] arr = time.split("-");
            int day = Integer.parseInt(arr[2].substring(0, 2));
            String curr = key.substring(runs * 32, runs * 32 + 32);
            while(day > 0) {
                curr = Driver.invSubBytes(curr);
                day--;
            }
            decrypted += curr;
        }
        return decrypted;
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
}

// 54466886699773344997733441155446
// 54686973497341546573744B6579000000000000000000000000000000000000