package dir2_WiiServer;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.awt.*;
import javax.swing.*;
import wiiremotej.*;
import wiiremotej.event.*;
import java.util.Observable;

import javax.sound.sampled.*;

import com.intel.bluetooth.BlueCoveConfigProperties;

import java.io.*;

//********************************************************
//NOTE
//********************************************************

// Also, please note that all code surrounded by these types of comments 
// are modification's to Michael Diamond's original 1/05/07 WRLImpl.java code
// which I found at:
// http://www.world-of-cha0s.hostrocket.com/WiiRemoteJ/WiiRemoteJ%20v1.6.zip.gz
// an original copy is included in the  source code.

//////////////////////////////////////////////////////////

/**
 * Implements WiiRemoteListener and acts as a general test class. Note that you can ignore the main method pretty much, as it mostly has to do with the graphs and GUIs.
 * At the very end though, there's an example of how to connect to a remote and how to prebuffer audio files.
 * 
 * @ModifyingAuthor by Jonathan Leung
 * @Version 6/1/10
 * 
 * @OriginalAuthor Michael Diamond
 * @OriginalVersion Version 1/05/07
 * 
 */

public class WiiServerRunner extends WiiRemoteAdapter
{

//********************************************************
// SETTINGS
//********************************************************
	
	// Please see the Readme to see how to configure these settings.
	
	private static boolean printMessage = false; //Print every time the Wiimote sends an update
	
	private static boolean sendToServerActivated  = true;	// Actually send the received data from the WiiMote over the server
	private static boolean sendNunchuckAcclerationActivated = true; // Actually send the Nunchuck data from the WiiMote over the server if it is connected
	
	private static boolean paintSqrtXXYYZZ = false;	// Currently this does nothing (See Readme)
	private static boolean paintXYZ = true; // Currently this does nothing (See Readme)
	
	private static boolean specialFunctionsOn = false; // Michael Diamond implemented many cool features with WiiRemoteJ
													   // that can be activated with combinational button presses.
													   // These features are by default turned off because they are unnecessary
													   // for WiiConductor.
	
//********************************************************
// Added Static Variables
//********************************************************
	public static IntermediaryObservervable observable; // Used to communicate to WiiServerThreads
	private static String messageToBeObserved = ""; // Message observed by WiiServerThreads
	private static int count = 0;
	
	private static int xxyyzz = 0;
	private static int lastXXYYZZ = 0;
//////////////////////////////////////////////////////////

    private static boolean accelerometerSource = true; //true = wii remote, false = nunchuk
    private static boolean lastSource = true;
    
    private static boolean mouseTestingOn;
    private static int status = 0;
    private static int accelerometerStatus = 0;
    private static int analogStickStatus = 0;
    private static JFrame mouseTestFrame;
    private static JPanel mouseTestPanel;
    
    private WiiRemote remote;
    private static JFrame graphFrame;
    private static JPanel graph;
    private static int[][] pixels;
    private static int t = 0;
    private static int x = 0;
    private static int y = 0;
    private static int z = 0;
    
    private static int lastX = 0;
    private static int lastY = 0;
    private static int lastZ = 0;
    
    private static PrebufferedSound prebuf;
    
    
    /***************************
     * Constructor
     ***************************/
    public WiiServerRunner(WiiRemote remote) throws IOException
    {
        this.remote = remote;
		
        // Start the WiiServer in a new thread
        (new Thread(new WiiServer())).start();	
        
        // Create an observable object that every WiiServerThread can observe
		observable = new IntermediaryObservervable();
		
		System.out.println("Started Server");
    } 

    
	/*******************************************************************************
	 * IntermediaryObservable
	 * 
	 * Please read the section on Observers and Observables README for 
	 * a more detailed explanation for how and why they are needed in this project.
	 * 
	 * Essentially, IntermediaryObservable passes messages from this WiiServerRunner.java file
	 * to all WiiServerThreads that are currently running which pass it to their 
	 * Receiver. It does this by using Java's Observable class.
	 * 
	 * These String messages are formed by concatenating WiiMote and Nunchuck 
	 * accelerometer data into one string message and is sent every time
	 * accelerationInputReceived() is called.
	 * 
	 * Optional Reading: Another attempted implementation was to not concatenate 
	 * all the data into one String, messageToBeObserved,  and send it over as 
	 * soon as each piece of data is received. However, because 
	 * accelerationInputReceived() and extensionInputReceived() were both called
	 * at very frequent rates,  many packets were lost with this  implementation.
	 ******************************************************************************/
    class IntermediaryObservervable extends Observable {
    	// Tells any observers that what they are observing has changed.
 		protected void setChanged() {
 			super.setChanged();
 		}
 	}
    
    
    /*****************************************************************************
     * newObserver()
     * 
     * Since each WiiServerThread is an observer of IntermediaryObservable,
     * newObserver() is called by WiiServer every time a thread is added
     * (meaning that a new Receiver has been added)
     *****************************************************************************/     
 	public static void newObserver(Observer observer) {
 		observable.addObserver(observer);
 	}
 

 	/*****************************************************************************
     * sendToBeObservedByWiiServerThread()
     * 
     * Sends the message that contains accelerometer and button data to all
     * WiiServerThread Observers.
     * 
     * Note that each message that is sent has a unique id, the count.
     * The count is an indicator of how many messages have been sent before it
     * and is used on the receiving end to determine if WiiServerRunner.java sent any packets
     * that were not received. This can be determined if on the receiving end,
     * the count jumps from say 1001 to 1003. You know you lost message 1002.
     * 
     * From testing locally, there is an extremely minimal packet loss 
     * (1 in thousands of packets) which doesn't really make a difference.
     * 
     *****************************************************************************/     
 	public void sendToBeObservedByWiiServerThread(String message) {
 		
 		//Tell WiiServerThread observers that what they are observing has changed
 		observable.setChanged(); 
 		
 		//Construct the message to be observed by the WiiServerThread
 		message += "count=" + Integer.toString(count) + ", "; //add the message count number
 		
 		//Notify the observers with the updated instance of what they are observing
 		observable.notifyObservers(message);
 		count++;
 		
        if (printMessage) { //SETTING
        	System.out.println(message);
        }
 	}
    
 	/*****************************************************************************
     * getTime()
     * 
     * Returns the current system time in milliseconds as a string.
     *****************************************************************************/     
    public String getTime() {
    	return Long.toString(System.currentTimeMillis());
    }
    
 	/*****************************************************************************
     * accelerationInputReceived()
     * 
     * This method is called every time the WiiRemoteJ has registered a new
     * accelerometer change from the WiiMote and so this method is called with 
     * a very high frequency.
     * 
     * This acceleration data is concatenated to the messageToBeObserved.
     * 
     * Also, because the acceleration data from the WiiMote is the most time
     * critical in the WiiConductor application, messageToBeObserved is 
     * sendToBeObservedByWiiServerThread as soon as the accelerationInputReceived.
     *****************************************************************************/ 
    public void accelerationInputReceived(WRAccelerationEvent evt)
    {
    	// If the accelerometerSource set to be plotted is from the WiiMote
        if (accelerometerSource)
        {
        	// Store the previous values for graphing purposes.
            lastX = x;
            lastY = y;
            lastZ = z;
            lastXXYYZZ = xxyyzz;
            
            // Get and store the acceleration in the x,y, and z directions.
            double raw_x = evt.getXAcceleration();
            double raw_y = evt.getYAcceleration();
            double raw_z = evt.getZAcceleration();
            
            // Concatenate the accelerometer values into the messageToBeObserved
            messageToBeObserved += "wx=" + Double.toString(raw_x) + ", " + //wx stands for WiiMote X Acceleration
		    					   "wy=" + Double.toString(raw_y) + ", " +
		    				       "wz=" + Double.toString(raw_z) + ", " +
		    				      //Time is also included to ensure that there is not that much delay between sending and receiving the data.
		    				       "time=" + Long.toString(System.currentTimeMillis()) + ", ";
		    
            //Actually send the data to be observed by the WiiServerThread observers.
            if(sendToServerActivated) {
            	sendToBeObservedByWiiServerThread(messageToBeObserved);
            	messageToBeObserved = "";
            }
            
            // THE BELOW CODE IS FOR PLOTTING THE ACCELERATION GRAPH
            
            // Scale the accelerometer values appropriately 
            x = (int)(raw_x/5*300)+300;
            y = (int)(raw_y/5*300)+300;
            z = (int)(raw_z/5*300)+300;
            
            // Calculate the magnitude of the acceleration
            xxyyzz = (int)(Math.sqrt(x*x+y*y+z*z)/5*300)+300;
            
            t++;
            
            graph.repaint();
            
            //System.out.println("R: " + evt.getRoll());
            //System.out.println("P: " + evt.getPitch());
        }
    }
    
    
 	/*****************************************************************************
     * extensionInputReceived()
     * 
     * Serves the same function as accelerationInputReceived() for the Nunchuck.
     *****************************************************************************/     
    public void extensionInputReceived(WRExtensionEvent evt)
    {
    	//Checks to see if the Nunchuck is connected
        if (evt instanceof WRNunchukExtensionEvent) {
            WRNunchukExtensionEvent NEvt = (WRNunchukExtensionEvent)evt;
            
            // If it is connected, then get the acceleration
            WRAccelerationEvent AEvt = NEvt.getAcceleration();
            
            // Extract the acceleration from the Nunchuck event.
	        double raw_nx = AEvt.getXAcceleration();
	        double raw_ny = AEvt.getYAcceleration();
	        double raw_nz = AEvt.getZAcceleration();
	        
	        //Actually send the Nunchuck accelerometer data to be observed by
	        //the server thread.
	        if(sendNunchuckAcclerationActivated) {
		         messageToBeObserved += "nx=" + Double.toString(raw_nx) + ", " +
									    "ny=" + Double.toString(raw_ny) + ", " +
									    "nz=" + Double.toString(raw_nz) + ", " +
									   //Time is also included to ensure that there is not that much delay between sending and receiving the data.
									    "time=" + Long.toString(System.currentTimeMillis()) + ", ";
	        }
            
	        // If the accelerometerSource set to be plotted is from the Nunchuk
            if (!accelerometerSource)
            {
            	// THE BELOW CODE IS FOR PLOTTING THE ACCELERATION GRAPH
            	
            	// Store the previous values for graphing purposes.
                lastX = x;
                lastY = y;
                lastZ = z;
                
                // Scale the accelerometer values appropriately 
                x = (int)(raw_nx/5*300)+300;
                y = (int)(raw_ny/5*300)+300;
                z = (int)(raw_nz/5*300)+300;
                
                t++; // increment time
                
                graph.repaint();
            }
            
            if (NEvt.wasReleased(WRNunchukExtensionEvent.C)) {
            	System.out.println("C");
            	
            	// If Button C is pressed, add it to the string of things to be
            	// told to the WiiServerThread
            	messageToBeObserved += "button=C, " +
            						   "time=" + getTime() + ", ";
            	}
            if (NEvt.wasPressed(WRNunchukExtensionEvent.Z)) {
            	System.out.println("Z");
            	
            	// If Button Z is pressed, add it to the string of things to be
            	// told to the WiiServerThread
            	messageToBeObserved +="button=Z, " +
            						  "time=" + getTime() + ", ";	
            	}
        }
        else if (evt instanceof WRClassicControllerExtensionEvent)
        {
            WRClassicControllerExtensionEvent CCEvt = (WRClassicControllerExtensionEvent)evt;
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.A))System.out.println("A!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.B))System.out.println("B!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.Y))System.out.println("Y!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.X))System.out.println("X!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.LEFT_Z))System.out.println("ZL!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.RIGHT_Z))System.out.println("ZR!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.LEFT_TRIGGER))System.out.println("TL!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.RIGHT_TRIGGER))System.out.println("TR!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_LEFT))System.out.println("DL!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_RIGHT))System.out.println("DR!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_UP))System.out.println("DU!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.DPAD_DOWN))System.out.println("DD!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.PLUS))System.out.println("Plus!");
            if (CCEvt.wasPressed(WRClassicControllerExtensionEvent.MINUS))System.out.println("Minus!");
            if (CCEvt.isPressed(WRClassicControllerExtensionEvent.HOME))
            {
                System.out.println("L shoulder: " + CCEvt.getLeftTrigger());
                System.out.println("R shoulder: " + CCEvt.getRightTrigger());
            }
        }
        else if (evt instanceof WRGuitarExtensionEvent)
        {
            WRGuitarExtensionEvent GEvt = (WRGuitarExtensionEvent)evt;
            if (GEvt.wasPressed(WRGuitarExtensionEvent.MINUS))System.out.println("Minus!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.PLUS))System.out.println("Plus!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.STRUM_UP))System.out.println("Strum up!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.YELLOW))System.out.println("Yellow!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.GREEN))System.out.println("Green!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.BLUE))System.out.println("Blue!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.RED))System.out.println("Red!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.ORANGE))System.out.println("Orange!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.STRUM_DOWN))System.out.println("Strum down!");
            if (GEvt.wasPressed(WRGuitarExtensionEvent.GREEN+WRGuitarExtensionEvent.RED))
            {
                System.out.println("Whammy bar: " + GEvt.getWhammyBar());
                AnalogStickData AS = GEvt.getAnalogStickData();
                System.out.println("Analog- X: " + AS.getX() + " Y: " + AS.getY());
            }
        }
    }
    
 	/*****************************************************************************
     * buttonInputReceived()
     * 
     * Checks to see if any buttons are pressed and if so adds them to the 
     * messageToBeObserved along with the time the button was pressed.
     * 
     * Also added the button combo functions:
     * 
     * 		Home + 1
     * 			Toggle sendToServer
     * 
     * 		Home + 2
     * 			Toggle sendNunchuckAcceleration
     * 
     * 		Home + A + B
     * 			Disconnect WiiMote and end program
     * 
     * Note that Michael Diamand has added many functions that are pretty cool
     * but not necessary for WiiConductor. If you enable specialFunctionsOn,
     * you weill be able to use them. They are implemented below.
     *****************************************************************************/ 
    public void buttonInputReceived(WRButtonEvent evt)
    {  
    	if (evt.wasPressed(WRButtonEvent.TWO)){System.out.println("2");			messageToBeObserved +="button=2, " 		+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.ONE)){System.out.println("1");			messageToBeObserved +="button=1, " 		+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.B)){System.out.println("B");			messageToBeObserved +="button=B, " 		+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.A)){System.out.println("A");			messageToBeObserved +="button=A, " 		+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.MINUS)){System.out.println("MINUS");	messageToBeObserved +="button=MINUS, " 	+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.HOME)){System.out.println("HOME");		messageToBeObserved +="button=HOME, " 	+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.LEFT)){System.out.println("LEFT");		messageToBeObserved +="button=LEFT, " 	+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.RIGHT)){System.out.println("RIGHT");	messageToBeObserved +="button=RIGHT, " 	+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.DOWN)){System.out.println("DOWN");		messageToBeObserved +="button=DOWN, " 	+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.UP)){System.out.println("UP");			messageToBeObserved +="button=UP, " 	+ "time=" + getTime() + ", ";	}
    	if (evt.wasPressed(WRButtonEvent.PLUS)){System.out.println("PLUS");		messageToBeObserved +="button=PLUS, " 	+ "time=" + getTime() + ", ";	}
        
    	//*****************************************************
    	// Special Functions
    	//*****************************************************
    	
    	if (evt.isPressed(WRButtonEvent.HOME)) {
    		if(evt.wasPressed(WRButtonEvent.ONE)) {
    			if(sendToServerActivated == true) {
    				sendToServerActivated = false;
    				System.out.println("sendToServer = false");
    			}
    			else {
    				sendToServerActivated = true;
    				System.out.println("sendToServer = true");
    			}
    		}
    		if(evt.wasPressed(WRButtonEvent.TWO)) {
    			if(sendNunchuckAcclerationActivated == true) {
    				sendNunchuckAcclerationActivated = false;
    				System.out.println("sendNunchuckAccleration = false");
    			}
    			else {
    				sendNunchuckAcclerationActivated = true;
    				System.out.println("sendNunchuckAccleration = true");
    			}
    		}
    		
            if (evt.wasPressed(WRButtonEvent.A + WRButtonEvent.B))
            {
                remote.disconnect();
                System.exit(0);
            }
    	}
        
    	//*****************************************************
    	// Michael Diamond's Special Functions
    	//*****************************************************
    	
    	if (specialFunctionsOn) {
	        try {
	            //if (evt.isPressed(WRButtonEvent.MINUS) && evt.wasPressed(WRButtonEvent.PLUS))
	                //System.out.println("Avg Tardiness: " + remote.totalTardiness/remote.reportsProcessed);
	            
	            if (evt.isPressed(WRButtonEvent.HOME))
	            {
	                boolean lightChanged = false;
	                if (evt.wasPressed(WRButtonEvent.A + WRButtonEvent.B))
	                {
	                    remote.disconnect();
	                    System.exit(0);
	                }
	                else if (evt.wasPressed(WRButtonEvent.PLUS) && !mouseTestingOn)
	                {
	                    mouseTestingOn = true;
	                    remote.getButtonMaps().add(new ButtonMouseMap(WRButtonEvent.B, java.awt.event.InputEvent.BUTTON1_MASK));
	                    remote.getButtonMaps().add(new ButtonMouseMap(WRButtonEvent.A, java.awt.event.InputEvent.BUTTON3_MASK));
	                    remote.getButtonMaps().add(new ButtonMouseWheelMap(WRButtonEvent.UP, -5, 100));
	                    remote.getButtonMaps().add(new ButtonMouseWheelMap(WRButtonEvent.DOWN, 5, 100));
	                    mouseTestFrame.setVisible(true);
	                    mouseCycle();
	                }
	                else if (evt.wasPressed(WRButtonEvent.MINUS) && mouseTestingOn)
	                {
	                    mouseTestingOn = false;
	                    remote.getButtonMaps().remove(new ButtonMouseMap(WRButtonEvent.B, java.awt.event.InputEvent.BUTTON1_MASK));
	                    remote.getButtonMaps().remove(new ButtonMouseMap(WRButtonEvent.A, java.awt.event.InputEvent.BUTTON3_MASK));
	                    remote.getButtonMaps().remove(new ButtonMouseWheelMap(WRButtonEvent.UP, -5, 100));
	                    remote.getButtonMaps().remove(new ButtonMouseWheelMap(WRButtonEvent.DOWN, 5, 100));
	                    mouseTestFrame.setVisible(false);
	                    remote.setMouse(null);
	                }
	                else if (evt.wasPressed(WRButtonEvent.ONE))
	                {
	                    accelerometerSource = !accelerometerSource;
	                    if (accelerometerSource)graphFrame.setTitle("Accelerometer graph: Wii Remote");
	                    else graphFrame.setTitle("Accelerometer graph: Nunchuk");
	                }
	                else if (evt.wasPressed(WRButtonEvent.TWO)) //code for Wii Remote memory dump/comparison
	                {
	                    Thread thread = new Thread(new Runnable()
	                    {
	                        public void run()
	                        {
	                            try
	                            {
	                                File dataF = new File("data.dat");
	                                byte dataO[] = null;
	                                if (dataF.exists())
	                                {
	                                    dataO = new byte[0x0040];
	                                    DataInputStream dataS = new DataInputStream(new FileInputStream(dataF));
	                                    dataS.readFully(dataO);
	                                    dataS.close();
	                                }
	                                
	                                File data2F = new File("data2.dat");
	                                byte data2O[] = null;
	                                if (data2F.exists())
	                                {
	                                    data2O = new byte[0xFFFF];
	                                    DataInputStream data2S = new DataInputStream(new FileInputStream(data2F));
	                                    data2S.readFully(data2O);
	                                    data2S.close();
	                                }
	                                
	                                System.out.println("Searching address...");
	                                //byte[] address = new byte[]{0x00, 0x17, (byte)0xAB};
	                                //byte[] address = new byte[]{0x0F, 0x04, 0x00, 0x01, 0x01, 0x04};
	                                
	                                /**/
	                                byte[] data = remote.readData(new byte[]{0x00, 0x00, 0x00, 0x00}, 0x0040);
	                                System.out.println("Read complete (data)");
	                                if (!dataF.exists())
	                                {
	                                    DataOutputStream dataOS = new DataOutputStream(new FileOutputStream(dataF));
	                                    dataOS.write(data, 0, data.length);
	                                    dataOS.close();
	                                }
	                                else
	                                {
	                                    System.out.println("Comparing arrs (data)");
	                                    for (int c = 0; c < data.length; c++)
	                                    {
	                                        //System.out.println("0x" + Integer.toHexString(data[c]) + " : 0x" + Integer.toHexString(dataO[c]));
	                                        if (data[c] != dataO[c])System.out.println("Flash: 0x" + Integer.toHexString(c));
	                                    }
	                                    System.out.println("Comparing complete");
	                                }
	                                /**/
	                                
	                                
	                                /*
	                                byte[] data2 = remote.readData(new byte[]{0x04, (byte)0xA2, 0x00, 0x00}, 65535);
	                                System.out.println("Read complete (data2)");
	                                if (!data2F.exists())
	                                {
	                                    DataOutputStream data2OS = new DataOutputStream(new FileOutputStream(data2F));
	                                    data2OS.write(data2, 0, data2.length);
	                                    data2OS.close();
	                                }
	                                else
	                                {
	                                    System.out.println("Comparing arrs (data2)");
	                                    for (int c = 0; c < data2.length; c++)
	                                    {
	                                        System.out.println("0x" + Integer.toHexString(data2[c]) + " : 0x" + Integer.toHexString(data2O[c]));
	                                        if (data2[c] != data2O[c])System.out.println("Register: 0x" + Integer.toHexString(c + 0x04A20000));
	                                    }
	                                    System.out.println("Comparing complete");
	                                }
	                                /**/
	                                
	                                System.out.println("Search complete.");
	                            }
	                            catch (Exception e){e.printStackTrace();}
	                        }
	                    });
	                    thread.start();
	                }
	                else if (mouseTestingOn)
	                {
	                    boolean change = false;
	                    if (evt.wasPressed(WRButtonEvent.HOME+WRButtonEvent.RIGHT))
	                    {
	                        status = (status+1)%4;
	                        change = true;
	                    }
	                    else if (evt.wasPressed(WRButtonEvent.HOME+WRButtonEvent.LEFT))
	                    {
	                        status = (status+3)%4;
	                        change = true;
	                    }
	                    
	                    if (status == 0)
	                    {
	                        if (evt.wasPressed(WRButtonEvent.DOWN))
	                        {
	                            accelerometerStatus = (accelerometerStatus+1)%4;
	                            change = true;
	                        }
	                        else if (evt.wasPressed(WRButtonEvent.UP))
	                        {
	                            accelerometerStatus = (accelerometerStatus+3)%4;
	                            change = true;
	                        }
	                    }
	                    else if (status == 3)
	                    {
	                        if (evt.wasPressed(WRButtonEvent.DOWN))
	                        {
	                            analogStickStatus = (analogStickStatus+1)%6;
	                            change = true;
	                        }
	                        else if (evt.wasPressed(WRButtonEvent.UP))
	                        {
	                            analogStickStatus = (analogStickStatus+5)%6;
	                            change = true;
	                        }
	                    }
	                    
	                    if (change)mouseCycle();
	                }
	            }
	            else if (evt.wasPressed(WRButtonEvent.TWO))
	            {
	                remote.requestStatus();
	                if (remote.isPlayingSound())remote.stopSound();
	            }
	            else if (evt.wasPressed(WRButtonEvent.ONE))
	            {
	                if (prebuf != null)remote.playPrebufferedSound(prebuf, WiiRemote.SF_PCM8S);
	            }
	            else if (evt.wasPressed(WRButtonEvent.PLUS))
	            {
	                if (remote.isSpeakerEnabled())
	                {
	                    double volume = (remote.getSpeakerVolume()*20+1)/20;
	                    if (volume <= 1)remote.setSpeakerVolume(volume);
	                    System.out.println("Volume: " + remote.getSpeakerVolume());
	                }
	            }
	            else if (evt.wasPressed(WRButtonEvent.MINUS))
	            {
	                if (remote.isSpeakerEnabled())
	                {
	                    double volume = (remote.getSpeakerVolume()*20-1)/20;
	                    if (volume >= 0)remote.setSpeakerVolume(volume);
	                    System.out.println("Volume: " + remote.getSpeakerVolume());
	                }
	            }
	        }
	        catch(Exception e){e.printStackTrace();}
	    }
    }
    
 	/*****************************************************************************
     * 
     *  All below methods were not modified from Micahel Diamond's WRLImpl.java
     *  and I have not included any comments.
     *  
     *  Note also that the Main function is all the way at the bottom.
     *  
     *****************************************************************************/  
    
    public void extensionConnected(WiiRemoteExtension extension)
    {
        System.out.println("Extension connected!");
        try
        {
            remote.setExtensionEnabled(true);
        }catch(Exception e){e.printStackTrace();}
    }
    
    public void extensionPartiallyInserted()
    {
        System.out.println("Extension partially inserted. Push it in more next time!");
    }
    
    public void extensionUnknown()
    {
        System.out.println("Extension unknown. Did you try to plug in a toaster or something?");
    }
    
    public void extensionDisconnected(WiiRemoteExtension extension)
    {
        System.out.println("Extension disconnected. Why'd you unplug it, eh?");
    }
    
    private void mouseCycle()
    {
        try
        {
            if (status == 0)
            {
                if (accelerometerStatus == 0)remote.setMouse(MotionAccelerometerMouse.getDefault());
                else if (accelerometerStatus == 1)remote.setMouse(TiltAccelerometerMouse.getDefault());
                else if (accelerometerStatus == 2)remote.setMouse(new MotionAccelerometerMouse(80, 60, AccelerometerMouse.NUNCHUK_EXTENSION, 0.06, 0.08));
                else if (accelerometerStatus == 3)remote.setMouse(new TiltAccelerometerMouse(10, 10, AccelerometerMouse.NUNCHUK_EXTENSION, 0.1, 0.1));
            }
            else if (status == 1)remote.setMouse(IRMouse.getDefault());
            else if (status == 2)remote.setMouse(IRAccelerometerMouse.getDefault());
            else if (status == 3)
            {
                if (analogStickStatus == 0)remote.setMouse(AbsoluteAnalogStickMouse.getDefault());
                else if (analogStickStatus == 1)remote.setMouse(RelativeAnalogStickMouse.getDefault());
                else if (analogStickStatus == 2)remote.setMouse(new AbsoluteAnalogStickMouse(1, 1, AnalogStickMouse.CLASSIC_CONTROLLER_LEFT));
                else if (analogStickStatus == 3)remote.setMouse(new RelativeAnalogStickMouse(10, 10, 0.05, 0.05, AnalogStickMouse.CLASSIC_CONTROLLER_LEFT));
                else if (analogStickStatus == 4)remote.setMouse(new AbsoluteAnalogStickMouse(1, 1, AnalogStickMouse.CLASSIC_CONTROLLER_RIGHT));
                else if (analogStickStatus == 5)remote.setMouse(new RelativeAnalogStickMouse(10, 10, 0.05, 0.05, AnalogStickMouse.CLASSIC_CONTROLLER_RIGHT));
            }
            mouseTestPanel.repaint();
        }
        catch(Exception e){e.printStackTrace();}
            
    }
    
    public void disconnected()
    {
        System.out.println("Remote disconnected... Please Wii again.");
        System.exit(0);
    }
    
    public void statusReported(WRStatusEvent evt)
    {
        System.out.println("Battery level: " + (double)evt.getBatteryLevel()/2+ "%");
        System.out.println("Continuous: " + evt.isContinuousEnabled());
        System.out.println("Remote continuous: " + remote.isContinuousEnabled());
    }
    
    public void IRInputReceived(WRIREvent evt)
    {
        /*for (IRLight light : evt.getIRLights())
        {
            if (light != null)
            {
                System.out.println("X: "+light.getX());
                System.out.println("Y: "+light.getY());
            }
        }*/
        
    }
    
    
    public static void main(String args[]) throws IllegalStateException, IOException
    {
 

    	System.setProperty(BlueCoveConfigProperties.PROPERTY_JSR_82_PSM_MINIMUM_OFF, "true");
        //basic console logging options...
        WiiRemoteJ.setConsoleLoggingAll();
        //WiiRemoteJ.setConsoleLoggingOff();

        //********************************************************
        // JPanel Plotting 
        //********************************************************       
        try
        {
            mouseTestFrame = new JFrame();
            mouseTestFrame.setTitle("Mouse test");
            final int LS = 50; //line spacing
            mouseTestFrame.setSize(4*LS, 7*LS);
            mouseTestFrame.setResizable(false);
            
            mouseTestPanel = new JPanel()
            {
                public void paintComponent(Graphics graphics)
                {
                    graphics.clearRect(0, 0, 4*LS, 7*LS);
                    graphics.setColor(Color.YELLOW);
                    if (status == 0)graphics.fillRect(status*LS, (accelerometerStatus+1)*LS, LS, LS);
                    else if (status == 3)graphics.fillRect(status*LS, (analogStickStatus+1)*LS, LS, LS);
                    else graphics.fillRect(status*LS, LS, LS, LS);
                    
                    graphics.setColor(Color.BLACK);
                    graphics.drawString("WM", (int)(LS*0.5), (int)(LS*1.5));
                    graphics.drawString("WT", (int)(LS*0.5), (int)(LS*2.5));
                    graphics.drawString("NM", (int)(LS*0.5), (int)(LS*3.5));
                    graphics.drawString("NT", (int)(LS*0.5), (int)(LS*4.5));
                    graphics.drawString("**", (int)(LS*1.5), (int)(LS*1.5));
                    graphics.drawString("**", (int)(LS*2.5), (int)(LS*1.5));
                    graphics.drawString("NA", (int)(LS*3.5), (int)(LS*1.5));
                    graphics.drawString("NR", (int)(LS*3.5), (int)(LS*2.5));
                    graphics.drawString("LA", (int)(LS*3.5), (int)(LS*3.5));
                    graphics.drawString("LR", (int)(LS*3.5), (int)(LS*4.5));
                    graphics.drawString("RA", (int)(LS*3.5), (int)(LS*5.5));
                    graphics.drawString("RR", (int)(LS*3.5), (int)(LS*6.5));
                    
                    paintChildren(graphics);
                }
            };
            
            mouseTestPanel.setLayout(new FlowLayout());
            mouseTestPanel.add(new JLabel("A          I       IA         AS"));
            mouseTestFrame.add(mouseTestPanel);
            
            graphFrame = new JFrame();
            graphFrame.setTitle("Accelerometer graph: Wii Remote");
            graphFrame.setSize(800, 600);
            graphFrame.setResizable(false);
            
            t = 801;
            pixels = new int[800][600];
            graph = new JPanel()
            {
                public void paintComponent(Graphics graphics)
                {
                    if (t >= 800 || accelerometerSource != lastSource)
                    {
                        t = 0;
                        lastSource = accelerometerSource;
                        graphics.clearRect(0, 0, 800, 600);
                        graphics.fillRect(0, 0, 800, 600);
                        graphics.setColor(Color.WHITE);
                        graphics.drawLine(0, 300, 800, 300);
                        
                        graphics.drawLine(0, 100, 800, 100);
                        graphics.drawLine(0, 200, 800, 200);
                        graphics.drawLine(0, 300, 800, 300);
                        graphics.drawLine(0, 400, 800, 400);
                        graphics.drawLine(0, 500, 800, 500);
                    }
                    
                    if(paintXYZ) {
                    	graphics.setColor(Color.RED);
                    	graphics.drawLine(t, lastX, t, x);
                        graphics.setColor(Color.GREEN);
                        graphics.drawLine(t, lastY, t, y);
                        graphics.setColor(Color.BLUE);
                        graphics.drawLine(t, lastZ, t, z);
                    }
                    
                    if(paintSqrtXXYYZZ) {
                    	graphics.setColor(Color.PINK);
                    	graphics.drawLine(t, lastXXYYZZ, t, xxyyzz);
                    }
                    
                    
                }
            };
            graphFrame.add(graph);
            graphFrame.setVisible(true);

            //********************************************************
            // Find and connect to a Wii Remote
            //********************************************************              
            WiiRemote remote = null;
            
            while (remote == null) {
                try {
                    remote = WiiRemoteJ.findRemote();
                }
                catch(Exception e) {
                    remote = null;
                    e.printStackTrace();
                    System.out.println("Failed to connect remote. Trying again.");
                }
            }
            
            //WiiRemote remote = WiiRemoteJ.connectToRemote("001F32FD09B0");
            remote.addWiiRemoteListener(new WiiServerRunner(remote));
            remote.setAccelerometerEnabled(true);
            //remote.setSpeakerEnabled(true);
            remote.setIRSensorEnabled(true, WRIREvent.BASIC);
            remote.setLEDIlluminated(0, true);
        
            remote.getButtonMaps().add(new ButtonMap(WRButtonEvent.HOME, ButtonMap.NUNCHUK, WRNunchukExtensionEvent.C, new int[]{java.awt.event.KeyEvent.VK_CONTROL}, 
                java.awt.event.InputEvent.BUTTON1_MASK, 0, -1));
                        
            /* 
            // Prebuffer a preformatted audio file
            
            System.out.println("Buffering audio file...");
            long time = System.currentTimeMillis();
            AudioInputStream audio = AudioSystem.getAudioInputStream(new java.io.File("Audio.au"));
            prebuf = WiiRemote.bufferSound(audio);
            time = System.currentTimeMillis()-time;
            time /= 1000;
            System.out.println("Prebuf done: " + time + " seconds.");
            */
            final WiiRemote remoteF = remote;
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){remoteF.disconnect();}}));
        }
        catch(Exception e){e.printStackTrace();}
    }
}