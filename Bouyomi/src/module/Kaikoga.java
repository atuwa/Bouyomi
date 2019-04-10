package module;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;

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
	public Kaikoga(){
		try{
			BouyomiProxy.load(kaikogaDB,"kaikoga.txt");
			lastWriteHashCode=kaikogaDB.hashCode();
		}catch(IOException e){
			e.printStackTrace();
		}
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
		if(con.text.equals("グレートカイコガ２")||con.text.equals("グレートカイコガ2")||con.text.equals("グレートカイコガ")){
			int r=new SecureRandom().nextInt(100)+1;//当選率5%
			String s=(r<=5 ? "ボロン (" : r<=15 ? "おしい(" : "はずれ (")+r+(con.user==null ? ")" : ")/*抽選者："+con.user);
			DiscordAPI.chatDefaultHost(s);
			System.out.println(s);
			if(r<=5) hit(con,con.userid);
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
		for(int index=0;index<Math.min(5,kaikogaDB.size());index++){
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