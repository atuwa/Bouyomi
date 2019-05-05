package module;

import java.util.Random;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;

/**NicoAlartモジュールが無いとエラー吐くよ*/
public class Ryosios implements IModule{

	@Override
	public void call(Tag tag){
		if(tag.con.text.equals("おっさん生きてる？")||tag.con.text.equals("おっさん死んでる？")) {
			String lives=NicoAlart.alarted.get("1003067");
			if(lives!=null&&!lives.isEmpty()) {
				DiscordAPI.chatDefaultHost("生きてる。良かった");
				return;
			}
			Random r=new Random();
			String[] list= {"多分息してない/*もしかしたら生きてるかも"
					,"多分生きてない/*生命維持装置が故障してるかも"
					,"コンビニ行ってる"
					,"洋ドラ見てる",
					"ギャザやってる",
					"ガリ喰ってる"};
			if(r.nextInt(100)<5) {
				DiscordAPI.chatDefaultHost("トイレ行ってる");
			}else {
				int i=r.nextInt(list.length);
				DiscordAPI.chatDefaultHost(list[i]);
			}
		}
	}
}
