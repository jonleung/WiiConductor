/*
 * Note that Garage Band can only go up to 240 BPM == .25 of a second
 * 													=  250ms
 * Print an error when you go over this speed
 * 
 * But allow people to input things 
 */
// Make it obvious that the piece has begun or not begun, "The piece has not begun, press A for the piece to restart / begin"

package dir3_WiiReceiver_Configure4_WiiConductor;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

public class ReceiverRunnerAnalysis{

	//******************************************
	// Settings
	//******************************************
	
	// Please see the Readme to see how to configure these settings.
	
	boolean clickSoundOn = true;
	
	static int BPM_ACCEPTABLE_DELTA = 25;	//Default 25
	static int BPM_MOVING_AVERAGE_WIDTH = 5;	//Default 5
	static int BPM_INITIALIZED_VALUE = 100;
	
	static int VOLUME_ACCEPTABLE_DELTA = 4;	//Default 4
	static int VOLUME_MOVING_AVERAGE_WIDTH = 5;	//Default 5
	static int VOLUME_INITIALIZED_VALUE = 70;
	
	static String TYPE_OF_AVERAGE = "weighted"; //simple, weighted (weighted is Default)
	
	static boolean BPM_DEBUG = true;
	static boolean VOLUME_DEBUG = false;
	
	////////////////////////////////////////////
	
	boolean concertInProgress = false;		// Stores the state of whether or not this program is exerting control over Garage Band.
	
	SoundPlayer soundPlayer;		// Used to play the sound of a metronome click.
	
	private GarageBandScriptEngine garageBand;		// Class that sends applescript messages to Garage Band.
	
	//BPM Variables
	long prevTime;
	long curTime;
	long delay;
	long prevTickTime;
	
	int liveBpm;
	int prevLiveBpm;
	int prevSetBpm;
	
	LinkedList<Integer> bpmMovingAverage = new LinkedList<Integer>();
	int bpmArraySum;
	int bpmArrayAvg;
	int prevBpmArrayAvg;
	
	//Volume Variables
	
	double maxSwipeMagnitude = 0;
	int liveVolume;
	int prevSetVolume;
	
	LinkedList<Integer> volumeMovingAverage = new LinkedList<Integer>();
	int volumeArraySum;
	int volumeArrayAvg;
	
	
	public void reset() {
		prevTime = System.currentTimeMillis();
		
		bpmMovingAverage.clear();
		for(int i=0; i<BPM_MOVING_AVERAGE_WIDTH; i++) {
			bpmMovingAverage.push(BPM_INITIALIZED_VALUE);
		}
		bpmArrayAvg = BPM_INITIALIZED_VALUE;
		bpmArraySum = BPM_INITIALIZED_VALUE * BPM_MOVING_AVERAGE_WIDTH;
		
		volumeMovingAverage.clear();
		for(int i=0; i<VOLUME_MOVING_AVERAGE_WIDTH; i++) {
			volumeMovingAverage.push(VOLUME_INITIALIZED_VALUE);
		}
		volumeArrayAvg = VOLUME_INITIALIZED_VALUE;
		volumeArraySum = VOLUME_INITIALIZED_VALUE * VOLUME_MOVING_AVERAGE_WIDTH;
		System.out.println("Press 'A' to start controlling Garage Band.");
		
		concertInProgress = true;
	}
	
	public ReceiverRunnerAnalysis()  {
		reset();
		garageBand = new GarageBandScriptEngine();
		soundPlayer = new SoundPlayer();
		System.out.println("Press A to start playing Garage Band\n\n");
	}
	
	 
	/**********************************************
	 * analyzeIncommingWiiAcceleration()
	 * 
	 * This method is called every time the WiiMote sends an updated acceleration.
	 * It also determines when the user made a conducting gesture and
	 * plays a sound and calculates the BPM of that gesture.
	 * 
	 *********************************************/
	public void analyzeIncommingWiiAcceleration(double x, double y, double z, long time) {
		
		// Calculate the absolute magnitude of acceleration
		double liveMagnitude = Math.sqrt(x*x+y*y+z*z);
		
		// If this magnitude is greater than a certain value
		// Then it is determined that the motion is making this gesture intentaionally
		if (liveMagnitude > 1.6) {
			
			curTime = System.currentTimeMillis(); // Store current time
			delay = curTime - prevTime; // Determine how long its been since the last time
									    // the magnitude was greater than 1.6
			
			// If the delay since the last time the WiiMote experienced a magnitude greater than 1.6,
			// is greater than 50ms, this must mean that the user has just finished the course
			// of their conducting stroke and the program should capture this time as a beat.
			
			// If the time since the last registered 'click' is greater than 200ms,
			// it means that the acceleration currently being experienced is most likely not
			// caused accidently in a any way.
			if(delay > 50 && curTime - prevTickTime > 250) {
				if(clickSoundOn) soundPlayer.play(); // Play the sound of a tick
				
				// Calculate the bpm solely based on the time between this tick and the last tick
				liveBpm = (int) Math.round(60.0/ ((curTime - prevTickTime) /1000.00)); 
				
				// Messages to the Console
				if(BPM_DEBUG) System.out.printf("\n\n  liveBpm=%d  \t  prevTickDelta=%d  \t  prevGarageSetBpm=%d", liveBpm, liveBpm - prevLiveBpm, prevSetBpm);
				
				prevTickTime = curTime; // Now that you have registered this instance as a 'click',
										// store the current time into the time of the previous tick.
				
				updateBpmMovingAverage(liveBpm); // Determine the average BPM
				prevLiveBpm = liveBpm;
				
				updateVolumeMovingAverage(maxSwipeMagnitude); // Determine the average volume.
				maxSwipeMagnitude = 0; // Reset the maximum swipe magnitude over the course of a swipe.
			}
			prevTime = curTime;
		}
		maxSwipeMagnitude = Math.max(maxSwipeMagnitude, liveMagnitude); // store the largest acceleration magnitude
																		// over the course of a swipe which will be used 
																		// to determine the loudest volume
	}
	
	public void analyzeIncommingNunchuckAcceleration(double nx, double ny, double nz, long time) {
		//The Nunchuck is not used in this alpha version.
		//I am hoping that in the future it can be used for cueing in tracks in garage band. 
	}
	
	
	public void updateBpmMovingAverage(int newBpm) {
		bpmMovingAverage.remove();	// Remove the oldest BPM in the moving average
		bpmMovingAverage.offer(newBpm);	// Add the newest BPM
		
		bpmArraySum = 0;	// Reset the sum
		
		Iterator<Integer> itr = bpmMovingAverage.iterator();
		
		// Simple Moving Average
		if (TYPE_OF_AVERAGE.toLowerCase().equals("simple")) {
			while (itr.hasNext()) {		// Iterate through all moving average values
				int next = itr.next();
				bpmArraySum += next;	// and sum them together.
			}
			bpmArrayAvg = bpmArraySum/BPM_MOVING_AVERAGE_WIDTH;	// Then calculate a mean average
		}
		// Weighted Moving Average
		else {
			int weightCount = 1;	// Initialize the weight to start weighing the oldest value by
			int summorial = 0;		// The sum of all the weightCounts
			while(itr.hasNext()) {	
				bpmArraySum += weightCount * itr.next();	// Starting with the oldest value, multiply it by its weight and add it to the sum
				summorial += weightCount;	// Sum all the weightCounts together
				weightCount += .5;	// Increase the weight by a linear proportion for the next most recent value
			}		
			bpmArrayAvg = bpmArraySum/summorial;	// Calculate the weighted average by dividing out all the weights from the sum
		}
		
		if(BPM_DEBUG) System.out.printf("\tbpmMovAvg=%d  \t  bpmMovAvgDelta=%d",bpmArrayAvg,bpmArrayAvg-prevBpmArrayAvg);
		
		prevBpmArrayAvg = bpmArrayAvg;
		
		// Send To Garage Band if new average is smaller or bigger than the currently set BPM 
		// This is to prevent making lots and lots of tiny jumps that don't sound good in Garage Band.
		if ((bpmArrayAvg > prevSetBpm+BPM_ACCEPTABLE_DELTA) ||
		    (bpmArrayAvg < prevSetBpm-BPM_ACCEPTABLE_DELTA)) {
			
			if(concertInProgress) { // Only if you have started garage band
				garageBand.setBPM(bpmArrayAvg);
			}
			prevSetBpm = bpmArrayAvg; // Remember the previously set BPM
			if(BPM_DEBUG) System.out.print("    BPM changed to: " + bpmArrayAvg);
		}
	}
	
	public void updateVolumeMovingAverage(double newMaxSwipeMagnitude) {
		int newVolume = (int)((newMaxSwipeMagnitude-1)*18); // Scale magnitude to volume from 1-100
		if(VOLUME_DEBUG) System.out.printf ("origMagnitude=%f\tnewVolume=%d\n", newMaxSwipeMagnitude, newVolume);
		
		// Make sure that the new volume is in the range of 1-100
		if(newVolume > 100) newVolume = 100;
		else if(newVolume < 10) newVolume = 10;
		
		volumeMovingAverage.remove(); // Remove the oldest Volume level in the moving average
		volumeMovingAverage.offer(newVolume); // Add the newest Volume to the moving average
		
		volumeArraySum = 0; // Reset the moving averagesum
		
		Iterator<Integer> itr = volumeMovingAverage.iterator();
		
		// Simple Moving Average
		if(TYPE_OF_AVERAGE.toLowerCase().equals("simple")) {
			while (itr.hasNext()) {	// Iterate through all moving average values
				int next = itr.next();
				volumeArraySum += next;	// and sum them together.
			}
			volumeArrayAvg = volumeArraySum/VOLUME_MOVING_AVERAGE_WIDTH; // Then calculate the mean average
		}
		else {
			int weightCount = 1;	// Initialize the weight to start weight the oldest value by
			int summorial = 0;	// The sum of all the weightCounts
			while(itr.hasNext()) {
				volumeArraySum += weightCount * itr.next();	// Starting with the oldest value, multiply it by its weight and add it to the sum
				summorial += weightCount; // Sum all the weightCounts together
				weightCount+=.5; // Increase the weight by a linear proportion for the next most recent value
			}
			volumeArrayAvg = volumeArraySum/summorial; // Calculate the weighted average by dividing out all the weights from the sum
		}
		
		if(VOLUME_DEBUG) System.out.printf("\tvolumeMovingAvg=%d", volumeArrayAvg);
		
		// Send To Garage Band if new average is smaller or bigger than the currently set VOLUME 
		// This is to prevent making lots and lots of tiny jumps that don't sound good in Garage Band.
		if(volumeArrayAvg > prevSetVolume + VOLUME_ACCEPTABLE_DELTA || 
		   volumeArrayAvg < prevSetVolume - VOLUME_ACCEPTABLE_DELTA) {
			
			if(concertInProgress) { // Only if you have started garage band
				garageBand.setVolume(volumeArrayAvg);
			}
			prevSetVolume = volumeArrayAvg; // Remember the previously set volume
			if(VOLUME_DEBUG) System.out.println("\t\t\t\t\t\t\t\tVolume Changed To: " + volumeArrayAvg);
		}
	}
	

	
	//********************************
	// Buttons
	//********************************
	
	public void Button_A(){
		System.out.println("\n\nButton_A: From the Top!");
		
		garageBand.setBPM(BPM_INITIALIZED_VALUE);
		garageBand.setVolume(VOLUME_INITIALIZED_VALUE);
		garageBand.play();
		
	}
	
	public void Button_UP(){System.out.println("UP");}
	public void Button_DOWN(){System.out.println("DOWN");}
	public void Button_LEFT(){System.out.println("LEFT");}
	public void Button_RIGHT(){System.out.println("RIGHT");}
	public void Button_MINUS(){System.out.println("MINUS");System.exit(0);}
	public void Button_PLUS() {System.out.println("PLUS");}
	public void Button_HOME(){System.out.println("HOME");}
	public void Button_B(){System.out.println("B");}
	public void Button_1(){System.out.println("1");}
	public void Button_2(){System.out.println("2");}
	
	// Nunchuck Buttons
	public void Button_C(){System.out.println("C");}
	public void Button_Z(){System.out.println("Z");}



}
