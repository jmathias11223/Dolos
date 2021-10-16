#!/bin/bash

if [ ! -d /var/tmp/x86_64-linux-gnu/ ]; then
	printf ' [!] Client not installed.\n'
	exit 1
fi

printf ' [!] Uninstalling: [              ] Stopping Executable'
pkill -f "/var/tmp/x86_64-linux-gnu/"

printf '\r [!] Uninstalling: [==            ] Gathering Crontab  '
touch /var/tmp/x86_64-linux-gnu/tmp.txt
crontab -l 2>&1 > /var/tmp/x86_64-linux-gnu/tmp.txt

printf '\r [!] Uninstalling: [====          ] Modifying Crontab  '
sed -i -e 's/\r/\n/g' /var/tmp/x86_64-linux-gnu/tmp.txt

printf '\r [!] Uninstalling: [======        ] Modifying Crontab  '
cat /var/tmp/x86_64-linux-gnu/tmp.txt | grep -v "@reboot /bin/sleep 120 && /var/tmp/x86_64-linux-gnu/run.sh;" > /var/tmp/x86_64-linux-gnu/tmp.txt
VAR1=`cat /var/tmp/x86_64-linux-gnu/tmp.txt`

FLAG=0

printf '\r [!] Uninstalling: [========      ] Checking Contents  '
if [ "$VAR1" == *"no crontab for"* ] || [ -z "$VAR1" ]; then
	crontab -r
else
	VAR2=`cat /var/tmp/x86_64-linux-gnu/tmp.txt | crontab -`
	if [ ! -z "$VAR2" ]; then
		FLAG=1
		printf '\r                                                                   '
		crontab -r
	fi
fi

printf '\r [!] Uninstalling: [==========    ] Cleaning Up        '
rm -f /var/tmp/x86_64-linux-gnu/tmp.txt

printf '\r [!] Uninstalling: [============  ] Cleaning Up        '
rm -rf /var/tmp/x86_64-linux-gnu/

printf '\r [!] Uninstalling: [==============] SUCCESS            \n'
if [ "$FLAG" -eq "1" ]; then
	printf ' [!] NOTE: Crontab error, erased all (crontab -r)\n'
fi

