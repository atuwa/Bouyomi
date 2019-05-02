package module;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import bouyomi.BouyomiProxy;
import bouyomi.DailyUpdate;
import bouyomi.DailyUpdate.IDailyUpdate;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;

public class Rundom implements IModule,IDailyUpdate,IAutoSave{

	/**ファイルごとにインスタンス作る<br>
	 * 最大21億4748万3647種類登録可能*/
	private class Files{
		private ArrayList<String> list=new ArrayList<String>();
		private boolean saved;
		/**今日のindex*/
		private int now;
		public String name;
		public String file;
		public Files(String file) throws IOException{
			this.file=file;
			BouyomiProxy.load(list,file);
			saved=true;
			now=Math.abs(rundom.nextInt());//初期値
			save=false;
			//System.out.println(get()+"("+now+")で初期化");
		}
		/**@return 追加したらtrue 既にあればfalse*/
		public boolean add(String s) {
			if(list.contains(s))return false;
			list.add(s);
			saved=false;
			return true;
		}
		/**@return 削除したらtrue 要素がなかったりしたらfalse*/
		public boolean remove(String s) {
			if(list.remove(s)) {
				saved=false;
				return true;
			}
			return false;
		}
		/**今日の*/
		public String get() {
			int index=now;
			while(index>=list.size())index-=list.size();
			return list.get(index);
		}
		public void save(String file) {
			if(saved)return;//保存済の場合はスキップ
			try{
				BouyomiProxy.save(list,file);
				saved=true;
			}catch(IOException e){
				//超適当例外処理
				e.printStackTrace();
			}
		}
		public void update() {
			now=Math.abs(rundom.nextInt());
			save=false;
			DiscordAPI.chatDefaultHost("今日の"+name+"が"+get()+"に変更されました");
			//System.out.println(get()+"("+now+")で更新");
		}
	}
	/**k=ファイル v=管理用インスタンス*/
	private HashMap<String,Files> files=new HashMap<String,Files>();
	private Random rundom=new SecureRandom();
	/**k=ファイル v=現在のランダム値*/
	private HashMap<String,String> rundoms=new HashMap<String,String>();
	/**今日のIndexを保存したか*/
	protected boolean save;
	public Rundom() {
		DailyUpdate.Ragister("module.Rundom",this);
		//行区切りでファイルを書く
		try{
			BouyomiProxy.load(rundoms,"rundoms.txt");
			for(Entry<String, String> s:rundoms.entrySet()) {
				try{
					String k=s.getKey();
					String v=s.getValue();
					//kはファイル名
					int dot=k.lastIndexOf('.');
					//nameは拡張子抜き
					String name;
					if(dot<0)name=k;
					else name=k.substring(0,dot);
					Files f=new Files(k);
					f.name=name;
					files.put(k,f);
					if(!v.isEmpty())try {
						f.now=Integer.parseInt(v);
						f.saved=true;
						//System.out.println(f.get()+"("+f.now+")を読み込み");
					}catch(NumberFormatException nfe) {

					}
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			save=true;
		}catch(IOException e1){
			//超適当な例外処理
			e1.printStackTrace();
		}
	}
	@Override
	public void update() {
		//日替わり処理
		for(Files e:files.values()) {
			e.update();
		}
	}
	@Override
	public void call(Tag tag){
		for(Files e:files.values()){
			String p=tag.getTag(e.name+"登録");
			if(p!=null) {
				if(!p.isEmpty()&&e.add(p)) {
					System.out.println(p+"を"+e.name+"に登録");
					tag.con.addTask.add("登録成功");
				}else tag.con.addTask.add("登録失敗");
			}
			p=tag.getTag(e.name+"削除");
			if(p!=null) {
				if(e.remove(p))tag.con.addTask.add("削除成功");
				else tag.con.addTask.add("削除失敗");
			}
			p=tag.getTag("今日の"+e.name);
			if(p!=null) {
				DiscordAPI.chatDefaultHost(e.get());
			}
			p=tag.getTag("今日の"+e.name+"変更");
			if(p!=null) {
				e.update();
				DiscordAPI.chatDefaultHost(e.get());
			}
		}
	}
	@Override
	public void autoSave() throws IOException{
		shutdownHook();
	}
	public void shutdownHook() {
		for(Entry<String, Files> e:files.entrySet()) {
			Files v=e.getValue();
			v.save(e.getKey());
			rundoms.put(v.file,Integer.toString(v.now));
		}
		if(save)return;
		try{
			BouyomiProxy.save(rundoms,"rundoms.txt");
			save=true;
		}catch(IOException e1){
			e1.printStackTrace();
		}
	}
}
