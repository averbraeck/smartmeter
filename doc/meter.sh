#!/bin/bash
cd /home/alexandv
stty -F /dev/ttyUSB0 speed 115200 cs8 -cstopb -parenb >/dev/null 2>&1
head -100 /dev/ttyUSB0 > temp
date=$(date '+%Y-%m-%d')
file=meter/meter_$date.txt
if [ ! -f $file ]; then
    touch $file
fi
let start=0
while IFS= read -r line; do
    if [[ "$line" == "/"* ]]; then
        if [ "$start" == "1" ]; then
            break 1
        else
            let start=1
            echo $date >> $file
            date +%H:%M:%S >> $file
            echo $line >> $file
            continue
        fi
    else
        if [[ "$line" == "" ]]; then
            continue
        fi
        if [ "$start" == "1" ]; then
            echo $line >> $file
        fi
    fi
done < temp
