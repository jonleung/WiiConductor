package dir3_WiiReceiver_Configure4_WiiConductor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundPlayer {
  
  //******************************
  // SETTINGS
  //******************************
  String clickDotWavFileLocation = this.getClass().getResource("click.wav").toString();
  ////////////////////////////////
  
  File soundFile;
  AudioInputStream sound;
  DataLine.Info info;
  Clip clip;
  
  public SoundPlayer() {
    
    Pattern p = Pattern.compile("file:(.*)");
    Matcher m = p.matcher(clickDotWavFileLocation);
    if (m.find()) {
      clickDotWavFileLocation = m.group(1);
    }
    
    soundFile = new File(clickDotWavFileLocation);
    try {
      sound = AudioSystem.getAudioInputStream(soundFile);
    } 
    catch (UnsupportedAudioFileException e) {e.printStackTrace();}
    catch (IOException e) {e.printStackTrace();System.out.println("Cannot find click.wav, please include an absolute path in SoundPlayer.java");}

    // load the sound into memory (a Clip)
    info = new DataLine.Info(Clip.class, sound.getFormat());
    try {
      clip = (Clip) AudioSystem.getLine(info);
    }
    catch (LineUnavailableException e) {e.printStackTrace();}
    
    try {
      clip.open(sound);
    }
    catch (LineUnavailableException e) {e.printStackTrace();} 
    catch (IOException e) {e.printStackTrace();}
  }
  
  public void play() {
    this.clip.start();
    this.clip.setFramePosition(0);
  }

}
