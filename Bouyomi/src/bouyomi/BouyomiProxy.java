package bouyomi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

/**棒読みちゃん専用のプロキシです*/
public class BouyomiProxy{
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
		FW.put("ｱ","ア");FW.put("ｲ","イ");FW.put("ｳ","ウ");FW.put("ｴ","エ");FW.put("ｵ","オ");
		FW.put("ｶ","カ");FW.put("ｷ","キ");FW.put("ｸ","ク");FW.put("ｹ","ケ");FW.put("ｺ","コ");
		FW.put("ｻ","サ");FW.put("ｼ","シ");FW.put("ｽ","ス");FW.put("ｾ","セ");FW.put("ｿ","ソ");
		FW.put("ﾀ","タ");FW.put("ﾁ","チ");FW.put("ﾂ","ツ");FW.put("ﾃ","テ");FW.put("ﾄ","ト");
		FW.put("ﾅ","ナ");FW.put("ﾆ","ニ");FW.put("ﾇ","ヌ");FW.put("ﾈ","ネ");FW.put("ﾉ","ノ");
		FW.put("ﾊ","ハ");FW.put("ﾋ","ヒ");FW.put("ﾌ","フ");FW.put("ﾍ","ヘ");FW.put("ﾎ","ホ");
		FW.put("ﾏ","マ");FW.put("ﾐ","ミ");FW.put("ﾑ","ム");FW.put("ﾒ","メ");FW.put("ﾓ","モ");
		FW.put("ﾔ","ヤ");FW.put("ﾕ","ユ");FW.put("ﾖ","ヨ");
		FW.put("ﾜ","ワ");FW.put("ｦ","ヲ");FW.put("ﾝ","ン");
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
	private static String command;//コンソールに入力されたテキスト
	public static int bouyomi_port,proxy_port;//棒読みちゃんのポート(サーバはlocalhost固定)
	public static long lastComment=System.currentTimeMillis();
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
		if(args.length>3&&!args[3].equals("-"))command=args[3];
		else {
			System.out.println("動画サーバのアドレス");
			command=br.readLine();//1行取得する
		}
		//0文字だったら無し、それ以外だったらそれ
		if(!command.isEmpty())TubeAPI.video_host=command;
		System.out.println("動画サーバ"+(TubeAPI.video_host==null?"無し":TubeAPI.video_host));
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
						}else if(!"exit".equals(command)) talk(proxy_port,command);//「exit」以外だったら読む
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
		TubeAPI.setAutoStop();
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
				BouyomiConection r=new BouyomiConection(s);//接続単位でインスタンス生成
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
