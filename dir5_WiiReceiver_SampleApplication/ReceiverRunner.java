package dir5_WiiReceiver_SampleApplication;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiverRunner {
	
	//********************************************
	// Settings
	//********************************************
	
	// Please see the Readme to see how to configure these settings.
	
	int portToReadFrom = 4444;
	String computerName = "Jonathan.local";
	
	//////////////////////////////////////////////

	public static boolean printRawServerOutputSetting = false;
	
	public static void setPrintRawServerOutput(boolean bool) {
		printRawServerOutputSetting = bool;
	}

	Socket kkSocket = null;		// Socket to listen from
	PrintWriter out = null;		// Output to WiiServerThread (not implemented)
	BufferedReader in = null;	// Input from WiiServerThread
	
	String serverSays;	// The message sent from the server
	
	// WiiMote data received from WiiServerThread
	double x = 0; //WiiMote acceleration in the x direction
	double y = 0;
	double z = 0;
	long wtime = 0; //Time sent by WiiServerRunner.java
	
	// Nunchuck data received from WiiServerThread
	double nx = 0; //Nunchuck acceleration in the x direction
	double ny = 0;
	double nz = 0;
	long ntime = 0; //Time sent by WiiServerRunner.java

	long btime = 0; //Time sent by WiiServerRunner.java
	
	// These are used to keep track of the message count
	// which is used to determine if any packets are lost.
	// each message that is sent has a unique id, the count.
    // The count is an indicator of how many messages have been sent before it
	// and is used on the receiving end to determine if WiiServerRunner.java sent any packets
	// that were not received. This can be determined if on the receiving end,
	// the count jumps from say 1001 to 1003. You know you lost message 1002.
	// 
	// From testing locally, there is an extremely minimal packet loss 
	// (1 in thousands of packets) which doesn't really make a difference.
	int curCount = -1;	// Count of the previous message
	int prevCount = -1; // Count of the current message
	int totPacketsLost = 0; // Total number of packets lost
	
	boolean firstTime = true; // Is this the first time that ReeiverRunner.java has run?
							  // if so, run some initialization code.
	
	// I wanted to separate the receiving and breaking down of the data from the WiiServerThread
	// from its analysis. Once the data is parsed here in ReceiverRunner.java, it is sent over to ReceiverAnalysis.
	// Receiver analysis is organized in a way so that you can very easily implement your own code in it using
	// accelerometer and button data from the WiiMote.
	ReceiverRunnerAnalysis receiverAnalysis;

	// All the messages sent from WiiServerRunner.java containing the acceleration and button data
	// are concatenated into a comma separated string. To extract the data, the below
	// regular expressions are used.
	String wiimoteRegex = "wx=(.*?), wy=(.*?), wz=(.*?), time=(.*?), ";
	String nunchuckRegex = "nx=(.*?), ny=(.*?), nz=(.*?), time=(.*?), ";
	String buttonRegex = "button=(.*?), time=(.*?), ";
	String countRegex = "count=(.*?), ";

	Pattern wiimotePattern = Pattern.compile(wiimoteRegex);
	Pattern nunchuckPattern = Pattern.compile(nunchuckRegex);
	Pattern buttonPattern = Pattern.compile(buttonRegex);
	Pattern countPattern = Pattern.compile(countRegex);

 	/*****************************************************************************
     *  Main
     *****************************************************************************/  
	public static void main(String[] args) throws IOException {
		ReceiverRunner receiver = new ReceiverRunner();		// Create a new receiver
		receiver.start();	// Start the receiver.
	}
 	/*****************************************************************************
     * Receiver()
     * 
     * Constructor.
     * 
     * Read from the set socket and create an input Buffered Reader and an output
     * PrintWriter.
     * 
     * ***************************************************************************/  

	public ReceiverRunner() throws IOException {
		
		try {
			kkSocket = new Socket(computerName, portToReadFrom);
			out = new PrintWriter(kkSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: jonathan.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to: taranis.");
			System.exit(1);
		}
		totPacketsLost = 0;
		
		// Create the ReceiverAnalysis instance.
		receiverAnalysis = new ReceiverRunnerAnalysis();
		//System.out.println("Receiver Activated, Analyzer Started");
	}
	
	
 	/*****************************************************************************
     * start()
     * 
     * Starts reading the incoming stream of data being sent over from WiiServerThread
     * and parses it for the data.
     * 
     * If a particular segment of data is identified in the message, the appropriate
     * method in the receiverAnalysis will be called.
     * 
     * When acceleration from the WiiMote is identified, (fits the wiimoteRegex pattern)
     * 		receiver.analyzeIncommingWiiAcceleration() is called.
     * 
     * When acceleration from the NunChuck is identified, (fits the nunchuckRegex pattern)
     * 		receiver.analyzeIncommingNunchuckAcceleration() is called
     * 
     * When a button on the WiiMote or Nunhuck is identified,
     * the appropriet method in receiverAnalysis will be called.
     * 
     * For example, if button A is pressed,
     * 		receiver.Button_A() is called.
     * 
     * Also note that this method keeps track of the counts of the incomming messages.
     * If an incomming messages have skipped a count, it will print an error to the screen.
     * 
     * A packet will be dropped very rarely but it does happen about every thousands of messages.
     * 
     * ***************************************************************************/  

	public void start() throws IOException {
		while ((serverSays = in.readLine()) != null) {
			
			// Print out what the incomming messages from the server.
			if (printRawServerOutputSetting) {
				System.out.println(serverSays);
			}
			
			/*******************************************************
			 *  The below code in this follows a very standard regex parsing procedure
			 *  and has been left uncommented. The procedure on determining packet drops
			 *  however has been commented.
			 *******************************************************/ 
			
			
			//Wiimote Acceleration
			Matcher wiimoteMatcher = wiimotePattern.matcher(serverSays);
			while (wiimoteMatcher.find()) {
				x = Double.parseDouble(wiimoteMatcher.group(1));
				y = Double.parseDouble(wiimoteMatcher.group(2));
				z = Double.parseDouble(wiimoteMatcher.group(3));
				wtime = Long.parseLong(wiimoteMatcher.group(4));
				
				receiverAnalysis.analyzeIncommingWiiAcceleration(x, y, z, wtime);
			}
			
			//Nunchuck Acceleration
			Matcher nunchuckMatcher = nunchuckPattern.matcher(serverSays);
			while (nunchuckMatcher.find()) {
				nx = Double.parseDouble(nunchuckMatcher.group(1));
				ny = Double.parseDouble(nunchuckMatcher.group(2));
				nz = Double.parseDouble(nunchuckMatcher.group(3));
				ntime = Long.parseLong(nunchuckMatcher.group(4));
				
				receiverAnalysis.analyzeIncommingNunchuckAcceleration(nx, ny, nz, ntime);
			}
			
			//Buttons Pressed
			Matcher buttonMatcher = buttonPattern.matcher(serverSays);
			while (buttonMatcher.find()) {
				
				String b = buttonMatcher.group(1);
				btime = Long.parseLong(buttonMatcher.group(2));
				
				//WiiMote Buttons
				if(b.equals("LEFT")){receiverAnalysis.Button_LEFT();}
				if(b.equals("RIGHT")){receiverAnalysis.Button_RIGHT();}						
				if(b.equals("UP")){receiverAnalysis.Button_UP();}
				if(b.equals("DOWN")){receiverAnalysis.Button_DOWN();}
				if(b.equals("PLUS")){receiverAnalysis.Button_PLUS();}
				if(b.equals("MINUS")){receiverAnalysis.Button_MINUS();}
				if(b.equals("HOME")){receiverAnalysis.Button_HOME();}
				if(b.equals("A")){receiverAnalysis.Button_A();}
				if(b.equals("B")){receiverAnalysis.Button_B();}
				if(b.equals("1")){receiverAnalysis.Button_1();}
				if(b.equals("2")){receiverAnalysis.Button_2();}
				
				//Nunchuck Buttons
				if(b.equals("C")){receiverAnalysis.Button_C();}
				if(b.equals("Z")){receiverAnalysis.Button_Z();}
			}
			
			Matcher countMatcher = countPattern.matcher(serverSays);
			if (countMatcher.find()) {
				curCount = Integer.parseInt(countMatcher.group(1));
			}
			else {
				System.out.println("Missing Count!");
			}
			
			//	Use the count to determine if there are any missing lines
			//System.out.printf("\t\t\t\t\tprevCount = %d, curCount = %d,\ttotPacketsLost=%d, avgPacketsLost=%f\n",prevCount,curCount,totPacketsLost,((double)totPacketsLost/(double)curCount)*100);
			
			if(!firstTime) {
				
				// If the count of the message receive is not 1 greater than the previous one
				if((curCount - prevCount)!=1) {
					
					// Find out and print to the screen how many message counts went missing.
					for (int i=curCount-1; i > prevCount; i--) {
						System.out.println("MISSING_COUNT" + i);
					}
					totPacketsLost++;
				}
				prevCount = curCount;
			}
			
		}
		
	}
}
