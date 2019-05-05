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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bouyomi.IModule.BouyomiEvent;

public class TubeAPI{

	public static boolean nowPlayVideo;
	public static String video_host=null;
	public static int VOL=30,DefaultVol=-1;
	public static String lastPlay,lastPlayUser;
	public static int maxHistory=32;//32個履歴を保持する
	/**履歴が入ってるリスト*/
	public static ArrayList<String> playHistory=new ArrayList<String>();
	static String HistoryFile="play.txt";
	private static long lastPlayDate;
	private static ExecutorService pool=new ThreadPoolExecutor(0,10,60L,TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());
	protected static int stopTime=480000;
	public static class PlayVideoEvent implements BouyomiEvent{
		public String videoID;
		public PlayVideoEvent(String videoID){
			this.videoID=videoID;
		}
	}
	public static class PlayVideoTitleEvent implements BouyomiEvent{
		public String title;
		public PlayVideoTitleEvent(String s){
			title=s;
		}
	}
	public static synchronized boolean playTube(BouyomiConection bc,String videoID) {
		if(videoID.indexOf('<')>=0||videoID.indexOf('>')>=0||videoID.indexOf('?')>=0)return false;
		if(System.currentTimeMillis()-lastPlayDate<5000) {
			System.out.println("前回の再生から5秒以内には再生できませんID="+videoID);
			if(bc!=null)bc.addTask.add("前回の再生から5秒以内には再生できません");
			return false;
		}
		if(videoID.indexOf(' ')>=0||videoID.indexOf('　')>=0) {
			videoID=videoID.trim();
		}
		/*
		int index=videoID.indexOf('&');
		String vid=videoID;
		if(index>0)vid=videoID.substring(0,index);
		if("v=grrX9elpi_A".equals(vid)||"v=15E9PJIZUwQ".equals(vid)) {
			System.out.println("再生禁止="+vid);
			if(bc!=null)bc.addTask.add("再生が禁止されています");
			return false;
		}
		*/
		try{
			nowPlayVideo=true;
			if(DefaultVol>=0)VOL=DefaultVol;
			lastPlayDate=System.currentTimeMillis();
			//videoID=URLEncoder.encode(videoID,"utf-8");//これ使うと動かない
			URL url=new URL("http://"+video_host+"/operation.html?"+videoID+"&vol="+VOL);
			//System.out.println(url.toString());
			url.openStream().close();
			lastPlay=videoID;
			BouyomiProxy.module.event(new PlayVideoEvent(videoID));
			if(bc!=null&&bc.user!=null&&!bc.user.isEmpty())lastPlayUser=bc.user;
			else lastPlayUser=null;
			if(playHistory.size()>=maxHistory){
				playHistory.remove(maxHistory-1);
			}
			playHistory.add(0,videoID);
			try{
				FileOutputStream fos=new FileOutputStream(HistoryFile,true);//追加モードでファイルを開く
				try{
					String d=new SimpleDateFormat("yyyy/MM/dd HH時mm分ss秒").format(new Date());
					StringBuilder s=new StringBuilder(videoID);
					s.append("\t再生時刻").append(d);
					if(bc!=null&&bc.user!=null) {
						s.append("\t").append(bc.user);
						if(bc.userid!=null)s.append("\t").append(bc.userid);
						else s.append("\t-");
					}
					s.append("\n");
					fos.write(s.toString().getBytes(StandardCharsets.UTF_8));//改行文字を追加してバイナリ化
				}finally {
					fos.close();
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
			pool.execute(new Runnable() {
				@Override
				public void run(){
					checkError();
					operation("play");
				}
			});
			pool.execute(new Runnable() {
				@Override
				public void run(){
					checkTitle();
				}
			});
			return true;
		}catch(IOException e){
			if(bc!=null)bc.addTask.add("再生プログラムとの通信に問題が発生しました");
			//System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	public static void checkTitle() {
		try{
			Thread.sleep(1000);
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		for(int i=0;i<5;i++) {
			try{
				Thread.sleep(500);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			String s=getLine("GETtitle=0");
			if(s!=null&&!s.isEmpty()&&!s.equals(lastPlay)) {
				BouyomiProxy.module.event(new PlayVideoTitleEvent(s));
				System.out.println("動画タイトル："+s);
				DiscordAPI.chatDefaultHost("/動画タイトル："+s);
				break;
			}
		}
	}
	public static void checkError() {
		for(int i=0;i<5;i++) {
			try{
				Thread.sleep(500);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			String s=getLine("GETerror=0");
			if(s!=null)try {
				int ec=Integer.parseInt(s);
				if(ec>0&&ec!=2) {
					System.out.println("動画再生エラー="+ec+"動画ID="+lastPlay);
					String c=Integer.toString(ec);
					StringBuilder dis=new StringBuilder("再生エラー");
					dis.append(c);
					switch(ec) {
						case 2:
							dis.append("\n/*リクエストに無効なパラメータ値が含まれています(参考)");
							break;
						case 5:
							dis.append("\n/*Youtubeのiframeプレイヤーでエラーが発生しました(参考)");
							break;
						case 100:
							dis.append("\n/*動画が見つかりません。削除されているか非公開に設定されているかもしれません(参考)");
							break;
						case 101:
						case 150:
							dis.append("\n/*動画の所有者が、埋め込み動画プレーヤーでの再生を許可していません(参考)");
							dis.append("\nhttps://atuwa.github.io/TubePlay4e/localserver/test.html で再生可能か確認できます");
							break;
						case 2500:
							dis.append("\n/*動画情報取得がタイムアウトしました");
							dis.append("\nhttps://atuwa.github.io/TubePlay4e/localserver/test.html で再生可能か確認できます");
							break;
					}
					if(!DiscordAPI.chatDefaultHost(dis.toString())) {
						BouyomiProxy.talk(BouyomiProxy.proxy_port,"再生エラー"+c);
					}
					break;
				}
			}catch(NumberFormatException e) {

			}
		}
	}
	public static synchronized int getVol(){
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
	public static synchronized String getLine(String op) {
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
				if(br!=null)br.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		return null;
	}
	public static synchronized boolean play(BouyomiConection bc,String url) {
		if(url.indexOf("https://www.youtube.com/")==0||
				url.indexOf("https://m.youtube.com/")==0||
				url.indexOf("https://youtube.com/")==0||
				url.indexOf("http://www.youtube.com/")==0||
				url.indexOf("http://m.youtube.com/")==0||
				url.indexOf("http://youtube.com/")==0) {
			String vid=extract(url,"v");
			String lid=extract(url,"list");
			if(vid!=null)return playTube(bc, vid);
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
				return playTube(bc, lid);
			}else{
				bc.addTask.add("URLを解析できませんでした");
				return false;
			}
		}else if(url.indexOf("https://youtu.be/")==0||url.indexOf("http://youtu.be/")==0) {
			int end=url.indexOf('?');
			String vid;
			if(end>=0)vid=url.substring(17,end);
			else vid=url.substring(17);
			return playTube(bc, "v="+vid);
		}else if(url.indexOf("v=")==0) {
			return playTube(bc, url);
		}else if(url.indexOf("list=")==0) {
			return playTube(bc, url);
		}else if(url.indexOf("nico=")==0) {
			return playTube(bc, url);
		}else if(url.indexOf("sc=")==0) {
			return playTube(bc, url);
		}else{
			if(playNico(bc, url, "sm","so","nm"))return true;
			Matcher scm = Pattern.compile("//api.soundcloud.com/tracks/[0-9]++").matcher(url);
			if(scm.find()) {
				String s=scm.group();
				Matcher scm2 = Pattern.compile("[0-9]++").matcher(s);
				if(scm2.find()) {
					url=scm2.group();
					//System.out.println("SC ID="+url);
					return playTube(bc, "sc="+url);
				}
			}
			bc.addTask.add("動画アイディーを抽出できませんでした");
			System.err.println("URL解析失敗="+url);
		}
		return false;
	}
	public static boolean playNico(BouyomiConection bc,String url,String... sm) {
		for(String s:sm) {
			Matcher m = Pattern.compile(s+"[0-9]++").matcher(url);
			if(m.find())return playTube(bc, "nico="+m.group());
		}
		return false;
	}
	public static synchronized String statusAllJson() {
		StringBuilder sb=new StringBuilder(64);//
		sb.append("{\n");
		String last;
		if(lastPlay!=null)last="\""+lastPlay+"\"";
		else last=lastPlay;
		sb.append("\"lastPlay\":").append(last).append(",\n");
		sb.append("\"stopTime\":").append(stopTime).append(",\n");
		sb.append("\"DefaultVol\":").append(DefaultVol).append(",\n");
		sb.append("\"Vol\":").append(VOL).append(",\n");
		sb.append("\"lastPlayDate\":").append(lastPlayDate).append(",\n");
		String title=getTitle();
		if(title!=null)title="\""+title+"\"";
		sb.append("\"lastPlayTitle\":").append(title).append("\n");
		sb.append("}\n");
		return sb.toString();
	}
	public static String getTitle() {
		String s=getLine("GETtitle=0");
		if(s==null||s.isEmpty()||s.equals(lastPlay))return null;
		return s;
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
	public static synchronized boolean operation(String op) {
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
		}else if(id.indexOf("nico=")==0){//ニコニコ
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
						if(nowPlayVideo&&System.currentTimeMillis()-BouyomiProxy.lastComment>stopTime) {
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
