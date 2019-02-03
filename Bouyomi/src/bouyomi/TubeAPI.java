package bouyomi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TubeAPI{

	public static boolean nowPlayVideo;
	public static String video_host=null;
	public static int VOL=30;
	public static boolean playTube(String videoID) {
		try{
			nowPlayVideo=true;
			URL url=new URL("http://"+video_host+"/operation.html?"+videoID+"&vol="+VOL);
			url.openStream().close();
			try{
				FileOutputStream fos=new FileOutputStream("play.txt",true);//追加モードでファイルを開く
				try{
					String d=new SimpleDateFormat("yyyy/MM/dd HH時mm分ss秒").format(new Date());
					fos.write((videoID+"\t再生時刻"+d+"\n").getBytes(StandardCharsets.UTF_8));//改行文字を追加してバイナリ化
				}finally {
					fos.close();
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
			return true;
		}catch(IOException e){
			System.err.println(e.getMessage());
			//e.printStackTrace();
		}
		return false;
	}
	public static void setAutoStop(){
		if(video_host!=null)new Thread("AutoVideoStop") {
			public void run() {
				try{
					Thread.sleep(60000);
					if(nowPlayVideo&&System.currentTimeMillis()-BouyomiProxy.lastComment>8*60000) {
						BouyomiProxy.talk(BouyomiProxy.proxy_port,"動画停止()");
					}
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}.start();
	}
}
