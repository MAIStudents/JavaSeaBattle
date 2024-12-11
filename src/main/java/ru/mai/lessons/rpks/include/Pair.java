package ru.mai.lessons.rpks.include;

public class Pair<K, V> {
  private final K key;
  private final V value;

  public Pair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public K first() {
    return key;
  }

  public V second() {
    return value;
  }

  @Override
  public String toString() {
    return "Pair{" + "key=" + key + ", value=" + value + '}';
  }
}
