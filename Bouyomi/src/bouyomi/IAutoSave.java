package bouyomi;

import java.io.IOException;
import java.util.ArrayList;

public interface IAutoSave{
	public static final ArrayList<IAutoSave> list=new ArrayList<IAutoSave>();
	public void autoSave()throws IOException;
	public default void shutdownHook() {}
	public static void thread() {
		new Thread("AutoSave") {
			@Override
			public void run() {
				while(true) {
					try{
						Thread.sleep(5*60*1000);
						synchronized(list){
							for(IAutoSave a:list) {
								try{
									a.autoSave();
								}catch(IOException e){
									e.printStackTrace();
								}
							}
						}
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		}.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				synchronized(list){
					for(IAutoSave a:list)a.shutdownHook();
				}
				DailyUpdate.updater.write();
			}
		});
	}
	public static void Register(IAutoSave a) {
		synchronized(list) {
			list.add(a);
		}
	}
}
