package module;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import bouyomi.BouyomiProxy;
import bouyomi.DailyUpdate;
import bouyomi.DailyUpdate.IDailyUpdate;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;

public class Dosukebe implements IModule,IDailyUpdate,IAutoSave{

	/**もう引いた人*/
	private ArrayList<String> used=new ArrayList<String>();
	private Random rundom=new SecureRandom();
	private static final int min=200,max=1000;//最低2%最大10%
	private int now=rundom.nextInt(max-min)+min;
	private boolean saved=false;
	public Dosukebe(){
		try{
			BouyomiProxy.load(used,"Dosukebe.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
		DailyUpdate.Ragister("Dosukebe",this);
	}
	@Override
	public void autoSave() throws IOException{
		shutdownHook();
	}
	public void shutdownHook() {
		if(saved)return;
		try{
			BouyomiProxy.save(used,"Dosukebe.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	/**日替わりシステムを引いた人リストの初期化に使う
	 * だから0時にリセット*/
	@Override
	public void update(){
		now=rundom.nextInt(max-min)+min;
		used.clear();
	}
	public void read(DataInputStream dis)throws IOException{
		now=dis.readInt();
	};
	public void write(DataOutputStream dos)throws IOException{
		dos.writeInt(now);
	};
	@Override
	public void call(Tag tag){
		if(tag.con.text.equals("ミュージックスタート")){
			if(tag.con.userid==null)tag.con.addTask.add("ユーザID取得エラー");
			if(used.contains(tag.con.userid)) {
				DiscordAPI.chatDefaultHost("今日は既に引いてます");
			}else{
				if(!tag.isAdmin()) {
					used.add(tag.con.userid);
					saved=false;
				}
				int rand=rundom.nextInt(10000);
				StringBuilder sb=new StringBuilder();
				if(rand<now) {
					sb.append("後は任せたドスケベ(再生システムは後で実装する)");
					//play();
				}else sb.append("却下");
				sb.append("(").append(rand).append(")");
				sb.append("確率").append(now/100d).append("%");
				DiscordAPI.chatDefaultHost(sb.toString());
			}
		}
		String s=tag.getTag("ドスケベ率");
		if(s!=null) {
			if(s.isEmpty())DiscordAPI.chatDefaultHost(now/100d+"%");
			else {
				try {
					String old=Float.toString(now/100f);
					now=(int) (Float.parseFloat(s)*100);
					DiscordAPI.chatDefaultHost("ドスケベ率を"+old+"%から"+now/100f+"%に変更しました");
				}catch(NumberFormatException nfe) {

				}
			}
		}
	}
	public void play() {
		File dir=new File("Dosukebe");
	}
}