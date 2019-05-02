package module;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;

import bouyomi.BouyomiProxy;
import bouyomi.DailyUpdate;
import bouyomi.DailyUpdate.IDailyUpdate;
import bouyomi.DiscordAPI;
import bouyomi.IAutoSave;
import bouyomi.IModule;
import bouyomi.Tag;

public class Dosukebe implements IModule,IDailyUpdate,IAutoSave{
	static {
		PlayThread.init();
	}
	/**もう引いた人*/
	private ArrayList<String> used=new ArrayList<String>();
	private Random rundom=new SecureRandom();
	private static final int min=1500,max=5000;//最低15%最大50%
	private int now=rundom.nextInt(max-min)+min;
	private boolean saved=false;
	public Dosukebe(){
		try{
			BouyomiProxy.load(used,"Dosukebe.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
		DailyUpdate.Ragister("Dosukebe",this);
	}
	@Override
	public void autoSave() throws IOException{
		shutdownHook();
	}
	public void shutdownHook() {
		if(saved)return;
		try{
			BouyomiProxy.save(used,"Dosukebe.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	/**日替わりシステムを引いた人リストの初期化に使う
	 * だから0時にリセット*/
	@Override
	public void update(){
		now=rundom.nextInt(max-min)+min;
		used.clear();
		DiscordAPI.chatDefaultHost("ドスケベ率が"+(now/100F)+"%に変更されました");
	}
	public void read(DataInputStream dis)throws IOException{
		now=dis.readInt();
	};
	public void write(DataOutputStream dos)throws IOException{
		dos.writeInt(now);
	};
	@Override
	public void call(Tag tag){
		if(tag.con.text.equals("ミュージックスタート")){
			if(tag.con.userid==null)tag.con.addTask.add("ユーザID取得エラー");
			if(used.contains(tag.con.userid)) {
				DiscordAPI.chatDefaultHost("今日は既に引いてます");
			}else{
				if(!tag.isAdmin()&&!"536401162300162049".equals(tag.con.userid)) {
					used.add(tag.con.userid);
					saved=false;
				}
				int rand=rundom.nextInt(10000);
				StringBuilder sb=new StringBuilder();
				if(rand<now) {
					//sb.append("後は任せたドスケベ(再生システムは後で実装する)");
					sb.append("再生開始");
					play();
				}else sb.append("却下");
				sb.append("(").append(rand).append(")");
				sb.append("/*確率").append(now/100d).append("%");
				DiscordAPI.chatDefaultHost(sb.toString());
			}
		}
		String s=tag.getTag("ドスケベ率");
		if(s!=null) {
			if(s.isEmpty())DiscordAPI.chatDefaultHost(now/100d+"%");
			else if(tag.isAdmin()){
				try {
					String old=Float.toString(now/100f);
					now=(int) (Float.parseFloat(s)*100);
					DiscordAPI.chatDefaultHost("ドスケベ率を"+old+"%から"+now/100f+"%に変更しました");
				}catch(NumberFormatException nfe) {

				}
			}
		}
	}
	public void play() {
		File dir=new File("Dosukebe");
		String[] list=dir.list();
		File f=new File(dir,list[rundom.nextInt(list.length)]);
		try{
			URL url=f.toURI().toURL();
			//System.out.println("1"+url);
			synchronized(PlayThread.tasks) {
				PlayThread.tasks.add(url);
				DiscordAPI.chatDefaultHost(f.getName());
				PlayThread.play();
			}
		}catch(MalformedURLException e){
			e.printStackTrace();
		}
	}
	private static class PlayThread extends Thread{
		private static JFrame frame=new JFrame("Dosukebe");
		private static ArrayList<URL> tasks=new ArrayList<URL>();
		private static PlayThread thread;
		private static JLabel label;
		public PlayThread() {
			super("PlayThread");
			//frame.setTitle("Dosukebe");
		}
		private static void init(){
			frame.getContentPane().setLayout(new FlowLayout());
			label = new JLabel("未再生");
			frame.getContentPane().add(label);
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.setSize(50,50);
			frame.setVisible(true);
			File f=new File("Dosukebe.png");
			if(f.exists())try{
				BufferedImage image=ImageIO.read(f);
				frame.setIconImage(image);
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		private static void play() {
			if(thread==null){
				thread=new PlayThread();
				thread.start();
			}
		}
		public void run() {
			WAVPlayer.Volume=20;
			while(true) {
				URL url;
				synchronized(tasks){
					url=tasks.get(0);
					tasks.remove(0);
				}
				label.setText("再生中："+url.toString());
				WAVPlayer.play(url.toString());
				label.setText("終了："+url.toString());
				boolean end=true;
				for(int i=0;i<100;i++) {
					if(tasks.size()>0) {
						end=false;
						break;
					}
					try{
						Thread.sleep(100);
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
				if(end)break;
			}
			thread=null;
		}
	}
}