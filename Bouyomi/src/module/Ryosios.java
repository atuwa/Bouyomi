package module;

import java.io.IOException;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;
import module.NicoAlart.Live;

/**NicoAlartモジュールが無いとエラー吐くよ*/
public class Ryosios implements IModule{

	@Override
	public void call(Tag tag){
		if(tag.con.text.equals("おっさん生きてる？")) {
			try{
				Live[] lives=NicoAlart.getLives(null,1003067);
				if(lives.length>0) {
					DiscordAPI.chatDefaultHost("生きてる。良かった");
					return;
				}
			}catch(IOException e){
				e.printStackTrace();
			}
			String[] list= {"多分息してない/*もしかしたら生きてるかも\"","多分生きてない/*生命維持装置が故障してるかも"};
			int i=new java.util.Random().nextInt(list.length);
			DiscordAPI.chatDefaultHost(list[i]);
		}
	}
}
