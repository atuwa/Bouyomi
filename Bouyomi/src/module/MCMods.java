package module;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;

public class MCMods implements IModule{

	@Override
	public void call(Tag tag){
		String t=tag.getTag("モジュールテスト");
		if(t!=null) {
			DiscordAPI.chatDefaultHost(t);
		}
	}

}
