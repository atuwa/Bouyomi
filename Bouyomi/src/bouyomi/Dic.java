package bouyomi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import bouyomi.ListMap.Value;

/**実験中機能(今は使われてない)*/
public class Dic{

	/**文字統一辞書(今は使われてない)*/
	public static HashMap<String,String> FW=new HashMap<String,String>();
	/**教育単純置換辞書(今は使われてない)*/
	public static ListMap<String,String> Study=new ListMap<String,String>();
	static {
		FW.put("０","0");FW.put("１","1");FW.put("２","2");FW.put("３","3");FW.put("４","4");
		FW.put("５","5");FW.put("６","6");FW.put("７","7");FW.put("８","8");FW.put("９","9");
		FW.put("A","A");FW.put("B","B");FW.put("C","C");FW.put("D","D");FW.put("E","E");
		FW.put("F","F");FW.put("G","G");FW.put("H","H");FW.put("I","I");FW.put("J","J");
		FW.put("K","K");FW.put("L","L");FW.put("M","M");FW.put("N","N");FW.put("O","O");
		FW.put("P","P");FW.put("Q","Q");FW.put("R","R");FW.put("S","S");FW.put("T","T");
		FW.put("U","U");FW.put("V","V");FW.put("W","W");FW.put("X","X");
		FW.put("Y","Y");FW.put("Z","Z");
		FW.put("a","A");FW.put("b","B");FW.put("c","C");FW.put("d","D");FW.put("e","E");
		FW.put("f","F");FW.put("g","G");FW.put("h","H");FW.put("i","I");FW.put("j","J");
		FW.put("k","K");FW.put("l","L");FW.put("m","M");FW.put("n","N");FW.put("o","O");
		FW.put("p","P");FW.put("q","Q");FW.put("r","R");FW.put("s","S");FW.put("t","T");
		FW.put("u","U");FW.put("v","V");FW.put("w","W");FW.put("x","X");
		FW.put("y","Y");FW.put("z","Z");
		FW.put("ａ","A");FW.put("ｂ","B");FW.put("ｃ","C");FW.put("ｄ","D");FW.put("ｅ","E");
		FW.put("ｆ","F");FW.put("ｇ","G");FW.put("ｈ","H");FW.put("ｉ","I");FW.put("ｊ","J");
		FW.put("ｋ","K");FW.put("ｌ","L");FW.put("ｍ","M");FW.put("ｎ","N");FW.put("ｏ","O");
		FW.put("ｐ","P");FW.put("ｑ","Q");FW.put("ｒ","R");FW.put("ｓ","S");FW.put("ｔ","T");
		FW.put("ｕ","U");FW.put("ｖ","V");FW.put("ｗ","W");FW.put("ｘ","X");
		FW.put("ｙ","Y");FW.put("ｚ","Z");
		FW.put("ｱ","ア");FW.put("ｲ","イ");FW.put("ｳ","ウ");FW.put("ｴ","エ");FW.put("ｵ","オ");
		FW.put("ｶ","カ");FW.put("ｷ","キ");FW.put("ｸ","ク");FW.put("ｹ","ケ");FW.put("ｺ","コ");
		FW.put("ｻ","サ");FW.put("ｼ","シ");FW.put("ｽ","ス");FW.put("ｾ","セ");FW.put("ｿ","ソ");
		FW.put("ﾀ","タ");FW.put("ﾁ","チ");FW.put("ﾂ","ツ");FW.put("ﾃ","テ");FW.put("ﾄ","ト");
		FW.put("ﾅ","ナ");FW.put("ﾆ","ニ");FW.put("ﾇ","ヌ");FW.put("ﾈ","ネ");FW.put("ﾉ","ノ");
		FW.put("ﾊ","ハ");FW.put("ﾋ","ヒ");FW.put("ﾌ","フ");FW.put("ﾍ","ヘ");FW.put("ﾎ","ホ");
		FW.put("ﾏ","マ");FW.put("ﾐ","ミ");FW.put("ﾑ","ム");FW.put("ﾒ","メ");FW.put("ﾓ","モ");
		FW.put("ﾔ","ヤ");FW.put("ﾕ","ユ");FW.put("ﾖ","ヨ");
		FW.put("ﾜ","ワ");FW.put("ｦ","ヲ");FW.put("ﾝ","ン");
	}
	private static void tag(String text) {
		int index=text.indexOf("教育(");
		if(index<0)index=text.indexOf("教育（");
		if(index<0)return;//タグを含まない時
		int ki=text.indexOf(')');//半角
		int zi=text.indexOf('）');//全角
		if(ki<0)ki=zi;
		if(ki<0)return;//閉じカッコが無い時
		if(ki<index+3)return;//閉じカッコの位置がおかしい時
		if(ki==index+3)return;//0文字
		String tag=text.substring(index+3,ki);
		int ee=tag.indexOf('=');
		if(ee+1>tag.length())return;
		String key=tag.substring(0,ee);
		String value=tag.substring(ee+1);
		Study.put(key,value);
	}
	//ReplaceStudy.dic
	/**実験中機能(今は使われてない)*/
	public static String ReplaceStudy(String text) throws IOException {
		tag(text);
		if(Study.isEmpty())return text;
		class CL implements BiConsumer<String,String>{
			public String d;
			public CL(String s) {
				d=s;
			}
			@Override
			public void accept(String t,String u){
				d=d.replaceAll(Matcher.quoteReplacement(t),Matcher.quoteReplacement(u));
			}
			public String toString() {
				return d;
			}
		}
		CL c=new CL(text);
		FW.forEach(c);
		Study.forEach(c);
		return c.toString();
	}
	//ReplaceStudy.dic
	/**実験中機能(今は使われてない)*/
	public static void loadStudy(String path) throws IOException {
		File rf=new File(path);
		FileInputStream fis=new FileInputStream(rf);
		InputStreamReader r=new InputStreamReader(fis,"UTF-8");
		BufferedReader br=new BufferedReader(r);
		Study.clear();
		try{
			while(true) {
				String rl=br.readLine();
				if(rl==null)break;
				int index=rl.indexOf("\t");
				if(rl.length()<index+4||index<1)continue;
				rl=rl.substring(index+3);
				index=rl.indexOf("\t");
				String key=rl.substring(0,index);
				String value=rl.substring(index+1);
				Study.put(key,value);
			}
			Study.rawList().sort(new Comparator<ListMap.Value<String,String>>() {
				@Override
				public int compare(Value<String, String> o1,Value<String, String> o2){
					return o2.getKey().length()-o1.getKey().length();
				}
			});
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			try{
				br.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}
