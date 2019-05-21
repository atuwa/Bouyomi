package module;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.LineBorder;

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
	private static BufferedImage img;
	private boolean saved=false;
	private int up;
	public Dosukebe(){
		try{
			BouyomiProxy.load(used,"Dosukebe.txt");
			saved=true;
		}catch(IOException e){
			e.printStackTrace();
		}
		DailyUpdate.Ragister("Dosukebe",this);
		String s=BouyomiProxy.Config.get("ドスケベ音量");
		if(s!=null&&!s.isEmpty())try{
			WAVPlayer.Volume=Float.parseFloat(s);
		}catch(NumberFormatException nfe) {

		}
		try{
			img=ImageIO.read(new File("mona.png"));
		}catch(IOException e){
			e.printStackTrace();
		}
		PlayThread.frame.bound();
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
					//used.add(tag.con.userid);
					//saved=false;
				}
				int rand=rundom.nextInt(10000);
				StringBuilder sb=new StringBuilder();
				int k=now+up;
				if(k>10000)k=10000;
				if(rand<k) {
					//sb.append("後は任せたドスケベ(再生システムは後で実装する)");
					sb.append("再生開始");
					up=0;
					play();
				}else {
					up+=500;
					sb.append("却下");
				}
				sb.append("(").append(rand).append(")");
				sb.append("/*確率").append(now/100d).append("+").append((k-now)/100d).append("%");
				String s=sb.toString();
				System.out.println(s);
				DiscordAPI.chatDefaultHost(s);
			}
		}
		if(tag.con.text.equals("ドスケベストップ")||tag.con.text.equals("ドスケベ停止")){
			if(WAVPlayer.nowPlay!=null)WAVPlayer.nowPlay.end();
		}
		String s=tag.getTag("ドスケベ率");
		if(s!=null) {
			if(s.isEmpty()) {
				//DiscordAPI.chatDefaultHost((tag.con.mute?"/":"")+now/100d+"%");
				StringBuilder sb=new StringBuilder();
				int k=now+up;
				if(k>10000)k=10000;
				if(tag.con.mute)sb.append("/");
				sb.append(now/100d).append("+").append((k-now)/100d).append("=").append(k/100d).append("%");
				DiscordAPI.chatDefaultHost(sb.toString());
			}else if(tag.isAdmin()||"536401162300162049".equals(tag.con.userid)){
				try {
					String old=Double.toString(now/100D);
					now=(int) (Double.parseDouble(s)*100D);
					DiscordAPI.chatDefaultHost((tag.con.mute?"/":"")+"ドスケベ率を"+old+"%から"+now/100D+"%に変更しました");
				}catch(NumberFormatException nfe) {

				}
			}
		}
		s=tag.getTag("ドスケベリスト");
		if(s!=null) {
			File dir=new File("Dosukebe");
			String[] list=dir.list();
			StringBuilder sb=new StringBuilder();
			if(tag.con.mute)sb.append("/");
			for(String t:list)sb.append(t).append("\n");
			DiscordAPI.chatDefaultHost(sb.toString());
		}
		s=tag.getTag("ミュージックスタート");
		if(s!=null) {
			if(s.isEmpty()) {
				if(!tag.con.text.equals("ミュージックスタート"))DiscordAPI.chatDefaultHost("パラメータが不正です");
			}else if(tag.isAdmin()||"536401162300162049".equals(tag.con.userid)) {
				File dir=new File("Dosukebe");
				String[] list=dir.list();
				boolean b=true;
				for(String t:list) {
					if(!s.equals(t))continue;
					File f=new File(dir,t);
					try{
						URL url=f.toURI().toURL();
						//System.out.println("1"+url);
						synchronized(PlayThread.tasks) {
							PlayThread.tasks.add(url);
							PlayThread.play();
						}
						b=false;
						DiscordAPI.chatDefaultHost(s+"を再生します");
					}catch(MalformedURLException e){
						e.printStackTrace();
					}
				}
				if(b)DiscordAPI.chatDefaultHost(s+"は存在しません");
			}else DiscordAPI.chatDefaultHost("権限がありません");
		}
		s=tag.getTag("ドスケベ再生待ち");
		if(s!=null) {
			StringBuilder sb=new StringBuilder("/");
			for(URL u:PlayThread.tasks) {
				File f=new File(u.getPath());
				String name=f.getName();
				sb.append("\n").append(name);
			}
			DiscordAPI.chatDefaultHost(sb.toString());
		}
		s=tag.getTag("ドスケベ音量");
		if(s!=null) {
			if(s.isEmpty()) {
				DiscordAPI.chatDefaultHost(tag.con.mute?"/":""+"ドスケベ音量は"+WAVPlayer.Volume+"です");
			}else volume(s);
		}
	}
	private void volume(String tag) {
		try{
			float Nvol=-1;
			switch(tag.charAt(0)){
				case '＋':
				case '－':
				case '+':
				case '-':
					tag=tag.replace('＋','+');
					tag=tag.replace('－','-');
					Nvol=WAVPlayer.Volume;//+記号で始まる時今の音量を取得
			}
			float vol=Float.parseFloat(tag);//要求された音量
			if(Nvol>=0)vol=Nvol+vol;//音量が取得させていたらそれに指定された音量を足す
			if(vol>100)vol=100;//音量が100以上の時100にする
			else if(vol<0)vol=0;//音量が0以下の時0にする
			System.out.println("ドスケベ音量"+vol);//ログに残す
			WAVPlayer.Volume=vol;//再生時に使う音量をこれにする
			WAVPlayer.setVolume(vol);
			BouyomiProxy.Config.put("ドスケベ音量",Float.toString(vol));
			StringBuffer sb=new StringBuffer("ドスケベ音量を");
			sb.append(vol);
			sb.append("にしました");
			DiscordAPI.chatDefaultHost(sb.toString());
		}catch(NumberFormatException e) {
			DiscordAPI.chatDefaultHost("数値を解析できません");
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
				PlayThread.play();
			}
		}catch(MalformedURLException e){
			e.printStackTrace();
		}
	}
	private static class PlayThread extends Thread{
		private static DosukebeWindow frame;
		private static ArrayList<URL> tasks=new ArrayList<URL>();
		private static PlayThread thread;
		public PlayThread() {
			super("PlayThread");
			//frame.setTitle("Dosukebe");
		}
		private static void init(){
			//OSのウィンドウ装飾を無くして、Look&Feelの装飾にしておきます。
			JFrame.setDefaultLookAndFeelDecorated(true);
			frame=new DosukebeWindow("Dosukebe");
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
			while(true) {
				URL url;
				synchronized(tasks){
					url=tasks.get(0);
					tasks.remove(0);
				}
				File f=new File(url.getPath());
				String name=f.getName();
				StringBuilder startmes=new StringBuilder("再生開始：");
				startmes.append(name);
				if(PlayThread.tasks.size()>0) {
					startmes.append("/*残り");
					startmes.append(PlayThread.tasks.size());
					startmes.append("曲");
				}
				DiscordAPI.chatDefaultHost(startmes.toString());
				frame.setText("再生中："+name);
				frame.bound();
				System.out.println("再生開始："+name);
				WAVPlayer.play(url.toString());
				System.out.println("再生終了");
				frame.setText("終了："+name);
				frame.bound();
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
	private static class DosukebeWindow extends JFrame{
		private double x,y=0;
		private double scale=1;
		private String text="未再生";
		private int w,h;
		public DosukebeWindow(String string) {
			super(string);
			Canvas sc=new Canvas();
			setContentPane(sc);
			JRootPane root=this.getRootPane();
			root.setBorder(new LineBorder(Color.black, 0));
			root.setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
			JLayeredPane layeredPane = root.getLayeredPane();
		    Component c = layeredPane.getComponent(1);
		    if (c instanceof JComponent) {
		      JComponent orgTitlePane = (JComponent) c;
		      orgTitlePane.removeAll();
		      orgTitlePane.setLayout(new BorderLayout());
		      orgTitlePane.add(new JPanel(new BorderLayout()));
		    }
			getContentPane().setLayout(new FlowLayout());
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			setSize(500,500);
			setVisible(true);
			mouseInput mouseInput=new mouseInput();
			this.addMouseListener(mouseInput);
			this.addMouseWheelListener(mouseInput);
			this.addMouseMotionListener(mouseInput);
	        //ウィンドウ範囲を示す枠をつけておく
	        //this.getRootPane().setBorder(new LineBorder(Color.black, 2));
			this.repaint();
		}
		public void setText(String s){
			text=s;
		}
		protected void bound() {
			BufferedImage bi=new BufferedImage(2,2,BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g=bi.createGraphics();
			g.setFont(new Font(null,Font.BOLD, (int) (40*scale)));
			w=g.getFontMetrics(g.getFont()).stringWidth(text);
			h=(int) (100*scale);
			Rectangle b=getBounds();
			if(img!=null)setBounds(b.x,b.y,(int)(img.getWidth()*scale)+w+8,(int)(img.getHeight()*scale)+h);
			//b=getBounds();
			//System.out.println("大きさ"+b.width+"x"+b.height+"="+(b.width*b.height)+"px");
			//背景色を透明にします。
			//ウィンドウ装飾を無くしておかないとjre1.7からはエラーが発生します。
			setBackground(new Color(0,0,0,0));
			this.repaint();
		}
		public class Canvas extends JPanel {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setFont(new Font(null,Font.BOLD, (int) (40*scale)));
				int imgw=0;
				if(img!=null)g.drawImage(img,0,0,imgw=(int)(img.getWidth()*scale),(int)(img.getHeight()*scale),null);
				g.setColor(Color.white);
				//g.fillRect(imgw,0,w-2,h);
				Polygon p=new Polygon();
				int framew=g.getFontMetrics(g.getFont()).stringWidth(text)+1;
				p.addPoint(imgw,0);
				p.addPoint(imgw,h);
				p.addPoint(imgw+framew/2-framew/6,h);
				p.addPoint(imgw+framew/4,(int) (h*1.5));
				p.addPoint(imgw+framew-2-framew/4,h);
				p.addPoint(imgw+framew-2,h);
				p.addPoint(imgw+framew-2,0);
				g.fillPolygon(p);
				g.setColor(Color.black);
				//g.drawRect(imgw,0,w-2,h);
				g.drawPolygon(p);
				int h=g.getFontMetrics(g.getFont()).getHeight();
				g.drawString(text,imgw,h);
				//g.fillRect(0,0,1000,1000);
			}
		}
		private class mouseInput extends MouseAdapter{
			int px,py;
			public void mouseWheelMoved(MouseWheelEvent e){
				scale+=e.getWheelRotation()/20d;
				if(scale<0.1)scale=0.1;
				else if(scale>5)scale=5;
				//System.out.println("拡大率"+scale+"倍");
				bound();
			}
			public void mouseReleased(MouseEvent e){

			}
			public void mousePressed(MouseEvent e){
				px=e.getX();
				py=e.getY();
				if(e.getButton()!=MouseEvent.BUTTON1) {
					setAlwaysOnTop(!isAlwaysOnTop());
				}
			}
			@Override
			public void mouseMoved(MouseEvent e){
				//System.out.println("mouseMoved");
			}
			@Override
			public void mouseDragged(MouseEvent e){
				//System.out.println("mouseDragged");
				Rectangle b=getBounds();
				y=b.y-py+e.getY();
				x=b.x-px+e.getX();
				setBounds((int)x,(int)y,b.width,b.height);
			}
		}
	}
}