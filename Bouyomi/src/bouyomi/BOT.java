package bouyomi;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

public class BOT{

	/**自動応答辞書*/
	public static HashMap<String,String> BOT=new HashMap<String,String>();
	//Google翻訳から持ってきた変数名
	public static HashMap<String,String> PartialMatchBOT=new HashMap<String,String>();
	public static String BOTpath;
	private static int MatchCount;
	private static String LastMatch;
	static {
		BOT.put("ちくわ大明神","b) 誰だいまの");//デフォルトの自動応答
	}
	public static void printList(){
		BOT.forEach(new BiConsumer<String,String>(){
			@Override
			public void accept(String key,String val){
				System.out.println("key="+key+" val="+val);
			}
		});
		PartialMatchBOT.forEach(new BiConsumer<String,String>(){
			@Override
			public void accept(String key,String val){
				System.out.println("key="+key+" val="+val);
			}
		});
	}
	public static void loadBOT() {
		try{
			BouyomiProxy.load(BOT,BOTpath);
		}catch(IOException e){
			e.printStackTrace();
		}
		try{
			File f=new File(BOTpath);
			String pf=f.getParent();
			String fp=new File(pf,"PM-"+f.getName()).getAbsolutePath();
			BouyomiProxy.load(PartialMatchBOT,fp);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	public static void saveBOT() {
		try{
			BouyomiProxy.save(BOT,BOTpath);
		}catch(IOException e){
			e.printStackTrace();
		}
		try{
			File f=new File(BOTpath);
			String pf=f.getParent();
			String fp=new File(pf,"PM-"+f.getName()).getAbsolutePath();
			BouyomiProxy.save(PartialMatchBOT,fp);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	public static boolean isRegister(String key,String value) {
		if(BOT.containsValue(key))return true;
		if(BOT.containsKey(value))return true;
		class R implements BiConsumer<String,String>{
			public boolean end;
			@Override
			public void accept(String key,String val){
				if(end)return;
				if(key.indexOf(val)>=0)end=true;
			}
		}
		R r=new R();
		PartialMatchBOT.forEach(r);
		return r.end;
	}
	public static void call(BouyomiConection bc) {
		if(!BotRes(bc,BOT,false)) {
			BotRes(bc,PartialMatchBOT,true);
		}
	}
	private static boolean BotRes(BouyomiConection bc,HashMap<String, String> BOT,boolean pm) {
		if(bc.addTask.isEmpty())for(Entry<String, String> e:BOT.entrySet()){
			String key=e.getKey();
			String val=e.getValue();
			if(pm) {
				if(bc.text.indexOf(key)>=0) {//読み上げテキストにキーが含まれている時
					if(key.equals(LastMatch))MatchCount++;
					if(MatchCount>5)return true;
					System.out.println("BOT応答キー =部分一致："+key);//ログに残す
					if(!DiscordAPI.chatDefaultHost(val))bc.addTask.add(val);//追加で言う
					LastMatch=key;
					return true;
				}
			}else if(bc.text.equals(key)) {//読み上げテキストがキーに一致した時
				if(key.equals(LastMatch))MatchCount++;
				if(MatchCount>5)return true;
				System.out.println("BOT応答キー ="+key);//ログに残す
				if(!DiscordAPI.chatDefaultHost(val))bc.addTask.add(val);//追加で言う
				LastMatch=key;
				return true;
			}
		}
		return false;
	}
	public static void tag(Tag t){
		BouyomiConection con=t.con;
		if(con.text.indexOf("応答(")==0||con.text.indexOf("応答（")==0) {//自動応答機能を使う時
			//System.out.println(text);//ログに残す
			int ei=con.text.indexOf('=');
			if(ei<0)ei=con.text.indexOf('＝');
			int ki=con.text.lastIndexOf(')');
			int zi=con.text.lastIndexOf('）');
			if(ki<zi)ki=zi;
			if(ei>0&&ki>1) {
				String key=con.text.substring(3,ei);
				String val=con.text.substring(ei+1,ki);
				t.removeTag("応答",key+"="+val);
				t.removeTag("応答",key+"＝"+val);
				if(!key.equals(val)&&!isRegister(key,val)) {
					con.addTask.add(key+" には "+val+" を返します");
					if(key.indexOf("部分一致：")==0||key.indexOf("部分一致:")==0) {
						System.out.println("部分一致応答登録（"+key+"="+val+")");//ログに残す
						PartialMatchBOT.put(key.substring(5),val);
					}else{
						System.out.println("完全一致応答登録（"+key+"="+val+")");//ログに残す
						BOT.put(key,val);
					}
				}else {
					con.addTask.add("登録できません");
					System.out.println("応答登録不可（"+key+"="+val+")");//ログに残す
				}
			}
		}
		String tag=t.getTag("応答破棄");
		if(tag!=null) {//自動応答機能を使う時
			//System.out.println(text);//ログに残す
			if(tag.indexOf("部分一致：")==0||tag.indexOf("部分一致:")==0) {
				if(PartialMatchBOT.remove(tag.substring(5))!=null) {
					con.addTask.add(tag+" には応答を返しません");
					System.out.println("部分一致応答破棄（"+tag+")");//ログに残す
				}else con.addTask.add(tag+" には応答が設定されてません");
			}else if(BOT.remove(tag)!=null) {
				con.addTask.add(tag+" には応答を返しません");
				System.out.println("応答破棄（"+tag+")");//ログに残す
			}else con.addTask.add(tag+" には応答が設定されてません");
		}
		tag=t.getTag("応答一覧");
		if(tag!=null) {
			final StringBuilder sb=new StringBuilder("/");
			BOT.forEach(new BiConsumer<String,String>(){
				@Override
				public void accept(String key,String val){
					sb.append("key=").append(key).append(" val=").append(val).append("\n");
				}
			});
			PartialMatchBOT.forEach(new BiConsumer<String,String>(){
				@Override
				public void accept(String key,String val){
					sb.append("部分一致").append("key=").append(key).append(" val=").append(val).append("\n");
				}
			});
			DiscordAPI.chatDefaultHost(sb.toString());
		}
	}
}
