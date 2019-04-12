package bouyomi;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Listを使ったMapです。順序が保証されますがHashMapより遅いです。<br>
 * Listは標準ではjava.util.ArrayListが使用されます。変更するにはmakeList()をOverrideしてください。*/
public class ListMap<K, V> extends AbstractMap<K, V>{
	public static class Value<K, V> implements Entry<K, V>{
		private K key;
		private V value;
		public Value(K key2,V value2){
			key=key2;
			value=value2;
		}
		public void setKey(K k){
			key=k;
		}
		public K getKey(){
			return key;
		}
		public V setValue(V v){
			final V old=value;
			value=v;
			return old;
		}
		public V getValue(){
			return value;
		}
		@Override
		public int hashCode(){
			final int prime=31;
			int result=1;
			result=prime*result+((key==null) ? 0 : key.hashCode());
			result=prime*result+((value==null) ? 0 : value.hashCode());
			return result;
		}
		public boolean equals(Object obj) {
			if(this==obj) return true;
			if(getClass()!=obj.getClass()) return false;
			if(obj instanceof Entry<?,?>);
			else return false;
			Entry<?,?> e=(Entry<?, ?>) obj;
			return equalsKey(e.getKey())&&equalsValue(e.getValue());
		}
		public boolean equalsValue(Object value2){
			if(value==null) return value2==null;
			return value.equals(value2);
		}
		public boolean equalsKey(Object key2){
			if(key==null) return key2==null;
			return key.equals(key2);
		}
	}
	private List<Value<K, V>> list=null;
	private ListMap<K, V>.KeySet ks;
	private ListMap<K, V>.Values vs;
	private ListMap<K, V>.EntrySet es;
	/** Overrideして使う */
	public List<Value<K, V>> makeList(){
		return new ArrayList<Value<K, V>>();
	}
	public List<Value<K, V>> rawList(){
		return list;
	}
	@Override
	public int size(){
		return list.size();
	}
	@Override
	public boolean isEmpty(){
		return list.isEmpty();
	}
	@Override
	public boolean containsKey(Object key){
		return get(key)!=null;
	}
	{
		if(list==null)list=makeList();
	}
	public ListMap(){

	}
	public ListMap(Map<? extends K, ? extends V> m){
		putAll(m);
	}
	public ListMap(List<Value<K,V>> list) {
		this.list=list;
	}
	@Override
	public synchronized boolean containsValue(Object value){
		for(Value<K, V> v:list){
			if(v.equalsValue(value)) return true;
		}
		return false;
	}
	@Override
	public synchronized V get(Object key){
		for(Value<K, V> v:list){
			if(v.equalsKey(key)) return v.getValue();
		}
		return null;
	}
	@Override
	public V put(K key,V value){
		Value<K, V> old=null;
		for(Value<K, V> v:list){
			if(v.equalsKey(key)){
				old=v;
				break;
			}
		}
		Value<K, V> nv=new Value<K, V>(key,value);
		if(old!=null) {
			int index=list.indexOf(old);
			list.remove(index);
			list.add(index,nv);
		}else list.add(nv);
		return old==null?null:old.getValue();
	}
	public V remove(Entry<?,?> e){
		Value<K, V> r=null;
		for(Value<K, V> v:list){
			if(v.equals(e)){
				r=v;
				break;
			}
		}
		if(r!=null) list.remove(r);
		return r==null ? null : r.getValue();
	}
	@Override
	public V remove(Object key){
		Value<K, V> r=null;
		for(Value<K, V> v:list){
			if(v.equalsKey(key)){
				r=v;
				break;
			}
		}
		if(r!=null) list.remove(r);
		return r==null ? null : r.getValue();
	}
	@Override
	public void putAll(Map<? extends K, ? extends V> m){
		m.keySet();
	}
	public K removeValue(Object value){
		Value<K, V> r=null;
		for(Value<K, V> v:list){
			if(v.equalsValue(value)){
				r=v;
				break;
			}
		}
		if(r!=null) list.remove(r);
		return r==null ? null : r.getKey();
	}
	@Override
	public void clear(){
		list.clear();
	}
	private class KeyIterator implements Iterator<K>{
		private Iterator<Value<K, V>> li=list==null ? null : list.iterator();
		@Override
		public boolean hasNext(){
			if(li==null) return false;
			return li.hasNext();
		}
		@Override
		public K next(){
			if(li==null) return null;
			Value<K, V> next=li.next();
			if(next==null) return null;
			return next.getKey();
		}
	}

	private class ValueIterator implements Iterator<V>{
		private Iterator<Value<K, V>> li=list==null ? null : list.iterator();
		@Override
		public boolean hasNext(){
			if(li==null) return false;
			return li.hasNext();
		}
		@Override
		public V next(){
			if(li==null) return null;
			Value<K, V> next=li.next();
			if(next==null) return null;
			return next.getValue();
		}
	}

	private class KeySet extends AbstractSet<K>{
		public final int size(){
			return ListMap.this.size();
		}
		public final void clear(){
			ListMap.this.clear();
		}
		public final Iterator<K> iterator(){
			return new KeyIterator();
		}
		public final boolean contains(Object o){
			return containsKey(o);
		}
		public final boolean remove(Object key){
			return ListMap.this.remove(key)!=null;
		}
		public synchronized final void forEach(Consumer<? super K> action){
			if(action==null)
				throw new NullPointerException();
			if(size()>0&&list!=null){
				for(int i=0;i<list.size();i++){
					Value<K, V> e=list.get(i);
					action.accept(e==null ? null : e.getKey());
				}
			}
		}
	}
	final class EntryIterator implements Iterator<Map.Entry<K,V>> {
		private Iterator<Value<K, V>> li=list==null ? null : list.iterator();
		@Override
		public boolean hasNext(){
			if(li==null) return false;
			return li.hasNext();
		}
		@Override
		public Entry<K, V> next(){
			if(li==null) return null;
			return li.next();
		}
	}
	private class EntrySet extends AbstractSet<Map.Entry<K, V>>{
		public final int size(){
			return ListMap.this.size();
		}
		public final void clear(){
			ListMap.this.clear();
		}
		public final Iterator<Map.Entry<K, V>> iterator(){
			return new EntryIterator();
		}
		public final boolean contains(Object o){
			if(!(o instanceof Map.Entry))return false;
			Map.Entry<?, ?> e=(Map.Entry<?, ?>) o;
			Object key=e.getKey();
			V v=get(key);
			return v!=null&&v.equals(e.getValue());
		}
		public final boolean remove(Object o){
			if(o instanceof Map.Entry){
				Map.Entry<?, ?> e=(Map.Entry<?, ?>) o;
				return ListMap.this.remove(e)!=null;
			}
			return false;
		}
		public synchronized final void forEach(Consumer<? super Map.Entry<K, V>> action){
			if(action==null)
				throw new NullPointerException();
			if(size()>0&&list!=null){
				for(Map.Entry<K, V> e:list){
					action.accept(e);
				}
			}
		}
	}

	private class Values extends AbstractCollection<V>{
		public final int size(){
			return ListMap.this.size();
		}
		public final void clear(){
			ListMap.this.clear();
		}
		public final Iterator<V> iterator(){
			return new ValueIterator();
		}
		public final boolean contains(Object o){
			return containsValue(o);
		}
		public final void forEach(Consumer<? super V> action){
			if(action==null)throw new NullPointerException();
			if(size()>0&&list!=null){
				for(Map.Entry<K, V> e:list){
					if(e!=null)action.accept(e.getValue());
				}
			}
		}
	}
	@Override
	public Set<K> keySet(){
		if(ks!=null) return ks;
		return ks=new KeySet();
	}
	@Override
	public Collection<V> values(){
		if(vs!=null)return vs;
		return vs=new Values();
	}
	@Override
	public Set<Entry<K, V>> entrySet(){
		if(es!=null)return es;
		return es=new EntrySet();
	}
	/**ソートする*/
	public void sortKey(final Comparator<K> c) {
		Comparator<Value<K,V>> c0=new Comparator<Value<K,V>>(){
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Value<K, V> o1,Value<K, V> o2){
				if(c==null) {
					K k=o2.getKey();
					if(k instanceof Comparable) {
						return ((Comparable<K>)k).compareTo(o1.getKey());
					}
					return 0;
				}
				return c.compare(o1.getKey(),o2.getKey());
			}
		};
		list.sort(c0);
	}
	/**ソートする*/
	public void sortValue(final Comparator<V> c) {
		Comparator<Value<K,V>> c0=new Comparator<Value<K,V>>(){
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Value<K, V> o1,Value<K, V> o2){
				if(c==null) {
					V v=o2.getValue();
					if(v instanceof Comparable) {
						return ((Comparable<V>)v).compareTo(o1.getValue());
					}
					return 0;
				}
				return c.compare(o1.getValue(),o2.getValue());
			}
		};
		list.sort(c0);
	}
}