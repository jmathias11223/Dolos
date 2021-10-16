# Python implementation of AES-256 encryption
#
# Use the encrypt() method and pass in the plaintext(for now it must be hex)
# and the key(also must be in hex, must be 64 hex characters long)
# Will return the encrypted String
#
# Author: Justin Mathias

# Takes a sequence of 128 bits as text and key and XORs them together
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
def sub_bytes(text: str) -> str:
    result = ""
    for i in range(0, 4):
        result = result + sbox(text[8 * i: 8 * i + 8])
    return result

# Performs the shift_rows operation on the given text
def shift_rows(text: str):
    # Convert to 2D array
    matrix = [[0 for i in range(4)] for j in range(4)]
    pos = 0
    for i in range(0, 4):
        for j in range(0, 4):
            matrix[j][i] = text[pos:pos + 2]
            pos += 2

    # Perform shift_rows operation
    for row in range(1, 4):
        for i in range(0, row):
            temp = matrix[row][0]
            matrix[row][0] = matrix[row][1]
            matrix[row][1] = matrix[row][2]
            matrix[row][2] = matrix[row][3]
            matrix[row][3] = temp

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

# Performs key expansion for AES-256
# Key must be 256 bits long (64 hex characters)
def key_expansion(keyUsed: str) -> str:
    # Set all local variables to be used in loops
    key = keyUsed
    forLoopIters = 8
    whileLoopIters = 480
    temp1 = key[len(key) - 8:]
    temp2 = key[:8]
    t1 = int(temp1, base=16)
    t2 = int(temp2, base=16)
    iteration = 0
    polynomial = 1

    while(len(key) < whileLoopIters):
        for i in range(0, forLoopIters):
            temp1 = key[len(key) - 8:]
            temp2 = key[8 * (i + forLoopIters * iteration):8 * (i + forLoopIters * iteration) + 8]
            if i == 0:
                temp1 = temp1[2:] + temp1[:2]       # Rotation
                temp1 = sbox(temp1)                 # Run it through sbox

                # XOR first two bits with polynomial 
                temp3 = temp1[:2]
                temp4 = temp1[2:]
                temp3 = hex(int(temp3, base=16) ^ polynomial)
                temp1 = temp3[2:] + temp4
                polynomial = (polynomial << 1) ^ (0x11b & -(polynomial >> 7))
            if i == 4:                              # For AES-256 only
                temp1 = sbox(temp1)
            
            # Perform final XOR operation
            t1 = int(temp1, base=16)
            t2 = int(temp2, base=16)
            result = hex(t1 ^ t2)[2:]

            # Add leading zeroes to match length 8
            while(len(result) < 8):
                result = "0" + result
            key = key + result
        iteration += 1
    return key.upper()

# Sbox operation for substitution of bytes
def sbox(bt: str) -> str:
    conversion = {"00": "63", "01": "7C", "02": "77", "03": "7B", "04": "F2", "05": "6B", 
    "06": "6F", "07": "C5", "08": "30", "09": "01", "0A": "67",
    "0B": "2B", "0C": "FE", "0D": "D7", "0E": "AB", "0F": "76",
    "10": "CA", "11": "82", "12": "C9", "13": "7D", "14": "FA",
    "15": "59", "16": "47", "17": "F0", "18": "AD", "19": "D4",
    "1A": "A2", "1B": "AF", "1C": "9C", "1D": "A4", "1E": "72",
    "1F": "C0", "20": "B7", "21": "FD", "22": "93", "23": "26",
    "24": "36", "25": "3F", "26": "F7", "27": "CC", "28": "34",
    "29": "A5", "2A": "E5", "2B": "F1", "2C": "71", "2D": "D8",
    "2E": "31", "2F": "15", "30": "04", "31": "C7", "32": "23",
    "33": "C3", "34": "18", "35": "96", "36": "05", "37": "9A",
    "38": "07", "39": "12", "3A": "80", "3B": "E2", "3C": "EB",
    "3D": "27", "3E": "B2", "3F": "75", "40": "09", "41": "83",
    "42": "2C", "43": "1A", "44": "1B", "45": "6E", "46": "5A",
    "47": "A0", "48": "52", "49": "3B", "4A": "D6", "4B": "B3",
    "4C": "29", "4D": "E3", "4E": "2F", "4F": "84", "50": "53",
    "51": "D1", "52": "00", "53": "ED", "54": "20", "55": "FC",
    "56": "B1", "57": "5B", "58": "6A", "59": "CB", "5A": "BE",
    "5B": "39", "5C": "4A", "5D": "4C", "5E": "58", "5F": "CF",
    "60": "D0", "61": "EF", "62": "AA", "63": "FB", "64": "43",
    "65": "4D", "66": "33", "67": "85", "68": "45", "69": "F9",
    "6A": "02", "6B": "7F", "6C": "50", "6D": "3C", "6E": "9F",
    "6F": "A8", "70": "51", "71": "A3", "72": "40", "73": "8F",
    "74": "92", "75": "9D", "76": "38", "77": "F5", "78": "BC",
    "79": "B6", "7A": "DA", "7B": "21", "7C": "10", "7D": "FF",
    "7E": "F3", "7F": "D2", "80": "CD", "81": "0C", "82": "13",
    "83": "EC", "84": "5F", "85": "97", "86": "44", "87": "17",
    "88": "C4", "89": "A7", "8A": "7E", "8B": "3D", "8C": "64",
    "8D": "5D", "8E": "19", "8F": "73", "90": "60", "91": "81",
    "92": "4F", "93": "DC", "94": "22", "95": "2A", "96": "90",
    "97": "88", "98": "46", "99": "EE", "9A": "B8", "9B": "14",
    "9C": "DE", "9D": "5E", "9E": "0B", "9F": "DB", "A0": "E0",
    "A1": "32", "A2": "3A", "A3": "0A", "A4": "49", "A5": "06",
    "A6": "24", "A7": "5C", "A8": "C2", "A9": "D3", "AA": "AC",
    "AB": "62", "AC": "91", "AD": "95", "AE": "E4", "AF": "79",
    "B0": "E7", "B1": "C8", "B2": "37", "B3": "6D", "B4": "8D",
    "B5": "D5", "B6": "4E", "B7": "A9", "B8": "6C", "B9": "56",
    "BA": "F4", "BB": "EA", "BC": "65", "BD": "7A", "BE": "AE",
    "BF": "08", "C0": "BA", "C1": "78", "C2": "25", "C3": "2E",
    "C4": "1C", "C5": "A6", "C6": "B4", "C7": "C6", "C8": "E8",
    "C9": "DD", "CA": "74", "CB": "1F", "CC": "4B", "CD": "BD",
    "CE": "8B", "CF": "8A", "D0": "70", "D1": "3E", "D2": "B5",
    "D3": "66", "D4": "48", "D5": "03", "D6": "F6", "D7": "0E",
    "D8": "61", "D9": "35", "DA": "57", "DB": "B9", "DC": "86",
    "DD": "C1", "DE": "1D", "DF": "9E", "E0": "E1", "E1": "F8",
    "E2": "98", "E3": "11", "E4": "69", "E5": "D9", "E6": "8E",
    "E7": "94", "E8": "9B", "E9": "1E", "EA": "87", "EB": "E9",
    "EC": "CE", "ED": "55", "EE": "28", "EF": "DF", "F0": "8C",
    "F1": "A1", "F2": "89", "F3": "0D", "F4": "BF", "F5": "E6",
    "F6": "42", "F7": "68", "F8": "41", "F9": "99", "FA": "2D",
    "FB": "0F", "FC": "B0", "FD": "54", "FE": "BB", "FF": "16"}
    result = ""
    for i in range(0, 4):
        result = result + conversion[bt[i * 2: i * 2 + 2].upper()]
    return result

def textToHex(txt: str) -> str:
    result = ""
    while(len(txt) > 0):
        curr = txt[0:1]
        temp = hex(ord(curr))[2:]
        while(len(temp) < 2):
            temp = "0" + temp
        result += temp
        txt = txt[1:]
    return result

# Performs AES-256 encryption in ECB mode
def encrypt(plaintext: str, key: str) -> str:
    plaintext = textToHex(plaintext)
    rounds = 14
    result = ""
    key = key_expansion(key)
    while(len(plaintext) > 0):
        text = ""
        if(len(plaintext) >= 32):
            text = plaintext[0:32]
            plaintext = plaintext[32:]
        else:
            text = plaintext
            plaintext = ""
        count = 0
        while(count < rounds - 1):
            text = add_round_key(text, key[count * 32:count * 32 + 32])
            # print("Add round key: " + text)
            text = sub_bytes(text)
            # print("Sub bytes: " + text)
            temp = shift_rows(text)
            text = mix_columns(temp)
            # print("Mix columns: " + text)
            count += 1
        text = add_round_key(text, key[count * 32:count * 32 + 32])
        # print("Add round key: " + text)
        text = sub_bytes(text)
        # print("Sub bytes: " + text)
        temp = shift_rows(text)
        temp_str = ""
        for i in range(0, 4):
            for j in range(0, 4):
                temp_str = temp_str + temp[j][i]
        # print("Shift rows: " + temp_str)
        count += 1
        text = add_round_key(temp_str, key[count * 32:count * 32 + 32])
        # print("Add round key: " + text)
        result = result + text
        # print()
    return result.upper()

def encrypt_file(filename: str, key: str):
    file = open(filename)
    arr = file.readlines()
    print(arr)
    file.close()
    text = ""
    for i in range(0, len(arr)):
        text += arr[i]
    encrypted = encrypt(text, key_expansion(key))
    file = open(filename, "w")
    file.write(encrypted)
    file.close()

# Test code
# print(encrypt("Secret message", "54657374696E6720707974686F6E20656E6372797074696F6E206D6574686F64"))
encrypt_file("testfile.txt", "54657374696E6720707974686F6E20656E6372797074696F6E206D6574686F64")