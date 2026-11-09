package com.urlshortener.util;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    ReentrantLock lock = new ReentrantLock();

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
    }

    @Override
    public V get(Object key) {
        return super.get(key);
    }

    @Override
    public V put(K key, V value) {
        lock.lock();
        try {
            return super.put(key, value);
        } finally {
            lock.unlock();
        }
    }

}
