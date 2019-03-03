package bouyomi;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Counter{

	private static String[] counterWords=null;
	private static int[] counter;
	private static long counterReset;
	private static long counterStart;
	static {
		loadCountWords();
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
		counterStart=System.currentTimeMillis();
		counterReset=counterStart+86400000;
	}
	public static void write() {
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
	public static void count(BouyomiConection con){
		String text=con.text;
		if(counterWords!=null&&counterWords.length>0) {
			if(text.equals("カウント取得")){
				DiscordAPI.chatDefaultHost(countString());
				return;
			}
			if(con.mute)return;
			for(int i=0;i<counterWords.length;i++) {
				String[] cw=counterWords[i].split(",");
				for(String c:cw) {
					if(text.indexOf(c)>=0) {
						counter[i]++;
						if(System.currentTimeMillis()>counterReset) {
							counterStart=System.currentTimeMillis();
							counterReset=counterStart+86400000;
							DiscordAPI.chatDefaultHost(countString()+"初期化");
						}
						break;
					}
				}
			}
		}
	}
	public static String countString() {
		SimpleDateFormat df=new SimpleDateFormat("dd日HH時");
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
