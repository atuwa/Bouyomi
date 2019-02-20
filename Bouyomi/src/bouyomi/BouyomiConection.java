package bouyomi;

import static bouyomi.BouyomiProxy.*;

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
	String text=null;
	private Object addTask=null;
	private char fb;//最初の文字
	private int len;
	public String em=null;//置き換えメッセージ
	private int type;
	public String user;
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
		if(text!=null) {
			String key="濰濱濲濳濴濵濶濷濸濹濺濻濼濽濾濿";
			int index=text.indexOf(key);
			if(index>0) {
				user=text.substring(0,index);
				text=text.substring(index+key.length());
				fb=text.charAt(0);
			}
		}
	}
	private void replace() throws IOException {
		//System.out.println("len="+len);
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
		if(Japanese.trans(text))em="";
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
			Tag tag;
			if(text==null)tag=null;
			else tag=new Tag(this);
			if(fb=='/'||fb=='\\'||text.indexOf("```")==0){//最初の文字がスラッシュの時は終了
				//System.out.println("スラッシュで始まる");
				mute=true;
				if(text!=null) {
					text=text.substring(1);
					tag.bot();
				}
				return;
			}
			if(text!=null) {
				text=tag.bot();
				em=tag.em;
				replace();
			}
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
			if(len>0)send(bouyomi_port,baos.toByteArray());//作ったデータを送信
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
}
