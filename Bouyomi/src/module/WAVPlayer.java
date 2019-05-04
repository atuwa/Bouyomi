package module;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class WAVPlayer extends Thread{
	private int loopCount;
	private URL url;
	private AudioInputStream ais;
	private AudioFormat format;
	private AudioFormat decodedFormat;
	private SourceDataLine line;
	private long playFrame;
	private boolean stop;
	private boolean end;
	private float vol;
	public static WAVPlayer nowPlay;
	public static String nowPlayURL;
	public static float Volume=10;
	public WAVPlayer(URL url) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		this.url=url;
		load();
		DataLine.Info info = new DataLine.Info (SourceDataLine.class,decodedFormat);
		// ラインを取得
		line = (SourceDataLine) AudioSystem.getLine (info);
		line.open(format);
		line.start();
	}
	public void run() {
		byte[] data=new byte[line.getBufferSize()];
		//while(true)
		{
			loopCount++;
			if(loopCount>3)stop=true;
			try {
				playFrame=0;
				int bytesRead;
				while((bytesRead=ais.read(data,0,data.length))!=-1){
					playFrame+=bytesRead/format.getFrameSize();
					line.write(data,0,bytesRead);
					//if(getFrameLength()-playFrame<60000)break;
					/*
					if(stop) try{
						join();
					}catch(InterruptedException t){

					}
					*/
					if(end)break;
				}
				//if(end)break;
				line.drain();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		try{
			ais.close();
		}catch(Exception e){}
		try{
			line.drain();
		}catch(Exception e){}
		try{
			line.stop();
		}catch(Exception e){}
		try{
			line.close();
		}catch(Exception e){}
	}
	public long getPlayFrame() {
		return playFrame;
	}
	public void end() {
		end=true;
	}
	public void stop(boolean s) {
		stop=s;
		if(!stop) {
			loopCount=0;
			interrupt();
		}
	}
	/**音量設定
	 * @return 実際に設定された音量*/
	public float setVol(float vol,SourceDataLine line){
		if(line==null)return this.vol;
		try{
			FloatControl control=(FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
			float f=(float) Math.log10(vol/100d)*20f;
			if(vol<=0) {
				f=control.getMinimum();
				vol=0;
			}else if(vol>=200) {
				f=control.getMaximum();
				vol=200;
			}
			if(f>control.getMaximum()) return vol;
			if(f<control.getMinimum()) return vol;
			if(f==Float.NaN) return vol;
			control.setValue(f);
			this.vol=vol;
			return vol;
		}catch(Exception n){
			n.printStackTrace();
		}
		return this.vol;
	}
	/**現在の音量*/
	public float getVolume(){
		return vol;
	}
	public static float setVolume(float vol){
		Volume=vol;
		if(nowPlay!=null)return nowPlay.setVol(vol);
		return -1;
	}
	public static void Stop() {
		if(nowPlay!=null)nowPlay.stop(true);
	}
	public static void play() {
		if(nowPlay!=null)nowPlay.stop(false);
	}
	public static boolean play(String url) {
		//if(url.indexOf("http")<0)return false;
		if(url.equals(nowPlayURL))nowPlay.stop(false);
		if(nowPlay!=null)nowPlay.end();
		try{
			nowPlay=new WAVPlayer(new URL(url));
			nowPlay.setVol(Volume);
			nowPlay.run();
			return true;
		}catch(UnsupportedAudioFileException|IOException|LineUnavailableException e){
			e.printStackTrace();
		}
		return false;
	}
	public static float nowVolume() {
		if(nowPlay!=null)return nowPlay.getVolume();
		return -1;
	}
	public void load() throws UnsupportedAudioFileException, IOException {
		AudioInputStream fis=AudioSystem.getAudioInputStream(url);
		format=fis.getFormat();
		decodedFormat=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				format.getSampleRate(),
				format.getSampleSizeInBits()==AudioSystem.NOT_SPECIFIED?16:format.getSampleSizeInBits(),
				format.getChannels(),
				format.getFrameSize()==AudioSystem.NOT_SPECIFIED?format.getChannels()*2:format.getFrameSize(),
				format.getSampleRate(),
				false); // PCMフォーマットを指定
		// 指定された形式へ変換
		//System.out.println(decodedFormat);
		ais = AudioSystem.getAudioInputStream(decodedFormat, fis);
		//ais=AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED,fis);
		format=ais.getFormat();
	}
	public static void main(String[] args) {
		play("http://localhost/test/test.mid");
		try{
			Thread.sleep(5000);
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		play("http://localhost/test/test.wav");
		try{
			Thread.sleep(5000);
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		play("file:///D:/%E3%83%80%E3%82%A6%E3%83%B3%E3%83%AD%E3%83%BC%E3%83%89/oujo.mp3");
		try{
			Thread.sleep(5000);
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		play("http://localhost/test/test.mp3");
	}
	public float setVol(float vol){
		return setVol(vol, line);
	}
}
