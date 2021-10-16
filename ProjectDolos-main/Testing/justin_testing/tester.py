import subprocess

value = [".test.txt", "hello"]
file = open("~/Documents/" + value[0][1:], "w")
file.write(value[1])