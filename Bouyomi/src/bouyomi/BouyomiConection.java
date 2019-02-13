package bouyomi;

import static bouyomi.BouyomiProxy.*;
import static bouyomi.TubeAPI.*;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BouyomiConection implements Runnable{

	/**このインスタンスの接続先*/
	private Socket soc;
	//コンストラクタ
	/**接続単位で別のインスタンス*/
	public BouyomiConection(Socket s){
		soc=s;
	}
	private String text=null;
	private Object addTask=null;
	private char fb;//最初の文字
	private int len;
	public String em=null;//置き換えメッセージ
	private int type;
	/**受け取った文字データ*/
	private ByteArrayOutputStream baos2;
	/**送信データ入れ*/
	private ByteArrayOutputStream baos;
	public boolean mute;
	private void read() throws IOException {
		InputStream is=soc.getInputStream();//Discord取得ソフトから読み込むストリーム
		int ch1=is.read();//コマンドバイトを取得
		int ch2=is.read();
		if((ch1|ch2)<0){//コマンドバイトを取得できない時終了
			//System.out.println("datatype ch1"+ch1+"ch2"+ch2);
			return;
		}
		short s=(short) ((ch2<<8)+(ch1<<0));
		if(s!=1){//読み上げコマンド以外の時
			if(s==0x10||s==0x20||s==0x30||s==0x40){//応答が必要ないコマンドの時
				send(bouyomi_port,new byte[] { (byte) ch1, (byte) ch2 });//棒読みちゃんに送信
				return;
			}
			System.out.println("datatype"+s);
			throw new IOException();//対応していないコマンドは例外を出して終了
		}
		baos=new ByteArrayOutputStream();//送信データバッファ
		baos.write(ch1);//コマンド指定バイトを送信データバッファに書き込む
		baos.write(ch2);
		byte[] d=new byte[8];
		int len=is.read(d);//この段階では変数lenは読み込みバイト数
		type=is.read();//文字コード読み込み
		if(len!=8||type<0){//読み込みバイト数が足りない時
			System.out.println("notLen9("+len+")");
			for(int i=0;i<len;i++)System.out.println(d[i]);
			throw new IOException();
		}
		baos.write(d);//その他パラメータを送信データバッファに書き込み
		ch1=is.read();//文字数を受信
		ch2=is.read();
		int ch3=is.read();
		int ch4=is.read();
		if((ch1|ch2|ch3|ch4)<0){//文字数のデータが足りない時
			System.out.println("DataLen");
			throw new IOException("DataLen");//例外を出して終了
		}
		//ここで変数lenはバイト数になる
		len=((ch1<<0)+(ch2<<8)+(ch3<<16)+(ch4<<24));//文字数データから数値に
		fb=(char) is.read();//最初の文字を取得
		baos2=new ByteArrayOutputStream();//メッセージバイナリ書き込み先
		baos2.write(fb);//最初の文字をメッセージバイナリバッファに
		this.len=len;
		for(int i=1;i<len;i++){//メッセージデータ取得
			int j=is.read();
			if(j<0){//すべてのメッセージを取得できない時
				System.out.println("DataRead");
				throw new IOException("DataRead");//例外を出して終了
			}
			baos2.write(j);
		}
		if(d[7]==0)text=baos2.toString("utf-8");//UTF-8でデコード
		else if(d[7]==1)text=baos2.toString("utf-16");//UTF-16でデコード
	}
	private void replace() throws IOException {
		//System.out.println("len="+len);
		bot(text);
		if(em!=null)return;
		//text=text.replaceAll("https?://[\\x21-\\x7F]++","URL省略");
		{//URL省略処理
			//URL判定基準を正規表現で指定
			Matcher m=Pattern.compile("https?://[\\x21-\\x7F]++").matcher(text);
			m.reset();
			boolean result = m.find();
	        if (result) {
				int co=0;//URLの数
	            do {
	            	co++;
	                result = m.find();
	            } while (result);
		        m.reset();
		        result = m.find();
	        	boolean b=true;
	            StringBuffer sb = new StringBuffer();
	            do {
	                if(b) {//初回
	                	b=false;
	                	if(co==1)m.appendReplacement(sb, "URL省略");//対象が一つの時
	                	else m.appendReplacement(sb, co+"URL省略");
	                }else m.appendReplacement(sb, "");//2回目以降
	                result = m.find();
	            } while (result);
	            m.appendTail(sb);
	            text=sb.toString();
	        }
		}
		if(em==null)ContinuationOmitted();//文字データが取得できてメッセージが書き換えられていない時
		if(text.length()>=90){//長文省略基準90文字以上
			em="長文省略";
			System.out.println("長文省略("+text.length()+"文字)");
			return;
		}
		Japanese.trans(text);
		//文字データが取得できた時
		//text=text.toUpperCase(Locale.JAPANESE);//大文字に統一する時
		if(text.indexOf("忘却(")>=0||text.toUpperCase().indexOf("(FORGET")>=0) {//忘却機能を使おうとした時
			System.out.println(text);//ログに残す
			em="b)現在忘却機能は使えません";//代わりに「使えない」と言う
		}else if(text.indexOf("教育(")>=0||text.indexOf("教育（")>=0) {//教育機能を使おうとした時
			System.out.println(text);//ログに残す
		}else if(text.indexOf("機能要望")>=0){//「機能要望」が含まれる時
			System.out.println(text);//ログに残す
			addTask="要望リストに記録しました";//残した事を追加で言う
			try{
				FileOutputStream fos=new FileOutputStream("Req.txt",true);//追加モードでファイルを開く
				try{
					fos.write((text+"\n").getBytes(StandardCharsets.UTF_8));//改行文字を追加してバイナリ化
				}catch(IOException e) {
					e.printStackTrace();
				}finally {
					fos.close();
				}
			}catch(IOException e) {
				e.printStackTrace();
				addTask="要望リストに記録できませんでした";//失敗した事を追加で言う
			}
		}
		BOT.forEach(new BiConsumer<String,String>(){
			@Override
			public void accept(String key,String val){
				if(addTask!=null)return;
				int bi=key.indexOf("部分一致:");
				if(bi!=0)bi=key.indexOf("部分一致：");
				if(bi==0&&key.length()>5) {
					key=key.substring(5);
					if(text.indexOf(key)>=0) {//読み上げテキストにキーが含まれている時
						System.out.println("BOT応答キー =部分一致："+key);//ログに残す
						if(DiscordAPI.chatDefaultHost(val))addTask="";
						else addTask=val;//追加で言う
					}
				}else 	if(text.equals(key)) {//読み上げテキストがキーに一致した時
					System.out.println("BOT応答キー ="+key);//ログに残す
					if(DiscordAPI.chatDefaultHost(val))addTask="";
					else addTask=val;//追加で言う
				}
			}
		});
	}
	/**連続短縮*/
	private void ContinuationOmitted() throws IOException {
		type=0;//文字コードをUTF-8に設定
		baos2.reset();//読み込んだメッセージのバイナリを破棄
		//メッセージバイナリバッファにUTF-8で書き込む
		OutputStreamWriter sw=new OutputStreamWriter(baos2,StandardCharsets.UTF_8);
		BufferedWriter bw=new BufferedWriter(sw);//文字バッファ
		char lc=0;//最後に追加した文字
		char cc=0;//連続カウント(9以下)
		byte comment=0;
		//int clen=0;
		HashMap<Character, Short> counter=new HashMap<Character,Short>();
		if(type==0)counter=null;
		for(int i=0;i<text.length();i++) {//文字データを1文字ずつ読み込む
			char r=text.charAt(i);//現在位置の文字を取得
			//連続カウントが9以上で次の文字が最後に書き込まれた文字と一致した場合次へ
			if(cc>8&&r==lc) {
				counter=null;
				continue;
			}
			if(r==lc)cc++;//次の文字が最後に書き込まれた文字と一致した場合連続カウントを増やす
			else cc=0;//次の文字が最後に書き込まれた文字と異なる場合カウントをリセットする
			if(comment==0&&(r=='/'||r=='／')) {//C言語風コメントアウト
				comment=1;
			}else if(comment==1) {
				if(r=='*'||r=='＊')comment=-1;
				else comment=0;
			}else if(comment==-1&&(r=='*'||r=='＊'))comment=-2;
			else if(comment==-2) {
				if(r=='/'||r=='／') {
					comment=0;
					continue;
				}else comment=-1;
			}
			if(comment<0)continue;
			/*
			if(lc=='。'||lc=='、')clen++;
			if(clen>10&&len>100) {
				em="省略";
				break;
			}
			*/
			lc=r;//最後に書き込まれた文字を次に書き込む文字に設定する
			bw.write(r);//文字バッファに書き込む
		}
		if(counter!=null){
			bw.flush();//バッファの内容をすべてバイナリに変換
			text=baos2.toString("utf-8");//UTF-8でデコード
			baos2.reset();//読み込んだメッセージのバイナリを破棄
			for(int i=0;i<text.length();i++) {//文字データを1文字ずつ読み込む
				char r=text.charAt(i);//現在位置の文字を取得
				Character c=Character.valueOf(r);
				Short v=counter.get(c);
				if(v==null)counter.put(c,(short) 1);
				else{
					short val=(short) (v.shortValue()+1);
					if(val>6)continue;
					else counter.put(c,val);
				}
				bw.write(r);//文字バッファに書き込む
			}
		}
		//System.out.println("clen="+clen);
		bw.flush();//バッファの内容をすべてバイナリに変換
		text=baos2.toString("utf-8");//UTF-8でデコード
	}
	public void run(){
		//System.out.println("接続");
		//long start=System.nanoTime();//TODO 処理時間計測用
		try{
			read();//受信処理
			lastComment=System.currentTimeMillis();
			if(fb=='/'||fb=='\\'){//最初の文字がスラッシュの時は終了
				//System.out.println("スラッシュで始まる");
				mute=true;
				if(text!=null) {
					text=text.substring(1);
					bot(text);
				}
				return;
			}
			if(text!=null)replace();
			else if(len>=250)em="長文省略";
			if(em!=null) {//メッセージが書き換えられていた時
				type=0;//文字コードをUTF-8に設定
				baos2.reset();//読み込んだメッセージのバイナリを破棄
				if(!em.isEmpty()) {//文字がある場合
					byte[] t=em.getBytes(StandardCharsets.UTF_8);//UTF-8でバイナリ化
					baos2.write(t);//バイナリデータをメッセージバイナリに書き込み
				}
			}
			//if(text!=null)System.out.println(text);
			len=baos2.size();//メッセージバイナリのバイト数を取得
			baos.write(type);//文字コードを送信データに追加
			baos.write((len >>> 0) & 0xFF);//テキストの文字数を送信データに追加
			baos.write((len >>> 8) & 0xFF);
			baos.write((len >>>  16) & 0xFF);
			baos.write((len >>>  24) & 0xFF);
			baos2.writeTo(baos);//メッセージバイナリデータを送信データに追加
			//System.out.println("W"+baos.size());
			send(bouyomi_port,baos.toByteArray());//作ったデータを送信
			//System.out.println("Write");
		}catch(IOException e){
			e.printStackTrace();//例外が発生したらログに残す
		}finally{//切断は確実に
			try{
				soc.close();//Discord受信ソフトから切断
			}catch(IOException e1){
				e1.printStackTrace();
			}
			//System.out.println((System.nanoTime()-start)+"ns");//TODO 処理時間計測用
		}
		if(addTask!=null) {//追加で言うデータがある時
			if(addTask instanceof ArrayList) {//データがArrayListの時
				for(Object s:(ArrayList<?>)addTask)talk(bouyomi_port,s.toString());//すべて送信
			}else if(!addTask.toString().isEmpty())talk(bouyomi_port,addTask.toString());//送信
		}
	}
	/**自作プロキシの追加機能*/
	public String bot(String text) {
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
				em=key+" には "+val+" を返します";
				System.out.println("応答登録（"+key+"="+val+")");//ログに残す
				BOT.put(key,val);
			}
		}else if(text.indexOf("応答破棄(")==0||text.indexOf("応答破棄（")==0) {//自動応答機能を使う時
			//System.out.println(text);//ログに残す
			int ei=text.lastIndexOf(')');
			int zi=text.lastIndexOf('）');
			if(ei<zi)ei=zi;
			if(ei>5) {
				String key=text.substring(5,ei);
				em=key+" には応答を返しません";
				System.out.println("応答破棄（"+key+")");//ログに残す
				BOT.remove(key);
			}
		}else if(video_host!=null) {//再生サーバが設定されている時
			video(text);
		}
		return null;
	}
	/**動画再生機能*/
	public void video(String text) {
		String tag=getTag(text,"動画再生");
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
				if(play(this, tag)) {
					em="動画を再生します。";
					int vol=DefaultVol<0?VOL:DefaultVol;
					if(vol>=0)em+="音量は"+vol+"です";
				}else if(em==null)em="動画を再生できませんでした";
			}
			//タグの部分を削除
			text=removeTag(text,"動画再生",tag);
			if(text.isEmpty())return;//1文字も残ってない時は終わり
		}
		tag=getTag(text,"動画URL");
		if(tag==null)tag=getTag(text,"動画ＵＲＬ");//全角英文字
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
			text=removeTag(text,"動画URL",tag);
			text=removeTag(text,"動画ＵＲＬ",tag);
			if(text.isEmpty())return;//1文字も残ってない時は終わり
		}
		tag=getTag(text,"動画ID");
		if(tag==null)tag=getTag(text,"動画ＩＤ");//全角英文字
		if(tag!=null) {
			if(lastPlay==null)em="再生されていません";//再生中の動画情報がない時
			else if(tag.isEmpty()) {//0文字
				if(mute) {
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
			text=removeTag(text,"動画ID",tag);
			text=removeTag(text,"動画ＩＤ",tag);
		}
		tag=getTag(text,"動画停止");
		if( ( tag!=null&&tag.isEmpty() ) ||"動画停止".equals(text)) {//動画停止
			System.out.println("動画停止");//ログに残す
			if(operation("stop")){
				TubeAPI.nowPlayVideo=false;
				em="動画を停止します";
			}else em="動画を停止できませんでした";
			text=removeTag(text,"動画停止","");
			return;
		}
		tag=getTag(text,"動画音量");
		if(tag==null)tag=getTag(text,"動画音声");
		if(tag==null)tag=getTag(text,"音量調整");
		if(tag==null)tag=getTag(text,"音量設定");
		if(tag!=null){//動画音量
			if(tag.isEmpty()) {
				int vol=getVol();//音量取得。取得失敗した時-1
				if(vol<0)em="音量を取得できません";
				else em="音量は"+vol+"です";
				System.out.println(em);
				if(!mute&&DiscordAPI.chatDefaultHost(em))em="";
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
			text=removeTag(text,"動画音量",tag);
			text=removeTag(text,"動画音声",tag);
			text=removeTag(text,"音量調整",tag);
			text=removeTag(text,"音量設定",tag);
			if(text.isEmpty())return;
		}
		tag=getTag(text,"初期音量");
		if(tag!=null) {
			if(tag.isEmpty()) {
				em="デフォルトの音量は"+DefaultVol+"です";
				System.out.println(em);
				if(!mute&&DiscordAPI.chatDefaultHost(em))em="";//スラッシュで始まる場合かDiscordに投稿できない時は読み上げる
				//Discordに投稿出来た時はその投稿されたメッセージを読むから読み上げメッセージは空白
			}else {
				try{
					int vol=Integer.parseInt(tag);//要求された音量
					if(vol<0) {
						System.out.println("初期音量 前回の動画音量");//ログに残す
						em="前に再生した時の音量を使うように設定します";//取得失敗した時これを読む
						DefaultVol=-1;//再生時に使う音量をこれにする
					}else {
						if(vol>100)vol=100;//音量が100以上の時100にする
						System.out.println("初期音量"+vol);//ログに残す
						DefaultVol=vol;//再生時に使う音量をこれにする
						em="次に再生する時は"+DefaultVol+"で再生します";//成功した時これを読む
					}
				}catch(NumberFormatException e) {

				}
				text=removeTag(text,"初期音量",tag);
			}
			if(text.isEmpty())return;
		}
	}
	public String removeTag(String text,String tagName,String val) {
		StringBuilder sb=new StringBuilder();
		StringBuilder sb0=new StringBuilder(tagName);
		sb0.append("(").append(val);//これ半角しか削除できない
		String remove=sb0.toString();
		int index=text.indexOf(remove);
		if(index<0) {
			StringBuilder sb1=new StringBuilder(tagName);
			sb0.append("（").append(val);//こっちで全角のカッコを処理
			remove=sb1.toString();
			index=text.indexOf(remove);
			if(index<0)return text;
		}
		if(index>0)sb.append(text.substring(0,index));//タグで始まる時以外
		if(text.length()>index+remove.length()+1)sb.append(text.substring(index+remove.length()+1));
		return sb.toString();
	}
	/**タグ取得*/
	public String getTag(String text,String key) {
		int index=text.indexOf(key+"(");
		if(index<0)index=text.indexOf(key+"（");
		if(index<0)return null;//タグを含まない時
		int ki=text.indexOf(')');//半角
		int zi=text.indexOf('）');//全角
		if(ki<zi)ki=zi;
		if(ki<0)return null;//閉じカッコが無い時
		if(ki==index+key.length()+1)return "";//0文字
		return text.substring(index+key.length()+1,ki).trim();//スペースは削除
	}
}
