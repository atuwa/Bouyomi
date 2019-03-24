package bouyomi;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SaveProxyData implements IAutoSave{

	public final String file;
	private BufferedOutputStream logFileOS;
	public SaveProxyData(String logFile) {
		file=logFile;
		try{
			FileOutputStream fos=new FileOutputStream(logFile,true);//追加モードでファイルを開く
			logFileOS=new BufferedOutputStream(fos);
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		IAutoSave.Register(this);
	}
	public void log(String s) {
		try{
			logFileOS.write((s.replace('\n',' ')+"\n").getBytes(StandardCharsets.UTF_8));//改行文字を追加してバイナリ化
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void shutdownHook() {
		try{
			if(logFileOS!=null)logFileOS.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void autoSave() throws IOException{
		if(logFileOS!=null) {
			logFileOS.flush();
			//System.out.println("定期ログフラッシュ");
		}
	}
}
