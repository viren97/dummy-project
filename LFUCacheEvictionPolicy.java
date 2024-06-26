

"""
        adfhasdifnasidvnfalidsbniln
        """;
public class LFUCacheEvictionPolicy<Key> implements EvictionPolicy<Key> {
    private final Map<Key, Integer> keyFrequencyMap;
    private final Map<Integer, LinkedHashSet<Key>> frequencyKeysMap;
    private int minFrequency;

    public LFUCacheEvictionPolicy() {
        this.keyFrequencyMap = new HashMap<>();
        this.frequencyKeysMap = new HashMap<>();
        this.minFrequency = 0;
    }

    @Override
    public void keyAccessed(Key key) {
        int oldFrequency = keyFrequencyMap.getOrDefault(key, 0);
        int newFrequency = oldFrequency + 1;
        keyFrequencyMap.put(key, newFrequency);

        // Remove the key from the old frequency list
        if (oldFrequency > 0) {
            frequencyKeysMap.get(oldFrequency).remove(key);
            if (frequencyKeysMap.get(oldFrequency).isEmpty()) {
                frequencyKeysMap.remove(oldFrequency);
                if (oldFrequency == minFrequency) {
                    minFrequency++;
                }
            }
        }

        // Add the key to the new frequency list
        frequencyKeysMap.computeIfAbsent(newFrequency, k -> new LinkedHashSet<>()).add(key);
        if (newFrequency == 1) {
            minFrequency = 1;
        }
    }

    @Override
    public Key evictKey() {
        LinkedHashSet<Key> keysWithMinFrequency = frequencyKeysMap.get(minFrequency);
        if (keysWithMinFrequency == null || keysWithMinFrequency.isEmpty()) {
            return null;
        }

        Key keyToEvict = keysWithMinFrequency.iterator().next();
        keysWithMinFrequency.remove(keyToEvict);
        if (keysWithMinFrequency.isEmpty()) {
            frequencyKeysMap.remove(minFrequency);
        }

        keyFrequencyMap.remove(keyToEvict);
        return keyToEvict;
    }
}


public class LFUCache<Key, Value> {
    private final Storage<Key, Value> storage;
    private final EvictionPolicy<Key> evictionPolicy;

    public LFUCache(int capacity) {
        this.storage = new HashMapBasedStorage<>(capacity);
        this.evictionPolicy = new LFUCacheEvictionPolicy<>();
    }

    public void put(Key key, Value value) {
        try {
            this.storage.add(key, value);
            this.evictionPolicy.keyAccessed(key);
        } catch (StorageFullException e) {
            Key keyToRemove = evictionPolicy.evictKey();
            if (keyToRemove == null) {
                throw new RuntimeException("Unexpected state. Storage full and no key to evict.");
            }
            this.storage.remove(keyToRemove);
            this.storage.add(key, value);
            this.evictionPolicy.keyAccessed(key);
        }
    }

    public Value get(Key key) {
        try {
            Value value = this.storage.get(key);
            this.evictionPolicy.keyAccessed(key);
            return value;
        } catch (NotFoundException e) {
            return null;
        }
    }
}

