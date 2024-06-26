
public class LRUEvictionPolicy<Key> implements EvictionPolicy<Key> {

    private DoublyLinkedList<Key> dll;
    private Map<Key, DoublyLinkedListNode<Key>> mapper;

    public LRUEvictionPolicy() {
        this.dll = new DoublyLinkedList<>();
        this.mapper = new HashMap<>();
    }

    @Override
    public void keyAccessed(Key key) {
        if (mapper.containsKey(key)) {
            dll.detachNode(mapper.get(key));
            dll.addNodeAtLast(mapper.get(key));
        } else {
            DoublyLinkedListNode<Key> newNode = dll.addElementAtLast(key);
            mapper.put(key, newNode);
        }
    }

    @Override
    public Key evictKey() {
        DoublyLinkedListNode<Key> first = dll.getFirstNode();
        if(first == null) {
            return null;
        }
        dll.detachNode(first);
        return first.getElement();
    }
}

public class Cache<Key, Value> {
    private final EvictionPolicy<Key> evictionPolicy;
    private final Storage<Key, Value> storage;

    public Cache(EvictionPolicy<Key> evictionPolicy, Storage<Key, Value> storage) {
        this.evictionPolicy = evictionPolicy;
        this.storage = storage;
    }

    public void put(Key key, Value value) {
        try {
            this.storage.add(key, value);
            this.evictionPolicy.keyAccessed(key);
        } catch (StorageFullException exception) {
            System.out.println("Got storage full. Will try to evict.");
            Key keyToRemove = evictionPolicy.evictKey();
            if (keyToRemove == null) {
                throw new RuntimeException("Unexpected State. Storage full and no key to evict.");
            }
            this.storage.remove(keyToRemove);
            System.out.println("Creating space by evicting item..." + keyToRemove);
            put(key, value);
        }
    }

    public Value get(Key key) {
        try {
            Value value = this.storage.get(key);
            this.evictionPolicy.keyAccessed(key);
            return value;
        } catch (NotFoundException notFoundException) {
            System.out.println("Tried to access non-existing key.");
            return null;
        }
    }


}