package bouyomi;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

public class DiscordAPI{
	public static String service_host;//Discord投稿ソフトのIPとポート
	public static DiscordAPI Default;
	public String server;
	public DiscordAPI(String service_host){
		server=service_host;
	}
	public static boolean chatDefaultHost(String c){
		if(Default==null)Default=new DiscordAPI(service_host);
		return Default.chat(c);
	}
	public boolean chat(String c){
		if(server==null||server.isEmpty())return false;
		try{
			String chat=URLEncoder.encode(c,"utf-8");
			URL url=new URL("http://"+server+"/discordchat?chat="+chat);
			url.openStream().close();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
}
