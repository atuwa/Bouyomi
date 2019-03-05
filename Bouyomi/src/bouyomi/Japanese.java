package bouyomi;

import java.util.ArrayList;
import java.util.HashMap;

//こけえもんのローマ字を翻訳する
public class Japanese{
	public static DiscordAPI chat_server;
	private static HashMap<String,String> map=new HashMap<String,String>();
	//ひらがなにする人 に変なことを言わせたくない
	public static ArrayList<String> NGword=new ArrayList<String>();
	public static long lastMatch;
	public static long block;
	private static int blockCunt;
	public static boolean active=true;
	static {
		map.put("a","あ");map.put("i","い");map.put("u","う");map.put("e","え");map.put("o","お");
		map.put("ka","か");map.put("ki","き");map.put("ku","く");map.put("ke","け");map.put("ko","こ");
		map.put("sa","さ");map.put("si","し");map.put("su","す");map.put("se","せ");map.put("so","そ");
		map.put("ta","た");map.put("ti","ち");map.put("tu","つ");map.put("te","て");map.put("to","と");
		map.put("na","な");map.put("ni","に");map.put("nu","ぬ");map.put("ne","ね");map.put("no","の");
		map.put("ha","は");map.put("hi","ひ");map.put("hu","ふ");map.put("he","へ");map.put("ho","ほ");
		map.put("ma","ま");map.put("mi","み");map.put("mu","む");map.put("me","め");map.put("mo","も");
		map.put("ya","や");map.put("yi","い");map.put("yu","ゆ");map.put("ye","いぇ");map.put("yo","よ");
		map.put("ra","ら");map.put("ri","り");map.put("ru","る");map.put("re","れ");map.put("ro","ろ");
		map.put("wa","わ");map.put("wi","うぃ");map.put("wu","う");map.put("we","うぇ");map.put("wo","を");

		map.put("ca","か");map.put("ci","し");map.put("cu","く");map.put("ce","せ");map.put("co","こ");
		map.put("va","ヴぁ");map.put("vi","ヴぃ");map.put("vu","ヴ");map.put("ve","ヴぇ");map.put("vo","ヴぉ");

		map.put("cha","ちゃ");map.put("chi","ち");map.put("chu","ちゅ");map.put("che","ちぇ");map.put("cho","ちょ");
		map.put("xtu","っ");map.put("tsu","つ");
		map.put("lya","ゃ");map.put("li","ぃ");map.put("lu","ぅ");map.put("le","ぇ");map.put("lo","ぉ");
		map.put("xa","ぁ");map.put("xi","ぃ");map.put("xu","ぅ");map.put("xe","ぇ");map.put("xo","ぉ");

		map.put("kya","きゃ");map.put("kyu","きゅ");map.put("kyo","きょ");
		map.put("sya","しゃ");map.put("syu","しゅ");map.put("syo","しょ");
		map.put("tya","ちゃ");map.put("tyu","ちゅ");map.put("tyo","ちょ");
		map.put("nya","にゃ");map.put("nyu","にゅ");map.put("nyo","にょ");
		map.put("hya","ひゃ");map.put("hyu","ひゅ");map.put("hyo","ひょ");
		map.put("mya","みゃ");map.put("myu","みゅ");map.put("myo","みょ");
		map.put("rya","りゃ");map.put("ryu","りゅ");map.put("ryo","りょ");
		map.put("gya","ぎゃ");map.put("gyu","ぎゅ");map.put("gyo","ぎょ");
		map.put("za","ざ");map.put("zi","じ");map.put("zu","ず");map.put("ze","ぜ");map.put("zo","ぞ");
		map.put("ja","じゃ");map.put("ji","じ");map.put("ju","じゅ");map.put("je","じぇ");map.put("jo","じょ");
		map.put("jya","じゃ");map.put("jyi","じぃ");map.put("jyu","じゅ");map.put("jye","じぇ");map.put("jyo","じょ");
		map.put("zya","じゃ");map.put("zyi","じぃ");map.put("zyu","じゅ");map.put("zye","じぇ");map.put("zyo","じょ");
		map.put("da","だ");map.put("di","ぢ");map.put("du","づ");map.put("de","で");map.put("do","ど");
		map.put("ga","が");map.put("gi","ぐ");map.put("ge","げ");map.put("go","ご");
		map.put("ba","ば");map.put("bi","び");map.put("bu","ぶ");map.put("be","べ");map.put("bo","ぼ");
		map.put("pa","ぱ");map.put("pi","ぴ");map.put("pu","ぷ");map.put("pe","ぺ");map.put("po","ぽ");
		map.put("fa","ふぁ");map.put("fi","ふぃ");map.put("fu","ふ");map.put("fe","ふぇ");map.put("fo","ふぉ");
		map.put("byi","びゃ");map.put("byi","びぃ");map.put("byu","びゅ");map.put("bye","びぇ");map.put("byo","びょ");
		map.put("sha","しゃ");map.put("shi","し");map.put("shu","しゅ");map.put("she","しぇ");map.put("sho","しょ");
		map.put("vyu","ヴゅ");

		map.put("nn","ん");map.put("n","ん");map.put(".","。");map.put(",","、");map.put("~","～");

		NGword.add("まんこ");NGword.add("ちんこ");NGword.add("ちんぽ");NGword.add("tんぽ");
		NGword.add("おぱい");NGword.add("うんち");NGword.add("うんこ");NGword.add("ほも");
		NGword.add("せくす");NGword.add("せいし");NGword.add("せいえき");NGword.add("ざめん");
		NGword.add("きんたま");NGword.add("まんまん");NGword.add("みるく");NGword.add("ぱいぱん");
		NGword.add("おなに");NGword.add("ぺにす");NGword.add("ちんちん");

		map.put("-","ー");
	}
	public static boolean isTrans(String text) {
		for(int i=0;i<text.length();i++) {
			char c=text.charAt(i);
			if(c=='-'||c=='?'||c==','||c=='.'||c=='!'||c==' '||c=='/');
			else if(c>=0x30&&c<0x40);
			else if(c<0x60||c>0x7A)return false;
		}
		return true;
	}
	public static boolean trans(String text) {
		//有効、5文字以上、投稿サーバあり、変換可能。を全て満たす時だけ変換
		if(!active||text.length()<5||chat_server==null||!isTrans(text))return false;
		if(block>System.currentTimeMillis())return false;
		StringBuilder result=new StringBuilder();
		for(int i=0;i<text.length();i++) {
			char c=text.charAt(i);
			if(i+1<text.length()) {
				if(c==text.charAt(i+1)) {
					if(c=='a'||c=='i'||c=='u'||c=='e'||c=='o'||c=='/'||c=='^');
					else if(c>=0x30&&c<0x40);
					else if(c!='n') {
						result.append('っ');
						continue;
					}
				}
				if(i+2<text.length()) {
					String ms=new String(new char[] {c,text.charAt(i+1),text.charAt(i+2)});
					String r=map.get(ms);
					if(r!=null) {
						i+=2;
						result.append(r);
						continue;
					}
				}
				String ms=new String(new char[] {c,text.charAt(i+1)});
				String r=map.get(ms);
				if(r!=null) {
					i+=1;
					result.append(r);
					continue;
				}
			}
			String ms=String.valueOf(c);
			String r=map.get(ms);
			if(r!=null) {
				result.append(r);
			}else result.append(ms);
		}
		String r=result.toString();
		//NGワードなしで使うならここでchat_server.chat(r);するといい
		System.out.println("ローマ字変換"+text+"="+result);
		for(int i=0;i<NGword.size();i++) {
			if(r.indexOf(NGword.get(i))>=0) {
				//chat_server.chat("/NGワードを含みます");
				block();
				return false;
			}
		}
		char[] taisaku=new char[] {' ','^','/','ー','_','\\','っ'};
		StringBuilder t=new StringBuilder();
		for(int i=0;i<r.length();i++) {
			char c=r.charAt(i);
			boolean flag=false;
			for(char ch:taisaku) {
				if(c==ch)flag=true;
			}
			if(flag);
			else t.append(c);
		}
		//System.out.println(t.toString());
		for(int ta=0;ta<taisaku.length;ta++) {
			for(int i=0;i<NGword.size();i++) {
				if(t.indexOf(NGword.get(i))>=0) {
					//chat_server.chat("/NGワード対策するな");
					block();
					return false;
				}
			}
		}
		if(isTrans(r))return false;
		chat_server.chat("/"+r);
		return true;
	}
	public static void block() {
		System.out.println("NG掛かった");
		if(System.currentTimeMillis()-lastMatch<2000)blockCunt++;//2秒以内にNGに掛かった時
		else blockCunt=0;//それ以上空いていた時カウントをリセット
		lastMatch=System.currentTimeMillis();//最後に掛かった時間
		if(blockCunt>3) {//3回以上掛かった時
			block=lastMatch+30*1000;//ブロック時間10秒
			chat_server.chat("/NGワードを連投されたので30秒間変換を停止します。");
		}
	}
}
