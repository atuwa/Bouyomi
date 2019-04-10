package bouyomi;

import java.util.ArrayList;

public class Admin{

	private ArrayList<String> list=new ArrayList<String>();

	public Admin() {
		try{
			BouyomiProxy.load(list,"admin.txt");
		}catch(Exception e){
			e.printStackTrace();
		}
		BouyomiProxy.module.load(this);
	}
	public boolean isAdmin(String id) {
		if(id==null||id.isEmpty())return false;
		return list.contains(id);
	}
}
