package module;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bouyomi.BouyomiProxy;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;
import bouyomi.Util.JsonUtil;

public class NicoAlart implements IModule,IAutoSave, Runnable{
	private Thread thread;
	private int lastWriteHashCode,lastWriteHashCodeA;
	private HashMap<String,String> shortcutDB=new HashMap<String,String>();
	public static HashMap<String,String> alarted=new HashMap<String,String>();
	public static class NicoLiveEvent implements BouyomiEvent{
		public Live[] live;
		private NicoLiveEvent(Live[] lv) {
			live=lv;
		}
	}
	public NicoAlart(){
		thread=new Thread(this,"定期ニコニコ生放送コミュニティ検索");
		thread.start();
	}
	@Override
	public void call(Tag tag){
		String s=tag.getTag("ニコニコ生放送コミュニティ検索");
		if(s!=null) {
			try{
				int cid=Integer.parseInt(s);
				DiscordAPI.chatDefaultHost("検索URL=https://api.search.nicovideo.jp/api/v2/live/contents/search"+getParm(null,cid));
				Live[] lives=getLives(null,cid);
				if(lives.length>0) {
					StringBuilder sb=new StringBuilder();
					for(Live lv:lives)sb.append(lv);
					s=sb.toString();
				}else s="放送されてません";
				DiscordAPI.chatDefaultHost(s);
			}catch(NumberFormatException|IOException e){
				e.printStackTrace();
			}
		}
		s=tag.getTag("ニコニコ生放送コミュニティ検索RAW","ニコニコ生放送コミュニティ検索raw");
		if(s!=null) {
			try{
				int cid=Integer.parseInt(s);
				String q="	ゲーム OR 描いてみた OR リスナーは外部記憶装置 OR 通知用";
				DiscordAPI.chatDefaultHost("検索URL=https://api.search.nicovideo.jp/api/v2/live/contents/search"+getParm(q,cid));
				String js=getLiveJSON(q,cid);
				DiscordAPI.chatDefaultHost(js);
			}catch(NumberFormatException|IOException e){
				e.printStackTrace();
				chatException(e);
			}
		}
		for(Entry<String, String> e:shortcutDB.entrySet()) {
			if(tag.con.text.equals(e.getKey()+"放送してる？")) {
				System.out.println(e.getKey()+"放送してる？");
				try{
					int id=Integer.parseInt(e.getValue());
					try{
						Live[] lives=getLives(null,id);
						if(lives.length>0) {
							DiscordAPI.chatDefaultHost("https://live2.nicovideo.jp/watch/"+lives[0].contentId+" でしてる");
						}else DiscordAPI.chatDefaultHost("多分してない/*ニコニコの検索サーバが遅延してるかも");
					}catch(IOException ex){
						DiscordAPI.chatDefaultHost("わかんにゃい！");
						chatException(ex);
					}
				}catch(NumberFormatException nfe) {

				}
			}
		}
		s=tag.getTag("放送チェックショートカット");
		if(s!=null) {
			int index=s.indexOf("=");
			if(index>0){
				try{
					String key=s.substring(0,index);
					String val=s.substring(index+1);
					Matcher m=Pattern.compile("co[0-9]++").matcher(val);
					if(m.find()) {
						m=Pattern.compile("[0-9]++").matcher(m.group());
						m.find();
						try {
							int co=Integer.parseInt(m.group());
							shortcutDB.put(key,val=Integer.toString(co));
							DiscordAPI.chatDefaultHost(val+"放送してる？ に完全一致でco"+co+"が放送してるか取得できるように登録");
						}catch(NumberFormatException nfe) {
							DiscordAPI.chatDefaultHost("コミュID指定ミスってる");
						}
					}
				}catch(Exception e) {
					chatException(e);
				}
			}
		}
		/*
		s=tag.getTag("おっさん放送してる？");
		if(s!=null) {
			try{
				Live[] lives=getLives(null,1003067);
				if(lives.length>0) {
					DiscordAPI.chatDefaultHost("https://live2.nicovideo.jp/watch/"+lives[0].contentId+" でしてる");
				}else DiscordAPI.chatDefaultHost("してない");
			}catch(IOException e){
				DiscordAPI.chatDefaultHost("わかんにゃい！");
			}
		}
		*/
		s=tag.getTag("このコミュ放送してる?","このコミュ放送してる？");
		if(s!=null) {
			Matcher m=Pattern.compile("co[0-9]++").matcher(s);
			boolean miss=false;
			if(m.find()) {
				m=Pattern.compile("[0-9]++").matcher(m.group());
				m.find();
				try {
					int co=Integer.parseInt(m.group());
					try{
						Live[] lives=getLives(null,co);
						if(lives.length>0) {
							DiscordAPI.chatDefaultHost("https://live2.nicovideo.jp/watch/"+lives[0].contentId+" でしてる");
						}else DiscordAPI.chatDefaultHost("してない");
					}catch(IOException e){
						miss=true;
					}
				}catch(NumberFormatException nfe) {
					DiscordAPI.chatDefaultHost("コミュID指定ミスってる");
				}
			}else miss=true;
			if(miss)DiscordAPI.chatDefaultHost("わかんにゃい！");
		}
	}
	@Override
	public void autoSave(){
		int hc=shortcutDB.hashCode();
		if(hc!=lastWriteHashCode) {
			lastWriteHashCode=hc;
			try{
				BouyomiProxy.save(shortcutDB,"NicoShortCut.txt");
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		hc=alarted.hashCode();
		if(hc!=lastWriteHashCodeA) {
			lastWriteHashCodeA=hc;
			try{
				BouyomiProxy.save(alarted,"NicoAlart.txt");
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	@Override
	public void shutdownHook(){
		if(!shortcutDB.isEmpty())try{
			BouyomiProxy.save(shortcutDB,"NicoShortCut.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
		if(!alarted.isEmpty())try{
			BouyomiProxy.save(alarted,"NicoAlart.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	public boolean chatException(Exception e) {
		StringWriter sw=new StringWriter();
		PrintWriter pw=new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.close();
		return DiscordAPI.chatDefaultHost(sw.toString());
	}
	@Override
	public void run(){
		try{
			BouyomiProxy.load(shortcutDB,"NicoShortCut.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
		shortcutDB.put("おっさん","1003067");
		try{
			BouyomiProxy.load(alarted,"NicoAlart.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
		lastWriteHashCode=shortcutDB.hashCode();
		lastWriteHashCodeA=alarted.hashCode();
		while(true) {
			try{
				check(1003067);
			}catch(IOException e1){
				e1.printStackTrace();
			}
			try{
				Thread.sleep(5*60*1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	public void check(int id) throws IOException {
		Live[] lives=getLives(null,id);
		if(lives.length>0) {
			String last=alarted.get(Integer.toString(id));
			if(!(lives[0].contentId.equals(last))) {
				BouyomiProxy.module.event(new NicoLiveEvent(lives));
				StringBuilder sb=new StringBuilder("生放送 ");
				sb.append(lives[0].title);
				sb.append(" が開始されました/*");
				for(Live lv:lives)sb.append(lv);
				String s=sb.toString();
				//System.out.println(s);
				DiscordAPI.chatDefaultHost(s);
			}
			alarted.put(Integer.toString(id),lives[0].contentId);
		}else {
			alarted.put(Integer.toString(id),"");
		}
	}
	public static void main(String[] args) throws IOException {
		Live[] l=getLives(null,1124081);
		for(Live lv:l)System.out.println(lv);
	}
	public static Live[] getLives(String q,int... id) throws IOException {
		//System.out.print("q="+q+" コミュニティ="+id[0]+"で検索実行...");
		String js=getLiveJSON(q,id);
		Object[] o=JsonUtil.getAsArray(js,"data");
		Live[] live=new Live[o.length];
		if(o!=null)for(int i=0;i<o.length;i++){
			@SuppressWarnings("unchecked")
			Map<String, Object> map=(Map<String,Object>)o[i];
			live[i]=new Live(map);
		}
		//System.out.println(live.length+"件です");
		return live;
	}
	public static class Live{
		public String title;
		public String contentId;
		/**説明*/
		public String description;
		public Live(Map<String,Object> map) {
			description=map.get("description").toString();
			contentId=map.get("contentId").toString();
			title=map.get("title").toString();
			//for(Entry<String, Object> e:map.entrySet())System.out.println(e.getKey()+"="+e.getValue());
		}
		@Override
		public int hashCode() {
			return title.hashCode()+contentId.hashCode()+description.hashCode();
		}
		@Override
		public boolean equals(Object o) {
			if(o instanceof Live);
			else return false;
			Live l=(Live) o;
			return title.equals(l.title)&&contentId.equals(l.contentId)&&description.equals(l.description);
		}
		@Override
		public String toString() {
			StringBuilder sb=new StringBuilder("配信ID=").append(contentId).append("\n");
			sb.append("配信URL=https://live2.nicovideo.jp/watch/").append(contentId).append("\n");
			sb.append("配信タイトル=").append(title).append("\n");
			//sb.append("説明文\n").append(description);
			return sb.toString();
		}
	}
	public static String getLiveJSON(String q,int... communityId) throws IOException {
		String url=getParm(q,communityId);
		URL url0=new URL("https://api.search.nicovideo.jp/api/v2/live/contents/search"+url);
		URLConnection uc=url0.openConnection();
		uc.setRequestProperty("User-Agent","Atuwa Bouyomi Proxy");
		InputStream is=uc.getInputStream();//POSTした結果を取得
		byte[] b=new byte[512];
		int len;
		ByteArrayOutputStream res=new ByteArrayOutputStream();
		while(true) {
			len=is.read(b);
			if(len<1)break;
			res.write(b,0,len);
		}
		return res.toString("utf-8");
	}
	private static String getParm(String q,int... communityId) {
		if(q==null)q="	ゲーム OR 描いてみた OR リスナーは外部記憶装置 OR 通知用";
		try{
			q=URLEncoder.encode(q,"utf-8");
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		StringBuilder sb=new StringBuilder("?q=");
		sb.append(q);
		sb.append("&targets=tags");
		sb.append("&_sort=-viewCounter");
		sb.append("&_context=AtuwaBouyomiProxy");
		sb.append("&fields=contentId,title,description");
		sb.append("&filters[liveStatus][0]=onair");
		for(int i=0;i<communityId.length;i++) {
			sb.append("&filters[communityId][");
			sb.append(i).append("]=");
			sb.append(communityId[i]);
		}
		//System.out.println(sb);
		return sb.toString();
	}
}