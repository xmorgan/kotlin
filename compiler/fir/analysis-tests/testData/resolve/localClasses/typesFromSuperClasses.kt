// FILE: A.java
public interface A<T> {
    java.util.Collection<T> getAsCollection();
}

// FILE: B.java
public interface B<E> extends A<E> {}

// FILE: main.kt

fun main(b: B<String>) {
    b.asCollection.size
}
