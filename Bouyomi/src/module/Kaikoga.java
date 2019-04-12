package module;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import bouyomi.BouyomiConection;
import bouyomi.BouyomiProxy;
import bouyomi.Counter;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.ListMap;
import bouyomi.ListMap.Value;
import bouyomi.Tag;

/** おまけ機能 */
public class Kaikoga implements IModule,IAutoSave{

	//k=id v=count
	private ListMap<String, String> kaikogaDB=new ListMap<String, String>();
	private int lastWriteHashCode;
	private int kakuritu;
	private Random rundom=new SecureRandom();
	public Kaikoga(){
		try{
			BouyomiProxy.load(kaikogaDB,"kaikoga.txt");
			lastWriteHashCode=kaikogaDB.hashCode();
		}catch(IOException e){
			e.printStackTrace();
		}
		class KakurituUpdate extends Thread{
			public KakurituUpdate(){
				super("KakurituUpdate");
				file="kaikoga.ritu";
				read();
			}
			private long lastUpdate;
			private String file;
			@Override
			public void run() {
				SimpleDateFormat f=new SimpleDateFormat("DDD");
				while(true) {
					String up=f.format(new Date(lastUpdate));
					String now=f.format(new Date());
					if(!up.equals(now)) {
						kakuritu=rundom.nextInt(10)+1;
						lastUpdate=System.currentTimeMillis();
						write();
						DiscordAPI.chatDefaultHost("カイコガボロン率が"+kakuritu+"%に変更されました");
					}
					try{
						Thread.sleep(60000);
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
			}
			private void read() {
				try{
					File f=new File(file);
					if(!f.exists()) {
						kakuritu=rundom.nextInt(10)+1;
						return;
					}
					FileInputStream fis=new FileInputStream(f);
					DataInputStream dis=new DataInputStream(fis);
					try{
						kakuritu=(int) dis.readLong();
						lastUpdate=dis.readLong();
					}catch(IOException e){
						e.printStackTrace();
					}finally {
						try{
							dis.close();
						}catch(IOException e){
							e.printStackTrace();
						}
					}
				}catch(FileNotFoundException e){
					e.printStackTrace();
				}
			}
			public void write() {
				try{
					FileOutputStream fos=new FileOutputStream(new File(file));
					DataOutputStream dos=new DataOutputStream(fos);
					try{
						dos.writeLong(kakuritu);
						dos.writeLong(lastUpdate);
					}catch(IOException e){
						e.printStackTrace();
					}finally {
						try{
							dos.close();
						}catch(IOException e){
							e.printStackTrace();
						}
					}
				}catch(FileNotFoundException e){
					e.printStackTrace();
				}
			}
		}
		new KakurituUpdate().start();
	}
	@Override
	public void call(Tag tag){
		if(DiscordAPI.service_host==null) return;//Discordに送信できないときはこの機能は動かない
		BouyomiConection con=tag.con;
		String str=tag.getTag("ボロンさせろ");
		if(str!=null){
			if(BouyomiProxy.admin.isAdmin(con.userid)){
				if(str.isEmpty()) str=con.userid;
				DiscordAPI.chatDefaultHost("了解。"+Counter.getUserName(str)+"の要求としてボロンさせます");
				hit(con,str);
			}else con.addTask.add("権限がありません");
		}
		str=tag.getTag("ボロン抹消");
		if(str!=null){
			if(BouyomiProxy.admin.isAdmin(con.userid)){
				if(str.isEmpty()) str=con.userid;
				String old=kaikogaDB.remove(str);
				if(old!=null) DiscordAPI.chatDefaultHost("了解。"+Counter.getUserName(str)+"のボロンを抹消します");
				else DiscordAPI.chatDefaultHost(Counter.getUserName(str)+"のボロンを抹消出来ませんでした");
			}else con.addTask.add("権限がありません");
		}
		str=tag.getTag("ボロン減算");
		if(str!=null){
			if(BouyomiProxy.admin.isAdmin(con.userid)){
				String c=kaikogaDB.get(str);
				String n;
				if(c==null){
					n="1";
				}else try{
					int count=Integer.parseInt(c);
					n=Integer.toString(count<1 ? 0 : count-1);
				}catch(NumberFormatException nfe){
					n="1";
				}
				if("0".equals(n)){
					kaikogaDB.remove(str);
					DiscordAPI.chatDefaultHost("了解。"+Counter.getUserName(str)+"のボロンを抹消します");
				}else{
					kaikogaDB.put(str,n);
					DiscordAPI.chatDefaultHost("了解。"+Counter.getUserName(str)+"のボロンを"+n+"にさせます");
				}
			}else con.addTask.add("権限がありません");
		}
		str=tag.getTag("ボロン率");
		if(str!=null) {
			DiscordAPI.chatDefaultHost("現在のボロン率は"+kakuritu+"%に変更されました");
		}
		if(con.text.equals("グレートカイコガ２")||con.text.equals("グレートカイコガ2")||con.text.equals("グレートカイコガ")){
			int r=rundom.nextInt(100)+1;//当選率可変
			String s=(r<=kakuritu ? "ボロン (" : r<=(kakuritu+10) ? "おしい(" : "はずれ (")+r+(con.user==null ? ")" : ")/*抽選者："+con.user);
			DiscordAPI.chatDefaultHost(s);
			System.out.println(s);
			if(r<=kakuritu) hit(con,con.userid);
			//if(r<5)con.addTask.add("おしい");
		}
		if(tag.getTag("カイコガランキング")!=null||"カイコガランキング".equals(con.text)){
			DiscordAPI.chatDefaultHost(rank());
		}
	}
	private void hit(BouyomiConection con,String id){
		con.addTask.add("おめでとう当たったよ");
		String c=kaikogaDB.get(id);
		String n;
		if(c==null){
			n="1";
		}else try{
			int count=Integer.parseInt(c);
			n=Integer.toString(count+1);
		}catch(NumberFormatException nfe){
			n="1";
		}
		kaikogaDB.put(id,n);
	}
	public String rank(){
		kaikogaDB.sortValue(null);
		long all=0;
		for(Value<String, String> v:kaikogaDB.rawList()){
			try{
				all+=Integer.parseInt(v.getValue());
			}catch(NumberFormatException nfe){

			}
		}
		if(all<=0) return "ボロンした回数合計0回";
		DecimalFormat fo=new DecimalFormat("##0.00%");
		StringBuilder sb=new StringBuilder("ボロンした合計");
		sb.append(all).append("回/*\n");
		for(int index=0;index<Math.min(500,kaikogaDB.size());index++){
			Value<String, String> v=kaikogaDB.rawList().get(index);
			String name=Counter.getUserName(v.getKey());
			sb.append(name).append(" が").append(v.getValue()).append("回");
			try{
				double i=Integer.parseInt(v.getValue());
				sb.append("(").append(fo.format(i/all)).append(")\n");
			}catch(NumberFormatException nfe){
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	@Override
	public void autoSave(){
		int hc=kaikogaDB.hashCode();
		if(hc==lastWriteHashCode) return;
		lastWriteHashCode=hc;
		shutdownHook();
	}
	@Override
	public void shutdownHook(){
		if(kaikogaDB.isEmpty()) return;
		try{
			BouyomiProxy.save(kaikogaDB,"kaikoga.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}