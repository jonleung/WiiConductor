package dir2_WiiServer;

import java.net.*;
import java.io.*;

public class WiiServer implements Runnable{
	
	//******************************
	// SETTINGS
	//******************************
	
	// Please see the Readme to see how to configure these settings.
	
	int portToServeOn = 4444;
	
	////////////////////////////////
	
	ServerSocket serverSocket = null;
	WiiServerThread thread;
	
	
 	/*****************************************************************************
     * WiiServer()
     * 
     * Constructor.
     * 
     * Initializes the socket connection and tries to serve on the indicated
     * portToServeOn.
     *****************************************************************************/  
    public WiiServer() throws IOException {
    	
        try {
            serverSocket = new ServerSocket(portToServeOn);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + portToServeOn);
            System.exit(-1);
        }
     
        System.out.println("Server Created!");
    }
    
 	/*****************************************************************************
     * run()
     * 
     * Tells the server to start listening for Receivers trying to connect to the
     * server. 
     * 
     * This server will create a new Wii Server Thread for every Receiver 
     * that will be the messenger between WiiServerRunner.java and a connected ReceiverRunner.java.
     *****************************************************************************/  

    public void run() {
        while (true) {
        	try {
        		
        		//Create a new WiiServerThread
				thread = new WiiServerThread(serverSocket.accept());
				
				//Make the instance of WiiServerThread an observer of WiiServerRunner.java
				WiiServerRunner.newObserver(thread);
				
				// Start the new thread
				thread.start();
				System.out.println("New Server Thread Created!");
			} 
        	catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
}