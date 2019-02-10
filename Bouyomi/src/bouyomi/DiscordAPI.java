package bouyomi;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

public class DiscordAPI{
	public static String service_host;//Discord投稿ソフトのIPとポート

	public static boolean chat(String c){
		if(service_host==null||service_host.isEmpty())return false;
		try{
			String chat=URLEncoder.encode(c,"utf-8");
			URL url=new URL("http://"+service_host+"/discordchat?chat="+chat);
			url.openStream().close();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
}
