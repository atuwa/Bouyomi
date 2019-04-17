package bouyomi;

public interface IModule{
	public void call(Tag tag);

	public default void postcall(Tag t) {};
}