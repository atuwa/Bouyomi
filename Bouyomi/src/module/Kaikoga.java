package module;

import java.security.SecureRandom;

import bouyomi.BouyomiConection;
import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;

/**おまけ機能*/
public class Kaikoga implements IModule{

	@Override
	public void call(Tag tag){
		BouyomiConection con=tag.con;
		if(DiscordAPI.service_host!=null&&(con.text.equals("グレートカイコガ2")||con.text.equals("グレートカイコガ"))){
			int r=new SecureRandom().nextInt(1000)+1;
			String s=(r==1?"ボロン (":"はずれ (")+r+(con.user==null?")":")/*抽選者："+con.user);
			DiscordAPI.chatDefaultHost(s);
			System.out.println(s);
			if(r==1)con.addTask.add("おめでとう当たったよ");
			if(r<50)con.addTask.add("おしい");
		}
	}
}