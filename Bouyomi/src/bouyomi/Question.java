package bouyomi;

import java.util.ArrayList;
import java.util.HashMap;

/**アンケート機能*/
public class Question{
	/**アンケート名*/
	public static String questionnaireName;
	/**データ*/
	public static int[] questionnaire;
	/**Indexのタイトル*/
	public static ArrayList<String> questionnaireList=new ArrayList<String>();
	/**ユーザの投票したIndex*/
	public static HashMap<String,Integer> questionnaireUserList=new HashMap<String,Integer>();
	/**タグ判定*/
	public static void tag(Tag tm,BouyomiConection bc) {
		if(DiscordAPI.service_host==null)return;//Discord投票システムが設定されてない時はアンケート機能を無効
		String text=bc.text;
		if(questionnaireName!=null)try {//アンケート中の時
			boolean b=true;
			for(int i=0;i<text.length();i++) {
				char c=text.charAt(i);
				if(c<0x30||c>0x39) {
					b=false;
					break;
				}
			}
			if(b) {
				int i=Integer.parseInt(text);//数値に変換
				questionnaire(bc, i);//成功したときそのIndexに投票
			}
		}catch(NumberFormatException nfe) {//失敗した時
			int i=questionnaireList.indexOf(text);//キーワードからIndexを取得
			questionnaire(bc, i);//そのIndexに投票。キーワードがない時は後ではじかれる
		}
		String tag=tm.getTag("アンケート");
		if(tag!=null&&questionnaireName!=null) {
			bc.em="実行中のアンケートを終了してください";
		}else if(tag!=null) {
			String[] keys;
			if(tag.isEmpty())keys=new String[]{""};
			else keys=tag.split(",");
			if(keys.length>0) {//最低でもタイトルは必須
				questionnaireName=keys[0];//最初の文字をタイトルに設定
				StringBuilder result=new StringBuilder("/アンケート名");//出力テキスト
				result.append(questionnaireName).append("\n");
				questionnaire=new int[keys.length-1];//最低大きさ0の配列
				for(int i=1;i<keys.length;i++) {
					String k=keys[i];
					questionnaireList.add(k);
					result.append(i-1).append(" : ").append(k).append("\n");
				}
				result.append("です");
				if(DiscordAPI.chatDefaultHost(result.toString())) {
					bc.em="アンケートを開始します";
				}else bc.em="開始したけどディスコードに接続できません";
			}
		}
		tag=tm.getTag("集計");
		if(tag!=null) {
			StringBuilder result=new StringBuilder("アンケート名");
			result.append(questionnaireName).append("\n");
			for(int i=0;i<questionnaire.length;i++) {
				String k=questionnaireList.get(i);
				result.append(k).append(" が").append(questionnaire[i]).append("票\n");
			}
			result.append("でした。");
			DiscordAPI.chatDefaultHost(result.toString());
			bc.em="アンケートを終了";
			questionnaireName=null;
			questionnaireList.clear();
			questionnaireUserList.clear();
		}
		if(text.indexOf("アンケート中?")>=0||text.indexOf("アンケート中？")>=0
				||text.indexOf("アンケ中?")>=0||text.indexOf("アンケ中？")>=0) {
			DiscordAPI.chatDefaultHost(questionnaireName==null?"してない":"してる");
		}
	}
	public static void questionnaire(BouyomiConection con,int index) {
		if(questionnaire.length<=index||index<0)return;//indexが無効な時に無視する。
		//例えばキーワードがない時、Index指定で範囲外の時
		//System.out.println("投票"+key);
		if(questionnaireUserList.containsKey(con.user)) {//ユーザが投票済の時
			Integer k=questionnaireUserList.get(con.user);//ユーザの投票先(index)
			questionnaire[k]--;
		}
		questionnaire[index]++;
		con.em="投票";
		questionnaireUserList.put(con.user,index);
	}
}