package module;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import bouyomi.BouyomiConection;
import bouyomi.BouyomiProxy;
import bouyomi.Counter.ICountEvent;
import bouyomi.DiscordAPI;
import bouyomi.IModule;
import bouyomi.Tag;

public class List implements IModule,ICountEvent{

	private static Driver driver;
	private String url="jdbc:sqlite:count.list";
	static{
		try{
			Class<?> c=BouyomiProxy.module.loader.loadClass("org.sqlite.JDBC");
			driver=(Driver) c.newInstance();
		}catch(ClassNotFoundException | InstantiationException | IllegalAccessException e){
			e.printStackTrace();
			driver=null;
		}
	}
	@Override
	public void call(Tag tag){
		if(driver==null)return;
		String user=tag.getTag("カウントリスト");
		if(tag.con.text.equals("カウントリスト")||user!=null) {
			try{
				Long.parseLong(user);
			}catch(NumberFormatException t) {
				user=null;
			}
			Connection connection=null;
			Statement statement=null;
			try{
				connection=driver.connect(url,new Properties());
				statement=connection.createStatement();
				int load=5;
				String sql;
				if(user==null)sql="select * from count limit "+load+" offset "+(counts(null)-load);
				else sql="select * from count where id = "+user+" limit "+load+" offset "+(counts(user)-load);
				//String sql="select * from count order by time desc";
				System.out.println(sql);
				ResultSet rs=statement.executeQuery(sql);
				//SimpleDateFormat sdf=new SimpleDateFormat("yy年MM月dd日HH時mm分ss秒SSS");
				StringBuilder sb=new StringBuilder();
				for(int i=0;i<5&&rs.next();i++){
					String w=rs.getString("word");
					if(w.length()>100)w=w.length()+"文字省略";
					sb.append(rs.getString("name")).append(w);
					//sb.append(sdf.format(new Date(rs.getLong("time"))));
					sb.append("\n");
				}
				DiscordAPI.chatDefaultHost(sb.toString());
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				try{
					if(statement!=null){
						statement.close();
					}
				}catch(SQLException e){
					e.printStackTrace();
				}
				try{
					if(connection!=null){
						connection.close();
					}
				}catch(SQLException e){
					e.printStackTrace();
				}
			}
			tag.con.text="";
			tag.con.mute=true;
		}
	}
	private int counts(String id) {
		Connection connection=null;
		Statement statement=null;
		try{
			connection=driver.connect(url,new Properties());
			statement=connection.createStatement();
			String sql="select COUNT(*) from count";
			if(id!=null)sql+=" where id = \""+id+"\"";
			//System.out.println(sql);
			return statement.executeQuery(sql).getInt(1);
		}catch(SQLException e){
			e.printStackTrace();
		}finally{
			try{
				if(statement!=null){
					statement.close();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
			try{
				if(connection!=null){
					connection.close();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}
		return -1;
	}
	@Override
	public void count(BouyomiConection bc,String word){
		//print(bc, word);
		Connection connection=null;
		Statement statement=null;
		try{
			connection=driver.connect(url,new Properties());
			statement=connection.createStatement();
			String sql="insert into count values('"+bc.userid+"','"+bc.user+"','"+word+"','"+bc.text+"',"+System.currentTimeMillis()+");";
			statement.execute(sql);
			//System.out.println("カウントDB書き込み");
		}catch(SQLException e){
			e.printStackTrace();
		}finally{
			try{
				if(statement!=null){
					statement.close();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
			try{
				if(connection!=null){
					connection.close();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}
	}
	@SuppressWarnings("unused")
	private void print(BouyomiConection bc,String word) {
		System.out.println("========ちんちん=========");
		System.out.println("ユーザID="+bc.userid);
		System.out.println("ニックネーム="+bc.user);
		System.out.println("一致した単語="+word);
		System.out.println("全文="+bc.text);
	}
}
