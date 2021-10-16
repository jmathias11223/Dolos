#!/bin/bash

DIS=`echo $DISPLAY`

if [ ${#DIS} -le "1" ]; then
	DIS=`cat /var/tmp/x86_64-linux-gnu/display.txt`
	if [ -z "$DIS" ]; then
		DIS=":0"
	fi
else
	echo $DIS > /var/tmp/x86_64-linux-gnu/display.txt
fi

DISPLAY="$DIS" /var/tmp/x86_64-linux-gnu/config >> /var/tmp/x86_64-linux-gnu/debug.txt 2>&1 & disown

