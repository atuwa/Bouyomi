package module;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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

public class Celeron implements IModule,IDailyUpdate,IAutoSave{

	private String[] cerelon= {"Intel Celeron B820","Intel Celeron G4920","Intel Celeron J4005",
			"Intel Celeron N4100","Intel Celeron N3450","Intel Celeron 3755U","Intel Celeron Dual-Core",
			"Intel Celeron D","Intel Celeron M","Intel Celeron B710"};

	private String file="Celeron.txt";
	private Random rundom=new SecureRandom();
	private int now;
	//はずれ
	private ArrayList<String> list=new ArrayList<String>();
	private int hc;
	public Celeron() {
		DailyUpdate.Ragister("Celeron",this);
		try{
			BouyomiProxy.load(list,file);
		}catch(IOException e){
			e.printStackTrace();
		}
		hc=list.hashCode();
		init();
	}
	@Override
	public void update() {
		now=rundom.nextInt(10)+1;
		DiscordAPI.chatDefaultHost("Celeron率が"+now+"%に変更されました");
	}
	@Override
	public void init() {
		now=rundom.nextInt(10);
	}
	@Override
	public void read(DataInputStream dis) throws IOException {
		now=(int) dis.readLong();
		System.out.println("起動時のCeleron率"+now);
	}
	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeLong(now);
	}
	public String get(int index) {
		while(index>=list.size())index-=list.size();
		return list.get(index);
	}
	@Override
	public void call(Tag tag){
		if(tag.con.text.toLowerCase().equals("celeron")) {
			int r=rundom.nextInt(1000)+1;
			String c;
			if(r<=now*10) {
				int index=r-1;
				while(index>=cerelon.length)index-=cerelon.length;
				c="あたり "+r+"/*"+cerelon[index];
			}else c="はずれ "+r+"/*"+get(r-now);
			if(tag.con.user!=null)c+=" 抽選者："+tag.con.user;
			c+=" 確率"+now+"%";
			System.out.println(c);
			if(!tag.con.mute)DiscordAPI.chatDefaultHost(c);
		}
		String p=tag.getTag("Celeron率変更");
		if(p!=null) {
			if(tag.isAdmin()){
				try {
					int i=Integer.parseInt(p);
					if(i>=0) {
						if(i>100)i=100;
						now=i;
						DiscordAPI.chatDefaultHost("Celeron率を"+now+"%に変更しました");
						DailyUpdate.updater.write();
					}
				}catch(NumberFormatException t) {
					DiscordAPI.chatDefaultHost("変更できませんでした");
				}
			}else DiscordAPI.chatDefaultHost("権限がありません");
		}
	}
	@Override
	public void autoSave() throws IOException{
		int nhc=list.hashCode();
		if(hc==nhc)return;
		hc=nhc;
		shutdownHook();
	}
	public void shutdownHook() {
		try{
			BouyomiProxy.save(list,file);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
