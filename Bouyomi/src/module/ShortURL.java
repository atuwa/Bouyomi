package module;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;

public class ShortURL implements IModule{

	@Override
	public void call(Tag tag){
		String url=tag.getTag("URL短縮","ＵＲＬ短縮");
		int mode=0;
		if(url==null) {
			url=tag.getTag("ナズ短縮","nazrin短縮","ナズーリン短縮");
			mode=1;
			if(url==null)return;
		}
		try{
			//String url="https://nicovideo.jp/watch/sm20285108";
			InputStream is=null;
			if(mode==0)is=kisume(url);
			else if(mode==1)is=nazrin(url);
			if(is==null)return;
			byte[] b=new byte[512];
			int len;
			ByteArrayOutputStream res=new ByteArrayOutputStream();
			while(true) {
				len=is.read(b);
				if(len<1)break;
				res.write(b,0,len);
			}
			String json=res.toString("utf-8");
			if(json.isEmpty()) {
				System.out.println("短縮エラー"+json);
				return;
			}
			String ret=null;
			if(mode==0)ret="https://kisu.me/"+JsonUtil.get(json,"shorten").toString();
			else if(mode==1)ret=JsonUtil.get(json,"shortURL").toString();
			System.out.println("短縮="+ret+"　元URL="+url);
			DiscordAPI.chatDefaultHost("短縮結果 "+ret);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	private InputStream kisume(String url) throws IOException {
		url=URLEncoder.encode(url,"utf-8");
		URL url0=new URL("https://kisu.me/api/shorten.php?url="+url);
		URLConnection uc=url0.openConnection();
		uc.setRequestProperty("User-Agent","Atuwa Bouyomi Proxy");
		return uc.getInputStream();//POSTした結果を取得
	}
	private InputStream nazrin(String url) throws IOException {
		//url=URLEncoder.encode(url,"utf-8");
		URL url0=new URL("https://nazr.in/api/short_links");
		URLConnection uc=url0.openConnection();
		uc.setDoOutput(true);//POST可能にする
		uc.setRequestProperty("User-Agent","Atuwa Bouyomi Proxy");
		uc.setRequestProperty("Accept","application/json");
		byte[] b=("{\"url\":\""+url+"\"}").getBytes(StandardCharsets.UTF_8);
		// データがJSONであること、エンコードを指定する
		uc.setRequestProperty("Content-Type", "application/json");
		// POSTデータの長さを設定
		//uc.setRequestProperty("Content-Length", String.valueOf(b.length));
		OutputStream os=uc.getOutputStream();//POST用のOutputStreamを取得
		os.write(b);
		return uc.getInputStream();//POSTした結果を取得
	}
	/**<a href="https://qiita.com/oyahiroki/items/006b3511fc4136d02ad1">ここから持ってきた</a>*/
	public static class JsonUtil{
	    public static Object[] getAsArray(String json, String code) {
	        Object obj = get(json, code);
	        if (obj instanceof Object[]) {
	            return (Object[]) obj;
	        } else {
	            return null;
	        }
	    }
	    public static Object get(String json, String code) {
	        // Get the JavaScript engine
	        ScriptEngineManager manager = new ScriptEngineManager();
	        ScriptEngine engine = manager.getEngineByName("JavaScript");
	        String script = "var obj = " + json + ";";
	        try {
	            engine.eval(script);
	            {
	                Object obj = engine.eval("obj." + code);
	                if (obj instanceof Map) {
	                    java.util.Map<?,?> map = (java.util.Map<?,?>) obj;
	                    Set<?> entrySet = map.entrySet();
	                    Object[] arr = new Object[entrySet.size()];
	                    int n = 0;
	                    for (Object objValue : map.values()) {
	                        if (objValue instanceof String) {
	                            String sValue = (String) objValue;
	                            arr[n] = sValue;
	                        } else {
	                            arr[n] = map.get(obj);
	                        }
	                        n++;
	                    }
	                    return arr;
	                }
	                return obj;
	            }
	        } catch (ScriptException e) {
	            e.printStackTrace();
	            return null;
	        }
	    }
	}
}