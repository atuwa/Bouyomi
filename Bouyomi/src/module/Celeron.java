package module;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import bouyomi.BouyomiConection;
import bouyomi.BouyomiProxy;
import bouyomi.DailyUpdate;
import bouyomi.DailyUpdate.IDailyUpdate;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;

public class Celeron implements IModule,IDailyUpdate,IAutoSave{

	private String[] celeron= {"Intel Celeron B820","Intel Celeron G4920","Intel Celeron J4005",
			"Intel Celeron N4100","Intel Celeron N3450","Intel Celeron 3755U","Intel Celeron Dual-Core",
			"Intel Celeron D","Intel Celeron M","Intel Celeron B710"};

	private String file="Celeron.txt";
	private Random rundom=new SecureRandom();
	protected int now;
	/**もう引いた人*/
	private HashMap<String,String> used=new HashMap<String,String>();
	//はずれ
	private ArrayList<String> list=new ArrayList<String>();
	private int hc;

	private boolean saved;
	public Celeron() {
		DailyUpdate.Ragister("Celeron",this);
		try{
			BouyomiProxy.load(list,file);
		}catch(IOException e){
			e.printStackTrace();
		}
		ArrayList<String> c=new ArrayList<String>();
		try {
			BouyomiProxy.load(c,"celeron.celeron.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
		for(String s:celeron)c.add(s);
		celeron=c.toArray(new String[c.size()]);
		hc=list.hashCode();
		init();
		try{
			BouyomiProxy.load(used,"CeleronUp.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void update() {
		used.clear();
		now=rundom.nextInt(10)+1;
		DiscordAPI.chatDefaultHost("Celeron率が"+now+"%に変更されました");
	}
	@Override
	public void init() {
		now=rundom.nextInt(10)+1;
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
				while(index>=celeron.length)index-=celeron.length;
				c="あたり "+r+"/*"+celeron[index];
			}else {
				if(r<=now*20)c="おしい";
				else c="はずれ ";
				c+=r+"/*"+get(r-now);
			}
			//System.out.println(c+" 確率"+now+"%");
			if(tag.con.user!=null) {
				//c=Util.IDtoMention(tag.con.userid)+c;
				c+=" 抽選者："+tag.con.user;
			}
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
		p=tag.getTag("Celeron率");
		if(p!=null) {
			DiscordAPI.chatDefaultHost("現在のCeleron率は"+now+"%です");
		}
		if(tag.con.userid!=null&&tag.con.mentions.contains("539105406107254804")) {
			agete(tag.con);
		}
	}
	private void agete(BouyomiConection con) {
		//System.out.println(tag.con.text);
		if(con.text.equals("Celeron率上げて")) {
			if(used.containsKey(con.userid)) {
				String v=used.get(con.userid);
				if(v.isEmpty()) {
					DiscordAPI.chatDefaultHost("ちょっと待って");
				}else if(Boolean.valueOf(v)){
					DiscordAPI.chatDefaultHost("今日は上げたでしょ");
				}else DiscordAPI.chatDefaultHost("今日は上げないよ");
				if(!BouyomiProxy.admin.isAdmin(con.userid))return;
			}
			saved=false;
			used.put(con.userid,"");
			if(DiscordAPI.chatDefaultHost("うーん...")) {
				new up(con.userid).start();
			}
		}
	}
	private class up extends Thread{
		private final String userid;
		public up(String user) {
			super("Celeron率上げて");
			userid=user;
		}
		public void run() {
			try{
				Thread.sleep(3000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			Random r=new Random();
			int i=30;
			if(r.nextInt(100)<10){
				DiscordAPI.chatDefaultHost("うーん...");
				try{
					Thread.sleep(2000);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
				i+=20;
			}
			if(r.nextInt(100)>i){
				used.put(userid,"false");
				DiscordAPI.chatDefaultHost("上げない");
				return;
			}
			int n=r.nextInt(4)+1;
			for(IModule m:BouyomiProxy.module.modules) {
				if(m instanceof Celeron) {
					((Celeron)m).now+=n;
					DiscordAPI.chatDefaultHost(n+"%上げた");
					used.put(userid,"true");
					break;
				}
			}
		}
	}
	@Override
	public void autoSave() throws IOException{
		saveused();
		int nhc=list.hashCode();
		if(hc==nhc)return;
		hc=nhc;
		saveCelerons();
	}
	public void shutdownHook() {
		saveused();
		saveCelerons();
	}
	public void saveCelerons(){
		try{
			BouyomiProxy.save(list,file);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	public void saveused() {
		if(!saved)try{
			BouyomiProxy.save(used,"Dosukebe.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
