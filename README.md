# smartmeter

The smartmeter Java project contains a small Java web server (based on nanoHTTPD) that displays current electricity and gas usage,
as well as usage over the day, past month, and past 12 months. I run it on a Raspberry Pi, close to the smart meter itself, since it 
is connected with a P1 USB cable, which has an RJ11 phone plug  (6P4C or 6P6C) on one side and a USB2 plug on the other side. 
The protocol is RS232 (serial data). The Raspberry Pi reads the data (a so-called telegram) every 60 seconds from the smart meter,
and appends it to a daily file with telegrams. At the end of the day, the file contains 1440 (24 * 60) telegrams. The shell script 
that reads the data is started every minute as a cron job. Documentation on how the script works, and how the cron job should be
initialized can be found in the doc folder.

The Java code in this project sets up a web server on port 3000, and shows a dashboard with the electricity and gas usage for the 
current day, totals for the past 30 days, and totals for the past 12 months. It uses nnoHTTPD to set-up the web server (see
https://github.com/NanoHttpd/nanohttpd for the project), bootstrap 3.4.1 for the look-and-feel (see https://getbootstrap.com/docs/3.4/),
and chart.js for displaying the graphs (see https://www.chartjs.org/). 

**Notes** 
- The project is in development, and might not always work.
- The project is targeted for my setup (power, meter type, Pi, user accounts, etc), and might need changes for the specific settings
of another user.
