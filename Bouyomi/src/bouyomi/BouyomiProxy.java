package bouyomi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

/**ただのプロキシなのでDiSpeak以外でも使えます(棒読みちゃん専用)*/
public class BouyomiProxy implements Runnable{
	/**文字統一辞書(今は使われてない)*/
	public static HashMap<String,String> FW=new HashMap<String,String>();
	/**教育単純置換辞書(今は使われてない)*/
	public static HashMap<String,String> Study=new HashMap<String,String>();
	/**自動応答辞書*/
	public static HashMap<String,String> BOT=new HashMap<String,String>();
	static {
		FW.put("０","0");FW.put("１","1");FW.put("２","2");FW.put("３","3");FW.put("４","4");
		FW.put("５","5");FW.put("６","6");FW.put("７","7");FW.put("８","8");FW.put("９","9");
		FW.put("A","A");FW.put("B","B");FW.put("C","C");FW.put("D","D");FW.put("E","E");
		FW.put("F","F");FW.put("G","G");FW.put("H","H");FW.put("I","I");FW.put("J","J");
		FW.put("K","K");FW.put("L","L");FW.put("M","M");FW.put("N","N");FW.put("O","O");
		FW.put("P","P");FW.put("Q","Q");FW.put("R","R");FW.put("S","S");FW.put("T","T");
		FW.put("U","U");FW.put("V","V");FW.put("W","W");FW.put("X","X");
		FW.put("Y","Y");FW.put("Z","Z");
		FW.put("a","A");FW.put("b","B");FW.put("c","C");FW.put("d","D");FW.put("e","E");
		FW.put("f","F");FW.put("g","G");FW.put("h","H");FW.put("i","I");FW.put("j","J");
		FW.put("k","K");FW.put("l","L");FW.put("m","M");FW.put("n","N");FW.put("o","O");
		FW.put("p","P");FW.put("q","Q");FW.put("r","R");FW.put("s","S");FW.put("t","T");
		FW.put("u","U");FW.put("v","V");FW.put("w","W");FW.put("x","X");
		FW.put("y","Y");FW.put("z","Z");
		FW.put("ａ","A");FW.put("ｂ","B");FW.put("ｃ","C");FW.put("ｄ","D");FW.put("ｅ","E");
		FW.put("ｆ","F");FW.put("ｇ","G");FW.put("ｈ","H");FW.put("ｉ","I");FW.put("ｊ","J");
		FW.put("ｋ","K");FW.put("ｌ","L");FW.put("ｍ","M");FW.put("ｎ","N");FW.put("ｏ","O");
		FW.put("ｐ","P");FW.put("ｑ","Q");FW.put("ｒ","R");FW.put("ｓ","S");FW.put("ｔ","T");
		FW.put("ｕ","U");FW.put("ｖ","V");FW.put("ｗ","W");FW.put("ｘ","X");
		FW.put("ｙ","Y");FW.put("ｚ","Z");
		BOT.put("ちくわ大明神","b) 誰だいまの");//デフォルトの自動応答
	}
	//ReplaceStudy.dic
	/**実験中機能(今は使われてない)*/
	public static void loadStudy(String path) throws IOException {
		File rf=new File(path);
		FileInputStream fis=new FileInputStream(rf);
		InputStreamReader r=new InputStreamReader(fis,"UTF-8");
		BufferedReader br=new BufferedReader(r);
		Study.clear();
		try{
			while(true) {
				String rl=br.readLine();
				if(rl==null)break;
				int index=rl.indexOf("\t");
				if(rl.length()<index+4||index<1)continue;
				rl=rl.substring(index+3);
				index=rl.indexOf("\t");
				String key=rl.substring(0,index);
				String value=rl.substring(index+1);
				Study.put(key,value);
			}
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			try{
				br.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	//ReplaceStudy.dic
	/**実験中機能(今は使われてない)*/
	public static String ReplaceStudy(String text) throws IOException {
		class CL implements BiConsumer<String,String>{
			public String d;
			public CL(String s) {
				d=s;
			}
			@Override
			public void accept(String t,String u){
				d=d.replaceAll(Matcher.quoteReplacement(t),Matcher.quoteReplacement(u));
			}
			public String toString() {
				return d;
			}
		}
		CL c=new CL(text);
		FW.forEach(c);
		Study.forEach(c);
		return c.toString();
	}
	/**このインスタンスの接続先*/
	private Socket soc;
	//コンストラクタ
	public BouyomiProxy(Socket s){
		soc=s;
	}
	/**接続単位で別のインスタンス*/
	private Object addTask=null;
	public void run(){
		//System.out.println("接続");
		//long start=System.nanoTime();
		try{
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
			ByteArrayOutputStream baos=new ByteArrayOutputStream();//送信データバッファ
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
			ByteArrayOutputStream baos2=new ByteArrayOutputStream();//メッセージバイナリ書き込み先
			baos2.write(fb);//最初の文字をメッセージバイナリバッファに
			for(int i=1;i<len;i++){//メッセージデータ取得
				int j=is.read();
				if(j<0){//すべてのメッセージを取得できない時
					System.out.println("DataRead");
					throw new IOException("DataRead");//例外を出して終了
				}
				baos2.write(j);
			}
			String text=null;
			if(d[7]==0)text=baos2.toString("utf-8");//UTF-8でデコード
			else if(d[7]==1)text=baos2.toString("utf-16");//UTF-16でデコード
			//この時点で受信は終わってる
			if(fb=='/'||fb=='\\'){//最初の文字がスラッシュの時は終了
				//System.out.println("スラッシュで始まる");
				if(text!=null) {
					text=text.substring(1);
					bot(text);
				}
				return;
			}
			//System.out.println("len="+len);
			String em=null;
			if(len>=200){//長文省略基準200文字以上
				em="長文省略";
				System.out.println("長文省略("+len+"文字)");
			}else if(text!=null) {//文字データが取得できた時
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
						addTask="要望リストに記録できませんでした";//残した事を追加で言う
					}
				}else em=bot(text);
				/*
				else if(text.toUpperCase().indexOf("YAMA(")>=0||text.indexOf("やまびこ(")>=0) {
					int index=text.indexOf("やまびこ(");
					if(index<0)text.indexOf("YAMA(");
					if(len-index>80) {
						em="長文やまびこ省略";
					}
				}else if(text.toUpperCase().indexOf("ECHO(")>=0||text.indexOf("エコー(")>=0) {
					int index=text.indexOf("エコー(");
					if(index<0)text.indexOf("ECHO(");
					if(len-index>100) {
						em="長文エコー省略";
					}
				}
				*/
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
			if(text!=null&&em==null) {//文字データが取得できてメッセージが書き換えられていない時
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
			if(em!=null) {//メッセージが書き換えられていた時
				byte[] t=em.getBytes(StandardCharsets.UTF_8);//UTF-8でバイナリ化
				type=0;//文字コードをUTF-8に設定
				baos2.reset();//読み込んだメッセージのバイナリを破棄
				baos2.write(t);//バイナリデータをメッセージバイナリに書き込み
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
			int ki=text.indexOf(')');
			if(ki<0)ki=text.indexOf('）');
			if(ei>0&&ki>1) {
				String key=text.substring(3,ei);
				String val=text.substring(ei+1,ki);
				em=key+" には "+val+" を返します";
				System.out.println("応答登録（"+key+"="+val+")");//ログに残す
				BOT.put(key,val);
			}
		}else if(text.indexOf("応答破棄(")==0||text.indexOf("応答破棄（")==0) {//BOT教育機能を使う時
			//System.out.println(text);//ログに残す
			int ei=text.indexOf(')');
			if(ei<0)ei=text.indexOf('）');
			if(ei>5) {
				String key=text.substring(5,ei);
				em=key+" には応答を返しません";
				System.out.println("応答破棄（"+key+")");//ログに残す
				BOT.remove(key);
			}
		}
		return em;
	}
	private static String command;//コンソールに入力されたテキスト
	public static int bouyomi_port,proxy_port;//棒読みちゃんのポート(サーバはlocalhost固定)
	//main関数、スタート地点
	public static void main(String[] args) throws IOException{
		InputStreamReader isr=new InputStreamReader(System.in);
		final BufferedReader br=new BufferedReader(isr);//コンソールから1行ずつ取得する為のオブジェクト
		if(args.length>0&&!args[0].equals("-"))command=args[0];
		else {
			System.out.println("プロキシサーバのポート(半角数字)");
			command=br.readLine();//1行取得する
		}
		//0文字だったらデフォルト、それ以外だったら数値化
		proxy_port=command.isEmpty()?50003:Integer.parseInt(command);
		System.out.println("プロキシサーバのポート"+proxy_port);
		if(args.length>1&&!args[1].equals("-"))command=args[1];
		else {
			System.out.println("棒読みちゃんのポート(半角数字)");
			command=br.readLine();//1行取得する
		}
		//0文字だったらデフォルト、それ以外だったら数値化
		bouyomi_port=command.isEmpty()?50001:Integer.parseInt(command);
		System.out.println("棒読みちゃんのポート"+bouyomi_port);
		if(args.length>2&&!args[2].equals("-"))command=args[2];
		else {
			System.out.println("応答辞書の場所");
			command=br.readLine();//1行取得する
		}
		//0文字だったらデフォルト、それ以外だったらそれ
		final String BOTpath=command.isEmpty()?"BOT.dic":command;//相対パス
		System.out.println("応答辞書の場所"+BOTpath);
		System.out.println("exitで終了");
		ServerSocket ss=new ServerSocket(proxy_port);//サーバ開始
		new Thread(){
			public void run(){
				while(true){
					try{
						command=br.readLine();//1行取得する
						if(command==null) System.exit(1);//読み込み失敗した場合終了
						if("saveBOT".equals(command)) {
							try{
								save(BOTpath);
							}catch(IOException e){
								e.printStackTrace();
							}
						}else if("loadBOT".equals(command)) {
							try{
								load(BOTpath);
							}catch(IOException e){
								e.printStackTrace();
							}
						}else if("listBOT".equals(command)) {
							BOT.forEach(new BiConsumer<String,String>(){
								@Override
								public void accept(String key,String val){
									System.out.println("key="+key+" val="+val);
								}
							});
						}else if(!"exit".equals(command)) talk(proxy_port,command);//「exit」以外だったら読む(このプロキシを通さない)
						else{
							ss.close();//サーバ終了
							System.exit(0);//プログラム終了
						}
					}catch(IOException e){//コンソールから取得できない時終了
						System.exit(1);
					}
				}
			}
		}.start();
		Runtime.getRuntime().addShutdownHook(new Thread("save") {
			public void run() {
				try{
					save(BOTpath);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		});
		try{
			load(BOTpath);
		}catch(IOException e){
			e.printStackTrace();
		}
		//スレッドプールを用意(最低1スレッド維持、空きスレッド60秒維持)
		ExecutorService pool=new ThreadPoolExecutor(1, Integer.MAX_VALUE,60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
		while(true){//無限ループ
			try{
				Socket s=ss.accept();//サーバ受付
				BouyomiProxy r=new BouyomiProxy(s);//接続単位でインスタンス生成
				pool.execute(r);//スレッドプールで実行する
			}catch(IOException e){//例外は無視

			}
		}
	}
	/**BOT応答辞書読み込み*/
	public static void load(String path) throws IOException {
		BOT.clear();
		FileInputStream fos=new FileInputStream(path);
		InputStreamReader isr=new InputStreamReader(fos,StandardCharsets.UTF_8);
		BufferedReader br=new BufferedReader(isr);
		try {
			while(true) {
				String line=br.readLine();
				if(line==null)break;
				int tab=line.indexOf('\t');
				if(tab<0||tab+1>line.length())continue;
				String key=line.substring(0,tab);
				String val=line.substring(tab+1);
				System.out.println("k="+key+" v="+val);
				BOT.put(key,val);
			}
		}finally {
			br.close();
		}
	}
	/**BOT応答辞書書き出し*/
	public static void save(String path) throws IOException {
		FileOutputStream fos=new FileOutputStream(path);
		final OutputStreamWriter osw=new OutputStreamWriter(fos,StandardCharsets.UTF_8);
		try {
			BOT.forEach(new BiConsumer<String,String>(){
			@Override
			public void accept(String key,String val){
				try{
					osw.write(key);
					osw.write("\t");
					osw.write(val);
					osw.write("\n");
					System.out.println("k="+key+" v="+val);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		});
		}finally {
			osw.flush();
			osw.close();
		}
	}
	/**棒読みちゃんに送信する*/
	public synchronized static void send(int port,byte[] data){
		Socket soc=null;
		try{
			//System.out.println("棒読みちゃんに接続");
			soc=new Socket("localhost",port);
			OutputStream os=soc.getOutputStream();
			//System.out.println("棒読みちゃんに接続完了");
			os.write(data);
		}catch(ConnectException e) {
			System.out.println("棒読みちゃんに接続できません");
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
	}
	/**文字列を送信*/
	public static void talk(int port,String message){
		short volume=-1;//音量　棒読みちゃん設定
		short speed=-1;//速度　棒読みちゃん設定
		short tone=-1;//音程　棒読みちゃん設定
		short voice=0;//声質　棒読みちゃん設定
		byte messageData[]=null;
		try{
			messageData=message.getBytes("UTF-8");
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		int length=messageData.length;
		byte data[]=new byte[15+length];
		data[0]=(byte) 1; // コマンド 1桁目
		data[1]=(byte) 0; // コマンド 2桁目
		data[2]=(byte) ((speed>>>0)&0xFF); // 速度 1桁目
		data[3]=(byte) ((speed>>>8)&0xFF); // 速度 2桁目
		data[4]=(byte) ((tone>>>0)&0xFF); // 音程 1桁目
		data[5]=(byte) ((tone>>>8)&0xFF); // 音程 2桁目
		data[6]=(byte) ((volume>>>0)&0xFF); // 音量 1桁目
		data[7]=(byte) ((volume>>>8)&0xFF); // 音量 2桁目
		data[8]=(byte) ((voice>>>0)&0xFF); // 声質 1桁目
		data[9]=(byte) ((voice>>>8)&0xFF); // 声質 2桁目
		data[10]=(byte) 0; // エンコード(0: UTF-8)
		data[11]=(byte) ((length>>>0)&0xFF); // 長さ 1桁目
		data[12]=(byte) ((length>>>8)&0xFF); // 長さ 2桁目
		data[13]=(byte) ((length>>>16)&0xFF); // 長さ 3桁目
		data[14]=(byte) ((length>>>24)&0xFF); // 長さ 4桁目
		System.arraycopy(messageData,0,data,15,length);
		send(port,data);
	}
}
