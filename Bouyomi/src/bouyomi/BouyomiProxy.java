package bouyomi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import bouyomi.Counter.CountData;
import bouyomi.Counter.CountData.Count;

/**棒読みちゃん専用のプロキシです*/
public class BouyomiProxy{
	public static HashMap<String,String> Config=new HashMap<String,String>();
	private static String command;//コンソールに入力されたテキスト
	public static int bouyomi_port,proxy_port;//棒読みちゃんのポート(サーバはlocalhost固定)
	public static long lastComment=System.currentTimeMillis();
	public static ModuleLoader module;
	public static SaveProxyData logger;
	public static Admin admin;
	public static SaveProxyData study_log;
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
		BOT.BOTpath=command.isEmpty()?"BOT.dic":command;//相対パス
		if(args.length>3) {
			if(args[3].equals("-"))command="";
			else command=args[3];
		}else {
			System.out.println("動画サーバのアドレス");
			command=br.readLine();//1行取得する
		}
		//0文字だったら無し、それ以外だったらそれ
		if(!command.isEmpty()) {
			TubeAPI.video_host=command;
			TubeAPI.loadHistory();
		}
		System.out.println("動画サーバ"+(TubeAPI.video_host==null?"無し":TubeAPI.video_host));
		if(args.length>4) {
			if(args[4].equals("-"))command="";
			else command=args[4];
		}else {
			System.out.println("Discord投稿サーバのアドレス");
			command=br.readLine();//1行取得する
		}
		//0文字だったら無し、それ以外だったらそれ
		if(!command.isEmpty())DiscordAPI.service_host=command;
		System.out.println("Discord投稿サーバ"+(DiscordAPI.service_host==null?"無し":DiscordAPI.service_host));

		if(args.length>5) {
			if(args[5].equals("-"))command="";
			else command=args[5];
		}else {
			System.out.println("ローマ字変換結果をDiscordに投稿するサーバのアドレス");
			command=br.readLine();//1行取得する
		}
		//0文字だったら無し、それ以外だったらそれ
		if(!command.isEmpty())Japanese.chat_server=new DiscordAPI(command);
		System.out.println("ローマ字変換結果をDiscordに投稿するサーバ"+(Japanese.chat_server==null?"無し":Japanese.chat_server.server));

		if(args.length>6) {
			if(args[6].equals("-"))command="";
			else command=args[6];
		}else {
			System.out.println("mp3とwavファイルを再生するサーバのアドレス");
			command=br.readLine();//1行取得する
		}
		//0文字だったら無し、それ以外だったらそれ
		if(!command.isEmpty())MusicPlayerAPI.host=command;
		System.out.println("mp3とwavファイルを再生するサーバ"+(MusicPlayerAPI.host==null?"無し":MusicPlayerAPI.host));

		if(args.length>7) {
			if(args[7].equals("-"))command="";
			else command=args[7];
		}else {
			System.out.println("ログファイル");
			command=br.readLine();//1行取得する
		}
		//0文字だったら無し、それ以外だったらそれ
		if(!command.isEmpty())logger=new SaveProxyData(command);
		System.out.println("ログ"+(logger==null?"無し":logger.file));

		if(args.length>8) {
			if(args[8].equals("-"))command="";
			else command=args[8];
		}
		//0文字だったら無し、それ以外だったらそれ
		File modulePath=null;
		if(!command.isEmpty()) {
			module=new ModuleLoader();
			modulePath=new File(command);
		}

		if(args.length>9) {
			if(args[9].equals("-"))command="";
			else command=args[9];
		}
		//0文字だったら無し、それ以外だったらそれ
		if(!command.isEmpty()) {
			try{
				Dic.loadStudy(command);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("教育"+(command.isEmpty()?"無し":command));

		ServerSocket ss=new ServerSocket(proxy_port);//サーバ開始
		new Thread("CommandReader"){
			public void run(){
				while(true){
					try{
						command=br.readLine();//1行取得する
						if(command==null) System.exit(1);//読み込み失敗した場合終了
						if(command.indexOf("clear")>=0) {
							System.out.println("入力クリア="+command);
						}else if(command.indexOf("stopTime")==0) {
							TubeAPI.stopTime=Integer.parseInt(command.substring(8));
							System.out.println("自動停止時間"+TubeAPI.stopTime+"ms");
						}else if(command.equals("setCounter")) {
							System.out.println("ユーザIDを入力");
							command=br.readLine();//1行取得する
							CountData cd=Counter.usercount.get(command);
							for(String id:Counter.usercount.keySet()) {
								CountData c=Counter.usercount.get(id);
								if(c!=null&&c.name.equals(command))cd=c;
							}
							if(cd==null) {
								System.out.println("新規登録");
								System.out.println("ID="+command);
								String id=command;
								System.out.println("ユーザ名を入力");
								command=br.readLine();//1行取得する
								if(command.isEmpty())continue;
								cd=new CountData(command);
								Counter.usercount.put(id,cd);
							}
							System.out.println("ユーザ名="+cd.name);
							System.out.println("カウント単語を入力");
							String w=br.readLine();
							Count c=cd.count.get(w);
							if(c==null)continue;
							try{
								System.out.println("現在の数値"+c.count);
								c.count=Long.parseLong(command=br.readLine());
								System.out.println(w+"のカウントを"+c.count+"に変更");
							}catch(NumberFormatException e) {

							}
						}else if("saveConfig".equals(command)) {
							try{
								save(Config,"config.txt");
							}catch(IOException e){
								e.printStackTrace();
							}
						}else if("saveBOT".equals(command)) {
							BOT.saveBOT();
						}else if("loadBOT".equals(command)) {
							BOT.loadBOT();
						}else if("addPass".equals(command)) {
							Pass.addPass();
						}else if("listBOT".equals(command)) {
							BOT.printList();
						}else if("exit".equals(command)){
							ss.close();//サーバ終了
							System.exit(0);//プログラム終了
						}else  talk(proxy_port,command);
					}catch(IOException e){//コンソールから取得できない時終了
						System.exit(1);
					}
				}
			}
		}.start();
		IAutoSave.thread();
		DailyUpdate.init();
		try{
			load(Config,"config.txt");
			if("無効".equals(Config.get("平仮名変換"))){
				Japanese.active=false;
			}
			if(Config.containsKey("初期音量")) {
				try{
					TubeAPI.DefaultVol=Integer.parseInt(Config.get("初期音量"));
				}catch(NumberFormatException e) {

				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread("save") {
			public void run() {
				BOT.saveBOT();
			}
		});
		IAutoSave.Register(new IAutoSave() {
			private int hash=Config.hashCode();
			@Override
			public void autoSave() throws IOException{
				int hc=Config.hashCode();
				if(hash==hc)return;
				hash=hc;
				shutdownHook();
			}
			@Override
			public void shutdownHook() {
				try{
					save(Config,"config.txt");
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		});
		TubeAPI.setAutoStop();
		BOT.loadBOT();
		Counter.init();
		admin=new Admin();
		Pass.read();
		study_log=new SaveProxyData("教育者.txt");
		if(module!=null) {
			module.load(modulePath);
			if(!module.isActive())module=null;
		}
		System.out.println("モジュール"+(module==null||!module.isActive()?"無し":module.path));
		DailyUpdate.updater.start();
		System.out.println("exitで終了");
		//スレッドプールを用意(最低1スレッド維持、空きスレッド60秒維持)
		ExecutorService pool=new ThreadPoolExecutor(1, Integer.MAX_VALUE,60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
		int threads=0;
		while(true){//無限ループ
			threads++;
			try{
				if(threads>3)System.err.println("警告：実行中のメッセージスレッドが"+threads+"件です");
				Socket s=ss.accept();//サーバ受付
				BouyomiConection r=new BouyomiTCPConection(s);//接続単位でインスタンス生成
				pool.execute(r);//スレッドプールで実行する
			}catch(IOException e){//例外は無視
				try{
					Thread.sleep(100);
				}catch(InterruptedException e1){
					e1.printStackTrace();
				}
			}
			threads--;
		}
	}
	public static void load(List<String> list,String path) throws IOException {
		list.clear();
		FileInputStream fos=new FileInputStream(path);
		InputStreamReader isr=new InputStreamReader(fos,StandardCharsets.UTF_8);
		BufferedReader br=new BufferedReader(isr);
		try {
			while(true) {
				String line=br.readLine();
				if(line==null)break;
				list.add(line);
			}
		}catch(FileNotFoundException fnf){

		}finally {
			br.close();
		}
	}
	public static void load(Map<String,String> map,String path) throws IOException {
		map.clear();
		FileInputStream fos=new FileInputStream(path);
		InputStreamReader isr=new InputStreamReader(fos,StandardCharsets.UTF_8);
		BufferedReader br=new BufferedReader(isr);
		try {
			while(true) {
				String line=br.readLine();
				if(line==null)break;
				int tab=line.indexOf('\t');
				if(tab<0||tab+1>line.length()) {
					map.put(line,"");//タブがない時ORフォーマットがおかしいときは行をキーにして値を0文字に
					continue;
				}
				String key=line.substring(0,tab);
				String val=line.substring(tab+1);
				System.out.println("k="+key+" v="+val);
				map.put(key,val);
			}
		}catch(FileNotFoundException fnf){

		}finally {
			br.close();
		}
	}
	public static void save(List<String> list,String path) throws IOException {
		FileOutputStream fos=new FileOutputStream(path);
		final OutputStreamWriter osw=new OutputStreamWriter(fos,StandardCharsets.UTF_8);
		try {
			for(String s:list) {
				osw.write(s);
				osw.append('\n');
			}
		}finally {
			osw.flush();
			osw.close();
		}
	}
	public static void save(Map<String,String> map,String path) throws IOException {
		FileOutputStream fos=new FileOutputStream(path);
		final OutputStreamWriter osw=new OutputStreamWriter(fos,StandardCharsets.UTF_8);
		try {
			map.forEach(new BiConsumer<String,String>(){
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
	private static int send_errors;
	/**棒読みちゃんに送信する*/
	public synchronized static void send(int port,byte[] data){
		Socket soc=null;
		try{
			//System.out.println("棒読みちゃんに接続");
			soc=new Socket("localhost",port);
			OutputStream os=soc.getOutputStream();
			//System.out.println("棒読みちゃんに接続完了");
			os.write(data);
			send_errors=0;
		}catch(ConnectException e) {
			send_errors++;
			if(send_errors>5)System.out.println("棒読みちゃんに接続できません");
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
		//System.out.println(message);
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
