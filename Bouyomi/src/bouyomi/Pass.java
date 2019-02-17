package bouyomi;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**ワンタイムパスワード機能*/
public class Pass{
	private static ArrayList<String> pass=new ArrayList<String>();
	public static void read() throws IOException {
		FileInputStream fis=new FileInputStream("pass.txt");
		InputStreamReader isr=new InputStreamReader(fis,StandardCharsets.UTF_8);
		BufferedReader br=null;
		try {
			br=new BufferedReader(isr);
			while(br.ready()) {
				String line=br.readLine();
				if(line==null)break;
				pass.add(line);
			}
		}finally {
			if(br!=null)br.close();
			isr.close();
			fis.close();
		}
	}
	public static void write() throws IOException {
		FileOutputStream fos=new FileOutputStream("pass.txt");
		OutputStreamWriter osw=new OutputStreamWriter(fos,StandardCharsets.UTF_8);
		try{
			for(int i=0;i<pass.size();i++) {
				osw.write(pass.get(i)+"\n");
			}
		}finally{
			osw.close();
		}
	}
	public static void exit(String pass) {
		if(pass==null||!Pass.pass.contains(pass))return;
		Pass.pass.remove(pass);
		System.out.println("ワンタイムパスワードで強制終了しますpass="+pass);
		try{
			write();
		}catch(IOException e){
			e.printStackTrace();
		}
		TubeAPI.operation("stop");
		System.exit(1000);
	}
}
