package module;

import java.io.IOException;
import java.security.SecureRandom;

import bouyomi.BouyomiConection;
import bouyomi.BouyomiProxy;
import bouyomi.Counter;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.ListMap;
import bouyomi.ListMap.Value;
import bouyomi.Tag;

/**おまけ機能*/
public class Kaikoga implements IModule,IAutoSave{

	//k=id v=count
	private ListMap<String,String> kaikogaDB=new ListMap<String,String>();
	public Kaikoga() {
		try{
			BouyomiProxy.load(kaikogaDB,"kaikoga.db");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void call(Tag tag){
		if(DiscordAPI.service_host==null)return;//Discordに送信できないときはこの機能は動かない
		BouyomiConection con=tag.con;
		if(con.text.equals("グレートカイコガ２")||con.text.equals("グレートカイコガ2")||con.text.equals("グレートカイコガ")){
			int r=new SecureRandom().nextInt(10)+1;//一時的に当選率10%
			String s=(r==1?"ボロン (":r<5?"おしい(":"はずれ (")+r+(con.user==null?")":")/*抽選者："+con.user);
			DiscordAPI.chatDefaultHost(s);
			System.out.println(s);
			if(r==1) {
				con.addTask.add("おめでとう当たったよ");
				String c=kaikogaDB.get(con.userid);
				String n;
				if(c==null) {
					n="1";
				}else try{
					int count=Integer.parseInt(c);
					n=Integer.toString(count+1);
				}catch(NumberFormatException nfe) {
					n="1";
				}
				kaikogaDB.put(con.userid,n);
			}
			//if(r<5)con.addTask.add("おしい");
		}
		if(tag.getTag("カイコガランキング")!=null) {
			DiscordAPI.chatDefaultHost(rank());
		}
	}
	public String rank() {
		kaikogaDB.sortValue(null);
		long all=0;
		for(Value<String,String> v:kaikogaDB.rawList()) {
			try{
				all+=Integer.parseInt(v.getValue());
			}catch(NumberFormatException nfe) {

			}
		}
		if(all<=0)return "ボロンした回数合計0回";
		StringBuilder sb=new StringBuilder("ボロンした合計");
		sb.append(all).append("回\n");
		for(Value<String,String> v:kaikogaDB.rawList()) {
			String name=Counter.getUserName(v.getKey());
			sb.append(name).append("が").append(v.getValue()).append("回");
			try{
				double i=Integer.parseInt(v.getValue());
				sb.append("/*(").append((i/all)*100).append("%)*/\n");
			}catch(NumberFormatException nfe) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	@Override
	public void autoSave() {
		if(kaikogaDB.isEmpty())return;
		try{
			BouyomiProxy.save(kaikogaDB,"kaikoga.db");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void shutdownHook(){
		autoSave();
	}
}