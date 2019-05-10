package module;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;

/**pingの書き込みを取得
 * pongを投稿
 * pongを取得
 * までの時間を計測する<br>
 * 応答(ping=pong)が必要*/
public class Ping implements IModule{

	private boolean ping;
	private long t;
	@Override
	public void call(Tag tag){
		if(tag.con.text.equals("ping")) {
			ping=true;
			t=System.currentTimeMillis();
		}
		if(tag.con.text.equals("pong")&&ping) {
			ping=false;
			String s=System.currentTimeMillis()-t+"ms";
			System.out.println("ping"+s);
			DiscordAPI.chatDefaultHost(s);
		}
	}
}
