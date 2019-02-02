package bouyomi;

import static bouyomi.BouyomiProxy.*;
import static bouyomi.TubeAPI.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.BiConsumer;

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
	private String em=null;//置き換えメッセージ
	private int type;
	private ByteArrayOutputStream baos2;
	private ByteArrayOutputStream baos;
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
		int type=is.read();//文字コード読み込み
		if(len!=8||type<0){//読み込みバイト数が足りない時
			System.out.println("notLen9");
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
		//ここで変数lenは文字数になる
		len=((ch1<<0)+(ch2<<8)+(ch3<<16)+(ch4<<24));//文字数データから数値に
		byte fb=(byte) is.read();//最初の文字を取得
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
	private void replace() {
		//System.out.println("len="+len);
		if(text!=null)em=bot(text);
		if(em==null) {
			text=text.replaceAll("https?://[\\x00-\\x7F]++","URL省略");
		}
		if(em==null&&text!=null?(text.length()>=100||len>=400):len>=250){//長文省略基準100文字以上
			em="長文省略";
			System.out.println("長文省略("+(text==null?len:text.length())+"文字)");
		}else if(text!=null&&em!=null) {//文字データが取得できた時
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
			final String FT=text;
			BOT.forEach(new BiConsumer<String,String>(){
				@Override
				public void accept(String key,String val){
					if(addTask!=null)return;
					int bi=key.indexOf("部分一致:");
					if(bi!=0)bi=key.indexOf("部分一致：");
					if(bi==0&&key.length()>5) {
						key=key.substring(5);
						if(FT.indexOf(key)>=0) {//読み上げテキストにキーが含まれている時
							System.out.println("BOT応答キー =部分一致："+key);//ログに残す
							addTask=val;//追加で言う
						}
					}else 	if(FT.equals(key)) {//読み上げテキストがキーに一致した時
						System.out.println("BOT応答キー ="+key);//ログに残す
						addTask=val;//追加で言う
					}
				}
			});
		}
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
		//int clen=0;
		for(int i=0;i<text.length();i++) {//文字データを1文字ずつ読み込む
			char r=text.charAt(i);//現在位置の文字を取得
			//連続カウントが9以上で次の文字が最後に書き込まれた文字と一致した場合次へ
			if(cc>8&&r==lc)continue;
			if(r==lc)cc++;//次の文字が最後に書き込まれた文字と一致した場合連続カウントを増やす
			else cc=0;//次の文字が最後に書き込まれた文字と異なる場合カウントをリセットする
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
		//System.out.println("clen="+clen);
		bw.flush();//バッファの内容をすべてバイナリに変換
	}
	public void run(){
		//System.out.println("接続");
		//long start=System.nanoTime();
		try{
			read();//受信処理
			lastComment=System.currentTimeMillis();
			if(fb=='/'||fb=='\\'){//最初の文字がスラッシュの時は終了
				//System.out.println("スラッシュで始まる");
				if(text!=null) {
					text=text.substring(1);
					bot(text);
				}
				return;
			}
			replace();
			if(text!=null&&em==null)ContinuationOmitted();//文字データが取得できてメッセージが書き換えられていない時
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
			//System.out.println((System.nanoTime()-start)+"ns");
		}
		if(addTask!=null) {//追完で言うデータがある時
			if(addTask instanceof ArrayList) {//データがArrayListの時
				for(Object s:(ArrayList<?>)addTask)talk(bouyomi_port,s.toString());//すべて送信
			}else talk(bouyomi_port,addTask.toString());//送信
		}
	}
	public String bot(String text) {
		String em=null;
		if(text.indexOf("応答(")==0||text.indexOf("応答（")==0) {//BOT教育機能を使う時
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
		}else if(text.indexOf("応答破棄(")==0||text.indexOf("応答破棄（")==0) {//BOT教育機能を使う時
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
			em=video(text);
		}
		return em;
	}
	public String video(String text) {
		String em=null;
		int ki=text.lastIndexOf(')');
		int zi=text.lastIndexOf('）');
		if(ki<zi)ki=zi;
		if(text.indexOf("動画再生(")==0||text.indexOf("動画再生（")==0) {//動画再生
			//System.out.println(text);//ログに残す
			if(ki==5) {
				try{
					URL url=new URL("http://"+video_host+"/operation.html?play");
					url.openStream().close();
					em="つづきを再生します。";
					int vol=getVol();
					if(vol>=0)em+="音量は"+vol+"です";
				}catch(IOException e){
					e.printStackTrace();
				}
			}if(ki>5) {
				String key=text.substring(5,ki);
				System.out.println("動画再生（"+key+")");//ログに残す
				if(play(key)) {
					em="動画を再生します。";
					int vol=getVol();
					if(vol>=0)em+="音量は"+vol+"です";
				}else em="動画を再生できませんでした";
			}
		}else if(text.indexOf("動画停止()")==0||text.indexOf("動画停止（）")==0) {//動画停止
			System.out.println("動画停止");//ログに残す
			try{
				URL url=new URL("http://"+video_host+"/operation.html?stop");
				url.openStream().close();
			}catch(IOException e){
				e.printStackTrace();
			}
			em="動画を停止します";
		}else if(text.indexOf("動画音量(")==0||text.indexOf("動画音量（")==0) {//動画音量
			if(ki==5) {
				int vol=getVol();
				if(vol<0)em="音量を取得できません";
				else em="音量は"+vol+"です";
				System.out.println(em);
			}if(ki>5) {
				String volS=text.substring(5,ki);
				if(volS!=null) {
					char fc=volS.charAt(0);
					int radix=10;
					if(volS.indexOf("0x")==0) {
						radix=16;
						volS=volS.substring(2);
					}
					try{
						int Nvol=-1;
						if(fc=='+')Nvol=getVol();
						else if(fc=='-')Nvol=getVol();
						int vol=Integer.parseInt(volS,radix);
						if(Nvol>=0)vol+=Nvol;
						System.out.println("動画音量"+vol);//ログに残す
						if(vol<0)vol=0;
						VOL=vol;
						try{
							URL url=new URL("http://"+video_host+"/operation.html?vol="+vol);
							url.openStream().close();
						}catch(IOException e){
							e.printStackTrace();
						}
						em="音量を"+vol+"にします";
					}catch(NumberFormatException e) {

					}
				}
			}
		}
		return em;
	}
	private int getVol(){
		BufferedReader br=null;
		try{
			URL url=new URL("http://"+video_host+"/operation.html?GETvolume");
			InputStream is=url.openStream();
			InputStreamReader isr=new InputStreamReader(is);
			br=new BufferedReader(isr);//1行ずつ取得する
			int vol=Integer.parseInt(br.readLine());
			return vol;
		}catch(NumberFormatException|IOException e){
			e.printStackTrace();
		}finally {
			try{
				br.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		return -1;
	}
	public boolean play(String url) {
		if(url.indexOf("https://www.youtube.com/watch?")==0||
				url.indexOf("https://m.youtube.com/watch?")==0||
				url.indexOf("https://youtube.com/watch?")==0||
				url.indexOf("http://www.youtube.com/watch?")==0||
				url.indexOf("http://m.youtube.com/watch?")==0||
				url.indexOf("http://youtube.com/watch?")==0) {
			int Vindex=url.indexOf("?v=");
			if(Vindex<0)Vindex=url.indexOf("&v=");
			if(Vindex<0)return false;
			String ss=url.substring(Vindex+3);
			int end=ss.indexOf("&");
			if(end<0)end=ss.length();
			return playTube("v="+ss.substring(0,end));
		}else if(url.indexOf("https://youtu.be/")==0||url.indexOf("http://youtu.be/")==0) {
			return playTube("v="+url.substring(17));
		}else if(url.indexOf("v=")==0) {
			return playTube(url);
		}else System.out.println("URL解析失敗"+url);
		return false;
	}
}
