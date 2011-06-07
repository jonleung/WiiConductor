package dir3_WiiReceiver_Configure4_WiiConductor;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;



public class GarageBandScriptEngine {
  ScriptEngineManager mgr;
  ScriptEngine engine;
  String playScript = "tell application \"GarageBand\" \n activate \n play song 1 \n end tell";
  String BPMScript1 = "tell application \"GarageBand\" \n set tempo_value to ";
  String BPMScript2 = " div 1 \n set the tempo of the front song to the tempo_value \n end tell";
  
  
  String VolumeScript1 = "set volume_setting to ";
  String VolumeScript2 = "\n set this_setting to the volume_setting * 0.01 \n tell application \"GarageBand\" \n set the master volume of the front song to this_setting \n end tell";
  public GarageBandScriptEngine(){
    mgr = new ScriptEngineManager();;
    engine = mgr.getEngineByName("AppleScript");
  }
  
  public void play() {
    System.out.println("Play");
    try {
      engine.eval(playScript);
    } catch (ScriptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  public void setBPM(int BPM) {
    System.out.println("BPM");
    try {
      engine.eval(BPMScript1 + BPM + BPMScript2);
    } catch (ScriptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  public void setVolume(int volume) {
    try {
      engine.eval(VolumeScript1 + volume + VolumeScript2);
    } catch (ScriptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
