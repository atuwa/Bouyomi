package bouyomi;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**アンケート機能*/
public class Question{
	//TODO 集計結果を多い順に並べ替え
	public static Question now;
	/**アンケート名*/
	public String questionnaireName;
	/**データ*/
	public int[] questionnaire;
	/**Indexのタイトル*/
	public ArrayList<String> questionnaireList=new ArrayList<String>();
	/**ユーザの投票したIndex*/
	public HashMap<String,Integer> questionnaireUserList=new HashMap<String,Integer>();
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
	public void end(String tag,BouyomiConection bc) {
		StringBuilder result=new StringBuilder("アンケート名").append(questionnaireName);
		long all=0;
		for(int i=0;i<questionnaire.length;i++)all+=questionnaire[i];
		result.append("(合計").append(all).append("票)\n");
		DecimalFormat fomat=new DecimalFormat("##0.##");
		for(int i=0;i<questionnaire.length;i++) {
			String k=questionnaireList.get(i);
			result.append(k).append(" が").append(questionnaire[i]).append("票/*(").append(fomat.format(questionnaire[i]/(double)all*100D)).append("%)*/\n");
		}
		result.append("でした。");
		if(!tag.equals("内容破棄"))DiscordAPI.chatDefaultHost(result.toString());
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
	/**タグ判定*/
	public static void tag(Tag tm,BouyomiConection bc) {
		if(DiscordAPI.service_host==null)return;//Discord投票システムが設定されてない時はアンケート機能を無効
		String text=bc.text;
		if(now!=null)now.add(bc);
		String tag=tm.getTag("アンケート");
		if(tag!=null){
			if(now!=null)bc.addTask.add("実行中のアンケートを終了してください");
			else {
				now=new Question();
				now.start(tag,bc);
			}
		}
		tag=tm.getTag("集計");
		if(tag!=null) {
			if(now==null) {
				bc.addTask.add("アンケートが実施されていません");
			}else {
				now.end(tag,bc);
				now=null;
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
}