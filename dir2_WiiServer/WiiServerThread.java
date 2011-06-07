package dir2_WiiServer;

import java.net.*;
import java.util.Observable;
import java.util.Observer;
import java.io.*;

public class WiiServerThread extends Thread implements Observer{
  private Socket socket = null;
  private String messageToClient = ""; // A local copy of the message received from
                     // WiiServerRunner.java containing the acceleration and button data

  /*****************************************************************************
   * WiiServerThread()
   * 
   * Constructor.
   * 
   *****************************************************************************/
  public WiiServerThread(Socket socket) {
    super("WiiServerThread");
    this.socket = socket;
  }
  

  /*****************************************************************************
   * update()
   * 
   * Because each WiiServerThread has been added as an Observer of WiiServerRunner.java,
   * and because WiiServerThread implements Observer, every time the
   * sendToBeObservedByWiiServerThread() method in WiiServerRunner.java is called with the 
   * updated Acceleration and Button data string, messageToBeObserved, 
   * this function will be called with the messageToBeObserved as the Object arg.
   * 
   *****************************************************************************/
  public void update(Observable o, Object arg) {
    //Store the message from WiiServerRunner.java with the acceleration and button data
    //as messageToClient
    messageToClient = (String) arg;
    
    //Now that we have updated data, interrupt "wait()" below, informing it
    //that the new data has arrived.
    this.interrupt();
  }
  
  /*****************************************************************************
   * run()
   * 
   * Every class that implements thread needs a run() function that will be
   * externally called to execute, in this case right after the class is created
   * in WiiServer.java 
   * 
   * WiiServerThread waits() until it gets a new message, or in other words,
   * "observes" a new message before sending it to its Receiver. Receiver will
   * then parse the string for the data. It knows that
   * a message has arrived when update() creates an interrupt.
   * 
   ****************************************************************************/
  public synchronized void run() {
    try {
      // Create a PrintWriter to send data over a socket connection
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    
      while (true) {
        try {
          // Wait to be interrupted when updated data arrives through update()
          wait();
        } 
        catch (InterruptedException e) {
          // Send the message with the updated data to this WiiServerThread's receiver
          // over the socket.
          out.println(messageToClient);
        }
      }   
    } 
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}