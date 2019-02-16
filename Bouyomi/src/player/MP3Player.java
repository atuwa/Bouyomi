package player;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;

public class MP3Player extends Thread implements IPlayer{

	private int frame=0;
	private Bitstream bitstream;
	private Decoder decoder;
	private MP3Device audio;
	private boolean closed=false;
	private boolean complete=false;
	private BufferedInputStream stream;
	private boolean is_run,stop,end;
	public int loopCount;
	private final URL file;
	private float vol=50;
	private boolean initVol;

	public MP3Player(URL file){
		this.file=file;
	}
	public void close(){
		try{
			if(stream!=null){
				AudioDevice audioDevice=this.audio;
				if(audioDevice!=null){
					this.closed=true;
					this.audio=null;
					audioDevice.close();
					try{
						this.bitstream.close();
					}catch(BitstreamException bitstreamException){
						// empty catch block
					}
				}
				stream.close();
			}
		}catch(IOException e){
			System.out.println("IOException");
		}finally{
			stream=null;
			is_run=false;
		}
	}
	public synchronized void run(){
		if(stream!=null||is_run){
			return;
		}else{
			is_run=true;
			end=false;
			complete=false;
			do{
				loopCount++;
				setFile();
				try{
					boolean bl=true;
					while(bl){
						if(stop) try{
							join();
						}catch(InterruptedException t){

						}
						bl=this.decodeFrame();
						if(end)break;
					}
					if(this.audio!=null){
						audio.flush();
						synchronized(this){
							this.complete=!this.closed;
							this.close();
						}
					}
				}catch(JavaLayerException e){
					e.printStackTrace();
					System.out.println("JavaLayerException");
				}finally{

				}
				if(end)break;
			}while(loopCount<3);
			close();
		}
	}
	protected boolean decodeFrame() throws JavaLayerException{
		try{
			if(audio==null){
				return false;
			}
			Header header=this.bitstream.readFrame();
			if(header==null){
				return false;
			}
			SampleBuffer sampleBuffer=(SampleBuffer) this.decoder.decodeFrame(header,this.bitstream);
			int fz=audio.getAudioFormat().getFrameSize()<1?1:audio.fmt.getFrameSize();
			synchronized(this){
				if(audio!=null){
					frame+=sampleBuffer.getBufferLength()/fz;
					audio.write(sampleBuffer.getBuffer(),0,sampleBuffer.getBufferLength());
				}
				if(!initVol&&audio.source!=null) {
					setVol(vol);
					/*
					if(audio.source.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
						System.out.println("SampleRateControlSupported");
					}else System.out.println("SampleRateControlNOTSupported");
					*/
					initVol=true;
				}
			}
			this.bitstream.closeFrame();
		}catch(RuntimeException runtimeException){
			runtimeException.printStackTrace();
			throw new JavaLayerException("Exception decoding audio frame",(Throwable) runtimeException);
		}
		return true;
	}
	private void setFile(){
		try{
			stream=new BufferedInputStream((file.openStream()));
			this.bitstream=new Bitstream(stream);
			this.decoder=new Decoder();
			//FactoryRegistry factoryRegistry=FactoryRegistry.systemRegistry();
			//this.audio=factoryRegistry.createAudioDevice();
			audio=new MP3Device();
			audio.open(this.decoder);
			initVol=false;
			closed=false;
			frame=0;
		}catch(IOException e){
			e.printStackTrace();
			// System.out.println("BUFFER ERR");
			//System.out.println("IOException");
		}catch(JavaLayerException e){
			// System.out.println("PLAYER ERR");
			System.out.println("JavaLayerException");
		}
	}
	public long getLoadFrame(){
		return frame;
	}
	public boolean isEnd(){
		return this.complete;
	}
	public void stop(boolean b){
		this.stop=b;
		if(!stop) {
			loopCount=0;
			interrupt();
		}
	}
	public void end(){
		end=true;
	}
	public float setVol(float vol){
		this.vol=vol;
		if(audio!=null)setVol(vol,audio.source);
		return vol;
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
			return vol;
		}catch(Exception n){
			n.printStackTrace();
		}
		return this.vol;
	}
	@Override
	public float getVolume(){
		return vol;
	}
}