package bouyomi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import bouyomi.ListMap.Value;

public class DailyUpdate extends Thread{
	public static DailyUpdate updater;
	public ListMap<String,IDailyUpdate> target=new ListMap<String,IDailyUpdate>();
	public static interface IDailyUpdate{
		public void update();
		public default void init() {};
		public default void read(DataInputStream dis)throws IOException{};
		public default void write(DataOutputStream dos)throws IOException{};
	}
	public static void Ragister(String id,IDailyUpdate u) {
		if(updater.target.containsKey(id))throw new RuntimeException("used id"+id);
		updater.target.put(id,u);
		updater.target.sortKey(null);
	}
	public static void init() {
		updater=new DailyUpdate();
	}
	private DailyUpdate(){
		super("DailyUpdate");
		file="DailyUpdate";
	}
	public void start() {
		super.start();
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
				lastUpdate=dis.readLong();
				while(true) {
					String s=dis.readUTF();
					if(s.isEmpty())break;
					int i=dis.readInt();
					byte[] b=new byte[i];
					dis.read(b);
					DataInputStream is=new DataInputStream(new ByteArrayInputStream(b));
					IDailyUpdate t=target.get(s);
					if(t!=null)try{
						t.read(is);
					}catch(IOException e) {
						e.printStackTrace();
					}
				}
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
				dos.writeLong(lastUpdate);
				for(Value<String, IDailyUpdate> v:target.rawList()) {
					dos.writeUTF(v.getKey());
					ByteArrayOutputStream bos=new ByteArrayOutputStream();
					DataOutputStream os=new DataOutputStream(bos);
					v.getValue().write(os);
					byte[] b=bos.toByteArray();
					dos.writeInt(b.length);
					dos.write(b);
				}
				dos.writeUTF("");
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
