package player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDeviceBase;

public class MP3Device
		extends AudioDeviceBase{
	public SourceDataLine source=null;
	public AudioFormat fmt=null;
	private byte[] byteBuf=new byte[4096];

	protected void setAudioFormat(AudioFormat audioFormat){
		this.fmt=audioFormat;
	}

	public AudioFormat getAudioFormat(){
		if(this.fmt==null){
			Decoder decoder=this.getDecoder();
			this.fmt=new AudioFormat(decoder.getOutputFrequency(),16,decoder.getOutputChannels(),true,false);
		}
		return this.fmt;
	}

	protected DataLine.Info getSourceLineInfo(){
		AudioFormat audioFormat=this.getAudioFormat();
		DataLine.Info info=new DataLine.Info(SourceDataLine.class,audioFormat);
		return info;
	}

	public void open(AudioFormat audioFormat) throws JavaLayerException{
		if(!this.isOpen()){
			this.setAudioFormat(audioFormat);
			this.openImpl();
			this.setOpen(true);
		}
	}

	public synchronized void close() {
		super.close();
		source.close();
	}
	protected void openImpl() throws JavaLayerException{}

	public void createSource() throws JavaLayerException{
		Throwable throwable=null;
		try{
			javax.sound.sampled.Line line=AudioSystem.getLine(this.getSourceLineInfo());
			if(line instanceof SourceDataLine){
				this.source=(SourceDataLine) line;
				this.source.open(this.fmt);
				this.source.start();
			}
		}catch(RuntimeException runtimeException){
			throwable=runtimeException;
		}catch(LinkageError linkageError){
			throwable=linkageError;
		}catch(LineUnavailableException lineUnavailableException){
			throwable=lineUnavailableException;
		}
		if(this.source==null){
			throw new JavaLayerException("cannot obtain source audio line",throwable);
		}
	}

	public int millisecondsToBytes(AudioFormat audioFormat,int n){
		return (int) ((double) ((float) n*(audioFormat.getSampleRate()*(float) audioFormat.getChannels()
				*(float) audioFormat.getSampleSizeInBits()))/8000.0);
	}

	protected void closeImpl(){
		if(this.source!=null){
			this.source.close();
		}
	}

	protected void writeImpl(short[] arrs,int n,int n2) throws JavaLayerException{
		if(this.source==null){
			this.createSource();
		}
		byte[] arrby=this.toByteArray(arrs,n,n2);
		this.source.write(arrby,0,n2*2);
	}

	protected byte[] getByteArray(int n){
		if(this.byteBuf.length<n){
			this.byteBuf=new byte[n+1024];
		}
		return this.byteBuf;
	}

	protected byte[] toByteArray(short[] arrs,int n,int n2){
		byte[] arrby=this.getByteArray(n2*2);
		int n3=0;
		while(n2-->0){
			short s=arrs[n++];
			arrby[n3++]=(byte) s;
			arrby[n3++]=(byte) (s>>>8);
		}
		return arrby;
	}

	protected void flushImpl(){
		if(this.source!=null){
			this.source.drain();
		}
	}

	public int getPosition(){
		int n=0;
		if(this.source!=null){
			n=(int) (this.source.getMicrosecondPosition()/1000L);
		}
		return n;
	}

	public void test() throws JavaLayerException{
		try{
			this.open(new AudioFormat(22050.0f,16,1,true,false));
			short[] arrs=new short[2205];
			this.write(arrs,0,arrs.length);
			this.flush();
			this.close();
		}catch(RuntimeException runtimeException){
			throw new JavaLayerException("Device test failed: "+runtimeException);
		}
	}
}