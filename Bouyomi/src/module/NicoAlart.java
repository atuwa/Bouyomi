package module;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;
import module.ShortURL.JsonUtil;

public class NicoAlart implements IModule, Runnable{
	private Thread thread;
	private Live last;
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
	}
	@Override
	public void run(){
		while(true) {
			try{
				Live[] lives=getLives(null,1003067);
				if(lives.length>0) {
					if(!lives[0].equals(last)) {
						StringBuilder sb=new StringBuilder("生放送 ");
						sb.append(lives[0].title);
						sb.append(" が開始されました/*");
						for(Live lv:lives)sb.append(lv);
						String s=sb.toString();
						DiscordAPI.chatDefaultHost(s);
					}
					last=lives[0];
				}
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
	public static void main(String[] args) throws IOException {
		NicoAlart a=new NicoAlart();
		Live[] l=a.getLives(null,1124081);
		for(Live lv:l)System.out.println(lv);
	}
	public Live[] getLives(String q,int... id) throws IOException {
		if(q==null)q="	ゲーム OR 描いてみた OR リスナーは外部記憶装置";
		//System.out.print("q="+q+" コミュニティ="+id[0]+"で検索実行...");
		String js=getLiveURL(q,id);
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
			sb.append("説明文\n").append(description);
			return sb.toString();
		}
	}
	public String getLiveURL(String q,int... communityId) throws IOException {
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
	public String getLive(int communityId) throws IOException {
		//url=URLEncoder.encode(url,"utf-8");
		URL url0=new URL("https://api.search.nicovideo.jp/api/v2/live/contents/search");
		URLConnection uc=url0.openConnection();
		uc.setDoOutput(true);//POST可能にする
		uc.setRequestProperty("User-Agent","Atuwa Bouyomi Proxy");
		uc.setRequestProperty("Accept","application/json");
		String json=getJSON(communityId);
		System.out.println(json);
		byte[] b=json.getBytes(StandardCharsets.UTF_8);
		// データがJSONであること、エンコードを指定する
		uc.setRequestProperty("Content-Type", "application/json");
		// POSTデータの長さを設定
		//uc.setRequestProperty("Content-Length", String.valueOf(b.length));
		OutputStream os=uc.getOutputStream();//POST用のOutputStreamを取得
		os.write(b);
		InputStream is=uc.getInputStream();//POSTした結果を取得
		b=new byte[512];
		int len;
		ByteArrayOutputStream res=new ByteArrayOutputStream();
		while(true) {
			len=is.read(b);
			if(len<1)break;
			res.write(b,0,len);
		}
		return res.toString("utf-8");
	}
	private String getParm(String q,int... communityId) {
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
	private String getJSON(int communityId) {
		StringBuilder sb=new StringBuilder("{\n");
		sb.append("\t\"type\": \"equal\",\n");
		sb.append("\t\"field\":\"communityId\",\n");
		sb.append("\t\"value\":").append(communityId).append("\n");
		sb.append("}");
		return sb.toString();
	}
}