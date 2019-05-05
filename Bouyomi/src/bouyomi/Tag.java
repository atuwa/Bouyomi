package bouyomi;

import static bouyomi.BouyomiProxy.*;
import static bouyomi.TubeAPI.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Tag{
	public BouyomiConection con;
	public boolean isTagTrim=true;
	public Tag(BouyomiConection bc) {
		con=bc;
	}
	/**自作プロキシの追加機能*/
	public void call() {
		if(con.text.equals("!help")) {
			DiscordAPI.chatDefaultHost("説明 https://github.com/atuwa/Bouyomi/wiki");
			con.text="";
			return;
		}
		if(module!=null)module.call(this);
		Counter.count(this);
		BOT.tag(this);
		String tag=getTag("平仮名変換");
		if(tag!=null) {
			Config.put("平仮名変換",tag);
			if(tag.equals("有効")) {
				Japanese.active=true;
				con.addTask.add("平仮名変換機能を有効にしました");
			}else if(tag.equals("無効")) {
				Japanese.active=false;
				con.addTask.add("平仮名変換機能を無効にしました");
			}
		}
		tag=getTag("強制終了");
		if(tag!=null) {
			Pass.exit(tag);
		}
		tag=getTag("自動停止時間");
		if(tag!=null) {
			if(isAdmin()){
				if(tag.isEmpty())TubeAPI.stopTime=480000;
				else try {
					TubeAPI.stopTime=Integer.parseInt(tag);
				}catch(NumberFormatException nfe) {
					StringWriter sw=new StringWriter();
					nfe.printStackTrace(new PrintWriter(sw));
					DiscordAPI.chatDefaultHost(sw.toString());
				}
				DiscordAPI.chatDefaultHost("自動停止時間を"+TubeAPI.stopTime+"msにしました");
			}else DiscordAPI.chatDefaultHost("権限がありません");
		}
		tag=getTag("ユーザID","ユーザＩＤ","ユーザーＩＤ","ユーザーID");
		if(tag!=null) {
			if(!tag.isEmpty()) {
				String id=Counter.getUserID(tag);
				System.out.println("ID取得「"+tag+"」のID="+id);
				if(con.mute);
				else if(id==null)DiscordAPI.chatDefaultHost("取得失敗");
				else DiscordAPI.chatDefaultHost("/"+id);
			}else {
				System.out.println("ID取得「"+con.user+"」のID="+con.userid);
				if(con.mute);
				else if(con.userid==null)DiscordAPI.chatDefaultHost("取得失敗");
				else DiscordAPI.chatDefaultHost("/"+con.userid);
			}
		}
		music();
		if(video_host!=null) {//再生サーバが設定されている時
			video();
		}
		Question.tag(this,con);
		if(module!=null)module.postcall(this);
	}
	public void music() {
		String tag=getTag("音楽再生");
		if(tag!=null) {
			String em;
			if(tag.isEmpty()) {
				MusicPlayerAPI.play();
				em="続きを再生します。";
			}else {
				MusicPlayerAPI.play(tag);
				em="音楽ファイルを再生します。";
			}
			float vol=MusicPlayerAPI.nowVolume();
			if(vol>=0)em+="音量は"+vol+"です";
			con.addTask.add(em);
		}
		tag=getTag("音楽音量");
		if(tag!=null) {
			if(tag.isEmpty()) {
				float vol=MusicPlayerAPI.nowVolume();
				con.addTask.add("音量は"+vol+"です");
				System.out.println("音楽音量"+vol);//ログに残す
			}else try{
				float vol=Float.parseFloat(tag);
				float Nvol=-10;
				switch(tag.charAt(0)){
					case '+':
					case '-':
					case 'ー':
						Nvol=MusicPlayerAPI.nowVolume();//+記号で始まる時今の音量を取得
				}
				if(Nvol==-1) {
					con.addTask.add("音量を変更できませんでした");//失敗した時これを読む
				}else {
					if(Nvol>=0)vol=Nvol+vol;//音量が取得させていたらそれに指定された音量を足す
					if(vol>100)vol=100;//音量が100以上の時100にする
					else if(vol<0)vol=0;//音量が0以下の時0にする
					if(MusicPlayerAPI.setVolume(vol)>=0)con.addTask.add("音量を"+vol+"にします");//動画再生プログラムにコマンド送信
					else con.addTask.add("音量を変更できませんでした");//失敗した時これを読む
					System.out.println(con.addTask.get(con.addTask.size()-1));//ログに残す
				}
			}catch(NumberFormatException e) {

			}
		}
		tag=getTag("音楽停止");
		if(tag!=null) {
			MusicPlayerAPI.stop();
			con.addTask.add("音楽を停止します。");
		}
	}
	/**動画再生機能*/
	public void video() {
		String tag=getTag("動画再生");
		if(tag!=null) {//動画再生
			//System.out.println(text);//ログに残す
			if(tag.isEmpty()) {
				if(operation("play")){
					String em="つづきを再生します。";
					int vol=getVol();
					if(vol>=0)em+="音量は"+vol+"です";
					con.addTask.add(em);
				}
			}else{
				System.out.println("動画再生（"+tag+")");//ログに残す
				if(play(con, tag)) {
					String em="動画を再生します。";
					int vol=DefaultVol<0?VOL:DefaultVol;
					if(vol>=0)em+="音量は"+vol+"です";
					con.addTask.add(em);
				}else con.addTask.add("動画を再生できませんでした");
			}
			if(con.text.isEmpty())return;//1文字も残ってない時は終わり
		}
		tag=getTag("動画タイトル");
		if(tag!=null) {
			String s=getTitle();
			if(con.mute)System.out.println(s);
			else if(s==null) {
				DiscordAPI.chatDefaultHost("/動画タイトルが取得できませんでした");
			}else DiscordAPI.chatDefaultHost("/動画タイトル："+s);
		}
		tag=getTag("動画URL");
		if(tag==null)tag=getTag("動画ＵＲＬ");//全角英文字
		if(tag!=null) {
			if(lastPlay==null)con.addTask.add("再生されていません");//再生中の動画情報がない時
			else if(tag.isEmpty()) {//0文字
				String url=IDtoURL(lastPlay);
				if(url==null)con.addTask.add("非対応形式です");
				else{
					if(lastPlayUser!=null)url="再生者："+lastPlayUser+"\n"+url;
					if(con.mute)System.out.println(url);
					else DiscordAPI.chatDefaultHost(url);
				}
			}else{
				try {
					int dc=Integer.parseInt(tag);//取得要求数
					dc=Integer.min(dc,playHistory.size());//データ量と要求数の少ない方に
					if(dc>0) {
						StringBuilder sb=new StringBuilder();
						sb.append(dc).append("件取得します/*\n");
						for(int i=0;i<dc;i++) {
							String s=playHistory.get(i);
							String url=IDtoURL(s);
							if(url==null)url=s;
							sb.append(url).append("\n");
						}
						if(con.mute)System.out.println(sb.toString());
						else DiscordAPI.chatDefaultHost(sb.toString());
					}
				}catch(NumberFormatException e) {

				}
			}
			if(con.text.isEmpty())return;//1文字も残ってない時は終わり
		}
		tag=getTag("動画ID","動画ＩＤ");//全角英文字
		if(tag!=null) {
			if(lastPlay==null)con.addTask.add("再生されていません");//再生中の動画情報がない時
			else if(tag.isEmpty()) {//0文字
				String url=lastPlay;
				if(lastPlayUser!=null)url="再生者："+lastPlayUser+"\n"+url;
				if(con.mute) {
					System.out.println(url);
				}else DiscordAPI.chatDefaultHost("/"+url);
			}else{
				try {
					int dc=Integer.parseInt(tag);//取得要求数
					dc=Integer.min(dc,playHistory.size());//データ量と要求数の少ない方に
					if(dc>0) {
						StringBuilder sb=new StringBuilder();
						sb.append(dc).append("件取得します/*\n");
						for(int i=0;i<dc;i++) {
							String s=playHistory.get(i);
							sb.append(s).append("\n");
						}
						if(con.mute)System.out.println(sb.toString());
						else DiscordAPI.chatDefaultHost(sb.toString());
					}
				}catch(NumberFormatException e) {

				}
			}
		}
		tag=getTag("動画停止");
		if(tag!=null){//動画停止
			if("動画停止".equals(con.text))con.text="";
			System.out.println("動画停止");//ログに残す
			if(operation("stop")){
				TubeAPI.nowPlayVideo=false;
				con.addTask.add("動画を停止します");
			}else con.addTask.add("動画を停止できませんでした");
			return;
		}
		tag=getTag("動画音量","動画音声","音量調整","音量設定");
		if(tag!=null){//動画音量
			if(tag.isEmpty()) {
				String em;
				int vol=getVol();//音量取得。取得失敗した時-1
				if(vol<0)em="音量を取得できません";
				else em="音量は"+vol+"です";
				System.out.println(em);
				if(con.mute) {
					//DiscordAPI.chatDefaultHost("/"+em);
				}else if(!DiscordAPI.chatDefaultHost(em))con.addTask.add(em);
			}else{
				try{
					int Nvol=-10;
					switch(tag.charAt(0)){
						case '＋':
						case '－':
						case '+':
						case '-':
							tag=tag.replace('＋','+');
							tag=tag.replace('－','-');
							Nvol=getVol();//+記号で始まる時今の音量を取得
					}
					int vol=Integer.parseInt(tag);//要求された音量
					if(Nvol==-1) {
						con.addTask.add("音量を変更できませんでした。音量を取得できませんでした");//失敗した時これを読む
					}else {
						if(Nvol>=0)vol=Nvol+vol;//音量が取得させていたらそれに指定された音量を足す
						if(vol>100)vol=100;//音量が100以上の時100にする
						else if(vol<0)vol=0;//音量が0以下の時0にする
						System.out.println("動画音量"+vol);//ログに残す
						VOL=vol;//再生時に使う音量をこれにする
						if(operation("vol="+vol))con.addTask.add("音量を"+vol+"にします");//動画再生プログラムにコマンド送信
						else con.addTask.add("音量を変更できませんでした。通信に失敗しました");//失敗した時これを読む
					}
				}catch(NumberFormatException e) {
					con.addTask.add("音量を変更できませんでした。数値を解析できません");//失敗した時これを読む
				}
			}
			if(con.text.isEmpty())return;
		}
		tag=getTag("初期音量");
		if(tag!=null) {
			if(tag.isEmpty()) {
				String em;
				if(DefaultVol<0)em="デフォルトの音量は前回の動画の音量です";
				else em="デフォルトの音量は"+DefaultVol+"です";
				System.out.println(em);
				if(con.mute) {
					//DiscordAPI.chatDefaultHost("/"+em);
				}else if(!DiscordAPI.chatDefaultHost(em))con.addTask.add(em);
				//Discordに投稿出来た時はその投稿されたメッセージを読むから読み上げメッセージは空白
			}else {
				try{
					int vol=Integer.parseInt(tag);//要求された音量
					if(vol<0) {
						System.out.println("初期音量 前回の動画音量");//ログに残す
						con.addTask.add("前に再生した時の音量を使うように設定します");//取得失敗した時これを読む
						DefaultVol=-1;//再生時に使う音量をこれにする
						Config.put("初期音量",String.valueOf(DefaultVol));
					}else {
						if(vol>100)vol=100;//音量が100以上の時100にする
						System.out.println("初期音量"+vol);//ログに残す
						DefaultVol=vol;//再生時に使う音量をこれにする
						con.addTask.add("次に再生する時は"+DefaultVol+"で再生します");//成功した時これを読む
						Config.put("初期音量",String.valueOf(DefaultVol));
					}
				}catch(NumberFormatException e) {

				}
			}
			if(con.text.isEmpty())return;
		}
		tag=getTag("VideoStatus","動画情報");
		if(tag!=null) {
			//con.text="";
			DiscordAPI.chatDefaultHost(statusAllJson());
		}
		if(DiscordAPI.service_host!=null) {
			tag=getTag("最頻再生動画");
			if(tag!=null) {
				String s=most(0);
				if(s==null) {
					if(!con.mute)DiscordAPI.chatDefaultHost("取得失敗");
				}else {
					String id=s.substring(0,s.indexOf('('));
					s+="\n"+IDtoURL(id);
					System.out.println(s);
					if(!con.mute)DiscordAPI.chatDefaultHost("/"+s);
				}
			}
			tag=getTag("最頻再生者");
			if(tag!=null) {
				String s;
				if(tag.equals("ID")) {
					s=most(3);
					s="ID="+s+"Nick="+Counter.getUserName(s);
				}else s=most(2);
				System.out.println(s);
				if(con.mute);
				else if(s==null)DiscordAPI.chatDefaultHost("取得失敗");
				else DiscordAPI.chatDefaultHost("/"+s);
			}
		}
	}
	private String most(int index){
		if(index<0)return null;
		try {
			FileInputStream fis=new FileInputStream(new File(HistoryFile));
			InputStreamReader isr=new InputStreamReader(fis,StandardCharsets.UTF_8);
			BufferedReader br=new BufferedReader(isr);
			class Counter{
				public int count=1;
			}
			HashMap<String, Counter> co=new HashMap<String,Counter>();
			try {
				while(br.ready()) {
					String line=br.readLine();
					if(line==null)break;
					String[] arr=line.split("\t");
					if(index>=arr.length)continue;
					String key=arr[index];
					Counter v=co.get(key);
					if(v==null)co.put(key,new Counter());
					else v.count++;
				}
				String most=null;
				int most_i=0;
				for(String s:co.keySet()) {
					Counter v=co.get(s);
					if(v.count>most_i) {
						most_i=v.count;
						most=s;
					}
				}
				return most+"("+most_i+"回)";
			}finally{
				br.close();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public String getTag(String... key) {
		for(String s:key) {
			String t=getTag(s);
			if(t!=null)return t;
		}
		return null;
	}
	/**タグ取得*/
	public String getTag(String key) {
		if(con.text.length()<1)return null;
		if(con.text.equals(key))return "";
		int index=con.text.indexOf(key+"(");
		if(index<0)index=con.text.indexOf(key+"（");
		if(index<0)return null;//タグを含まない時
		int ki=con.text.indexOf(')');//半角
		int zi=con.text.indexOf('）');//全角
		if(ki<0)ki=zi;
		if(ki<0)return null;//閉じカッコが無い時
		if(ki<index+key.length()+1)return null;//閉じカッコの位置がおかしい時
		if(ki==index+key.length()+1) {
			removeTag(key,"");
			return "";//0文字
		}
		String tag=con.text.substring(index+key.length()+1,ki);
		//System.out.println("タグ取得k="+key+"v="+tag);
		removeTag(key,tag);
		return isTagTrim?tag.trim():tag;
	}
	public void removeTag(String tagName,String val) {
		//System.out.println("元データ　"+con.text);
		StringBuilder sb0=new StringBuilder(tagName);
		sb0.append("(").append(val);//これ半角しか削除できない
		String remove=sb0.toString();
		int index=con.text.indexOf(remove);
		//System.out.println("タグ消去　"+remove+"&index="+index);
		if(index<0) {
			StringBuilder sb1=new StringBuilder(tagName);
			sb1.append("（").append(val);//こっちで全角のカッコを処理
			remove=sb1.toString();
			index=con.text.indexOf(remove);
			//System.out.println("タグ消去　"+remove+"&index="+index);
			if(index<0)return;
		}
		StringBuilder sb=new StringBuilder();
		if(index>0)sb.append(con.text.substring(0,index));//タグで始まる時以外
		if(con.text.length()>index+remove.length())sb.append(con.text.substring(index+remove.length()+1));
		con.text=sb.toString();
		//System.out.println("タグ消去結果　"+con.text);
	}
	public boolean isAdmin(){
		return BouyomiProxy.admin.isAdmin(con.userid);
	}
}
