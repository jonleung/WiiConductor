/*
 * Note that Garage Band can only go up to 240 BPM == .25 of a second
 * 													=  250ms
 * Print an error when you go over this speed
 * 
 * But allow people to input things 
 */
// Make it obvious that the piece has begun or not begun, "The piece has not begun, press A for the piece to restart / begin"

package dir5_WiiReceiver_SampleApplication;

public class ReceiverRunnerAnalysis {
	
	double x, y, z;
	double nx, ny, nz;
	
	double liveMagnitude, totalSumMagnitude;
	
	boolean startedCollecting, pressedA;
	long startedCollectingTime;

	// Reset all variables
	public void reset() {
		x=0;
		y=0;
		z=0;
		
		nx=0;
		ny=0;
		nz=0;
		
		liveMagnitude = 0;
		totalSumMagnitude = 0;
		
		startedCollecting = false;
		pressedA = false;
		startedCollectingTime = 0;
		
	}
	
	// Constructor
	public ReceiverRunnerAnalysis()  {
		reset();
		
		System.out.println("Pres 'A' and shake the wiimote as hard as you can for 5 seconds!\n");
	}
	
	// analyzeIncommingWiiAcceleration() is called every time the WiiMote sends an updated acceleration.
	public void analyzeIncommingWiiAcceleration(double newX, double newY, double newZ, long newTime) {
		x = newX;
		y = newY;
		z = newZ;
		
		double liveMagnitude = Math.sqrt(x*x + y*y + z*z);
		
		if (!startedCollecting && pressedA && liveMagnitude > 2.5) {
			System.out.println("You have 5 seconds on the clock!");
			startedCollecting = true;
			startedCollectingTime = System.currentTimeMillis();
		}
		if(startedCollecting && System.currentTimeMillis() - startedCollectingTime > 5000) {
			startedCollecting = false;
			System.out.println("Your Shake Magnitude = " + totalSumMagnitude);
			reset();
			System.out.println("Think you can do better? Press 'A' and try again!\n");
		}
		
//		if((System.currentTimeMillis() - startedCollectingTime)%1000 == 0) {
//			System.out.println(System.currentTimeMillis()/1000);
//		}
			
		if (startedCollecting) {
			totalSumMagnitude += liveMagnitude;
		}
	}
	
	// analyzeIncommingNunchuckAcceleration() is called every time the Nunchuck sends an updated acceleration.
	public void analyzeIncommingNunchuckAcceleration(double newNX, double newNY, double newNZ, long newTime) {
		this.nx = newNX;
		this.ny = newNY;
		this.nz = newNZ;
	}
	
	
	// The below methods are called when their respective methods are called.
	
	// WiiMote Buttons
	
	public void Button_LEFT(){
		System.out.println("Reset");
		reset();
	}
	
	public void Button_MINUS(){
		System.out.println("Exited Program");
		System.exit(0);
	}
	
	public void Button_A(){
		System.out.println("Don't rush now, I won't start counting until you start shaking!");
		pressedA = true;
	}
	
	public void Button_UP(){System.out.println("UP");}
	public void Button_DOWN(){System.out.println("DOWN");}
	public void Button_RIGHT(){System.out.println("RIGHT");}
	public void Button_PLUS() {System.out.println("PLUS");}
	public void Button_HOME(){System.out.println("HOME");}
	public void Button_B(){System.out.println("B");}
	public void Button_1(){System.out.println("1");}
	public void Button_2(){System.out.println("2");}
	
	// Nunchuck Buttons
	public void Button_C(){System.out.println("C");}
	public void Button_Z(){System.out.println("Z");}



}
