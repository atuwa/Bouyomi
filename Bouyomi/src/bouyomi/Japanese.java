package bouyomi;

import java.util.ArrayList;
import java.util.HashMap;

//こけえもんのローマ字を翻訳する
public class Japanese{
	public static DiscordAPI chat_server;
	private static HashMap<String,String> map=new HashMap<String,String>();
	//ひらがなにする人に変なことを言わせたくない
	public static ArrayList<String> NGword=new ArrayList<String>();
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
		map.put("da","だ");map.put("di","ぢ");map.put("du","づ");map.put("de","で");map.put("do","ど");
		map.put("ga","が");map.put("gi","ぐ");map.put("ge","げ");map.put("go","ご");
		map.put("ba","ば");map.put("bi","び");map.put("bu","ぶ");map.put("be","べ");map.put("bo","ぼ");
		map.put("pa","ぱ");map.put("pi","ぴ");map.put("pu","ぷ");map.put("pe","ぺ");map.put("po","ぽ");
		map.put("fa","ふぁ");map.put("fi","ふぃ");map.put("fu","ふ");map.put("fe","ふぇ");map.put("fo","ふぉ");
		map.put("byi","びゃ");map.put("byi","びぃ");map.put("byu","びゅ");map.put("bye","びぇ");map.put("byo","びょ");

		map.put("n","ん");map.put("nn","ん");

		NGword.add("mannko");NGword.add("manko");NGword.add("tinko");NGword.add("tinnko");
		NGword.add("oppai");NGword.add("unnti");NGword.add("unti");NGword.add("unnko");
		NGword.add("unko");NGword.add("tinnko");

		map.put("-","ー");
	}
	public static void trans(String text) {
		if(text.length()<5||chat_server==null)return;
		for(int i=0;i<text.length();i++) {
			char c=text.charAt(i);
			if(c=='-'||c=='?'||c==',');
			else if(c<0x5B||c>0x7E)return;
		}
		for(int i=0;i<NGword.size();i++) {
			if(text.indexOf(NGword.get(i))>=0)return;
		}
		StringBuilder result=new StringBuilder();
		for(int i=0;i<text.length();i++) {
			char c=text.charAt(i);
			if(i+1<text.length()) {
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
		System.out.println("ローマ字変換"+text+"="+result);
		chat_server.chat("/"+result.toString());
	}
}
