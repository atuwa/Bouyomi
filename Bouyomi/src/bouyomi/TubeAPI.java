package bouyomi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TubeAPI{

	public static boolean nowPlayVideo;
	public static String video_host=null;
	public static int VOL=30,DefaultVol=-1;
	public static String lastPlay;
	public static int maxHistory=32;//32個履歴を保持する
	/**履歴が入ってるリスト*/
	public static ArrayList<String> playHistory=new ArrayList<String>();
	private static String HistoryFile="play.txt";
	public static boolean playTube(String videoID) {
		if(videoID.indexOf("script")>=0)return false;
		try{
			nowPlayVideo=true;
			if(DefaultVol>=0)VOL=DefaultVol;
			//videoID=URLEncoder.encode(videoID,"utf-8");//これ使うと動かない
			URL url=new URL("http://"+video_host+"/operation.html?"+videoID+"&vol="+VOL);
			url.openStream().close();
			lastPlay=videoID;
			if(playHistory.size()>=maxHistory){
				playHistory.remove(maxHistory-1);
			}
			playHistory.add(0,videoID);
			try{
				FileOutputStream fos=new FileOutputStream(HistoryFile,true);//追加モードでファイルを開く
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
			int vol=(int) Double.parseDouble(l);
			return vol;
		}catch(NumberFormatException e){
			e.printStackTrace();
		}
		return -1;
	}
	public static String getLine(String op) {
		if(!op.contains("="))op+="=0";
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
				url.indexOf("http://www.youtube.com/")==0||
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
		}else if(url.indexOf("nico=")==0) {
			return playTube(url);
		}else if(url.indexOf("https://www.nicovideo.jp/watch/")==0
				||url.indexOf("http://www.nicovideo.jp/watch/")==0
				||url.indexOf("https://nicovideo.jp/watch/")==0
				||url.indexOf("http://nicovideo.jp/watch/")==0) {
			Pattern p = Pattern.compile("sm[0-9]++");
			Matcher m = p.matcher(url);
			if(m.find()) {
				url=m.group();
				//System.out.println("ニコニコ ID="+url);
				return playTube("nico="+url);
			}
		}else{
			Pattern p = Pattern.compile("sm[0-9]++");
			Matcher m = p.matcher(url);
			if(m.find()) {
				url=m.group();
				//System.out.println("ニコニコ ID="+url);
				return playTube("nico="+url);
			}
			bc.em="URLを解析できませんでした";
			System.out.println("URL解析失敗"+url);
		}
		return false;
	}
	/*//新しいの。上手いこと動かない
	public static String extract(String url,String name) {
		Matcher match=Pattern.compile(name+"=[a-zA-Z0-9]").matcher(url);
		if(match.find()) {
			return match.group();
		}else return null;
	}
	*/
	//古いの。新しいのがうまく動かないからこっちを使う
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
		if(!op.contains("="))op+="=0";
		try{
			URL url=new URL("http://"+video_host+"/operation.html?"+op);
			url.openStream().close();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
	public static String IDtoURL(String id) {
		if(id.indexOf("v=")==0){//動画
			return "https://www.youtube.com/watch?"+id;
		}else if(id.indexOf("list=")==0){//プレイリスト
			return "https://www.youtube.com/playlist?"+id;
		}else if(id.indexOf("nico=")==0){//プレイリスト
			return "https://www.nicovideo.jp/watch/"+id.substring(5);
		}
		return null;//それ以外
	}
	/**ファイルから再生履歴を読み込む*/
	public static void loadHistory() throws IOException {
		FileInputStream fis=new FileInputStream(new File(HistoryFile));
		InputStreamReader isr=new InputStreamReader(fis,StandardCharsets.UTF_8);
		BufferedReader br=new BufferedReader(isr);
		try {
			while(br.ready()) {
				String line=br.readLine();
				if(line==null)break;
				int index=line.indexOf('\t');
				if(index>=0)lastPlay=line.substring(0,index);
				else lastPlay=line;
				if(playHistory.size()>=maxHistory){
					playHistory.remove(maxHistory-1);
				}
				playHistory.add(0,lastPlay);
			}
		}finally{
			br.close();
		}
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
							else{
								System.out.println("動画自動停止");
								BouyomiProxy.talk(BouyomiProxy.proxy_port,"/動画停止()");
							}
						}
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
}
