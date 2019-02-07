package bouyomi;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	public static int getVol(){
		try{
			String l=getLine("GETvolume");
			if(l==null)return -1;
			int vol=Integer.parseInt(l);
			return vol;
		}catch(NumberFormatException e){
			e.printStackTrace();
		}
		return -1;
	}
	public static String getLine(String op) {
		BufferedReader br=null;
		try{
			URL url=new URL("http://"+video_host+"/operation.html?"+op);
			InputStream is=url.openStream();
			InputStreamReader isr=new InputStreamReader(is);
			br=new BufferedReader(isr);//1行ずつ取得する
			String line=br.readLine();
			return line;
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			try{
				br.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		return null;
	}
	public static boolean play(BouyomiConection bc,String url) {
		if(url.indexOf("https://www.youtube.com/")==0||
				url.indexOf("https://m.youtube.com/")==0||
				url.indexOf("https://youtube.com/")==0||
				url.indexOf("http://www.youtube.com/?")==0||
				url.indexOf("http://m.youtube.com/")==0||
				url.indexOf("http://youtube.com/")==0) {
			String vid=extract(url,"v");
			String lid=extract(url,"list");
			if(vid!=null)return playTube(vid);
			else if(lid!=null) {
				String indexS=extract(url,"index");
				int index=-1;
				if(indexS!=null) {
					indexS=indexS.substring(6);
					try{
						index=Integer.parseInt(indexS)-1;
					}catch(NumberFormatException nfe) {

					}
				}
				if(index>=0)lid+="&index="+index;
				return playTube(lid);
			}else{
				bc.em="URLを解析できませんでした";
				return false;
			}
		}else if(url.indexOf("https://youtu.be/")==0||url.indexOf("http://youtu.be/")==0) {
			return playTube("v="+url.substring(17));
		}else if(url.indexOf("v=")==0) {
			return playTube(url);
		}else if(url.indexOf("list=")==0) {
			return playTube(url);
		}else{
			bc.em="URLを解析できませんでした";
			System.out.println("URL解析失敗"+url);
		}
		return false;
	}
	public static String extract(String url,String name) {
		if(url==null||url.isEmpty())return null;
		StringBuilder sb=new StringBuilder(name);
		sb.append("=");
		int start=url.indexOf(new StringBuilder("?").append(sb).toString());
		if(start<0)start=url.indexOf(new StringBuilder("&").append(sb).toString());
		if(start<0)return null;
		String ss=url.substring(start+1);
		int end=ss.indexOf("&");
		if(end<0)return ss;
		return ss.substring(0,end);
	}
	/**@param op 実行するコマンド
	 * @return 正常に実行された時trueが返る*/
	public static boolean operation(String op) {
		try{
			URL url=new URL("http://"+video_host+"/operation.html?"+op);
			url.openStream().close();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
	public static void setAutoStop(){
		if(video_host==null)return;
		new Thread("AutoVideoStop") {
			public void run() {
				while(true) {
					try{
						Thread.sleep(60000);
						if(nowPlayVideo&&System.currentTimeMillis()-BouyomiProxy.lastComment>8*60000) {
							if("NOT PLAYING".equals(getLine("status")))nowPlayVideo=false;
							else BouyomiProxy.talk(BouyomiProxy.proxy_port,"/動画停止()");
						}
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
}
