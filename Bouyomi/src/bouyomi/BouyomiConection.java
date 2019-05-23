package bouyomi;

import static bouyomi.BouyomiProxy.*;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bouyomi.IModule.BouyomiEvent;

public class BouyomiConection implements Runnable{

	//コンストラクタ
	/**何らかの方法でパラメータを設定する*/
	public BouyomiConection(){

	}
	/**入力されたメッセージ*/
	public String text=null;
	/**追加メッセージ入れるやつ*/
	public ArrayList<String> addTask=new ArrayList<String>();
	/**AutuwaDisReader使えば取得できる(nullになる可能性あり)*/
	public String user,userid;
	/**読み上げ無し*/
	public boolean mute;
	/**ユーザIDが入ってる<br>
	 * ニックネームが必要な時はCounter.getUserName(id)を使う*/
	public ArrayList<String> mentions=new ArrayList<String>();
	private void urlcut(){
		//URL省略処理
		//URL判定基準を正規表現で指定
		Matcher m=Pattern.compile("https?://\\S++").matcher(text);
		//Matcher m=Pattern.compile("https?://[\\x21-\\xFF]++").matcher(text);//古いの
		m.reset();
		boolean result=m.find();
		if(result){
			int co=0;//URLの数
			do{
				co++;
				result=m.find();
			}while(result);
			m.reset();
			result=m.find();
			boolean b=true;
			StringBuffer sb=new StringBuffer();
			do{
				if(b){//初回
					b=false;
					if(co==1) m.appendReplacement(sb,"URL省略");//対象が一つの時
					else m.appendReplacement(sb,co+"URL省略");
				}else m.appendReplacement(sb,"");//2回目以降
				result=m.find();
			}while(result);
			m.appendTail(sb);
			text=sb.toString();
		}
	}
	public static class LongSentenceEvent implements BouyomiEvent{
		public String text;
		public BouyomiConection con;
		public String Overwrite="長文省略";
		public LongSentenceEvent(BouyomiConection bc) {
			con=bc;
			text=bc.text;
		}
	}
	private void replace() throws IOException{
		//System.out.println("len="+len);
		//text=text.replaceAll("file://[\\x21-\\x7F]++","ファイル");
		urlcut();
		{//画像URI処理
			//判定基準を正規表現で指定
			Matcher m=Pattern.compile("file://[\\x21-\\x7F]++").matcher(text);
			m.reset();
			boolean result=m.find();
			if(result){
				StringBuffer sb=new StringBuffer();
				do{
					String g=m.group().toLowerCase();
					String r="ファイル";
					if(g.endsWith(".png")||g.endsWith(".gif")||g.endsWith(".jpg")||g.endsWith(".jpeg")
							||g.endsWith(".webp")){
						r="画像";
					}else if(g.endsWith(".bmp")||g.endsWith(".xcf")){
						r="画像";
					}else if(g.endsWith(".txt")){
						r="テキストファイル";
					}else if(g.endsWith(".js")||g.endsWith(".java")){
						r="ソースファイル";
					}else if(g.endsWith(".mp4")||g.endsWith(".avi")||g.endsWith(".mov")){
						r="動画";
					}else if(g.endsWith(".wav")||g.endsWith(".mp3")){
						r="音楽";
					}else if(g.endsWith(".zip")||g.endsWith(".gz")||g.endsWith(".7z")||g.endsWith(".lzh")){
						r="圧縮ファイル";
					}else if(g.endsWith(".log")){
						r="ログファイル";
					}else{
						int li=g.lastIndexOf('.');
						if(li>=0&&li+1<g.length()){
							System.out.println("未定義ファイル"+g);
							String s=g.substring(li+1);
							char[] ca=new char[s.length()*2];
							int j=0;
							for(int i=0;i<ca.length;i+=2){
								ca[i]=s.charAt(j);
								ca[i+1]=',';
								j++;
							}
							r=String.valueOf(ca)+"ファイル";
						}
					}
					//System.out.println("ファイル="+r);
					m.appendReplacement(sb," "+r);//2回目以降
					result=m.find();
				}while(result);
				m.appendTail(sb);
				text=sb.toString().trim();
			}
		}
		if(Japanese.trans(text)){
			String n=text.replaceAll("nn","n");
			if(!text.equals(n)){
				text=n;
			}
		}
		//文字データが取得できた時
		//text=text.toUpperCase(Locale.JAPANESE);//大文字に統一する時
		if(text.indexOf("教育(")>=0||text.indexOf("教育（")>=0){//教育機能を使おうとした時
			System.out.println(text);//ログに残す
			System.out.println(user);
			String d=new SimpleDateFormat("yyyy/MM/dd HH時mm分ss秒").format(new Date());
			study_log.log(userid+"\t"+user+"\t"+d+"\t"+text);
		}else if(text.indexOf("忘却(")>=0||text.indexOf("忘却（")>=0){//忘却機能を使おうとした時
			System.out.println(text);//ログに残す
			System.out.println(user);
		}else if(text.indexOf("機能要望")>=0){//「機能要望」が含まれる時
			System.out.println(text);//ログに残す
			try{
				FileOutputStream fos=new FileOutputStream("Req.txt",true);//追加モードでファイルを開く
				try{
					fos.write((text+"\n").getBytes(StandardCharsets.UTF_8));//改行文字を追加してバイナリ化
				}catch(IOException e){
					e.printStackTrace();
				}finally{
					fos.close();
				}
				addTask.add("要望リストに記録しました");//残した事を追加で言う
			}catch(IOException e){
				e.printStackTrace();
				addTask.add("要望リストに記録できませんでした");//失敗した事を追加で言う
			}
		}
		BOT.call(this);
		//text=Dic.ReplaceStudy(text);
		//巨大数処理
		text=text.replaceAll("[0-9]{8,}+","数字省略");
		ContinuationOmitted();//文字データが取得できてメッセージが書き換えられていない時
		if(text.length()>=90){//長文省略基準90文字以上
			LongSentenceEvent e=new LongSentenceEvent(this);
			System.out.println("長文省略("+text.length()+(user==null?"文字)":"文字)"+user));
			BouyomiProxy.module.event(e);
			text=e.Overwrite;
			return;
		}
	}
	/** 連続短縮 */
	private void ContinuationOmitted() throws IOException{
		ByteArrayOutputStream baos2=new ByteArrayOutputStream();
		//メッセージバイナリバッファにUTF-8で書き込む
		OutputStreamWriter sw=new OutputStreamWriter(baos2,StandardCharsets.UTF_8);
		BufferedWriter bw=new BufferedWriter(sw);//文字バッファ
		char lc=0;//最後に追加した文字
		short cc=0;//連続カウント(9以下)
		byte comment=0;
		//int clen=0;
		boolean source=false;
		for(int i=0;i<text.length();i++){//文字データを1文字ずつ読み込む
			char r=text.charAt(i);//現在位置の文字を取得
			if((r=='ゝ'||r=='ゞ')&&i>0)r=lc;//text.charAt(i-1);
			//連続カウントが2以上で次の文字が`の場合source判定
			if(cc>0&&r=='`'){
				source=!source;
			}
			//連続カウントが9以上で次の文字が最後に書き込まれた文字と一致した場合次へ
			if(cc>8&&r==lc){
				continue;
			}
			if(r==lc) cc++;//次の文字が最後に書き込まれた文字と一致した場合連続カウントを増やす
			else cc=0;//次の文字が最後に書き込まれた文字と異なる場合カウントをリセットする
			if(comment==0&&(r=='/'||r=='／')){//C言語風コメントアウト
				comment=1;
			}else if(comment==1){
				if(r=='*'||r=='＊') comment=-1;
				else comment=0;
			}else if(comment==-1&&(r=='*'||r=='＊')) comment=-2;
			else if(comment==-2){
				if(r=='/'||r=='／'){
					comment=0;
					continue;
				}else comment=-1;
			}
			if(comment<0) continue;
			/*
			if(lc=='。'||lc=='、')clen++;
			if(clen>10&&len>100) {
				em="省略";
				break;
			}
			*/
			lc=r;//最後に書き込まれた文字を次に書き込む文字に設定する
			if(!source)bw.write(r);//文字バッファに書き込む
		}
		//System.out.println("clen="+clen);
		bw.flush();//バッファの内容をすべてバイナリに変換
		text=baos2.toString("utf-8");//UTF-8でデコード
	}
	public void run(){
		if(text==null||text.length()<1)return;
		//System.out.println("接続");
		//long start=System.nanoTime();//TODO 処理時間計測用
		try{
			lastComment=System.currentTimeMillis();
			//if(!userid.equals("544529530866368522"))return;
			Tag tag=new Tag(this);
			if(text.charAt(0)=='/'||text.charAt(0)=='\\'||(text!=null&&text.indexOf("```")==0)){//最初の文字がスラッシュの時は終了
				//System.out.println("スラッシュで始まる");
				//System.out.println(text);
				mute=true;
				if(text!=null){
					if(text!=null&&text.indexOf("```")==0) text=text.substring(3);
					else text=text.substring(1);
					tag.call();
				}
				return;
			}
			if(text!=null){
				mentions();
				tag.call();
				replace();
			}
			if(!mute) talk(bouyomi_port,text);//作ったデータを送信
			//System.out.println("Write");
		}catch(Throwable e){
			e.printStackTrace();//例外が発生したらログに残す
			System.out.println("例外の原因="+text);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH時mm分");
			System.out.println("発生時刻="+sdf.format(new Date()));
		}
		//System.out.println((System.nanoTime()-start)+"ns");//TODO 処理時間計測用
		if(!mute&&!addTask.isEmpty()){//データがArrayListの時
			StringBuilder sb=new StringBuilder();
			for(String s:addTask){
				sb.append(s).append("。");
				if(sb.length()>60){
					talk(bouyomi_port,sb.toString());//一旦送信
					sb=new StringBuilder();
				}
			}
			talk(bouyomi_port,sb.toString());//すべて送信
		}
		//if(!addTask.toString().isEmpty())talk(bouyomi_port,addTask.toString());//送信
	}
	/**メンションを処理*/
	private void mentions() {
		if(text.indexOf("@")>=0){//@がある時はメンション抽出
			//System.out.println(text);//ログに残す
			//DiscordAPI.chatDefaultHost(text);
			Matcher m=Pattern.compile("<@!?[0-9]++>").matcher(text);
			StringBuffer sb = new StringBuffer();
			while(m.find()) {
				m.appendReplacement(sb, "");
				Matcher m2=Pattern.compile("[0-9]++").matcher(m.group());
				m2.find();
				mentions.add(m2.group());
			}
			m.appendTail(sb);
			text=sb.toString();
			//System.out.println(text);
			text=text.trim();
			if(text.length()>1&&text.charAt(0)=='/')mute=true;
			//for(String s:mentions)System.out.println("メンションID="+s+"&ニックネーム="+Counter.getUserName(s));
		}
		if(text.indexOf("#")>=0){//#がある時はチャンネル抽出
			//System.out.println(text);//ログに残す
			//DiscordAPI.chatDefaultHost(text);
			Matcher m=Pattern.compile("<#[0-9]++>").matcher(text);
			StringBuffer sb = new StringBuffer();
			while(m.find()) {
				m.appendReplacement(sb, "");
				//Matcher m2=Pattern.compile("[0-9]++").matcher(m.group());
				//m2.find();
				//mentions.add(m2.group());
			}
			m.appendTail(sb);
			text=sb.toString();
			//for(String s:mentions)System.out.println("メンションID="+s+"&ニックネーム="+Counter.getUserName(s));
		}
	}
}
