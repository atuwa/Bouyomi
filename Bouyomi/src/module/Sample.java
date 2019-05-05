package module;

import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;
import bouyomi.TubeAPI.PlayVideoTitleEvent;
import bouyomi.Util;

public class Sample implements IModule{

	@Override
	public void call(Tag tag){
		if(tag.con.mentions.contains("539105406107254804")) {//メンションリストに539105406107254804が含まれる場合
			if(tag.con.text.contains("サンプルモジュール")) {//「サンプルモジュール」と言うメッセージを含む場合
				String m=Util.IDtoMention(tag.con.userid);//この書き込みをしたユーザIDからメンションを生成
				DiscordAPI.chatDefaultHost(m+"サンプルモジュール");//メンションとテキストを連結して投稿
			}
		}
		String s=tag.getTag("サンプルモジュール");//タグ取得
		if(s!=null) {//タグが無い時はnull
			String m=Util.IDtoMention(tag.con.userid);//この書き込みをしたユーザIDからメンションを生成
			DiscordAPI.chatDefaultHost(m+s.length());//メンションとタグの内容を連結して投稿
		}
	}
	@Override
	public void event(BouyomiEvent o) {
		/*
		if(o instanceof PlayVideoEvent) {
			PlayVideoEvent e=(PlayVideoEvent)o;
			System.out.println("動画再生を検出"+e.videoID);
		}
		*/
		if(o instanceof PlayVideoTitleEvent) {
			PlayVideoTitleEvent e=(PlayVideoTitleEvent)o;
			//System.out.println("動画タイトルを取得："+e.title);
			if(e.title.contains("オカリン"))DiscordAPI.chatDefaultHost("動画停止()/*タイトルに再生禁止ワードが含まれています");
		}
	}
}