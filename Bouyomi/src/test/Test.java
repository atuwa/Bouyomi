package test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Test{
	public static void main(String[] args){

		Connection connection=null;
		Statement statement=null;

		URLClassLoader loader;
		try{
			loader=new URLClassLoader(new URL[] { new File("D:/SOU/Desktop/sqlite-jdbc-3.27.2.1.jar").toURI().toURL() },Test.class.getClassLoader());
		}catch(MalformedURLException e2){
			e2.printStackTrace();
			return;
		}
		try{
			Class<?> c=loader.loadClass("org.sqlite.JDBC");
			String url="jdbc:sqlite:D:\\SOU\\Desktop\\sqlite-tools-win32-x86-3270200\\tintin.db";
			try{
				connection=((Driver) c.newInstance()).connect(url,new Properties());
			}catch(InstantiationException|IllegalAccessException e){
				e.printStackTrace();
				return;
			}
			statement=connection.createStatement();
			String sql="select * from tintin";
			ResultSet rs=statement.executeQuery(sql);
			while(rs.next()){
				System.out.println(rs.getString("id")+rs.getString("name")+rs.getString("word"));
			}
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}catch(SQLException e){
			e.printStackTrace();
		}finally{
			try{
				loader.close();
			}catch(IOException e1){
				e1.printStackTrace();
			}
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
}
