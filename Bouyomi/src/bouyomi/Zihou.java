package bouyomi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Zihou{
	private static String command;
	public static boolean ON;
	public static void main(String[] args) throws IOException {
		InputStreamReader isr=new InputStreamReader(System.in);
		final BufferedReader br=new BufferedReader(isr);
		System.out.println("棒読みちゃんのポート(半角数字)");
		command=br.readLine();
		BouyomiProxy.bouyomi_port=Integer.parseInt(command);
		System.out.println("exitで終了");
		new Thread(){
			public void run(){
				while(true){
					try{
						command=br.readLine();
						if(command==null) System.exit(1);
						if("exit".equals(command)) {
							System.exit(0);
						}else if("on".equalsIgnoreCase(command)) {
							ON=true;
							System.out.println("時報有効化");
							BouyomiProxy.talk(BouyomiProxy.proxy_port,"時報を無効にしました");
						}else if("off".equalsIgnoreCase(command)) {
							ON=false;
							System.out.println("時報無効化");
							BouyomiProxy.talk(BouyomiProxy.proxy_port,"時報を有効にしました");
						}else{
							BouyomiProxy.talk(BouyomiProxy.proxy_port,command);
						}
					}catch(IOException e){
						System.exit(1);
					}
				}
			}
		}.start();
		String pattern="aaKK";
		SimpleDateFormat f=new SimpleDateFormat(pattern, Locale.JAPANESE);
		String last=f.format(new Date());
		//System.out.println(last);
		while(true){
			String now=f.format(new Date());
			if(!now.equals(last)) {
				last=now;
				StringBuilder mes=new StringBuilder();
				for(int index=0;index<now.length();index++) {
					char c=now.charAt(index);
					if(c!='0')mes.append(c);
				}
				mes.append("時ぐらいをお知らせします。");
				if(ON) {
					BouyomiProxy.talk(BouyomiProxy.proxy_port,mes.toString());
					System.out.println(mes.toString());
				}
			}
			try{
				Thread.sleep(1000*60);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
