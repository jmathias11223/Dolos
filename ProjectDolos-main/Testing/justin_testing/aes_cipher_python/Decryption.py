# Python implementation of AES-256 decryption
#
# Use the decrypt() method and pass in the plaintext(for now it must be hex)
# and the key(also must be in hex, must be 64 hex characters long)
# Will return the encrypted String
#
# Author: Justin Mathias

import datetime

# Takes a sequence of 128 bits as text and key and XORs them together
from Encryption import key_expansion

def add_round_key(text: str, key: str) -> str:
    result = ""
    i = 0
    while(i < len(key)):
        first = 0
        if(i < len(text)):
            first = int(text[i:i + 2], base=16)
        sec = int(key[i:i + 2], base=16)
        temp = hex(first ^ sec)[2:]
        while(len(temp) < 2):
            temp = "0" + temp
        result = result + temp
        i += 2
    return result

# Feeds the sequence of bytes into the sbox function
def inv_sub_bytes(text: str) -> str:
    result = ""
    for i in range(0, 4):
        result = result + sbox(text[8 * i: 8 * i + 8])
    return result

def inv_shift_rows(text: str):
    # Convert to matrix
    matrix = [[0 for i in range(0, 4)] for j in range(0, 4)]
    pos = 0
    for row in range(0, 4):
        for col in range(0, 4):
            matrix[col][row] = text[pos:pos + 2]
            pos += 2
    
    # Perform shift row operation
    for row in range(0, 4):
        for i in range(0, row):
            temp = matrix[row][3]
            matrix[row][3] = matrix[row][2]
            matrix[row][2] = matrix[row][1]
            matrix[row][1] = matrix[row][0]
            matrix[row][0] = temp
    
    return matrix

# Performs matrix multiplication with the following matrix:
# [2, 3, 1, 1]
# [1, 2, 3, 1]
# [1, 1, 2, 3]
# [3, 1, 1, 2]
def mix_columns(text) -> str:
    result = ""
    matrix = [[2, 3, 1, 1], [1, 2, 3, 1], [1, 1, 2, 3], [3, 1, 1, 2]]
    newText = [[0 for i in range(0, 4)] for j in range(0, 4)]
    for col in range(0, 4):
        for row in range(0, 4):
            add = [0 for i in range(0, 4)]
            for matRow in range(0, 4):
                curr = int(text[matRow][col], base=16)
                if(matrix[row][matRow] == 2):
                    curr *= 2
                elif(matrix[row][matRow] == 3):
                    curr = (curr * 2) ^ curr
                add[matRow] = curr
            sum = add[0] ^ add[1] ^ add[2] ^ add[3]
            if(len(hex(sum)[2:]) >= 3):
                sum = sum ^ 0b100011011         # XOR with AES polynomial for overflow
            stringSum = hex(sum)[2:]
            while(len(stringSum) < 2):
                stringSum = "0" + stringSum
            newText[row][col] = stringSum
    # Reproduce String
    for i in range(0, 4):
        for j in range(0, 4):
            result = result + newText[j][i]

    return result

def sbox(bt: str) -> str:
    conversion = {"63": "00", "7C": "01", "77": "02", "7B": "03", "F2": "04", "6B": "05", 
    "6F": "06", "C5": "07", "30": "08", "01": "09", "67": "0A",
    "2B": "0B", "FE": "0C", "D7": "0D", "AB": "0E", "76": "0F",
    "CA": "10", "82": "11", "C9": "12", "7D": "13", "FA": "14",
    "59": "15", "47": "16", "F0": "17", "AD": "18", "D4": "19",
    "A2": "1A", "AF": "1B", "9C": "1C", "A4": "1D", "72": "1E",
    "C0": "1F", "B7": "20", "FD": "21", "93": "22", "26": "23",
    "36": "24", "3F": "25", "F7": "26", "CC": "27", "34": "28",
    "A5": "29", "E5": "2A", "F1": "2B", "71": "2C", "D8": "2D",
    "31": "2E", "15": "2F", "04": "30", "C7": "31", "23": "32",
    "C3": "33", "18": "34", "96": "35", "05": "36", "9A": "37",
    "07": "38", "12": "39", "80": "3A", "E2": "3B", "EB": "3C",
    "27": "3D", "B2": "3E", "75": "3F", "09": "40", "83": "41",
    "2C": "42", "1A": "43", "1B": "44", "6E": "45", "5A": "46",
    "A0": "47", "52": "48", "3B": "49", "D6": "4A", "B3": "4B",
    "29": "4C", "E3": "4D", "2F": "4E", "84": "4F", "53": "50",
    "D1": "51", "00": "52", "ED": "53", "20": "54", "FC": "55",
    "B1": "56", "5B": "57", "6A": "58", "CB": "59", "BE": "5A",
    "39": "5B", "4A": "5C", "4C": "5D", "58": "5E", "CF": "5F",
    "D0": "60", "EF": "61", "AA": "62", "FB": "63", "43": "64",
    "4D": "65", "33": "66", "85": "67", "45": "68", "F9": "69",
    "02": "6A", "7F": "6B", "50": "6C", "3C": "6D", "9F": "6E",
    "A8": "6F", "51": "70", "A3": "71", "40": "72", "8F": "73",
    "92": "74", "9D": "75", "38": "76", "F5": "77", "BC": "78",
    "B6": "79", "DA": "7A", "21": "7B", "10": "7C", "FF": "7D",
    "F3": "7E", "D2": "7F", "CD": "80", "0C": "81", "13": "82",
    "EC": "83", "5F": "84", "97": "85", "44": "86", "17": "87",
    "C4": "88", "A7": "89", "7E": "8A", "3D": "8B", "64": "8C",
    "5D": "8D", "19": "8E", "73": "8F", "60": "90", "81": "91",
    "4F": "92", "DC": "93", "22": "94", "2A": "95", "90": "96",
    "88": "97", "46": "98", "EE": "99", "B8": "9A", "14": "9B",
    "DE": "9C", "5E": "9D", "0B": "9E", "DB": "9F", "E0": "A0",
    "32": "A1", "3A": "A2", "0A": "A3", "49": "A4", "06": "A5",
    "24": "A6", "5C": "A7", "C2": "A8", "D3": "A9", "AC": "AA",
    "62": "AB", "91": "AC", "95": "AD", "E4": "AE", "79": "AF",
    "E7": "B0", "C8": "B1", "37": "B2", "6D": "B3", "8D": "B4",
    "D5": "B5", "4E": "B6", "A9": "B7", "6C": "B8", "56": "B9",
    "F4": "BA", "EA": "BB", "65": "BC", "7A": "BD", "AE": "BE",
    "08": "BF", "BA": "C0", "78": "C1", "25": "C2", "2E": "C3",
    "1C": "C4", "A6": "C5", "B4": "C6", "C6": "C7", "E8": "C8",
    "DD": "C9", "74": "CA", "1F": "CB", "4B": "CC", "BD": "CD",
    "8B": "CE", "8A": "CF", "70": "D0", "3E": "D1", "B5": "D2",
    "66": "D3", "48": "D4", "03": "D5", "F6": "D6", "0E": "D7",
    "61": "D8", "35": "D9", "57": "DA", "B9": "DB", "86": "DC",
    "C1": "DD", "1D": "DE", "9E": "DF", "E1": "E0", "F8": "E1",
    "98": "E2", "11": "E3", "69": "E4", "D9": "E5", "8E": "E6",
    "94": "E7", "9B": "E8", "1E": "E9", "87": "EA", "E9": "EB",
    "CE": "EC", "55": "ED", "28": "EE", "DF": "EF", "8C": "F0",
    "A1": "F1", "89": "F2", "0D": "F3", "BF": "F4", "E6": "F5",
    "42": "F6", "68": "F7", "41": "F8", "99": "F9", "2D": "FA",
    "0F": "FB", "B0": "FC", "54": "FD", "BB": "FE", "16": "FF"}
    result = ""
    for i in range(0, 4):
        result = result + conversion[bt[i * 2: i * 2 + 2].upper()]
    return result

def matrixToString(matrix) -> str:
    temp = ""
    for i in range(0, 4):
        for j in range(0, 4):
            temp = temp + matrix[j][i]
    return temp

def decrypt(ciphertxt: str, key: str):
    key = key_expansion(key)
    rounds = 14
    result = ""
    firstRound = True
    while(len(ciphertxt) > 0):
        text = ""
        if(len(ciphertxt) >= 32):
            text = ciphertxt[:32]
            ciphertxt = ciphertxt[32:]
        else:
            text = ciphertxt
            ciphertxt = ""
        count = rounds - 1
        if(firstRound):
            key = key[0:len(key) - 32]
            firstRound = False
        text = add_round_key(text, key[len(key) - 32:])
        temp = inv_shift_rows(text)
        text = matrixToString(temp)
        count -= 1
        text = inv_sub_bytes(text)
        text = add_round_key(text, key[len(key) - 64:len(key) - 32])
        while(count >= 0):
            matrix = [[0 for i in range(0, 4)] for j in range(0, 4)]
            pos = 0
            for row in range(0, 4):
                for col in range(0, 4):
                    matrix[col][row] = text[pos:pos + 2]
                    pos += 2
            text = mix_columns(matrix)
            pos = 0
            for row in range(0, 4):
                for col in range(0, 4):
                    matrix[col][row] = text[pos:pos + 2]
                    pos += 2
            text = mix_columns(matrix)
            pos = 0
            for row in range(0, 4):
                for col in range(0, 4):
                    matrix[col][row] = text[pos:pos + 2]
                    pos += 2
            text = mix_columns(matrix)
            matrix = inv_shift_rows(text)
            text = matrixToString(matrix)
            text = inv_sub_bytes(text)
            text = add_round_key(text, key[count * 32:count * 32 + 32])
            count -= 1
        result = result + text
    while(result[-1] == "0"):
        result = result[:-1]
    byte_arr = bytearray.fromhex(result)
    return byte_arr.decode().rstrip()

def decrypt_key(key: str) -> str:
    result = add_round_key(key, "30190dcc14585301f5bfc5b666c8477530190dcc14585301f5bfc5b666c84775")
    dt = datetime.datetime.today()
    day = dt.day
    while(day > 0):
        temp = ""
        temp += inv_sub_bytes(result[:32])
        temp += inv_sub_bytes(result[32:])
        result = temp
        day -= 1
    return result

def textToHex(txt: str) -> str:
    result = ""
    while(len(txt) > 0):
        curr = txt[0:1]
        result += hex(ord(curr))[2:]
        txt = txt[1:]
    return result

def decrypt_file(filename: str, key: str):
    file = open(filename)
    text = file.read()
    file.close()
    dec = decrypt(text, key_expansion(key))
    file = open(filename, "w")
    file.write(dec)
    file.close()

decrypt_file("testfile.txt", "54657374696E6720707974686F6E20656E6372797074696F6E206D6574686F64")
