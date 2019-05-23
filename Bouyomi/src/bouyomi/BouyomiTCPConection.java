package bouyomi;

import static bouyomi.BouyomiProxy.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class BouyomiTCPConection extends BouyomiConection{

	/** このインスタンスの接続先 */
	private Socket soc;
	private int type;
	/** 接続単位で別のインスタンス */
	private int len;
	/** 受け取った文字データ */
	protected ByteArrayOutputStream baos2;
	/** 送信データ入れ */
	protected ByteArrayOutputStream baos;
	public BouyomiTCPConection(Socket s){
		soc=s;
	}
	private String readString(InputStream is) throws IOException{
		int ch1=is.read();//文字数を受信
		int ch2=is.read();
		int ch3=is.read();
		int ch4=is.read();
		if((ch1|ch2|ch3|ch4)<0){//文字数のデータが足りない時
			System.out.println("DataLen");
			throw new IOException("DataLen");//例外を出して終了
		}
		//ここで変数lenはバイト数になる
		len=((ch1<<0)+(ch2<<8)+(ch3<<16)+(ch4<<24));//文字数データから数値に
		ByteArrayOutputStream baos0=new ByteArrayOutputStream();//メッセージバイナリ書き込み先
		for(int i=0;i<len;i++){//メッセージデータ取得
			int j=is.read();
			if(j<0){//すべてのメッセージを取得できない時
				System.out.println("DataRead");
				throw new IOException("DataRead");//例外を出して終了
			}
			baos0.write(j);
		}
		return baos0.toString("utf-8");//UTF-8でデコード
	}
	private void read() throws IOException{
		soc.setSoTimeout(10000);
		InputStream is=soc.getInputStream();//Discord取得ソフトから読み込むストリーム
		int ch1=is.read();//コマンドバイトを取得
		int ch2=is.read();
		if((ch1|ch2)<0){//コマンドバイトを取得できない時終了
			//System.out.println("datatype ch1"+ch1+"ch2"+ch2);
			return;
		}
		int s=((ch2<<8)+(ch1<<0));
		if(s==1||s==0xF001);
		else{//読み上げコマンド以外の時
			if(s==0x10||s==0x20||s==0x30||s==0x40){//応答が必要ないコマンドの時
				send(bouyomi_port,new byte[] { (byte) ch1, (byte) ch2 });//棒読みちゃんに送信
				return;
			}
			System.out.println("datatype"+s);
			throw new IOException();//対応していないコマンドは例外を出して終了
		}
		baos=new ByteArrayOutputStream();//送信データバッファ
		baos.write(1);
		baos.write(0);
		//baos.write(ch1);//コマンド指定バイトを送信データバッファに書き込む
		//baos.write(ch2);
		byte[] d=new byte[8];
		int len=is.read(d);//この段階では変数lenは読み込みバイト数
		type=is.read();//文字コード読み込み
		if(len!=8||type<0){//読み込みバイト数が足りない時
			System.out.println("notLen9("+len+")");
			for(int i=0;i<len;i++)
				System.out.println(d[i]);
			throw new IOException();
		}
		baos.write(d);//その他パラメータを送信データバッファに書き込み
		ch1=is.read();//文字数を受信
		ch2=is.read();
		int ch3=is.read();
		int ch4=is.read();
		if((ch1|ch2|ch3|ch4)<0){//文字数のデータが足りない時
			System.out.println("DataLen");
			throw new IOException("DataLen");//例外を出して終了
		}
		//ここで変数lenはバイト数になる
		len=((ch1<<0)+(ch2<<8)+(ch3<<16)+(ch4<<24));//文字数データから数値に
		this.len=len;
		readMessage(is);
		if(d[7]==0) text=baos2.toString("utf-8");//UTF-8でデコード
		else if(d[7]==1) text=baos2.toString("utf-16");//UTF-16でデコード
		//System.out.println(text);
		if(text!=null){
			String key="濰濱濲濳濴濵濶濷濸濹濺濻濼濽濾濿";
			int index=text.indexOf(key);
			if(index>0){
				user=text.substring(0,index);
				text=text.substring(index+key.length());
				if(logger!=null) logger.log(user+"\t"+text);
			}else if(s!=0xF001){
				if(logger!=null) logger.log(text);
			}
		}
		if(s==0xF001){
			userid=readString(is);
			user=readString(is);
			if(logger!=null) logger.log(user+"\t"+text);
		}
	}
	private void readMessage(InputStream is) throws IOException {
		baos2=new ByteArrayOutputStream();//メッセージバイナリ書き込み先
		if(len<1)return;
		char fb=(char) is.read();//最初の文字を取得
		baos2.write(fb);//最初の文字をメッセージバイナリバッファに
		for(int i=1;i<len;i++){//メッセージデータ取得
			int j=is.read();
			if(j<0){//すべてのメッセージを取得できない時
				System.out.println("DataRead");
				throw new IOException("DataRead");//例外を出して終了
			}
			baos2.write(j);
		}
	}
	public void run(){
		try{
			read();//受信処理
			super.run();
		}catch(IOException e){
			e.printStackTrace();
		}finally{//切断は確実に
			try{
				soc.close();//Discord受信ソフトから切断
			}catch(IOException e1){
				e1.printStackTrace();
			}
		}
	}
}
