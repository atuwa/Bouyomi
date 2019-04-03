package module;

import java.io.IOException;
import java.util.HashMap;

import bouyomi.BouyomiProxy;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;

public class Memo implements IModule,IAutoSave{

	private HashMap<String,String> data=new HashMap<String,String>();
	private boolean saved=true;
	{
		try{
			BouyomiProxy.load(data,"Memo.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void call(Tag tag){
		String t=tag.getTag("memo");
		String u=tag.con.userid;
		if(t==null||u==null)return;
		if(t.isEmpty()) {
			DiscordAPI.chatDefaultHost(data.getOrDefault(u,"null"));
		}else {
			data.put(u,t);
			saved=false;
			tag.con.addTask.add("メモしました");
		}
	}
	@Override
	public void autoSave(){
		if(!saved)try{
			BouyomiProxy.save(data,"Memo.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void shutdownHook() {
		try{
			BouyomiProxy.save(data,"Memo.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}