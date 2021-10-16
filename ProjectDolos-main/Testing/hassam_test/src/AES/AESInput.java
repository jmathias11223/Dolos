package AES;

/**
 * Object used to store necessary data for encryption/decryption processes
 * 
 * @author Justin Mathias
 * @version 2021.06.08
 */
public class AESInput {
    private String text;
    private String key;
    private boolean encrypt;

    /**
     * Constructor
     * 
     * @param t String to encrypt or decrypt
     * @param k String to be used as the key, must be 256-bit key
     * @param enc Set to true for encryption, false for decryption
     */
    public AESInput(String t, String k, boolean enc) {
        text = t;
        key = k;
        encrypt = enc;
    }


    public String getText() {
        return text;
    }


    public String getKey() {
        return key;
    }


    public String encryptOrDecrypt() {
        if(encrypt) {
            return "Encrypt";
        }
        return "Decrypt";
    }


    public void setText(String t) {
        text = t;
    }


    public void setKey(String k) {
        key = k;
    }


    public void setEncryptOrDecrypt(boolean enc) {
        encrypt = enc;
    }
}
