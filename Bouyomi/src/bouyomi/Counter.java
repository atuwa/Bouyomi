package bouyomi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

public class Counter{

	private static String[] counterWords=null;
	private static int[] counter;
	private static long counterStart;
	/**k=ユーザIDv=データ*/
	public static HashMap<String,CountData> usercount=new HashMap<String,CountData>();

	public static class CountData implements Comparable<CountData>{
		public CountData(String key){
			name=key;
		}
		public CountData(DataInputStream is) throws IOException{
			if(is!=null)read(is);
		}
		/**カウント*/
		public class Count{
			public long count=0;
			public Count() {}
			public Count(long l) {
				count=l;
			}
		}
		/**ユーザー名*/
		public String name;
		/**k=ワード,v=カウント*/
		public final HashMap<String,Count> count=new HashMap<String,Count>();
		/**wordのカウントを増やす*/
		public void add(String word) {
			Count c=count.get(word);
			if(c==null) {
				c=new Count();
				count.put(word,c);
			}
			c.count++;
		}
		public void write(DataOutputStream dos) throws IOException {
			dos.writeUTF(name);
			for(String w:count.keySet()) {
				dos.writeUTF(w);
				dos.writeLong(count.get(w).count);
			}
			dos.writeUTF("");
		}
		public void read(DataInputStream dis) throws IOException {
			name=dis.readUTF();
			while(true) {
				String word=dis.readUTF();
				if(word.isEmpty())break;
				long l=dis.readLong();
				count.put(word,new Count(l));
			}
		}
		@Override
		public int compareTo(CountData o){
			BigInteger bi=new BigInteger(new byte[32]);
			byte writeBuffer[] = new byte[8];
			for(Count c:count.values()){
				long v=c.count;
				writeBuffer[0] = (byte)(v >>> 56);
				writeBuffer[1] = (byte)(v >>> 48);
				writeBuffer[2] = (byte)(v >>> 40);
				writeBuffer[3] = (byte)(v >>> 32);
				writeBuffer[4] = (byte)(v >>> 24);
				writeBuffer[5] = (byte)(v >>> 16);
				writeBuffer[6] = (byte)(v >>>  8);
				writeBuffer[7] = (byte)(v >>>  0);
				bi=bi.add(new BigInteger(writeBuffer));
			}
			BigInteger b=new BigInteger(new byte[32]);
			for(Count c:o.count.values()){
				long v=c.count;
				writeBuffer[0] = (byte)(v >>> 56);
				writeBuffer[1] = (byte)(v >>> 48);
				writeBuffer[2] = (byte)(v >>> 40);
				writeBuffer[3] = (byte)(v >>> 32);
				writeBuffer[4] = (byte)(v >>> 24);
				writeBuffer[5] = (byte)(v >>> 16);
				writeBuffer[6] = (byte)(v >>>  8);
				writeBuffer[7] = (byte)(v >>>  0);
				b=b.add(new BigInteger(writeBuffer));
			}
			return bi.compareTo(b)*-1;
		}
	}
	public static void writeUserData() {
		try{
			FileOutputStream fos=new FileOutputStream("count.db");
			BufferedOutputStream bos=new BufferedOutputStream(fos);
			DataOutputStream dos=new DataOutputStream(bos);
			for(String w:usercount.keySet()) {
				try{
					dos.writeUTF(w);
					usercount.get(w).write(dos);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
			dos.writeUTF("");
			dos.close();
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	public static void readUserData() {
		File f=new File("count.db");
		if(!f.exists())return;
		try{
			FileInputStream fis=new FileInputStream(f);
			BufferedInputStream bis=new BufferedInputStream(fis);
			DataInputStream dis=new DataInputStream(bis);
			while(true) {
				try{
					String user=dis.readUTF();
					if(user.isEmpty())break;
					usercount.put(user,new CountData(dis));
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}
	public static void addData(BouyomiConection bc,String word) {
		String key=bc.user;
		if(key==null||key.isEmpty())return;
		String id=bc.userid;
		if(id==null||id.isEmpty())return;
		CountData c=usercount.get(id);
		if(c==null) {
			c=new CountData(key);
			usercount.put(id,c);
		}
		c.name=key;
		c.add(word);
	}
	static {
		loadCountWords();
		readUserData();
	}
	private static void loadCountWords(){
		File f=new File("count.txt");
		if(!f.exists())return;
		ArrayList<String> al=new ArrayList<String>();
		ArrayList<String> al2=new ArrayList<String>();
		BufferedReader br=null;
		try{
			FileInputStream fis=new FileInputStream(f);
			InputStreamReader isr=new InputStreamReader(fis,StandardCharsets.UTF_8);
			br=new BufferedReader(isr);
			while(true) {
				String r=br.readLine();
				if(r==null)break;
				System.out.println("カウントデータ　"+r);
				String[] a=r.split("\t");
				if(a.length<1)continue;
				al.add(a[0]);
				if(a.length<2)continue;
				al2.add(a[1]);
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			if(br!=null) try{
				br.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		counterWords=al.toArray(new String[al.size()]);
		counter=new int[counterWords.length];
		for(int i=0;i<al2.size();i++) {
			try{
				counter[i]=Integer.parseInt(al2.get(i));
			}catch(NumberFormatException nfe) {

			}
		}
		try{
			BasicFileAttributes b=Files.readAttributes(f.toPath(),BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
			counterStart=b.creationTime().toMillis();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	public static void write() {
		writeUserData();
		File f=new File("count.txt");
		if(!f.exists())return;
		BufferedOutputStream bos=null;
		try{
			FileOutputStream fos=new FileOutputStream(f);
			bos=new BufferedOutputStream(fos);
			for(int i=0;i<counterWords.length;i++) {
				StringBuilder s=new StringBuilder(counterWords[i]);
				s.append("\t").append(counter[i]).append("\n");
				try{
					bos.write(s.toString().getBytes(StandardCharsets.UTF_8));
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}finally {
			if(bos!=null) try{
				bos.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	public static void count(Tag tag){
		String s0=tag.getTag("カウントユーザ");
		if(s0!=null&&!usercount.isEmpty()) {
			Collection<CountData> es=usercount.values();
			CountData[] arr=es.toArray(new CountData[es.size()]);
			Arrays.sort(arr);
			StringBuilder sb=new StringBuilder("/「");
			for(CountData d:arr) {
				sb.append(d.name).append("」「");
			}
			sb.deleteCharAt(sb.length()-1);
			DiscordAPI.chatDefaultHost(sb.toString());
		}
		String s=tag.getTag("カウント取得");
		if(s!=null&&!usercount.isEmpty()) {
			CountData cd=usercount.get(s);
			if(s.isEmpty()) {
				Collection<CountData> es=usercount.values();
				CountData[] arr=es.toArray(new CountData[es.size()]);
				Arrays.sort(arr);
				StringBuilder sb=new StringBuilder("/");
				int end=Math.min(arr.length,5);
				for(int i=0;i<end;i++) {
					CountData c=arr[i];
					BigInteger bi=new BigInteger(new byte[32]);
					byte writeBuffer[] = new byte[8];
					for(String w:c.count.keySet()){
						long v=c.count.get(w).count;
						writeBuffer[0] = (byte)(v >>> 56);
						writeBuffer[1] = (byte)(v >>> 48);
						writeBuffer[2] = (byte)(v >>> 40);
						writeBuffer[3] = (byte)(v >>> 32);
						writeBuffer[4] = (byte)(v >>> 24);
						writeBuffer[5] = (byte)(v >>> 16);
						writeBuffer[6] = (byte)(v >>>  8);
						writeBuffer[7] = (byte)(v >>>  0);
						bi=bi.add(new BigInteger(writeBuffer));
					}
					sb.append(c.name).append("の書き込み(").append(bi.toString()).append("回)\n");
					/*
					for(String w:c.count.keySet()) {
						sb.append(w).append("が").append(c.count.get(w).count).append("回\n");
					}
					*/
				}
				DiscordAPI.chatDefaultHost(sb.toString());
				cd=null;
			}else for(String id:usercount.keySet()) {
				CountData c=usercount.get(id);
				if(c!=null&&c.name.equals(s))cd=c;
			}
			if(cd!=null) {
				StringBuilder sb=new StringBuilder("/");
				sb.append(cd.name).append("の書き込み\n");
				for(String w:cd.count.keySet()) {
					sb.append(w).append("が").append(cd.count.get(w).count).append("回\n");
				}
				DiscordAPI.chatDefaultHost(sb.toString());
			}else if(!s.isEmpty())DiscordAPI.chatDefaultHost("/データ無し");
		}
		String text=tag.con.text;
		if(counterWords!=null&&counterWords.length>0) {
			if(text.equals("カウント取得")){
				DiscordAPI.chatDefaultHost(countString());
				return;
			}
			if(tag.con.mute)return;
			for(int i=0;i<counterWords.length;i++) {
				String[] cw=counterWords[i].split(",");
				for(String c:cw) {
					if(text.indexOf(c)>=0) {
						counter[i]++;
						addData(tag.con,counterWords[i]);
						break;
					}
				}
			}
		}
	}
	public static String countString() {
		SimpleDateFormat df=new SimpleDateFormat("MM月dd日HH時");
		String sdt=df.format(new Date(counterStart));
		StringBuilder c=new StringBuilder("/");
		for(int n=0;n<counterWords.length;n++) {
			c.append(counterWords[n]).append("が").append(counter[n]).append("回\n");
		}
		c.deleteCharAt(c.length()-1);
		c.append("書き込まれました\n");
		c.append(sdt).append("開始");
		c.append(df.format(new Date())).append("現在");
		return c.toString();
	}
}
