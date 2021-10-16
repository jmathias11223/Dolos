package AES;

import java.util.ArrayList;

public class MatrixMultiplication {


	public static void main(String[] args) {
		String hex = Driver.textToHex("Hello.");
		System.out.println(Driver.hexToText(hex));
	}

	/**
	 * Performs an addition of two polynomials represented by arrays in Z2.
	 * 
	 * @param arr  First polynomial
	 * @param arr2 Second polynomial
	 * @return ret Result of addition
	 */
	public static int[] addition(int[] arr, int[] arr2) {
		int[] ret = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ret[i] = (arr[i] + arr2[i]) % 2;
		}
		return ret;
	}

	/**
	 * Performs a multiplication of two polynomials represented by arrays in Z2.
	 * 
	 * @param arr  First polynomial
	 * @param arr2 Second polynomial
	 * @return ret Result of multiplication
	 */
	public static int[] multiplication(int[] arr, int[] arr2) {
		int[] ret = { 0, 0, 0, 0, 0, 0, 0, 0 };
		if (arr[arr.length - 1] == 1) {
			ret = arr;
		}
		// Count the 1s and their positions in arr2
		ArrayList<Integer> positions = new ArrayList<Integer>();
		for (int i = arr2.length - 2; i >= 0; i--) {
			if (arr2[i] == 1) {
				positions.add(i);
			}
		}
		// Perform repeated xtime functions
		int[] previousXtime = arr;
		int count = 0;
		for (int i = arr.length - 2; i >= 0; i--) {
			previousXtime = xtime(previousXtime);
			if (count < positions.size() && positions.get(count) == i) {
				ret = addition(ret, previousXtime);
				count++;
			}
		}
		return ret;
	}

	/**
	 * Performs the xtime function operation on a given polynomial If the result is
	 * an overflow, XORs the array with the AES polynomial
	 * 
	 * @param arr Array to be xtimed
	 * @return ret
	 */
	public static int[] xtime(int[] arr) {
		int[] newArr = new int[9];
		// Left shift by one
		for (int i = arr.length - 1; i >= 0; i--) {
			newArr[i] = arr[i];
		}
		newArr[newArr.length - 1] = 0;
		if (newArr[0] == 1) {
			int[] AESPolynomial = { 1, 0, 0, 0, 1, 1, 0, 1, 1 };
			newArr = addition(newArr, AESPolynomial);
		}
		// Transfer newArr to an int array of length 8
		int[] ret = new int[8];
		for (int i = 1; i < newArr.length; i++) {
			ret[i - 1] = newArr[i];
		}
		return ret;
	}
}
