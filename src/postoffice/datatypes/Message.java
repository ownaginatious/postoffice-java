package postoffice.datatypes;

public interface Message<T extends Message<T>> {

	public byte[] marshall();
	
	public T demarshall();
}
