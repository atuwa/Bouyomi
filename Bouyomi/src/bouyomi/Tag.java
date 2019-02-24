package bouyomi;

import static bouyomi.BouyomiProxy.*;
import static bouyomi.TubeAPI.*;

import java.util.function.BiConsumer;

public class Tag{
	private BouyomiConection con;
	private String text;
	public String em;
	public Tag(BouyomiConection bc) {
		con=bc;
		text=con.text;
	}
	/**自作プロキシの追加機能*/
	public String bot() {
		if(text.equals("!help")) {
			DiscordAPI.chatDefaultHost("説明 https://github.com/atuwa/Bouyomi/wiki");
			return "";
		}
		if(text.indexOf("応答(")==0||text.indexOf("応答（")==0) {//自動応答機能を使う時
			//System.out.println(text);//ログに残す
			int ei=text.indexOf('=');
			if(ei<0)ei=text.indexOf('＝');
			int ki=text.lastIndexOf(')');
			int zi=text.lastIndexOf('）');
			if(ki<zi)ki=zi;
			if(ei>0&&ki>1) {
				String key=text.substring(3,ei);
				String val=text.substring(ei+1,ki);
				if(!key.equals(val)&&!isReturn(key)) {
					em=key+" には "+val+" を返します";
					if(key.indexOf("部分一致：")==0||key.indexOf("部分一致:")==0) {
						System.out.println("部分一致応答登録（"+key+"="+val+")");//ログに残す
						PartialMatchBOT.put(key.substring(5),val);
					}else{
						System.out.println("完全一致応答登録（"+key+"="+val+")");//ログに残す
						BOT.put(key,val);
					}
				}else {
					em="登録できません";
					System.out.println("応答登録不可（"+key+"="+val+")");//ログに残す
				}
			}
		}
		String tag=getTag("応答破棄");
		if(tag!=null) {//自動応答機能を使う時
			//System.out.println(text);//ログに残す
			if(BOT.remove(tag)!=null) {
				em=tag+" には応答を返しません";
				System.out.println("応答破棄（"+tag+")");//ログに残す
			}else em=tag+" には応答が設定されてません";
		}
		tag=getTag("応答一覧");
		if(tag!=null) {
			final StringBuilder sb=new StringBuilder("/");
			BOT.forEach(new BiConsumer<String,String>(){
				@Override
				public void accept(String key,String val){
					sb.append("key=").append(key).append(" val=").append(val).append("\n");
				}
			});
			PartialMatchBOT.forEach(new BiConsumer<String,String>(){
				@Override
				public void accept(String key,String val){
					sb.append("部分一致").append("key=").append(key).append(" val=").append(val).append("\n");
				}
			});
			DiscordAPI.chatDefaultHost(sb.toString());
		}
		tag=getTag("平仮名変換");
		if(tag!=null) {
			Config.put("平仮名変換",tag);
			if(tag.equals("有効")) {
				Japanese.active=true;
				em="平仮名変換機能を有効にしました";
			}else if(tag.equals("無効")) {
				Japanese.active=false;
				em="平仮名変換機能を無効にしました";
			}
		}
		Question.tag(this,con);
		tag=getTag("強制終了");
		if(tag!=null) {
			Pass.exit(tag);
		}
		music();
		if(video_host!=null) {//再生サーバが設定されている時
			video();
		}
		return text;
	}
	public void music() {
		String tag=getTag("音楽再生");
		if(tag!=null) {
			if(tag.isEmpty()) {
				MusicPlayerAPI.play();
				em="続きを再生します。";
			}else {
				MusicPlayerAPI.play(tag);
				em="音楽ファイルを再生します。";
			}
			float vol=MusicPlayerAPI.nowVolume();
			if(vol>=0)em+="音量は"+vol+"です";
		}
		tag=getTag("音楽音量");
		if(tag!=null) {
			if(tag.isEmpty()) {
				float vol=MusicPlayerAPI.nowVolume();
				em="音量は"+vol+"です";
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
					em="音量を変更できませんでした";//失敗した時これを読む
				}else {
					if(Nvol>=0)vol=Nvol+vol;//音量が取得させていたらそれに指定された音量を足す
					if(vol>100)vol=100;//音量が100以上の時100にする
					else if(vol<0)vol=0;//音量が0以下の時0にする
					if(MusicPlayerAPI.setVolume(vol)>=0)em="音量を"+vol+"にします";//動画再生プログラムにコマンド送信
					else em="音量を変更できませんでした";//失敗した時これを読む
					System.out.println(em);//ログに残す
				}
			}catch(NumberFormatException e) {

			}
		}
		tag=getTag("音楽停止");
		if(tag!=null) {
			MusicPlayerAPI.stop();
			em="音楽を停止します。";
		}
	}
	/**動画再生機能*/
	public void video() {
		String tag=getTag("動画再生");
		if(tag!=null) {//動画再生
			//System.out.println(text);//ログに残す
			if(tag.isEmpty()) {
				if(operation("play")){
					em="つづきを再生します。";
					int vol=getVol();
					if(vol>=0)em+="音量は"+vol+"です";
				}
			}else{
				System.out.println("動画再生（"+tag+")");//ログに残す
				if(play(con, tag)) {
					em="動画を再生します。";
					int vol=DefaultVol<0?VOL:DefaultVol;
					if(vol>=0)em+="音量は"+vol+"です";
				}else if(em==null)em="動画を再生できませんでした";
			}
			if(text.isEmpty())return;//1文字も残ってない時は終わり
		}
		tag=getTag("動画URL");
		if(tag==null)tag=getTag("動画ＵＲＬ");//全角英文字
		if(tag!=null) {
			if(lastPlay==null)em="再生されていません";//再生中の動画情報がない時
			else if(tag.isEmpty()) {//0文字
				em="";
				String url=IDtoURL(lastPlay);
				if(url==null)em="非対応形式です";
				else DiscordAPI.chatDefaultHost(url);
			}else{
				em="";
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
						DiscordAPI.chatDefaultHost(sb.toString());
					}
				}catch(NumberFormatException e) {

				}
			}
			if(text.isEmpty())return;//1文字も残ってない時は終わり
		}
		tag=getTag("動画ID","動画ＩＤ");//全角英文字
		if(tag!=null) {
			if(lastPlay==null)em="再生されていません";//再生中の動画情報がない時
			else if(tag.isEmpty()) {//0文字
				if(con.mute) {
					em="";
					System.out.println(lastPlay);
				}else DiscordAPI.chatDefaultHost("/"+lastPlay);
			}else{
				em="";
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
						DiscordAPI.chatDefaultHost(sb.toString());
					}
				}catch(NumberFormatException e) {

				}
			}
		}
		tag=getTag("動画停止");
		if( ( tag!=null&&tag.isEmpty() ) ||"動画停止".equals(text)) {//動画停止
			System.out.println("動画停止");//ログに残す
			if(operation("stop")){
				TubeAPI.nowPlayVideo=false;
				em="動画を停止します";
			}else em="動画を停止できませんでした";
			return;
		}
		tag=getTag("動画音量","動画音声","音量調整","音量設定");
		if(tag!=null){//動画音量
			if(tag.isEmpty()) {
				int vol=getVol();//音量取得。取得失敗した時-1
				if(vol<0)em="音量を取得できません";
				else em="音量は"+vol+"です";
				System.out.println(em);
				if(!con.mute&&DiscordAPI.chatDefaultHost(em))em="";
			}else{
				try{
					int Nvol=-10;
					switch(tag.charAt(0)){
						case '+':
						case '-':
						case 'ー':
							Nvol=getVol();//+記号で始まる時今の音量を取得
					}
					int vol=Integer.parseInt(tag);//要求された音量
					if(Nvol==-1) {
						em="音量を変更できませんでした";//失敗した時これを読む
					}else {
						if(Nvol>=0)vol=Nvol+vol;//音量が取得させていたらそれに指定された音量を足す
						if(vol>100)vol=100;//音量が100以上の時100にする
						else if(vol<0)vol=0;//音量が0以下の時0にする
						System.out.println("動画音量"+vol);//ログに残す
						VOL=vol;//再生時に使う音量をこれにする
						if(operation("vol="+vol))em="音量を"+vol+"にします";//動画再生プログラムにコマンド送信
						else em="音量を変更できませんでした";//失敗した時これを読む
					}
				}catch(NumberFormatException e) {

				}
			}
			if(text.isEmpty())return;
		}
		tag=getTag("初期音量");
		if(tag!=null) {
			if(tag.isEmpty()) {
				if(DefaultVol<0)em="デフォルトの音量は前回の動画の音量です";
				else em="デフォルトの音量は"+DefaultVol+"です";
				System.out.println(em);
				if(!con.mute&&DiscordAPI.chatDefaultHost(em))em="";//スラッシュで始まる場合かDiscordに投稿できない時は読み上げる
				//Discordに投稿出来た時はその投稿されたメッセージを読むから読み上げメッセージは空白
			}else {
				try{
					int vol=Integer.parseInt(tag);//要求された音量
					if(vol<0) {
						System.out.println("初期音量 前回の動画音量");//ログに残す
						em="前に再生した時の音量を使うように設定します";//取得失敗した時これを読む
						DefaultVol=-1;//再生時に使う音量をこれにする
						Config.put("初期音量",String.valueOf(DefaultVol));
					}else {
						if(vol>100)vol=100;//音量が100以上の時100にする
						System.out.println("初期音量"+vol);//ログに残す
						DefaultVol=vol;//再生時に使う音量をこれにする
						em="次に再生する時は"+DefaultVol+"で再生します";//成功した時これを読む
						Config.put("初期音量",String.valueOf(DefaultVol));
					}
				}catch(NumberFormatException e) {

				}
			}
			if(text.isEmpty())return;
		}
		tag=getTag("VideoStatus");
		if(tag!=null) {
			em="";
			DiscordAPI.chatDefaultHost(statusAllJson());
		}
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
		int index=text.indexOf(key+"(");
		if(index<0)index=text.indexOf(key+"（");
		if(index<0)return null;//タグを含まない時
		int ki=text.indexOf(')');//半角
		int zi=text.indexOf('）');//全角
		if(ki<zi)ki=zi;
		if(ki<0)return null;//閉じカッコが無い時
		if(ki<index+key.length()+1)return null;//閉じカッコの位置がおかしい時
		if(ki==index+key.length()+1)return "";//0文字
		String tag=text.substring(index+key.length()+1,ki).trim();//スペースは削除
		removeTag(key,tag);
		return tag;
	}
	public void removeTag(String tagName,String val) {
		StringBuilder sb0=new StringBuilder(tagName);
		sb0.append("(").append(val);//これ半角しか削除できない
		String remove=sb0.toString();
		int index=text.indexOf(remove);
		if(index<0) {
			StringBuilder sb1=new StringBuilder(tagName);
			sb0.append("（").append(val);//こっちで全角のカッコを処理
			remove=sb1.toString();
			index=text.indexOf(remove);
			if(index<0)return;
		}
		StringBuilder sb=new StringBuilder();
		if(index>0)sb.append(text.substring(0,index));//タグで始まる時以外
		if(text.length()>index+remove.length())sb.append(text.substring(index+remove.length()+1));
		text=sb.toString();
	}
}
