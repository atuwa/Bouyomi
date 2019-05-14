package module;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bouyomi.BouyomiProxy;
import bouyomi.DailyUpdate;
import bouyomi.DailyUpdate.IDailyUpdate;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;

public class いちご丸 implements IModule,IAutoSave,IDailyUpdate{

	private SecureRandom ランダム生成源=new SecureRandom();
	private int 合計距離;
	private boolean 保存済;
	private ArrayList<String> 今日引いた人達=new ArrayList<String>();
	public いちご丸(){
		try{
			BouyomiProxy.load(今日引いた人達,"いちご丸.txt");
			合計距離=Integer.parseInt(今日引いた人達.get(0),16);
			今日引いた人達.remove(0);
			保存済=true;
		}catch(IOException|NumberFormatException e){
			e.printStackTrace();
		}
		DailyUpdate.Ragister("いちご丸",this);
	}
	@Override
	public void call(Tag tag){
		if(tag.con.text.equals("痩せろデブ")) {
			if("534060228099178537".equals(tag.con.userid)||tag.isAdmin()) {
				new 抽選(tag).呼び出し();
			}else if(今日引いた人達.contains(tag.con.userid)) {
				DiscordAPI.chatDefaultHost("今日はもう引いたでしょ");
			}else new 抽選(tag).呼び出し();
		}
		if(tag.con.text.equals("今何メートル")||tag.con.text.toLowerCase().equals("今何m")) {
			DiscordAPI.chatDefaultHost(tag.con.mute?"/":""+合計距離+"m");
		}
		if(tag.con.text.equals("今何キロメートル")||tag.con.text.toLowerCase().equals("今何km")) {
	        DecimalFormat df = new DecimalFormat("#,##0.0");
			DiscordAPI.chatDefaultHost(tag.con.mute?"/":""+df.format(合計距離/1000D)+"km");
		}
		String パラメータ=tag.getTag("ちゃんと歩いたよ");
		if(パラメータ!=null) {
			boolean キロ指定=パラメータ.toLowerCase().contains("k");
			if(!キロ指定)キロ指定=パラメータ.contains("キロ");
			if(!キロ指定)キロ指定=パラメータ.contains("㌔");
			if(!キロ指定)キロ指定=パラメータ.contains("ｷﾛ");
			Matcher m=Pattern.compile("[0-9０-９]++").matcher(パラメータ);
			if(m.find()){
				String 数値抽出文字列=m.group();
				int 指定値=Integer.parseInt(数値抽出文字列);
				if(キロ指定)指定値*=1000;
				if(指定値>20000) {
					DiscordAPI.chatDefaultHost("指定ミスってない？("+指定値+"m)");
				}else {
					合計距離=合計距離-指定値;
					System.out.println("いちご丸が"+指定値+"m歩いて残り"+合計距離+"m");
					if(合計距離<-2000)合計距離=-2000;
					保存済=false;
			        DecimalFormat df = new DecimalFormat("#,##0.0");
					DiscordAPI.chatDefaultHost(tag.con.mute?"/残り":"残り"+合計距離+"m("+df.format(合計距離/1000D)+"km)");
				}
			}else tag.con.addTask.add("数値を指定してください");
		}
		パラメータ=tag.getTag("いちご値");
		if(パラメータ!=null) {
			if(パラメータ.isEmpty()) {
				DiscordAPI.chatDefaultHost(tag.con.mute?"/":""+Integer.toString(合計距離));
			}else if(tag.isAdmin())	try{
				合計距離=Integer.parseInt(パラメータ);
		        DecimalFormat df = new DecimalFormat("#,##0.0");
				DiscordAPI.chatDefaultHost(tag.con.mute?"/残り":"残り"+合計距離+"m("+df.format(合計距離/1000D)+"km)");
			}catch(NumberFormatException nfe) {}
		}
	}
	private class 抽選{
		private Tag tag;
		private int ランダム値;
		public 抽選(Tag tag){
			this.tag=tag;
		}
		public void 呼び出し() {
			今日引いた人達.add(tag.con.userid);
			ランダム値=ランダム生成源.nextInt(1000);
			if(ランダム値<10)むしゃむしゃ();
			else if(ランダム値<310)行く();
			else やだ();
		}
		private void むしゃむしゃ(){
			//単位はメートルで
			int 距離=ランダム生成源.nextInt(350)+50;
			String s=距離+"m減った("+ランダム値+")/*抽選者："+tag.con.user;
			System.out.println(s);
			if(tag.con.mute)s="/"+s;
			DiscordAPI.chatDefaultHost(s);
			合計距離=合計距離-距離;
			保存済=false;
		}
		private void 行く(){
			//単位はメートルで
			int 距離=ランダム生成源.nextInt(400)+100;
			String s=距離+"m行く("+ランダム値+")/*抽選者："+tag.con.user;
			System.out.println(s);
			if(tag.con.mute)s="/"+s;
			DiscordAPI.chatDefaultHost(s);
			合計距離=合計距離+距離;
			保存済=false;
		}
		private void やだ(){
			String s="行かない("+ランダム値+")/*抽選者："+tag.con.user;
			System.out.println(s);
			if(tag.con.mute)s="/"+s;
			DiscordAPI.chatDefaultHost(s);
		}
	}
	@Override
	public void autoSave() throws IOException{
		shutdownHook();
	}
	public void shutdownHook() {
		if(保存済)return;
		@SuppressWarnings("unchecked")
		ArrayList<String> コピー=(ArrayList<String>) 今日引いた人達.clone();
		String 距離文字列=Integer.toHexString(合計距離);
		コピー.add(0,距離文字列);
		try{
			BouyomiProxy.save(コピー,"いちご丸.txt");
			保存済=true;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void update(){
		DiscordAPI.chatDefaultHost("痩せろデブが引けるようになりました");
		今日引いた人達.clear();
		保存済=false;
	}
}