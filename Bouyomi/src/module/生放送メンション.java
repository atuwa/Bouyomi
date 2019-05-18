package module;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import bouyomi.BouyomiProxy;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;
import bouyomi.Util;
import bouyomi.Util.JsonUtil;
import module.NicoAlart.Live;
import module.NicoAlart.NicoLiveEvent;

/**生放送が始まった時に希望者にだけメンション飛ばす機能*/
public class 生放送メンション implements IModule, IAutoSave{

	private HashMap<String,String> 希望者リスト=new HashMap<String,String>();
	private boolean saved=true;
	public 生放送メンション() {
		try{
			BouyomiProxy.load(希望者リスト,"生放送メンション希望者.txt");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void call(Tag tag){
		if(tag.con.userid==null)return;//IDが取得できない時は無効化
		String co=tag.getTag("生放送メンション希望");
		if(co!=null) {
			int cid=getCommunityId(co);
			if(cid>=0) {//抽出成功
				String uid=tag.con.userid;
				String e=希望者リスト.get(uid);
				if(e==null||e.isEmpty())e=Integer.toString(cid);//新規登録
				else{//追加
					e=e+","+cid;//既にあるやつ＋区切り文字＋追加分
				}
				希望者リスト.put(uid,e);
				DiscordAPI.chatDefaultHost("登録値="+e);
			}
		}
		co=tag.getTag("生放送メンション解除");
		if(co!=null) {
			String uid=tag.con.userid;
			String e=希望者リスト.get(uid);
			if(e!=null) {
				int cid=getCommunityId(co);
				if(cid>=0) {//抽出成功
					co=Integer.toString(cid);
					if(e.contains(co)) {
						e=e.replace(co,"");
						e=e.replace(",,",",");
						if(!e.isEmpty()&&e.charAt(e.length()-1)==',') {
							e=e.substring(0,e.length()-1);
						}
						DiscordAPI.chatDefaultHost("登録値="+e);
						希望者リスト.put(uid,e);
					}
				}
			}
		}
		co=tag.getTag("生放送メンション一覧");
		if(co!=null) {
			String uid=tag.con.userid;
			String e=希望者リスト.get(uid);
			if(e!=null) {
				DiscordAPI.chatDefaultHost("登録値="+e);
			}else DiscordAPI.chatDefaultHost("登録されてません");
		}
		if(tag.isAdmin()) {
			co=tag.getTag("生放送イベント生成");
			if(co!=null) {
				Object[] o=JsonUtil.getAsArray(co,"data");
				Live[] live=new Live[o.length];
				if(o!=null)for(int i=0;i<o.length;i++){
					@SuppressWarnings("unchecked")
					Map<String, Object> map=(Map<String,Object>)o[i];
					live[i]=new Live(map);
				}
				BouyomiProxy.module.event(new NicoLiveEvent(live));
			}
		}
	}
	private int getCommunityId(String co) {
		int cid=NicoAlart.getCo(co);//コミュID抽出
		if(cid<0) {
			String s=NicoAlart.shortcutDB.get(co);
			try{
				cid=Integer.parseInt(s);
			}catch(NumberFormatException nfe) {

			}
		}
		return cid;
	}
	public void event(BouyomiEvent o) {
		if(希望者リスト.size()<1)return;
		if(o instanceof NicoLiveEvent) {
			Live[] arr=((NicoLiveEvent)o).live;
			for(Live lv:arr) {
				if(lv.communityId==null)continue;
				StringBuilder sb=new StringBuilder("/生放送「"+lv.title+"」が開始されました\n");
				System.out.println(sb.toString());
				for(Entry<String, String> es:希望者リスト.entrySet()) {
					String rv=es.getValue();//希望者ごとの希望するコミュニティCSV
					for(String c:rv.split(",")) {
						if(lv.communityId.equals(c)) {
							sb.append(Util.IDtoMention(es.getKey())).append("\n");
							break;
						}
					}
				}
				DiscordAPI.chatDefaultHost(sb.toString());
			}
		}
	}
	@Override
	public void autoSave() throws IOException{
		if(!saved)shutdownHook();
	}
	@Override
	public void shutdownHook() {
		try{
			BouyomiProxy.save(希望者リスト,"生放送メンション希望者.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}