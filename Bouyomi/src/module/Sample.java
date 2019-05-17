package module;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;
import bouyomi.TubeAPI;
import bouyomi.TubeAPI.PlayVideoEvent;
import bouyomi.TubeAPI.PlayVideoTitleEvent;
import bouyomi.Util;

public class Sample implements IModule{

	@Override
	public void call(Tag tag){
		if(tag.con.mentions.contains("539105406107254804")) {//メンションリストに539105406107254804が含まれる場合
			if(tag.con.text.contains("サンプルモジュール")) {//「サンプルモジュール」と言うメッセージを含む場合
				String m=Util.IDtoMention(tag.con.userid);//この書き込みをしたユーザIDからメンションを生成
				DiscordAPI.chatDefaultHost(m+"サンプルモジュール");//メンションとテキストを連結して投稿
			}
		}
		String s=tag.getTag("サンプルモジュール");//タグ取得
		if(s!=null) {//タグが無い時はnull
			String m=Util.IDtoMention(tag.con.userid);//この書き込みをしたユーザIDからメンションを生成
			DiscordAPI.chatDefaultHost(m+s.length());//メンションとタグの内容を連結して投稿
		}
		if(tag.con.mentions.contains("539105406107254804")) {
			if(tag.con.text.equals("働け")||tag.con.text.equals("仕事しろ")) {
				DiscordAPI.chatDefaultHost("やだ");
			}
		}
		String seedS=tag.getTag("ランダム文字列");
		if(seedS!=null) {
			String[] parm=seedS.isEmpty()?null:seedS.split(",");
			long seedN=0;
			if(parm!=null&&parm.length>1)try{
				seedN=Long.parseLong(parm[1]);
			}catch(NumberFormatException nfe) {
				char[] ca=parm[1].toCharArray();
				for(char c:ca)seedN+=c;
			}
			int len=10;
			if(parm!=null)try{
				len=Integer.parseInt(parm[0]);
			}catch(NumberFormatException nfe) {

			}
			Random r;
			if(seedN==0)r=new Random();
			else r=new Random(seedN);
			StringBuilder sb=new StringBuilder("/");
			for(int i=0;i<len;i++) {
				sb.append((char)r.nextInt(65514));
			}
			DiscordAPI.chatDefaultHost(sb.toString());
		}
		String org=tag.getTag("文字化け");
		if(org!=null) {
			try{
				byte[] b=org.getBytes(StandardCharsets.UTF_8);
				StringBuilder sb=new StringBuilder("/Shift-JIS\n```");
				String result=new String(b,"shift-jis");
				sb.append(result);
				sb.append("```\nEUC-JP\n```");
				result=new String(b,"euc-jp");
				sb.append(result);
				sb.append("```");
				DiscordAPI.chatDefaultHost(sb.toString());
			}catch(UnsupportedEncodingException e){
				e.printStackTrace();
			}
		}
		org=tag.getTag("16進数E");
		if(org!=null) {
			byte[] ba=org.getBytes(StandardCharsets.UTF_8);
			StringBuilder sb=new StringBuilder("/");
			for(byte b:ba) {
				int i=b&0x000000FF;
				String hex=Integer.toHexString(i);
				if(hex.length()<2)sb.append("0");
				sb.append(hex);
			}
			DiscordAPI.chatDefaultHost(sb.toString());
		}
		org=tag.getTag("16進数D");
		if(org!=null) {
			StringReader r=new StringReader(org);
			byte[] ba=new byte[org.length()/2];
			char[] cbuf=new char[2];
			for(int i=0;i<ba.length;i++) {
				try{
					int rl=r.read(cbuf);
					if(rl<0)break;
					ba[i]=(byte) (Integer.parseInt(String.valueOf(cbuf),16)&0xFF);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			String d=new String(ba,StandardCharsets.UTF_8);
			DiscordAPI.chatDefaultHost(d);
		}
	}
	@Override
	public void event(BouyomiEvent o) {
		if(o instanceof PlayVideoEvent) {
			PlayVideoEvent e=(PlayVideoEvent)o;
			//System.out.println("動画再生を検出"+e.videoID);
			if(e.videoID.equals("nico=sm14223749")) {
				//DiscordAPI.chatDefaultHost("動画停止()/*この動画は再生禁止です");
				return;
			}
		}
		if(o instanceof PlayVideoTitleEvent) {
			PlayVideoTitleEvent e=(PlayVideoTitleEvent)o;
			if("nico=sm14223749".equals(TubeAPI.lastPlay)){
				new wait("動画停止()/*この動画は再生禁止です").start();
				return;
			}
			//System.out.println("動画タイトルを取得："+e.title);
			if(e.title.contains("オカリン"))new wait("動画停止()/*タイトルに再生禁止ワードが含まれています").start();
		}
	}
	private class wait extends Thread{
		private String st;
		public wait(String s) {
			st=s;
		}
		@Override
		public void run() {
			try{
				Thread.sleep(1500);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			DiscordAPI.chatDefaultHost(st);
		}
	}
}