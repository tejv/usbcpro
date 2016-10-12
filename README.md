# USBPD/USBC Protocol Analyzer GUI

![selection_002](https://cloud.githubusercontent.com/assets/22388206/19316287/786e1b62-90be-11e6-9669-a4b8badd5d12.png)

# Build details

->Linux

Make sure user has read/write permisssion to the usb device. If not then Clicking Start/Stop menu item
You will get " Start Command fail"
To fix this
Create a file 

/etc/udev/rules.d/99-userusbdevices.rules 

with below line and replug the device. 

SUBSYSTEM=="usb",ATTR{idVendor}=="04b4",ATTR{idProduct}=="0072",MODE="0660",GROUP="plugdev"

where 04b4 is the vendor id, 0072 is product id of usb device.

->Windows

Make sure device driver used is libusb. If not uninstall current driver and install libusb.

->Build

Clone the project and use gradle to build the project or can also import project in eclipse.
Or use prebuilt binaries in my maven-repo.

# Quick Start Guide
--------------------------------------------------------------------------------
Quick Start Guide USBCPro PD Analyzer
Author: Tejender Sheoran
Email: tejendersheoran@gmail.com

Copyright (C) <2016>  <Tejender Sheoran>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

-------------------------------------------------------------------------------
Overview
--------------------------------------------------------------------------------
1. This analyzer uses CY4500 TypeC PD Anlyzer kit as low level hardware. Although it can work 
   with CY8CKIT-059.
2. USBCPro PD Analyzer GUI is used to present data to user.
3. USB interface is used for data logging from hw to PC.

--------------------------------------------------------------------------------
Setup
-------------------------------------------------------------------------------
1. Make sure hw is programmed with USBCPro.hex. If first time programming use Miniprog 
   header to program the board otherwise fw can be updated from GUI after step 6.
2. Install latest JRE(Java Runtime environment) from http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html
3. Install libusb win32 driver. By default CY4500 kit will bind to cypress driver. You need to manually reinstall libusb win32 driver.
4. Note if device is plugged to different port. You need to install libusb win32 driver for that port because by default device bind to cypress driver.
5. Double click on usbcpro.jar.
6. Log window will show "USBCPro HW Attached".
7. Press Ctrl+ R to start capturing.
8. If attach/detach detection is also needed. Then one modification need to be done in CY4500 REV 2 HW. Short Q1.4 to J4.1 and short Q1.1 to J4.2.

--------------------------------------------------------------------------------
HW LED INDICATORS
--------------------------------------------------------------------------------
1. White - Analyzer is just idle with no start/stop. Default when connected to PC white led is glowing.
2. Green - Analyzer is capturing the data.
3. Yellow - Analyzer has stopped capturing data
4. Blue - Analyzer internal buffer has overflowed means data is not taken out from anlayzer fast enough. 
   Once buffer overflow happens led keep glowing as blue even if buffer is emptied. This can be cleared by reset (Ctrl + R)
5. Purple - Analyzer in bootloader mode.

--------------------------------------------------------------------------------
Caveats
--------------------------------------------------------------------------------
A. Attach/Detach 

	1. CC Attach/Detach detection works in following manner. Since CC attach voltage range 
	overlaps with Ra connected range, Also there are 3 Rp values each having its own voltage ranges.
	In addition to above analyzer kit may be plugged in /out/connected in various ways.
	Keeping all this in view, its very difficult to know which is active cc line for all cases.
	2. Way this analyzer works is it looks for 250mV-2.4V on either cc line. Debounce it for some time.
	Then connect a pull down of 5K on this line. If it is Rd then voltage will drop to almost half. If Ra voltage will drop about a quarter.
	All this is done within 5ms. So during this time analyzer is intruding the bus.
	3. Once active cc line is detected. Detach is normal debounce of 50ms (Detach Debounce param (Def 8000)in GUI) for voltage > 2.4V.
	4. This scheme works well when Ra is present in cable.
	5. For non marked cable when connect happens and if Detach Debounce param in GUI is say 1000 (correspond to 8ms). Analyzer puts 5k on active cc line.
       This line drops below 800mV(for 3A rp) i.e Ra range. Other line will be 5V.
	   This is a detach condition for source while it is debouncing for attach. In this case 2 DRPs won't be able to connect.
       This can be avoided by using Detach Debounce param of 8000 (50ms) so that analyzer waits atleast 50ms to do another intrusion if actually detach happens.	   
	   Once 2 ports are connected then detach debounce can be reduced back to 1000 if fast response is needed.
	So ATTACH/DETACH feature need to be used with care. By default it is disabled.
	It can be enabled/Disabled by
	1. Go to View->Attach/Detach Enable
	2. Then Click on Actions->Reset (Press Ctrl + R)
	
B. This version also supports logging data for a long time. Old data will be automatically cleared.
   You will be able to see last 4000 messages. By default this feature is enabled. To disable/enable long logging. Go to View->Long Logging.	
--------------------------------------------------------------------------------
HW Triggers Description
--------------------------------------------------------------------------------
To enable Triggers. Go to Triggers tab in Advanced Options window.

1. Start Trigger(SOM): This pin triggers on start of a message. It will go high 
   then immediately go low at start of message. If check box for this trigger is selected
   and trigger is set using trigger set button, then trigger will be generated at start of
   message with specified "serial no" in adjacent textbox. If this trigger is not
   set then this trigger will be generated at start of every message.
   If you want to use this trigger. Then enter the Sno and Click Set Trigger button. 
   If everything is fine you will get a message "Trigger Set Successful". 
2. End Trigger(EOM): This pin triggers on end of a message. It will go high 
   then immediately go low at end of message. If check box for this trigger is selected
   and trigger is set using trigger set button, then trigger will be generated at end of
   message with specified "serial no" in adjacent textbox. If this trigger is not
   set then this trigger will be generated at end of every message.
   If you want to use this trigger. Then enter the Sno and Click Set Trigger button. 
   If everything is fine you will get a message "Trigger Set Successful"  
3. Msg  Trigger(MTR): This pin triggers on end of a particular message.
   This is enabled/disabled/Set from GUI. 
   You can enable/disable SOP type , message type, message id, message class, count to trigger on a unique 
   message. This feature is handy when you want to see waveforms on oscilloscope at 
   a particular event.   
   Click Set Trigger button. If everything is fine you will get a message "Trigger Set Successful".    

--------------------------------------------------------------------------------  
GUI Controls
--------------------------------------------------------------------------------
Go to Actions/File/View Menu or follow shortcut keys to perform various actions.
1. Start/Stop Capture (Ctrl + X)- Enable/Disable the snooping logic. Reset timer and counters.
2. Reset (Ctrl + R)- Clear the current data as well as reset the hw. (i.e. Stop + Start)
3. Download FW (Ctrl + D) -  Updates the firmware. Use the .cyacd file to upgrade the fw.
4. Get HW/FW version (Ctrl + V)- Displays current version of HW/FW.
5. Clear data (Ctrl + C)- Clear the data view.
6. Save  (Ctrl + S)- Saves the accumulated data to .uc file. 
7. Open  (Ctrl + O)- Open a .uc file and load it on display.
   Drag and drop a .uc file onto dataview will also open the file. Also uc file can be linked to open with 
   this application using a .bat file.

--------------------------------------------------------------------------------   
Working
--------------------------------------------------------------------------------
1. Click on Action->Start/Stop Capture menu (Ctrl + X) to enable/disable analyzing. Status bar will display "Start/Stop Success"
2. Use Set trigger button to set triggers.  
   
--------------------------------------------------------------------------------   
Other Features
--------------------------------------------------------------------------------
1. Duration field show the total duration a a packet.
2. Delta field show time difference between end of last packet and start of current packet.
3. Start Delta : show the start time difference of last 2 selected messages.
4. You can hide/show an column by right clicking on data view table header.
5. You can rearrange columns as per your convenience by dragging column headers.
6. Drag and drop a .uc file onto data view will also open the file.
7. If "OK" field is blue that means packet has EOP error.
8. If "OK" field is yellow means CRC error.
9. If "OK" field is red means both crc and eop error in packet.
10. This version also support attach/detach detection, vbus up/down detection and Rp level detection. To disable
    attach/detach detection Go to View->Attach/Detach enable menu.
11. This version also supports logging data for a long time. Old data will be automatically cleared.
    You will be able to see last 4000 messages. To enable/disable long logging. Go to View->Long Logging.
	
  
