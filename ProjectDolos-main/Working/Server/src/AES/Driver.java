package AES;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Program that models AES encryption & decryption
 * 
 * If you run this code in Eclipse, you can just press run on Driver.java(the
 * main class), and the console will prompt you for the name of the input file,
 * key file, whether this is for encryption or decryption, and whether this is
 * in ECB or CBC mode. Please only enter "encrypt" or "decrypt" for the
 * encryption/decryption prompt and only "CBC" or "ECB" for the ECB/CBC prompt.
 * 
 * When used with the RAT program, follow these steps: 1. Create an instance of
 * the AESInput object and fill it with all necessary information. 2. Call the
 * run() method from this class with the AESInput object as a parameter. 3. The
 * run() method will return the encrypted or decrypted String.
 * 
 * @author Justin Mathias
 * @version 2021.06.08
 *
 */
public class Driver {

	public static final int AES_POLYNOMIAL = 0b100011011;

	/**
	 * Main runner method, used only for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.println("Please enter the name of the input file below:");
		String fileName = sc.next();
		File input = new File(fileName);
		Scanner fileScanner = null;
		try {
			fileScanner = new Scanner(input);
		} catch (FileNotFoundException e) {
			System.out.println("Please enter a valid input file.");
			System.exit(0);
		}
		System.out.println("Please enter the name of the file containing the key below:");
		String keyFileName = sc.next();
		File keyFile = new File(keyFileName);
		Scanner keyScanner = null;
		try {
			keyScanner = new Scanner(keyFile);
		} catch (FileNotFoundException e) {
			System.out.println("Please enter a valid key file.");
			System.exit(0);
		}
		System.out.println("Would you like to encrypt or decrypt? Type either encrypt or decrypt below.");
		String option = sc.next();
		System.out.println("CBC or ECB?");
		String cbcOrEcb = sc.next();

		String in = fileScanner.next().toUpperCase();
		String k = keyScanner.next();
		AESType type = AESType.AES128;
		if (k.length() == 48) {
			type = AESType.AES192;
		} else if (k.length() == 64) {
			type = AESType.AES256;
		}
		String key = keyExpansion(k, type);

		boolean mode = false; // True for CBC, false for ECB
		if (option.equalsIgnoreCase("encrypt")) {
			if (cbcOrEcb.equalsIgnoreCase("CBC")) {
				mode = true;
			} else {
				mode = false;
			}
			String encrypted = encrypt(in, key, type, mode);
			System.out.println("Encrypted text:\n" + encrypted);
		} else if (option.equalsIgnoreCase("decrypt")) {
			String decrypted = decrypt(in, key, type, mode);
			System.out.println("Decrypted text:\n" + decrypted);
		} else {
			System.out.println("Please enter a valid option for encrypt or decrypt.");
			System.exit(0);
		}

		// System.out.println(decrypt("8d04fff27a081a77de2009d1402e6e03", key,
		// AESType.AES128));

		sc.close();
		fileScanner.close();
		keyScanner.close();
	}

	/**
	 * Main run method, call this with the appropriate AESInput object to get the
	 * encrypted or decrypted text
	 * 
	 * @param input AESInput object with all necessary information
	 * @return Encrypted or decrypted String
	 */
	public static String run(AESInput input) {
		if (input.encryptOrDecrypt().equals("Encrypt")) {
			return encrypt(textToHex(input.getText()), keyExpansion(input.getKey(), AESType.AES256), AESType.AES256,
					false); // AES256, CBC mode
		}
		String dec = decrypt(input.getText(), keyExpansion(input.getKey(), AESType.AES256), AESType.AES256, false);
		while (dec.substring(dec.length() - 2).equals("00")) {
			dec = dec.substring(0, dec.length() - 2);
		}
		return hexToText(dec);
	}

	/**
	 * Encrypts the given plaintext using the key given. User must specify the type
	 * of AES encryption(AES-128, AES-192, or AES-256) and the mode of
	 * encryption(CBC or ECB).
	 * 
	 * @return Encrypted text
	 */
	public static String encrypt(String plaintxt, String key, AESType type, boolean mode) {
		int rounds;
		switch (type) {
			case AES128:
				rounds = 10;
				break;

			case AES192:
				rounds = 12;
				break;

			case AES256:
				rounds = 14;
				break;

			default:
				rounds = 0;
				break;
		}
		StringBuilder ret = new StringBuilder();
		boolean firstRound = true;
		while (plaintxt.length() > 0) {
			String text;
			if (plaintxt.length() >= 32) {
				text = plaintxt.substring(0, 32);
				plaintxt = plaintxt.substring(32);
			} else {
				text = plaintxt;
				plaintxt = "";
			}
			int count = 0;
			while (count < rounds - 1) {
				if (!firstRound && count == 0 && mode) {
					text = addRoundKey(text, key.substring(count * 32, count * 32 + 32));
					text = addRoundKey(text, ret.toString().substring(ret.length() - 32));
				} else {
					text = addRoundKey(text, key.substring(count * 32, count * 32 + 32));
				}
				text = subBytes(text);
				String[][] temp = new String[4][4];
				temp = shiftRows(text);
				text = mixColumns(temp);
				count++;
			}
			text = addRoundKey(text, key.substring(count * 32, count * 32 + 32));
			text = subBytes(text);
			String[][] temp = new String[4][4];
			temp = shiftRows(text);
			StringBuilder str = new StringBuilder();
			// Reproduce String
			for (int i = 0; i < temp.length; i++) {
				for (int j = 0; j < temp[0].length; j++) {
					str.append(temp[j][i]);
				}
			}
			count++;
			String encrypted = str.toString();
			ret.append(addRoundKey(encrypted, key.substring(count * 32, count * 32 + 32)));
			count++;
			firstRound = false;
		}
		return ret.toString();
	}

	/**
	 * Decrypts the given ciphertext using the key given.
	 * 
	 * @param ciphertxt Ciphertext to decrypt
	 * @param key       The key used in the decryption
	 * @param type      AES-128, 192, or 256
	 * @param mode      CBC or ECB mode(true for CBC, false for ECB)
	 */
	public static String decrypt(String ciphertxt, String key, AESType type, boolean mode) {
		int rounds;
		switch (type) {
			case AES128:
				rounds = 10;
				break;

			case AES192:
				rounds = 12;
				break;

			case AES256:
				rounds = 14;
				break;

			default:
				rounds = 0;
				break;
		}
		StringBuilder ret = new StringBuilder();
		boolean firstRound = true;
		while (ciphertxt.length() > 0) {
			String text;
			if (ciphertxt.length() >= 32) {
				text = ciphertxt.substring(0, 32);
				ciphertxt = ciphertxt.substring(32);
			} else {
				text = ciphertxt;
				ciphertxt = "";
			}
			int count = rounds - 1;
			if (firstRound) {
				key = key.substring(0, key.length() - 32);
			}
			text = addRoundKey(text, key.substring(key.length() - 32));
			String[][] temp = new String[4][4];
			temp = invShiftRows(text);
			StringBuilder str = new StringBuilder();
			// Reproduce String
			for (int i = 0; i < temp.length; i++) {
				for (int j = 0; j < temp[0].length; j++) {
					str.append(temp[j][i]);
				}
			}
			count--;
			String store = str.toString();
			text = invSubBytes(store);
			text = addRoundKey(text, key.substring(key.length() - 64, key.length() - 32));
			while (count >= 0) {
				String[][] tempArr = new String[4][4];
				// Convert to 2D array
				int pos = 0;
				for (int row = 0; row < tempArr.length; row++) {
					for (int col = 0; col < tempArr[0].length; col++) {
						tempArr[col][row] = text.substring(pos, pos + 2);
						pos += 2;
					}
				}
				// text = invMixColumns(tempArr);
				text = mixColumns(tempArr);
				pos = 0;
				for (int row = 0; row < tempArr.length; row++) {
					for (int col = 0; col < tempArr[0].length; col++) {
						tempArr[col][row] = text.substring(pos, pos + 2);
						pos += 2;
					}
				}
				text = mixColumns(tempArr);
				pos = 0;
				for (int row = 0; row < tempArr.length; row++) {
					for (int col = 0; col < tempArr[0].length; col++) {
						tempArr[col][row] = text.substring(pos, pos + 2);
						pos += 2;
					}
				}
				text = mixColumns(tempArr);
				tempArr = invShiftRows(text);
				// Reproduce String
				str = new StringBuilder();
				for (int i = 0; i < temp.length; i++) {
					for (int j = 0; j < temp[0].length; j++) {
						str.append(tempArr[j][i]);
					}
				}
				text = invSubBytes(str.toString());
				if (count == 0 && !firstRound && mode) {
					text = addRoundKey(text, ret.toString().substring(ret.toString().length() - 32));
				}
				text = addRoundKey(text, key.substring(count * 32, count * 32 + 32));
				count--;
			}
			ret.append(text);
			firstRound = false;
		}
		return ret.toString();
	}

	/**
	 * Takes a sequence of 128 bits as text and key and XORs them together
	 * 
	 * @param text Text to encrypt
	 * @param key  Key to encrypt with
	 * @return text XOR key
	 */
	public static String addRoundKey(String text, String key) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < key.length(); i += 2) {
			long first;
			if (i >= text.length()) {
				first = 0;
			} else {
				first = Long.parseLong(text.substring(i, i + 2), 16);
			}
			long sec = Long.parseLong(key.substring(i, i + 2), 16);
			String temp = Long.toHexString(first ^ sec);
			while (temp.length() < 2) {
				temp = "0" + temp;
			}
			str.append(temp);
		}
		return str.toString().toUpperCase();
	}

	/**
	 * Performs the subBytes operation on the text
	 * 
	 * @param text Text to substitute using sbox (128 bits; 16 bytes, 32 characters)
	 * @return Substituted text
	 */
	public static String subBytes(String text) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			str.append(sbox(text.substring(8 * i, 8 * i + 8)));
		}
		return str.toString().toUpperCase();
	}

	/**
	 * Performs the inverse subBytes operation on the text
	 * 
	 * @param text Text to substitute using sbox (128 bits; 16 bytes, 32 characters)
	 * @return Substituted text
	 */
	public static String invSubBytes(String text) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			str.append(invsbox(text.substring(8 * i, 8 * i + 8)));
		}
		return str.toString().toUpperCase();
	}

	/**
	 * Performs the shiftRows operation as part of the AES cipher
	 * 
	 * @param text Text to shift
	 * @return Matrix with shifted text
	 */
	public static String[][] shiftRows(String text) {
		// Convert to 2D array
		String[][] matrix = new String[4][4];
		int pos = 0;
		for (int row = 0; row < matrix.length; row++) {
			for (int col = 0; col < matrix[0].length; col++) {
				matrix[col][row] = text.substring(pos, pos + 2);
				pos += 2;
			}
		}

		// Perform shift row operation
		for (int row = 1; row < matrix.length; row++) {
			for (int i = 0; i < row; i++) {
				String temp = matrix[row][0];
				matrix[row][0] = matrix[row][1];
				matrix[row][1] = matrix[row][2];
				matrix[row][2] = matrix[row][3];
				matrix[row][3] = temp;
			}
		}

		return matrix;
	}

	/**
	 * Performs the inverse ShiftRows operation on the given text as part of the AES
	 * decryption process
	 * 
	 * @param text Text to shift
	 * @return Matrix with shifted text
	 */
	public static String[][] invShiftRows(String text) {
		// Convert to 2D array
		String[][] matrix = new String[4][4];
		int pos = 0;
		for (int row = 0; row < matrix.length; row++) {
			for (int col = 0; col < matrix[0].length; col++) {
				matrix[col][row] = text.substring(pos, pos + 2);
				pos += 2;
			}
		}

		// Perform shift row operation
		for (int row = 1; row < matrix.length; row++) {
			for (int i = 0; i < row; i++) {
				String temp = matrix[row][3];
				matrix[row][3] = matrix[row][2];
				matrix[row][2] = matrix[row][1];
				matrix[row][1] = matrix[row][0];
				matrix[row][0] = temp;
			}
		}

		return matrix;
	}

	/**
	 * Perform matrix multiplication with the following 2D array: [2, 3, 1, 1] [1,
	 * 2, 3, 1] [1, 1, 2, 3] [3, 1, 1, 2]
	 * 
	 * @param text Matrix to multiply with
	 * @return String representation of resulting matrix
	 */
	public static String mixColumns(String[][] text) {
		int[][] matrix = { { 2, 3, 1, 1 }, { 1, 2, 3, 1 }, { 1, 1, 2, 3 }, { 3, 1, 1, 2 } };
		String newText[][] = new String[4][4];
		StringBuilder str = new StringBuilder();
		for (int col = 0; col < text[0].length; col++) { // Traverse each column of text
			for (int row = 0; row < text.length; row++) { // Traverse each column entry in text
				long[] add = new long[4];
				for (int matRow = 0; matRow < matrix.length; matRow++) {
					long curr = Long.parseLong(text[matRow][col], 16);
					if (matrix[row][matRow] == 2) {
						curr *= 2;
					} else if (matrix[row][matRow] == 3) {
						curr = (curr * 2) ^ curr;
					}
					add[matRow] = curr;
				}
				long sum = add[0] ^ add[1] ^ add[2] ^ add[3];
				if (Long.toHexString(sum).length() >= 3) { // Check for overflow
					sum = sum ^ AES_POLYNOMIAL;
				}
				String stringSum = Long.toHexString(sum);
				while (stringSum.length() < 2) { // Prepend zeros
					stringSum = "0" + stringSum;
				}
				newText[row][col] = stringSum;
			}
		}

		// Reproduce String
		for (int i = 0; i < newText.length; i++) {
			for (int j = 0; j < newText[0].length; j++) {
				str.append(newText[j][i]);
			}
		}
		return str.toString().toUpperCase();
	}

	/**
	 * Perform matrix multiplication with the following 2D array: [E, B, D, 9] [9,
	 * E, B, D] [D, 9, E, B] [B, D, 9, E]
	 * 
	 * @param text Matrix to multiply with
	 * @return String representation of resulting matrix
	 */
	public static String invMixColumns(String[][] text) {
		int[][] matrix = { { 0xE, 0xB, 0xD, 0x9 }, { 0x9, 0xE, 0xB, 0xD }, { 0xD, 0x9, 0xE, 0xB },
				{ 0xB, 0xD, 0x9, 0xE } };
		String newText[][] = new String[4][4];
		StringBuilder str = new StringBuilder();
		for (int col = 0; col < text[0].length; col++) { // Traverse each column of text
			for (int row = 0; row < text.length; row++) { // Traverse each column entry in text
				int[] rows = new int[4];
				int[] columns = new int[4];
				for (int i = 0; i < text[row].length; i++) { // Pick out the rows and columns
					rows[i] = Integer.parseInt(text[row][i], 16);
					columns[i] = matrix[i][col];
				}
				long sum = 0;
				for (int i = 0; i < 4; i++) {
					sum = sum + (rows[i] * columns[i]);
				}

				int[] bin = hexToBinary16(sum);
				int len = lengthOfBin(bin);
				while (len > 8) { // Check for overflow
					long polynomial = AES_POLYNOMIAL << (len - 9);
					sum = sum ^ polynomial;
					bin = hexToBinary16(sum);
					len = lengthOfBin(bin);
				}
				String stringSum = Long.toHexString(sum);

				while (stringSum.length() < 2) { // Prepend zeros
					stringSum = "0" + stringSum;
				}
				newText[row][col] = stringSum;
			}
		}

		// Reproduce String
		for (int i = 0; i < newText.length; i++) {
			for (int j = 0; j < newText[0].length; j++) {
				str.append(newText[j][i]);
			}
		}
		return str.toString().toUpperCase();
	}

	// Converts hex to binary
	public static int[] hexToBinary(int hex) {
		int[] arr = new int[8];
		int temp = hex;
		for (int i = arr.length - 1; i >= 0; i--) {
			arr[i] = (temp >> (7 - i)) & 1;
		}
		return arr;
	}

	// Converts hex to a 16-bit binary value
	public static int[] hexToBinary16(long hex) {
		int arr[] = new int[16];
		long temp = hex;
		for (int i = arr.length - 1; i >= 0; i--) {
			arr[i] = (int) ((temp >> (15 - i)) & 1);
		}
		return arr;
	}

	// Returns the length of the binary number
	public static int lengthOfBin(int[] bin) {
		int i = 0;
		while (i < bin.length && bin[i] == 0) {
			i++;
		}
		return bin.length - i;
	}

	/**
	 * Converts ASCII text to hex
	 * 
	 * @param text ASCII text
	 * @return Hex representation
	 */
	public static String textToHex(String text) {
		StringBuilder str = new StringBuilder();
		// System.out.println("Original Key " + text);
		char[] arr = text.toCharArray();
		// System.out.println("Character array Key " + arr.length);
		for (char c : arr) {
			str.append(Integer.toHexString(c));
		}
		// System.out.println("TO STRING " + str.toString());
		return str.toString().toUpperCase();
	}

	// Converts the given hex value to ASCII text
	public static String hexToText(String hex) {
		StringBuilder str = new StringBuilder();
		char[] arr = hex.toCharArray();
		for (int i = 0; i < arr.length; i += 2) {
			str.append((char) Integer.parseInt("" + arr[i] + arr[i + 1], 16));
		}
		return str.toString();
	}

	/**
	 * Key expansion algorithm
	 * 
	 * @param keyUsed Initial key
	 * @param type    AES-128, 192, or 256
	 * @return Expanded key
	 */
	public static String keyExpansion(String keyUsed, AESType type) {
		String key = keyUsed; // Initial key
		int forLoopIterations = 4; // Number of for loop iterations; 4 for AES-128, 6 for AES-192, 8 for AES-256
		int whileLoopIterations = 176; // Number of while loop iterations; 176 for AES-128, 208 for AES-192, 240 for
										// AES-256
		switch (type) {
			case AES128:
				forLoopIterations = 4;
				whileLoopIterations = 176;
				break;

			case AES192:
				forLoopIterations = 6;
				whileLoopIterations = 208;
				break;

			case AES256:
				forLoopIterations = 8;
				whileLoopIterations = 240;
				break;

			default:
				break;
		}
		whileLoopIterations *= 2; // Each byte has 2 hex values in it

		String temp1 = key.substring(key.length() - 8); // Setting temp1
		String temp2 = key.substring(0, 8); // Setting temp2
		long t1 = Long.parseLong(temp1, 16); // Parse hex values to decimal values
		long t2 = Long.parseLong(temp2, 16);
		StringBuilder builder = new StringBuilder(); // Store key
		builder.append(key);
		String sboxed;
		int iteration = 0;
		long polynomial = 1;

		while (builder.toString().length() < whileLoopIterations) {
			for (int i = 0; i < forLoopIterations; i++) {
				temp1 = builder.substring(builder.toString().length() - 8);
				temp2 = builder.substring(8 * (i + forLoopIterations * iteration),
						8 * (i + forLoopIterations * iteration) + 8);
				if (i == 0) {
					temp1 = temp1.substring(2) + temp1.substring(0, 2); // Rotation
					temp1 = sbox(temp1); // Run it through sbox
					// XOR with polynomial on first two bits
					String temp3 = temp1.substring(0, 2);
					String temp4 = temp1.substring(2);
					temp3 = Long.toHexString(Long.parseLong(temp3, 16) ^ polynomial).toUpperCase();
					temp1 = temp3 + temp4;
					polynomial = (polynomial << 1) ^ (0x11b & -(polynomial >> 7));
				}
				if (i == 4 && forLoopIterations == 8) { // For AES-256 only
					temp1 = sbox(temp1);
				}
				t1 = Long.parseLong(temp1, 16);
				t2 = Long.parseLong(temp2, 16);
				sboxed = Long.toHexString(t1 ^ t2).toUpperCase();
				while (sboxed.length() < 8) {
					sboxed = "0" + sboxed;
				}
				builder.append(sboxed);
				// System.out.println(sboxed);
			}
			iteration++;
		}

		return builder.toString();
	}

	/**
	 * Method that models the sbox lookup table
	 * 
	 * @param bt 8-character String to convert
	 * @return Converted string
	 */
	public static String sbox(String bt) {
		String conversion = "00	63\r\n" + "01 7C\r\n" + "02	77\r\n" + "03	7B\r\n" + "04	F2\r\n" + "05	6B\r\n"
				+ "06	6F\r\n" + "07	C5\r\n" + "08	30\r\n" + "09	01\r\n" + "0A	67\r\n" + "0B	2B\r\n"
				+ "0C	FE\r\n" + "0D	D7\r\n" + "0E	AB\r\n" + "0F	76\r\n" + "10	CA\r\n" + "11	82\r\n"
				+ "12	C9\r\n" + "13	7D\r\n" + "14	FA\r\n" + "15	59\r\n" + "16	47\r\n" + "17	F0\r\n"
				+ "18	AD\r\n" + "19	D4\r\n" + "1A	A2\r\n" + "1B	AF\r\n" + "1C	9C\r\n" + "1D	A4\r\n"
				+ "1E	72\r\n" + "1F	C0\r\n" + "20	B7\r\n" + "21	FD\r\n" + "22	93\r\n" + "23	26\r\n"
				+ "24	36\r\n" + "25	3F\r\n" + "26	F7\r\n" + "27	CC\r\n" + "28	34\r\n" + "29	A5\r\n"
				+ "2A	E5\r\n" + "2B	F1\r\n" + "2C	71\r\n" + "2D	D8\r\n" + "2E	31\r\n" + "2F	15\r\n"
				+ "30	04\r\n" + "31	C7\r\n" + "32	23\r\n" + "33	C3\r\n" + "34	18\r\n" + "35	96\r\n"
				+ "36	05\r\n" + "37	9A\r\n" + "38	07\r\n" + "39	12\r\n" + "3A	80\r\n" + "3B	E2\r\n"
				+ "3C	EB\r\n" + "3D	27\r\n" + "3E	B2\r\n" + "3F	75\r\n" + "40	09\r\n" + "41	83\r\n"
				+ "42	2C\r\n" + "43	1A\r\n" + "44	1B\r\n" + "45	6E\r\n" + "46	5A\r\n" + "47	A0\r\n"
				+ "48	52\r\n" + "49	3B\r\n" + "4A	D6\r\n" + "4B	B3\r\n" + "4C	29\r\n" + "4D	E3\r\n"
				+ "4E	2F\r\n" + "4F	84\r\n" + "50	53\r\n" + "51	D1\r\n" + "52	00\r\n" + "53	ED\r\n"
				+ "54	20\r\n" + "55	FC\r\n" + "56	B1\r\n" + "57	5B\r\n" + "58	6A\r\n" + "59	CB\r\n"
				+ "5A	BE\r\n" + "5B	39\r\n" + "5C	4A\r\n" + "5D	4C\r\n" + "5E	58\r\n" + "5F	CF\r\n"
				+ "60	D0\r\n" + "61	EF\r\n" + "62	AA\r\n" + "63	FB\r\n" + "64	43\r\n" + "65	4D\r\n"
				+ "66	33\r\n" + "67	85\r\n" + "68	45\r\n" + "69	F9\r\n" + "6A	02\r\n" + "6B	7F\r\n"
				+ "6C	50\r\n" + "6D	3C\r\n" + "6E	9F\r\n" + "6F	A8\r\n" + "70	51\r\n" + "71	A3\r\n"
				+ "72	40\r\n" + "73	8F\r\n" + "74	92\r\n" + "75	9D\r\n" + "76	38\r\n" + "77	F5\r\n"
				+ "78	BC\r\n" + "79	B6\r\n" + "7A	DA\r\n" + "7B	21\r\n" + "7C	10\r\n" + "7D	FF\r\n"
				+ "7E	F3\r\n" + "7F	D2\r\n" + "80	CD\r\n" + "81	0C\r\n" + "82	13\r\n" + "83	EC\r\n"
				+ "84	5F\r\n" + "85	97\r\n" + "86	44\r\n" + "87	17\r\n" + "88	C4\r\n" + "89	A7\r\n"
				+ "8A	7E\r\n" + "8B	3D\r\n" + "8C	64\r\n" + "8D	5D\r\n" + "8E	19\r\n" + "8F	73\r\n"
				+ "90	60\r\n" + "91	81\r\n" + "92	4F\r\n" + "93	DC\r\n" + "94	22\r\n" + "95	2A\r\n"
				+ "96	90\r\n" + "97	88\r\n" + "98	46\r\n" + "99	EE\r\n" + "9A	B8\r\n" + "9B	14\r\n"
				+ "9C	DE\r\n" + "9D	5E\r\n" + "9E	0B\r\n" + "9F	DB\r\n" + "A0	E0\r\n" + "A1	32\r\n"
				+ "A2	3A\r\n" + "A3	0A\r\n" + "A4	49\r\n" + "A5	06\r\n" + "A6	24\r\n" + "A7	5C\r\n"
				+ "A8	C2\r\n" + "A9	D3\r\n" + "AA	AC\r\n" + "AB	62\r\n" + "AC	91\r\n" + "AD	95\r\n"
				+ "AE	E4\r\n" + "AF	79\r\n" + "B0	E7\r\n" + "B1	C8\r\n" + "B2	37\r\n" + "B3	6D\r\n"
				+ "B4	8D\r\n" + "B5	D5\r\n" + "B6	4E\r\n" + "B7	A9\r\n" + "B8	6C\r\n" + "B9	56\r\n"
				+ "BA	F4\r\n" + "BB	EA\r\n" + "BC	65\r\n" + "BD	7A\r\n" + "BE	AE\r\n" + "BF	08\r\n"
				+ "C0	BA\r\n" + "C1	78\r\n" + "C2	25\r\n" + "C3	2E\r\n" + "C4	1C\r\n" + "C5	A6\r\n"
				+ "C6	B4\r\n" + "C7	C6\r\n" + "C8	E8\r\n" + "C9	DD\r\n" + "CA	74\r\n" + "CB	1F\r\n"
				+ "CC	4B\r\n" + "CD	BD\r\n" + "CE	8B\r\n" + "CF	8A\r\n" + "D0	70\r\n" + "D1	3E\r\n"
				+ "D2	B5\r\n" + "D3	66\r\n" + "D4	48\r\n" + "D5	03\r\n" + "D6	F6\r\n" + "D7	0E\r\n"
				+ "D8	61\r\n" + "D9	35\r\n" + "DA	57\r\n" + "DB	B9\r\n" + "DC	86\r\n" + "DD	C1\r\n"
				+ "DE	1D\r\n" + "DF	9E\r\n" + "E0	E1\r\n" + "E1	F8\r\n" + "E2	98\r\n" + "E3	11\r\n"
				+ "E4	69\r\n" + "E5	D9\r\n" + "E6	8E\r\n" + "E7	94\r\n" + "E8	9B\r\n" + "E9	1E\r\n"
				+ "EA	87\r\n" + "EB	E9\r\n" + "EC	CE\r\n" + "ED	55\r\n" + "EE	28\r\n" + "EF	DF\r\n"
				+ "F0	8C\r\n" + "F1	A1\r\n" + "F2	89\r\n" + "F3	0D\r\n" + "F4	BF\r\n" + "F5	E6\r\n"
				+ "F6	42\r\n" + "F7	68\r\n" + "F8	41\r\n" + "F9	99\r\n" + "FA	2D\r\n" + "FB	0F\r\n"
				+ "FC	B0\r\n" + "FD	54\r\n" + "FE	BB\r\n" + "FF	16";
		conversion = conversion.replaceAll("\r", "");
		Scanner sc = new Scanner(conversion);
		sc.useDelimiter("\n");
		boolean first = false;
		boolean sec = false;
		boolean third = false;
		boolean fourth = false;
		String bt1 = bt.substring(0, 2);
		String bt2 = bt.substring(2, 4);
		String bt3 = bt.substring(4, 6);
		String bt4 = bt.substring(6, 8);

		while (sc.hasNext()) {
			String comp = sc.next().replaceAll("\t", " ");
			if (bt.substring(0, 2).equalsIgnoreCase(comp.substring(0, 2)) && !first) {
				bt1 = comp.substring(3, 5);
				first = true;
			}
			if (bt.substring(2, 4).equalsIgnoreCase(comp.substring(0, 2)) && !sec) {
				bt2 = comp.substring(3, 5);
				sec = true;
			}
			if (bt.substring(4, 6).equalsIgnoreCase(comp.substring(0, 2)) && !third) {
				bt3 = comp.substring(3, 5);
				third = true;
			}
			if (bt.substring(6, 8).equalsIgnoreCase(comp.substring(0, 2)) && !fourth) {
				bt4 = comp.substring(3, 5);
				fourth = true;
			}
		}
		String ret = bt1 + bt2 + bt3 + bt4;
		sc.close();
		return ret;
	}

	/**
	 * Method that models the inverse sbox lookup table
	 * 
	 * @param bt 8-character String to convert
	 * @return Converted string
	 */
	public static String invsbox(String bt) {
		String conversion = "00	63\r\n" + "01 7C\r\n" + "02	77\r\n" + "03	7B\r\n" + "04	F2\r\n" + "05	6B\r\n"
				+ "06	6F\r\n" + "07	C5\r\n" + "08	30\r\n" + "09	01\r\n" + "0A	67\r\n" + "0B	2B\r\n"
				+ "0C	FE\r\n" + "0D	D7\r\n" + "0E	AB\r\n" + "0F	76\r\n" + "10	CA\r\n" + "11	82\r\n"
				+ "12	C9\r\n" + "13	7D\r\n" + "14	FA\r\n" + "15	59\r\n" + "16	47\r\n" + "17	F0\r\n"
				+ "18	AD\r\n" + "19	D4\r\n" + "1A	A2\r\n" + "1B	AF\r\n" + "1C	9C\r\n" + "1D	A4\r\n"
				+ "1E	72\r\n" + "1F	C0\r\n" + "20	B7\r\n" + "21	FD\r\n" + "22	93\r\n" + "23	26\r\n"
				+ "24	36\r\n" + "25	3F\r\n" + "26	F7\r\n" + "27	CC\r\n" + "28	34\r\n" + "29	A5\r\n"
				+ "2A	E5\r\n" + "2B	F1\r\n" + "2C	71\r\n" + "2D	D8\r\n" + "2E	31\r\n" + "2F	15\r\n"
				+ "30	04\r\n" + "31	C7\r\n" + "32	23\r\n" + "33	C3\r\n" + "34	18\r\n" + "35	96\r\n"
				+ "36	05\r\n" + "37	9A\r\n" + "38	07\r\n" + "39	12\r\n" + "3A	80\r\n" + "3B	E2\r\n"
				+ "3C	EB\r\n" + "3D	27\r\n" + "3E	B2\r\n" + "3F	75\r\n" + "40	09\r\n" + "41	83\r\n"
				+ "42	2C\r\n" + "43	1A\r\n" + "44	1B\r\n" + "45	6E\r\n" + "46	5A\r\n" + "47	A0\r\n"
				+ "48	52\r\n" + "49	3B\r\n" + "4A	D6\r\n" + "4B	B3\r\n" + "4C	29\r\n" + "4D	E3\r\n"
				+ "4E	2F\r\n" + "4F	84\r\n" + "50	53\r\n" + "51	D1\r\n" + "52	00\r\n" + "53	ED\r\n"
				+ "54	20\r\n" + "55	FC\r\n" + "56	B1\r\n" + "57	5B\r\n" + "58	6A\r\n" + "59	CB\r\n"
				+ "5A	BE\r\n" + "5B	39\r\n" + "5C	4A\r\n" + "5D	4C\r\n" + "5E	58\r\n" + "5F	CF\r\n"
				+ "60	D0\r\n" + "61	EF\r\n" + "62	AA\r\n" + "63	FB\r\n" + "64	43\r\n" + "65	4D\r\n"
				+ "66	33\r\n" + "67	85\r\n" + "68	45\r\n" + "69	F9\r\n" + "6A	02\r\n" + "6B	7F\r\n"
				+ "6C	50\r\n" + "6D	3C\r\n" + "6E	9F\r\n" + "6F	A8\r\n" + "70	51\r\n" + "71	A3\r\n"
				+ "72	40\r\n" + "73	8F\r\n" + "74	92\r\n" + "75	9D\r\n" + "76	38\r\n" + "77	F5\r\n"
				+ "78	BC\r\n" + "79	B6\r\n" + "7A	DA\r\n" + "7B	21\r\n" + "7C	10\r\n" + "7D	FF\r\n"
				+ "7E	F3\r\n" + "7F	D2\r\n" + "80	CD\r\n" + "81	0C\r\n" + "82	13\r\n" + "83	EC\r\n"
				+ "84	5F\r\n" + "85	97\r\n" + "86	44\r\n" + "87	17\r\n" + "88	C4\r\n" + "89	A7\r\n"
				+ "8A	7E\r\n" + "8B	3D\r\n" + "8C	64\r\n" + "8D	5D\r\n" + "8E	19\r\n" + "8F	73\r\n"
				+ "90	60\r\n" + "91	81\r\n" + "92	4F\r\n" + "93	DC\r\n" + "94	22\r\n" + "95	2A\r\n"
				+ "96	90\r\n" + "97	88\r\n" + "98	46\r\n" + "99	EE\r\n" + "9A	B8\r\n" + "9B	14\r\n"
				+ "9C	DE\r\n" + "9D	5E\r\n" + "9E	0B\r\n" + "9F	DB\r\n" + "A0	E0\r\n" + "A1	32\r\n"
				+ "A2	3A\r\n" + "A3	0A\r\n" + "A4	49\r\n" + "A5	06\r\n" + "A6	24\r\n" + "A7	5C\r\n"
				+ "A8	C2\r\n" + "A9	D3\r\n" + "AA	AC\r\n" + "AB	62\r\n" + "AC	91\r\n" + "AD	95\r\n"
				+ "AE	E4\r\n" + "AF	79\r\n" + "B0	E7\r\n" + "B1	C8\r\n" + "B2	37\r\n" + "B3	6D\r\n"
				+ "B4	8D\r\n" + "B5	D5\r\n" + "B6	4E\r\n" + "B7	A9\r\n" + "B8	6C\r\n" + "B9	56\r\n"
				+ "BA	F4\r\n" + "BB	EA\r\n" + "BC	65\r\n" + "BD	7A\r\n" + "BE	AE\r\n" + "BF	08\r\n"
				+ "C0	BA\r\n" + "C1	78\r\n" + "C2	25\r\n" + "C3	2E\r\n" + "C4	1C\r\n" + "C5	A6\r\n"
				+ "C6	B4\r\n" + "C7	C6\r\n" + "C8	E8\r\n" + "C9	DD\r\n" + "CA	74\r\n" + "CB	1F\r\n"
				+ "CC	4B\r\n" + "CD	BD\r\n" + "CE	8B\r\n" + "CF	8A\r\n" + "D0	70\r\n" + "D1	3E\r\n"
				+ "D2	B5\r\n" + "D3	66\r\n" + "D4	48\r\n" + "D5	03\r\n" + "D6	F6\r\n" + "D7	0E\r\n"
				+ "D8	61\r\n" + "D9	35\r\n" + "DA	57\r\n" + "DB	B9\r\n" + "DC	86\r\n" + "DD	C1\r\n"
				+ "DE	1D\r\n" + "DF	9E\r\n" + "E0	E1\r\n" + "E1	F8\r\n" + "E2	98\r\n" + "E3	11\r\n"
				+ "E4	69\r\n" + "E5	D9\r\n" + "E6	8E\r\n" + "E7	94\r\n" + "E8	9B\r\n" + "E9	1E\r\n"
				+ "EA	87\r\n" + "EB	E9\r\n" + "EC	CE\r\n" + "ED	55\r\n" + "EE	28\r\n" + "EF	DF\r\n"
				+ "F0	8C\r\n" + "F1	A1\r\n" + "F2	89\r\n" + "F3	0D\r\n" + "F4	BF\r\n" + "F5	E6\r\n"
				+ "F6	42\r\n" + "F7	68\r\n" + "F8	41\r\n" + "F9	99\r\n" + "FA	2D\r\n" + "FB	0F\r\n"
				+ "FC	B0\r\n" + "FD	54\r\n" + "FE	BB\r\n" + "FF	16";
		conversion = conversion.replaceAll("\r", "");
		Scanner sc = new Scanner(conversion);
		sc.useDelimiter("\n");
		boolean first = false;
		boolean sec = false;
		boolean third = false;
		boolean fourth = false;
		String bt1 = bt.substring(0, 2);
		String bt2 = bt.substring(2, 4);
		String bt3 = bt.substring(4, 6);
		String bt4 = bt.substring(6, 8);

		while (sc.hasNext()) {
			String comp = sc.next().replaceAll("\t", " ");
			if (bt.substring(0, 2).equalsIgnoreCase(comp.substring(3, 5)) && !first) {
				bt1 = comp.substring(0, 2);
				first = true;
			}
			if (bt.substring(2, 4).equalsIgnoreCase(comp.substring(3, 5)) && !sec) {
				bt2 = comp.substring(0, 2);
				sec = true;
			}
			if (bt.substring(4, 6).equalsIgnoreCase(comp.substring(3, 5)) && !third) {
				bt3 = comp.substring(0, 2);
				third = true;
			}
			if (bt.substring(6, 8).equalsIgnoreCase(comp.substring(3, 5)) && !fourth) {
				bt4 = comp.substring(0, 2);
				fourth = true;
			}
		}
		String ret = bt1 + bt2 + bt3 + bt4;
		sc.close();
		return ret;
	}

	public static String encryptKey(String key) {
		if (key.length() > 32) {
			key = key.substring(0, 32);
		}
		// System.out.println("Before Text to Hex " + key);
		key = textToHex(key);
		// System.out.println("After Text to Hex " + key);
		while (key.length() < 64) {
			key = key + "00";
		}
		// System.out.println("After Padding " + key);
		String encrypted = "";
		for (int runs = 0; runs < 2; runs++) {
			String curr = key.substring(runs * 32, runs * 32 + 32);
			// Run sbox
			String time = getTime();
			String[] arr = time.split("-");
			int day = Integer.parseInt(arr[2].substring(0, 2));
			while (day > 0) {
				curr = subBytes(curr);
				day--;
			}
			encrypted += curr;
		}
		key = "";
		for (int i = 0; i < 2; i++) {
			// Perform a logical XOR on a static hex value to get rid of any patterns
			key += addRoundKey(encrypted.substring(i * 32, i * 32 + 32), "30190dcc14585301f5bfc5b666c84775");
		}
		return key;
	}

	// Decrypts key
	public static String decryptKey(String key) {
		String decrypted = "";
		for (int i = 0; i < 2; i++) {
			decrypted += addRoundKey(key.substring(i * 32, i * 32 + 32), "30190dcc14585301f5bfc5b666c84775");
		}
		key = decrypted;
		decrypted = "";
		for (int runs = 0; runs < 2; runs++) {
			// Run sbox
			String time = getTime();
			String[] arr = time.split("-");
			int day = Integer.parseInt(arr[2].substring(0, 2));
			String curr = key.substring(runs * 32, runs * 32 + 32);
			while (day > 0) {
				curr = invSubBytes(curr);
				day--;
			}
			decrypted += curr;
		}
		return decrypted;
	}

	private static String getTime() {
		Date date = new Date(System.currentTimeMillis());
		return new SimpleDateFormat("yyyy-MM-dd'_'HHmmss").format(date);
	}
}
