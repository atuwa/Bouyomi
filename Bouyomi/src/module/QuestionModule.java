package module;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import bouyomi.BouyomiConection;
import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;
import bouyomi.Util;

public class QuestionModule implements IModule{

	private static Object lock=new Object();
	public static Question now;
	@Override
	public void call(Tag tm){
		BouyomiConection bc=tm.con;
		if(DiscordAPI.service_host==null)return;//Discord投票システムが設定されてない時はアンケート機能を無効
		String text=bc.text;
		synchronized(lock) {
			if(now!=null)now.add(bc);
		}
		String tag=tm.getTag("アンケート");
		if(tag!=null){
			synchronized(lock) {
				if(now!=null)bc.addTask.add("実行中のアンケートを終了してください");
				else {
					now=new Question();
					now.start(tag,bc);
					now.start();
				}
			}
		}
		tag=tm.getTag("締切時間","自動集計");
		if(tag!=null) {
			synchronized(lock) {
				if(now==null);
				else if(tag.isEmpty()){
					long e=now.endTime-System.currentTimeMillis();
					e=e/1000;
					DiscordAPI.chatDefaultHost(e+"秒後に集計します");
				}else try{
					int i=Integer.parseInt(tag);
					if(i<0) {
						now.endTime=-1;
						tm.con.addTask.add("自動で集計しません");
					}else {
						tm.con.addTask.add(i+"分後に集計します");
						i=i*60000;
						now.endTime=System.currentTimeMillis()+i;
					}
				}catch(NumberFormatException nfe) {
					tm.con.addTask.add("数字だけいれて");
				}
			}
		}
		tag=tm.getTag("集計");
		if(tag!=null) {
			synchronized(lock) {
				if(now==null) {
					bc.addTask.add("アンケートが実施されていません");
				}else {
					now.end(tag,bc);
					now=null;
				}
			}
		}
		tag=tm.getTag("アンケート追加");
		if(tag!=null) {
			synchronized(lock) {
				if(now==null) {
					bc.addTask.add("アンケートが実施されていません");
				}else {
					now.addKey(tag);
				}
			}
		}
		if(tm.con.text.equals("どれに投票したっけ")) {
			if(now==null) {
				bc.addTask.add("アンケートが実施されていません");
			}else {
				now.target(bc);
			}
		}
		tag=tm.getTag("アンケート中間結果");
		if(tag!=null) {
			synchronized(lock) {
				if(now==null) {
					bc.addTask.add("アンケートが実施されていません");
				}else {
					now.chat();
				}
			}
		}
		if(text.indexOf("アンケート中?")>=0||text.indexOf("アンケート中？")>=0
				||text.indexOf("アンケ中?")>=0||text.indexOf("アンケ中？")>=0) {
			DiscordAPI.chatDefaultHost(now==null?"してない":"してる");
		}
	}
	public static int to_i(String s) {
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<s.length();i++) {//数字部分だけ抽出
			char c=s.charAt(i);
			if(c>=0x30&&c<0x3A)sb.append(c);//半角数字
			else if(c>=0xEFBC90&&c<0xEFBC9A)sb.append(c);//全角数字
		}
		try {
			return Integer.parseInt(sb.toString());//何故か全角対応
		}catch(NumberFormatException nfe) {}
		return 0;
	}
	/**アンケート機能*/
	public class Question extends Thread{
		public long startTime=System.currentTimeMillis();
		public long endTime=-1;
		/**アンケート名*/
		public String questionnaireName;
		/**データ*/
		public int[] questionnaire;
		/**Indexのタイトル*/
		public ArrayList<String> questionnaireList=new ArrayList<String>();
		/**ユーザの投票したIndex*/
		public HashMap<String,Integer> questionnaireUserList=new HashMap<String,Integer>();
		private boolean end;
		public Question() {
			super("アンケート自動集計");
		}
		@Override
		public void run() {
			while(!end) {
				if(endTime>0) {
					synchronized(this) {
						if(endTime<System.currentTimeMillis()) {
							DiscordAPI.chatDefaultHost("時間になったので集計()");
							break;
						}
					}
				}
				try{
					Thread.sleep(10000);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
		public void addKey(String k) {
			int index=questionnaireList.indexOf(k);
			if(index>=0) {
				DiscordAPI.chatDefaultHost(index+" : "+k+" が登録済です");
				return;
			}
			questionnaire=Arrays.copyOf(questionnaire,questionnaire.length+1);
			questionnaireList.add(k);
			DiscordAPI.chatDefaultHost((questionnaire.length-1)+" : "+k+" を追加しました");
		}
		public void start(String tag,BouyomiConection bc) {
			String[] keys;
			if(tag.isEmpty())keys=new String[]{""};
			else keys=tag.split(",");
			if(keys.length>0) {//最低でもタイトルは必須
				questionnaireName=keys[0];//最初の文字をタイトルに設定
				StringBuilder result=new StringBuilder("/アンケート名");//出力テキスト
				result.append(questionnaireName).append("\n");
				questionnaire=new int[keys.length-1];//最低大きさ0の配列
				for(int i=1;i<keys.length;i++) {
					String k=keys[i].trim();
					questionnaireList.add(k);
					result.append(i-1).append(" : ").append(k).append("\n");
				}
				result.append("です");
				if(DiscordAPI.chatDefaultHost(result.toString())) {
					bc.addTask.add("アンケートを開始します");
				}else bc.addTask.add("開始したけどディスコードに接続できません");
			}
		}
		/**結果通知*/
		public synchronized void chat() {
			StringBuilder result=new StringBuilder("アンケート名").append(questionnaireName);
			long all=0;
			for(int i=0;i<questionnaire.length;i++)all+=questionnaire[i];
			result.append("(合計").append(all).append("票)\n");
			DecimalFormat fomat=new DecimalFormat("##0.##");
			class Val implements Comparable<Val>{
				private final String k;
				private final int v;
				public Val(String key,int value) {
					k=key;
					v=value;
				}
				@Override
				public int compareTo(Val o){
					return o.v-v;
				}
			}
			Val[] v=new Val[questionnaire.length];
			for(int i=0;i<v.length;i++)v[i]=new Val(questionnaireList.get(i),questionnaire[i]);
			Arrays.sort(v);
			for(int i=0;i<v.length;i++) {
				result.append(v[i].k).append(" が").append(v[i].v).append("票");
				if(all>0)result.append("/*(").append(fomat.format(v[i].v/(double)all*100D)).append("%)*/");
				result.append("\n");
			}
			result.append("でした。");
			DiscordAPI.chatDefaultHost(result.toString());
		}
		public synchronized void end(String tag,BouyomiConection bc) {
			end=true;
			if(!tag.equals("内容破棄"))chat();
			bc.addTask.add("アンケートを終了");
			questionnaireName=null;
			questionnaireList.clear();
			questionnaireUserList.clear();
		}
		public void add(BouyomiConection bc) {
			try {//アンケート中の時
				int i=Integer.parseInt(bc.text);//数値に変換
				questionnaire(bc, i);//成功したときそのIndexに投票
			}catch(NumberFormatException nfe) {//失敗した時
				int i=questionnaireList.indexOf(bc.text);//キーワードからIndexを取得
				questionnaire(bc, i);//そのIndexに投票。キーワードがない時は後ではじかれる
			}
		}
		public void target(BouyomiConection con) {
			if(con.userid==null)return;
			StringBuilder sb=new StringBuilder();
			if(con.userid!=null)sb.append(Util.IDtoMention(con.userid));
			String user=con.userid==null?con.user:con.userid;
			if(questionnaireUserList.containsKey(user)) {//ユーザが投票済の時
				Integer k=questionnaireUserList.get(user);//ユーザの投票先(index)
				sb.append(questionnaireList.get(k));
				sb.append("に投票してます");
				DiscordAPI.chatDefaultHost(sb.toString());
			}else {
				sb.append("投票してません");
				DiscordAPI.chatDefaultHost(sb.toString());
			}
		}
		private void questionnaire(BouyomiConection con,int index) {
			if(questionnaire.length<=index||index<0)return;//indexが無効な時に無視する。
			//例えばキーワードがない時、Index指定で範囲外の時
			//System.out.println("投票"+key);
			con.text="";
			String user=con.userid==null?con.user:con.userid;
			if(user!=null&&questionnaireUserList.containsKey(user)) {//ユーザが投票済の時
				Integer k=questionnaireUserList.get(user);//ユーザの投票先(index)
				questionnaire[k]--;
				con.addTask.add("上書き投票");
			}else con.addTask.add("投票");
			questionnaire[index]++;
			if(user!=null)questionnaireUserList.put(user,index);
		}
	}
}
