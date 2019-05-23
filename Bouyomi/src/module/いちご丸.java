package module;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bouyomi.BouyomiProxy;
import bouyomi.Counter;
import bouyomi.DailyUpdate;
import bouyomi.DailyUpdate.IDailyUpdate;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;
import bouyomi.Util;

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
			Matcher m=Pattern.compile("[0-9０-９]++.?[0-9０-９]*+").matcher(パラメータ);
			if(m.find()){
				String 数値抽出文字列=m.group();
				double 指定値=Double.parseDouble(数値抽出文字列);
				if(キロ指定)指定値*=1000;
				if(指定値>20000) {
					DiscordAPI.chatDefaultHost("指定ミスってない？("+指定値+"m)");
				}else {
					int 元の距離=合計距離;
					合計距離=(int) (合計距離-指定値);
					StringBuilder システムメッセージ=new StringBuilder("いちご丸が");
					システムメッセージ.append(元の距離-合計距離).append("m歩いて残り");
					システムメッセージ.append(合計距離).append("m");
					運動ログ(システムメッセージ.toString());
					System.out.println(システムメッセージ);
					StringBuilder 投稿メッセージ=new StringBuilder();
					if(tag.con.mute)投稿メッセージ.append("/");
					投稿メッセージ.append("残り");
					boolean limit=false;
					if(リミッター())limit=true;
					投稿メッセージ.append(合計距離).append("m(");
			        DecimalFormat df = new DecimalFormat("#,##0.0");
					投稿メッセージ.append(df.format(合計距離/1000D)).append("km)");
					if(limit)投稿メッセージ.append("下限-2km");
					保存済=false;
					DiscordAPI.chatDefaultHost(投稿メッセージ.toString());
				}
			}else tag.con.addTask.add("数値を指定してください");
		}
		パラメータ=tag.getTag("いちご値");
		if(パラメータ!=null) {
			if(パラメータ.isEmpty()) {
				DiscordAPI.chatDefaultHost(tag.con.mute?"/":""+Integer.toString(合計距離));
			}else if(tag.isAdmin())	try{
				合計距離=Integer.parseInt(パラメータ);
				保存済=false;
		        DecimalFormat df = new DecimalFormat("#,##0.0");
				DiscordAPI.chatDefaultHost(tag.con.mute?"/残り":"残り"+合計距離+"m("+df.format(合計距離/1000D)+"km)");
			}catch(NumberFormatException nfe) {}
		}
		パラメータ=tag.getTag("痩せろデブをもう一回引けるようにする");
		if(パラメータ!=null&&tag.isAdmin()) {
			if(パラメータ.isEmpty()) {
				DiscordAPI.chatDefaultHost(tag.con.mute?"/":""+今日引いた人達.size()+"件の引いた人リストを消去しました");
				今日引いた人達.clear();
			}else if(今日引いた人達.contains(パラメータ)) {
				if(今日引いた人達.remove(パラメータ)) {
					DiscordAPI.chatDefaultHost(tag.con.mute?"/":""+Counter.getUserName(パラメータ)+"を引いた人リストから消去しました");
				}
			}
		}
	}
	private void 運動ログ(String ログテキスト) {
		SimpleDateFormat 日付フォーマットするやつ = new SimpleDateFormat("yyyy年MM月dd日HH時mm分");
		String 日付時刻=日付フォーマットするやつ.format(new Date());
		try{
			FileOutputStream fos=new FileOutputStream("いちご丸.log",true);//追加モードで開く
			BufferedOutputStream ログ出力先=new BufferedOutputStream(fos);
			try{
				ログ出力先.write((日付時刻+"\t"+ログテキスト+"\t"+System.currentTimeMillis()+"\n").getBytes(StandardCharsets.UTF_8));
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				try{
					ログ出力先.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
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
			ランダム値=ランダム生成源.nextInt(1000);//0～1000のランダムを生成
			if(ランダム値<1)まさかこれを引くとは();
			else if(ランダム値<11)むしゃむしゃ();
			else if(ランダム値<261)行く();
			else やだ();
			保存済=false;
		}
		private void まさかこれを引くとは(){
			合計距離+=10000;//10km増える
			StringBuilder sb=new StringBuilder();
			if(tag.con.mute)sb.append("/");
			sb.append(Util.IDtoMention(tag.con.userid));
			sb.append("0.1%のこれを引くとは凄い運だな\n10km行く");
			sb.append("(").append(ランダム値).append(")");
			if(リミッター())sb.append("20km上限");
			String s=sb.toString();
			System.out.println(s);
			DiscordAPI.chatDefaultHost(s);
		}
		private void むしゃむしゃ(){
			//単位はメートルで
			int 距離=ランダム生成源.nextInt(350)+50;
			合計距離=合計距離-距離;
			StringBuilder sb=new StringBuilder();
			if(tag.con.mute)sb.append("/");
			sb.append(距離);
			sb.append("m減った(").append(ランダム値).append(")");
			if(リミッター())sb.append("-2km下限");
			sb.append("/*抽選者：").append(tag.con.user);
			String s=sb.toString();
			System.out.println(s);
			DiscordAPI.chatDefaultHost(s);
		}
		private void 行く(){
			//単位はメートルで
			int 距離=ランダム生成源.nextInt(450)+300;
			合計距離=合計距離+距離;
			StringBuilder sb=new StringBuilder();
			if(tag.con.mute)sb.append("/");
			sb.append(距離);
			sb.append("m行く(").append(ランダム値).append(")");
			if(リミッター())sb.append("20km上限");
			sb.append("/*抽選者：").append(tag.con.user);
			String s=sb.toString();
			System.out.println(s);
			DiscordAPI.chatDefaultHost(s);
		}
		private void やだ(){
			String s="行かない("+ランダム値+")/*抽選者："+tag.con.user;
			System.out.println(s);
			if(tag.con.mute)s="/"+s;
			DiscordAPI.chatDefaultHost(s);
		}
	}
	protected boolean リミッター() {
		int 最大値=20*1000;//TODO 上限20km
		int 最小値=-2*1000;//TODO 下限-2km
		if(合計距離>最大値) {
			合計距離=最大値;
			return true;
		}
		if(合計距離<最小値) {
			合計距離=最小値;
			return true;
		}
		return false;
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