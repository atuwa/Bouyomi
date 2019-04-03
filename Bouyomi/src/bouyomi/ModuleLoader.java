package bouyomi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class ModuleLoader{
	public ArrayList<IModule> modules=new ArrayList<IModule>();
	public URLClassLoader loader;
	public File path;
	public void load(File f) {
		if(!f.isDirectory())return;
		path=f;
		//System.out.println(f);
		try{
			loader=new URLClassLoader(new URL[] {f.toURI().toURL()});
			for(String s:f.list()) {
				int i=s.lastIndexOf(".class");
				if(i<=0||s.indexOf('$')>=0)continue;
				String name=s.substring(0,i);
				System.out.println("モジュール"+name);
				try{
					Class<?> c=loader.loadClass(f.getName()+"."+name);
					Object o=c.newInstance();
					if(o instanceof IModule)modules.add((IModule)o);
					if(o instanceof IAutoSave)IAutoSave.Register((IAutoSave)o);
				}catch(InstantiationException | IllegalAccessException e){
					//e.printStackTrace();
				}catch(ClassNotFoundException | NoClassDefFoundError e) {
					e.printStackTrace();
				}
			}
		}catch(MalformedURLException e){
			e.printStackTrace();
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
}