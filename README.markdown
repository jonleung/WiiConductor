# [WiiConductor](http://WiiConductor.com) #
#### Version: 06-06-11 ####
Control the tempo and the volume of a song with a Wii Remote ([WiiMote](http://en.wikipedia.org/wiki/Wii_Remote))
as if you were a conductor of an orchestra.
Built with Java & GarageBand for Mac OS X.
Wave the "baton" back and forth more quickly and Garage Band will speed up;
wave it more forcefully and Garage Band will get louder.

If you find anything confusing or have made any improvements to the source, please let me know so that it can help someone else!
I want to try to make this as easy as possible for any beginner to pick up and run with.
You can find my email on [jonathanjl.com](http://jonathanjl.com)

## Step 1: Adding the Jar Files ##

Include the jar files in the 'dir1_Jars' folder when you are compiling the code:

* **'bluecove-2.1.1-SNAPSHOT.jar'** is used to connect with the WiiMote over Bluetooth and 

* **'WiiRemoteJ.jar'** is a Java API for getting data from the WiiRemote developed by Michael Diamond.

If you are using an IDE like Eclipse, when you import the code into a new project, include both of these jar files. Here are [instructions](http://www.wikihow.com/Add-JARs-to-Project-Build-Paths-in-Eclipse-(Java)) on how to do that in Eclipse.

## Quick High Level Overview ##
	
There are two main parts to WiiConductor: The **Server** side and **Receiver** side.

**WiiServerRunner.java** acts as the server and **ReceiverRunner.java** as the receiver. These are the _only_ java files you need to run.

When you run **WiiServerRunner.java** (found in 'dir2\_WiiServer') it gets the live data from the WiiMote including x,y,z accelerometer values and button press events and sends it over a socket like a server.

When you run **ReceiverRunner.java** (found in 'dir3\_WiiReceiverRunner), it reads and processes the WiiMote data from the socket.

The code is broken down into these two separate processes because it allows you to disconnect the Receiver from the WiiServer, make any changes in the Receiver analysis code, and then reconnect the Receiver to the WiiServer again. This is a quick and painless process.

The alternative way of structuring the code was to put the analysis code in the same process that connected to the WiiMote but this would require you to disconnect and reconnect the WiiMote each time you made any code changes to the analysis code. A relatively long and painful process.

Also, with the current server, receiver setup, multiple **ReceiverRunner.java** processes can connect to the Wii Server over the local network! 

## Setting Up and Running WiiConductor ##

1. ## Bluetooth

	Turn on your Mac's Bluetooth.
	
	System Preferences -> Bluetooth
	
	Then make sure the 'on' checkbox is checked.

2. ## Wii Server 
	
	The code for the WiiServer is in '**dir2\_WiiServer**'.
	Before you can run **WiiServerRunner.java**, you must configure one of its dependencies,
	**WiiServer.java**

	1. ## Configuring "WiiServer.java" ##

		By default **WiiServer.java** is listening for and broadcasting on port 4444.
		If for some reason this port is in use on your computer, change it 
		to an unused port by editing the **"portToServeOn"** variable.
	
				int portToServeOn = 4444;
	
	2. ## Running "WiiServerRunner.java":

		Now its time to run "WiiServerRunner.java".
		This will create a plot that graphs the x,y,z acceleration (x=Red, y=Green, and z=Blue).

		Also in the console you, you will see a message that says:
		
			BlueCove version 2.1.1-SNAPSHOT on mac

		**Wait for at least 6 seconds** after the the plot appears,
		and then press "1" & "2" buttons on the WiiMote simultaneously
		to make the WiiMote findable over bluetooth. 
		You will know when the WiiMote is broadcasting when the blue LEDs on the WiiMote
		are flashing.

		When it successfully connects, you should see a stream of text in the console 
		that looks like this:
	
			BlueCove version 2.1.1-SNAPSHOT on mac
			Jun 4, 2011 3:51:18 PM wiiremotej.WiiDeviceDiscoverer deviceDiscovered
			FINE: Found  (0014515795B3)
			Jun 4, 2011 3:51:18 PM wiiremotej.WiiDeviceDiscoverer deviceDiscovered
			FINE:  is NOT a known device.
			...
			Server Created!
			Started Server
			...
			Jun 4, 2011 3:51:28 PM wiiremotej.WiiRemote$1 run
			FINEST: Data write finished.
			Jun 4, 2011 3:51:29 PM wiiremotej.WiiRemote$1 run
			FINEST: Data write finished.

		**However:**
		half the time, you will see error messages looking like this (below message is shortened):
	
	
			java.io.IOException: Error sending data!
				at wiiremotej.WiiRemote.sendCommand(WiiRemote.java:613)
				at wiiremotej.WiiRemote.readData(WiiRemote.java:1401)
				at wiiremotej.WiiRemote.readData(WiiRemote.java:1353)
				at wiiremotej.WiiRemote.calibrateAccelerometer(WiiRemote.java:1426)
				at wiiremotej.WiiRemote.construct(WiiRemote.java:576)
				at wiiremotej.WiiRemote.<init>(WiiRemote.java:223)
				at wiiremotej.WiiDeviceDiscoverer.getDevice(WiiDeviceDiscoverer.java:121)
				at wiiremotej.WiiRemoteJ.findDeviceInternal(WiiRemoteJ.java:342)
				at wiiremotej.WiiRemoteJ.findDevice(WiiRemoteJ.java:316)
				at wiiremotej.WiiRemoteJ.findRemote(WiiRemoteJ.java:357)
				at Wii.main(WiiServerRunner.java:231)
			Caused by: java.io.IOException: Connection closed
				at com.intel.bluetooth.BluetoothStackOSX.l2Send(Native Method)
				at com.intel.bluetooth.BluetoothL2CAPConnection.send(BluetoothL2CAPConnection.java:132)
				at wiiremotej.WiiRemote.sendCommand(WiiRemote.java:609)
				... 10 more
			...
			...
				
		
		If you see this second message, terminate the process.
		But before trying to run it again though, make sure that you WiiMote stops broadcasting 
		by holding down the power button. If this doesn't stop the WiiMote from broadcasting
		(ie the LEDs are still flashing),
		just *wait* until it stops broadcasting before running **WiiServerRunner.java** again.
	
		For some reason, WiiServerRunner.java only picks up your WiiMote if the Wiimote starts broadcasting
		about 5 seconds AFTER you start **WiiServerRunner.java**
	
		If you see the below error message printing over and over on the screen (below message is shortened): 
	
			Failed to connect remote. Trying again.
			...
			java.lang.IllegalStateException: Bluetooth failed to initialize. There is probably a problem with your local Bluetooth stack or API.
				at wiiremotej.WiiRemoteJ.<clinit>(WiiRemoteJ.java:74)
				at Wii.main(Wii.java:136)
			Caused by: javax.bluetooth.BluetoothStateException: Bluetooth Device is not ready
				at com.intel.bluetooth.BluetoothStackOSX.getLocalDeviceBluetoothAddress(Native Method)
				at javax.bluetooth.LocalDevice.<init>(LocalDevice.java:71)
				at javax.bluetooth.LocalDevice.getLocalDeviceInstance(LocalDevice.java:78)
				at javax.bluetooth.LocalDevice.getLocalDevice(LocalDevice.java:95)
				at wiiremotej.WiiRemoteJ.<clinit>(WiiRemoteJ.java:67)
				... 1 more
				...
				...
	
		This means that you forgot to turn on your Bluetooth ^_^
	
		You may need several attempts to connect the WiiMote and it _will_ fail every other time 
		according to Michael Diamond because of some issue with Apple's Bluetooth software.

		If in WiiServerRunner.java you change  "printMessage" to "true" you will see a stream of data of what the server is sending.
		However, by default, it is false so that you can see other messages on the console.
		To test if your server is active in the non-printing mode, just press a button on the WiiMote after you think it has connected
		and you will see a message with the name of the button on the terminal pop up if it is connected.


3. ## Wii Conductor WiiReceiver ##

	The code for the WiiReceiver with WiiCondcutor is in '**dir3\_WiiReceiver\_Configure4_WiiConductor**'

	1. ## Configuring **WiiReceiverRunner.java** ##
		
		1. ## If **WiiServerRunner.java** is running on the same computer
			Change the **"computerName"** variable to the name of your computer.

			If you are on Snow Leopard, go to 
	
			System Preferences -> Sharing

			And at the top under the Text Field, you will see something that reads:

				"Computers on your local network can access your computer at: YOUR\_COMPUTER_NAME.local"
	
			Set the "computerName" variable to exactly to "YOUR_COMPUTER_NAME.local" and
			set the "portToReadFrom" to whatever you set it to in **WiiServer.java**
	
				String computerName = "Jonathan.local";
				int portToReadFrom = 4444;
		
		2. ## If **WiiServerRunner.java** is running on another computer on the network##
	
			Instead of your own computer's computer name, use the ip address of the computer
			that is running **WiiServerRunner.java** for the **computerName** variable.
	
		
				String computerName = "167.232.65.2";
				int portToReadFrom = 4444;
		

	2. ## Configuring "SoundPlayer.java"##
	
		Included in the same folder is **"click.wav"** 
		which is the sound of a single metronome tick.
		
		Change the **"clickDotWaveFileLocation"** to wherever your "click.wav" is located.
	
			String clickDotWavFileLocation = "/Users/Jonathan/Home/WiiConduct/click.wav";
		
	
	3. ## Running ReceiverRunner.java ##

		After you have launched the WiiServer by running **WiiServerRunner.java**,
		you can connect **ReceiverRunner.java** by simply running it
	
		If it doesn't seem to work, then check to make sure that port 4444 is not currently in use.
		A future development would be for the program to check if the port is already in use and then
		change the port numbers accordingly.
		
		If anyone runs into any problems, please email me.
		You can find my email on [jonathanjl.com](http://jonathanjl.com)

4. ##Garage Band##

	Open Garage Band and load up your favorite song in MIDI 
	(drag your midi file into the main area that says "Drag Apple Loops Here.")
	Included in the "**dir4\_media**" folder is the garage band file "ImperialMarch.gb"
	Although you can use any garage band file, in order for the rhythms to line up and stay consistent throughout the piece,
	you can only use garage band projects with audio that contains only midi data.

	
5. ##Start Conducting!##

	Now for the fun part!
	
	Press 'A' on the WiiMote to start playing Garage Band.
	
	Start [waving your Wii baton like a conductor!](http://www.youtube.com/watch?v=0REJ-lCGiKU).
	
	Wave it faster to increase the tempo and with greater force to increase volume.
	
	Lastly, note that there are some protective measures so that you do not accidentally
	change tempo.
	
	In order to change tempo, you must change your more than 25bpms greater 
	or less than than the previously set value. So if your current BPM is 80, 
	you must conduct faster than 105bpms or slower than 55bpm in order for it 
	to actually change.
					
	Also note that the BPM the program looks at is a weighted moving avereage BPM,
	which is created from the last 5 beats. So even if you changed BPMs on the last
	beat, it will be smoothed down by the prior 4 beats.

10. ## Special Button Combos

	A. '**Home**' + '**1**'
	toggle "sendToServer" (See below 1.2)

	B. '**Home**' + '**2**'
	toggle "sendNunchuckAcceleration" in ""**WiiServerRunner.java**""

	C. '**Home**' + '**A**' + '**B**'
	disconnect the WiiMote and terminate the program
					
## Done! ----------------------------------------------------------------------------

Well that's all you need to know to use WiiCondcut! 

However, if you want to configure settings in WiiCondcuct,
understand how the source works
or adapt this code to your own project, read on!

## Understanding the code basics  ##

Heres a quick rundown of all the java files ('-->' means 'runs'):

	WiiServerRunner.java --> WiiServer.java --> WiiServerThread.java
	
	WiiServerThread.java 	>>>sendsMessagesOverTheSocketTo>>> 	 ReceiverRunner.java

	ReceiverRunner.java --> ReceiverAnalysis.java --> SoundPlayer.java
                                    	
                                            	  --> GarageBandScriptEngine.java
	
	//Note: There is a new WiiServerThread instance for each Receiver.

When you run **WiiServerRunner.java**, it gets the live data from WiiMote including 
x,y,z accelerometer values and button press events using Michael Diamond's WiiRemoteJ API.

**WiiServerRunner.java** will then create an instance of **WiiServer.java** which waits for an instance of
**ReceiverRunner.java** to run. 

When **ReceiverRunner.java** is run, it will connect to **WiiServer.java**.

**WiiServer.java** will create a new **WiiServerThread.java** which will be directly responsible for
sending x,y,z accelerometer data and button press events live from **WiiServerRunner.java** to 
**ReceiverRunner.java** with very little lag. (Note that Nunchuck data will only be sent if one is connected.)

**ReceiverRunner.java** then parses the incoming data sent from **WiiServerThread.java**.

When **Reciever.java** has parsed the message, it will send it neatly over to its
**ReceiverAnalysis.java** instance. 

**ReceiverAnalysis** is where WiiCondcutor takes the accelerometer and button data
and has code to actually analyze it.

The code is heavily commented so if you want to learn more, dig in!

You may ask, why separate the data gathering code and the analysis code 
as separate server and receiver instances; why not just combine them and then only
have to execute one java file?

When we initially wrote WiiConductor, that's what we did. However, if you do this,
every time you changed your code, you would have to find and reconnect to the WiiMote,
a finicky process that sometimes takes as long as a minute for even a tiny code change.

With the current setup , you only need to connect the WiiMote with WiiServerRunner.java _once_
and when you want to make any code changes, you would make them in the **ReceiverAnalysis.java**
class and very simply reconnect to the server socket broadcasting the WiiMote data.
No need to disconnect and reconnect to the WiiMote itself!

Also because the server is multi-threaded, this means that you will be able to connect
multiple receivers to a single WiiMote server instance! This allows computer over the local
network to run the reciever code and also data from the WiiMote as well 
with an average lag of "250ms" over a fast connection. This will enable you to build 
other cool projects like say an application that uses the WiiMote as a mouse 
over multiple computers!

## Settings you may want to change							
		
Note that all configurable settings in a given java file are enclosed in a settings box that looks like:

	//********************************
	// SETTINGS
	//*********************************
	
	All your settings are in here.
	
	//////////////////////////////////

1. ## WiiServerRunner.java ##

	1.  **printMessage = true**	
		
		Display every message in the console that is being sent by the server. 	(default = false)
		
	
	2. **sendToServer = true**		
	
		Actually send the message to the server. (default = true)
	
	3. **sendNunchuckAcceleration = true**
		
		If the Nunchuck is attached, send its acceleration and button data over the server
		(default = true)
	
	4. **paintSqrtXXYYZZ = true**
		
		Although I attempt to plot sqrt(x*x+y*y+z*z) to the canvas, it does not seem to work so this setting
		currently does nothing.
	
	5. **paintXYZ = true**
		
		Paint the x, y, and z accelerations to the canvas.
		(default = true)

	6. **specialFunctionsOn = true**
		
		Michael Diamond included many cool features in WiiRemoteJ that WiiConductor does not use.
		For example, you can control mouse movement on the laptop screen with a WiiMote and Sensor bar.
		You can read the code to see some of the cool functions he has implemented.

2. ## ReceiverAnalysis.java

	1. **BPM\_ACCEPTABLE\_DELTA**
		
		The number of BPMs your moving average tempo needs to change before it actually makes an effect in garage band.
		The default value is 30 which may seem like a lot but even for a trained musician, this is just enough.
 
	2. **BPM\_MOVING\_AVERAGE\_WIDTH**
		
		Each time the RecieverAnalysis registers a click, it moves the BPM of that click into a weighted moving average.
		The default value of BPM\_MOVING\_AVERAGE\_WIDTH = 5 means that you are averaging the past 5 conducting thrusts or swipes and 
		averaging those BPMs with a linear weighted average.
	
	3. **BPM\_INITIALIZED\_VALUE**
		
		The speed at which Garage Band starts at. This is achieved by setting every term in the moving average to this value.
		Change this to the BPM you want to start conducting the piece at.

	4. **VOLUME\_ACCEPTABLE\_DELTA**
		
		same corresponding function for volume as 1
	
	5. **VOLUME\_MOVING\_AVERAGE\_WIDTH**
		
		same corresponding function for volume as 2

	6. **VOLUME\_INITIALIZED\_VALUE**
		
		same corresponding function for volume as 3
	
	7. **TYPE\_OF\_AVERAGE**
			
			
			static String TYPE_OF_AVERAGE = "weighted";
		
		**OR**	
			
			
			static String TYPE_OF_AVERAGE = "simple";
			
		For both the BPM and the Volume, there is a moving average that smooths 
		the the BPM that is sent to Garage Band so even if your conducting is
		not completely precise, and you manage to screw up a beat or to, it will
		not have dire consequences.
	
		Now, to have the most recent values take more weight than the previous
		values, so that you can change tempo or volume faster use a weighted
		average which weights the most recently captured beat or 
		volume more.

	8. **BPM\_DEBUG**
		
		Print out variables that are related to the BPM.

	9. **VOLUME\_DEBUG**
	
		Print out variables that are related to the Volume.
					
		
## Adapting WiiConductor to your own project ## 
	
You can use directly use the WiiServer in '**dir2_WiiServer**' for your project.

On the Receiver end, there is a much simpler sample application in 
'**dir5\_WiiReceiver\_SampleApplication**' that you can modify.

The idea of the application is to shake the remote as hard as possible
for 5 seconds. The harder you shake it, the higher your score will be!

To run this application, have **WiiServerRunner.java** and just like
WiiCondcutor, run **ReceiverRunner.java** and follow the instructions
in the console.

## Visualizing the WiiMote

There is a program in '**dir6_Visualization**' that is better at visualizing
the accelerations given by the WiiMote. This will be useful for writing your
own code to determine acceleration levels.

## Original WiiRemoteJ Source Code

Checkout the original source code it **'dir7_OriginalWiiRemoteJ'**.
There is some good documentation in here on WiiRemoteJ.
		
## Thanks!

Thanks for reading through this long tutorial!
I urge you to send any questions, comments, or suggestions.

My email can be found on [jonathanjl.com](http://jonathanjl.com)

## Wanna Help?
* Improve the intuitiveness of when the "click" sounds.
* Make transitioning between BPMs smoother
* Ignore "DoubleClicks" (ie if the program detects a 200bpm when before it is 80bpm and 80bpm aftewards)
* If a port is taken, automatically move to a new one
* Make a video to explain how the code works.
* Use the Nunchuck to cue tracks in Garage Band
* Change the PrintWriter to an Object Stream
* Your cool creative idea!

## Contributors

1. Initial WiiRemoteJ API developed by Michael Diamond.
2. The pre-alpha version of the conducting code without the server receiver
separation was developed during the TechCrunch Disrupt Hackathon from 05/21/11 - 05-22/11 by:
	* Andrew Braunstein
	* Benjamin Shyong
	* Chase Lambert
	* Jonathan Leung
3. The Server Receiver integration along with code cleanup and additional developments
to the conducting code were made by Jonathan Leung and submitted on 06/06/11.