package bouyomi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import bouyomi.Counter.ICountEvent;
import bouyomi.IModule.BouyomiEvent;

public class ModuleLoader{
	public ArrayList<IModule> modules=new ArrayList<IModule>();
	private ArrayList<URL> jars=new ArrayList<URL>();
	public URLClassLoader loader;
	public File path;
	public void load(File f) {
		if(!f.isDirectory())return;
		path=f;
		try{
			jars.add(f.toURI().toURL());
		}catch(MalformedURLException e1){
			e1.printStackTrace();
		}
		File jd=new File("jar");
		jd.mkdir();
		isJar(jd);
		loader=new URLClassLoader(jars.toArray(new URL[jars.size()]));
		for(String s:f.list()) {
			int i=s.lastIndexOf(".class");
			if(i<=0||s.indexOf('$')>=0)continue;
			String name=s.substring(0,i);
			System.out.println("モジュール"+name);
			try{
				Class<?> c=loader.loadClass(f.getName()+"."+name);
				load(c.newInstance());
			}catch(InstantiationException | IllegalAccessException e){
				//e.printStackTrace();
			}catch(ClassNotFoundException | NoClassDefFoundError e) {
				e.printStackTrace();
			}
		}
	}
	public void load(Object o) {
		if(o instanceof IModule)modules.add((IModule)o);
		if(o instanceof IAutoSave)IAutoSave.Register((IAutoSave)o);
		if(o instanceof ICountEvent)ICountEvent.Register((ICountEvent)o);
	}
	private void isJar(File f) {
		if(f.isDirectory()) {
			for(File g:f.listFiles())isJar(g);
		}
		if(f.getName().endsWith(".jar")) {
			try{
				jars.add(f.toURI().toURL());
				System.out.println("ライブラリ"+f.getName());
			}catch(MalformedURLException e){
				e.printStackTrace();
			}
		}
	}
	public boolean isActive() {
		return !modules.isEmpty();
	}
	public void call(Tag t) {
		if(modules.isEmpty())return;
		for(IModule m:modules){
			m.call(t);
		}
	}
	public void postcall(Tag t) {
		if(modules.isEmpty())return;
		for(IModule m:modules){
			m.postcall(t);
		}
	}
	public void event(BouyomiEvent e){
		if(modules.isEmpty())return;
		for(IModule m:modules){
			m.event(e);
		}
	}
}