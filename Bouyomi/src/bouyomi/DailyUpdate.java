package bouyomi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DailyUpdate extends Thread{
	private static DailyUpdate updater;
	public ListMap<String,IDailyUpdate> target=new ListMap<String,IDailyUpdate>();
	public static interface IDailyUpdate{
		public void update();
		public void init();
		public void read(DataInputStream dis)throws IOException;
		public void write(DataOutputStream dos)throws IOException;
	}
	public static void Ragister(String id,IDailyUpdate u) {
		if(updater.target.containsKey(id))throw new RuntimeException("used id"+id);
		updater.target.put(id,u);
		updater.target.sortKey(null);
	}
	public static void init() {
		updater=new DailyUpdate();
		updater.start();
	}
	private DailyUpdate(){
		super("DailyUpdate");
		file="DailyUpdate";
		read();
	}
	private long lastUpdate;
	private String file;
	@Override
	public void run() {
		SimpleDateFormat f=new SimpleDateFormat("DDD");
		while(true) {
			String up=f.format(new Date(lastUpdate));
			String now=f.format(new Date());
			if(!up.equals(now)) {
				lastUpdate=System.currentTimeMillis();
				for(IDailyUpdate u:target.values())u.update();
				write();
			}
			try{
				Thread.sleep(60000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	private void read() {
		try{
			File f=new File(file);
			if(!f.exists()) {
				for(IDailyUpdate u:target.values())u.init();
				return;
			}
			FileInputStream fis=new FileInputStream(f);
			DataInputStream dis=new DataInputStream(fis);
			try{
				for(IDailyUpdate u:target.values())u.read(dis);
				lastUpdate=dis.readLong();
			}catch(IOException e){
				e.printStackTrace();
			}finally {
				try{
					dis.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}
	public void write() {
		try{
			FileOutputStream fos=new FileOutputStream(new File(file));
			DataOutputStream dos=new DataOutputStream(fos);
			try{
				for(IDailyUpdate u:target.values())u.write(dos);
				dos.writeLong(lastUpdate);
			}catch(IOException e){
				e.printStackTrace();
			}finally {
				try{
					dos.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}
}
