package storage;

public interface Storage<T> {

    T getData(String location);

    boolean saveData(String location, T data);
}
