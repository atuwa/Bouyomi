package bouyomi.test;

import java.security.SecureRandom;

public class TestKaikoga{
	public static void main(String[] args) {
		int num=-1;
		for(int j=0;j<1000;j++) {
			int i;
			for(i=0;;i++) {
				int r=new SecureRandom().nextInt(1000)+1;
				if(r==1)break;
			}
			//System.out.println(i);
			if(num<0)num=i;
			else num=(num+i)/2;
		}
		System.out.println(num);
	}
}
