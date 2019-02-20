package bouyomi;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MusicPlayerAPI{

	public static float Volume;
	public static String host;
	//ID=5
	public static boolean play(){
		return send(null,(byte)5);
	}
	//ID=4
	public static boolean play(String tag){
		if(tag.indexOf("http")!=0)return false;
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		DataOutputStream dos=new DataOutputStream(baos);
		try{
			dos.write(4);
			dos.writeUTF(tag);
		}catch(IOException e){
			e.printStackTrace();
		}
		return send(null,baos.toByteArray());
	}
	//ID=3
	public static float nowVolume(){
		byte[] b=new byte[4];
		if(send(b,(byte)3));
		else return -1;
		int ch1=b[0]<0?b[0]+256:b[0];
		int ch2=b[1]<0?b[1]+256:b[1];
		int ch3=b[2]<0?b[2]+256:b[2];
		int ch4=b[3]<0?b[3]+256:b[3];
		int i=((ch1<<24)+(ch2<<16)+(ch3<<8)+(ch4<<0));
		return Float.intBitsToFloat(i);
	}
	//ID=2
	public static float setVolume(float vol){
		int i=Float.floatToIntBits(vol);
		byte[] b=new byte[5];
		b[0]=2;
		b[1]=(byte) ((i>>>24)&0xFF);
		b[2]=(byte) ((i>>>16)&0xFF);
		b[3]=(byte) ((i>>>8)&0xFF);
		b[4]=(byte) ((i>>>0)&0xFF);
		send(null,b);
		return vol;
	}
	//ID=1
	public static boolean stop(){
		return send(null,(byte)1);
	}
	/**棒読みちゃんに送信する*/
	public synchronized static boolean send(byte[] read,byte... data){
		if(data.length<1||host==null||host.isEmpty())return false;
		Socket soc=null;
		try{
			int port=6000;
			int beginIndex=host.indexOf(':');
			if(beginIndex>0) {
				port=Integer.parseInt(host.substring(beginIndex+1));
				host=host.substring(0,beginIndex);
			}
			soc=new Socket(host,port);
			OutputStream os=soc.getOutputStream();
			os.write(data);
			if(read!=null&&read.length>0) {
				InputStream is=soc.getInputStream();
				for(int i=0;i<read.length;i++) {
					int r=is.read();
					if(r<0)throw new IOException("データ不足");
					read[i]=(byte) r;
				}
			}
			return true;
		}catch(ConnectException e) {
			System.out.println("再生サーバに接続できません");
		}catch(UnknownHostException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			try{
				if(soc!=null)soc.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		return false;
	}
}
