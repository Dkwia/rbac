package rbac.manager;

import rbac.model.*;
import rbac.*;
import rbac.repository.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(User user) {
        Objects.requireNonNull(user);
        lock.writeLock().lock();
        try {
            if (users.containsKey(user.username())) {
                throw new IllegalArgumentException(
                        "User with username '" + user.username() + "' already exists");
            }
            users.put(user.username(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(User user) {
        if (user == null) return false;
        lock.writeLock().lock();
        try {
            return users.remove(user.username()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(users.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return users.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            users.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<User> findByUsername(String username) {
        return findById(username);
    }

    public Optional<User> findByEmail(String email) {
        lock.readLock().lock();
        try {
            return users.values().stream()
                    .filter(u -> u.email().equals(email))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilter(UserFilter filter) {
        return snapshotUsers().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        return snapshotUsers().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        return snapshotUsers().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        lock.readLock().lock();
        try {
            return users.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update(String username,
                       String newFullName,
                       String newEmail) {
        lock.writeLock().lock();
        try {
            if (!users.containsKey(username)) {
                throw new NoSuchElementException(
                        "User '" + username + "' not found");
            }
            User updated = User.validate(username, newFullName, newEmail);
            users.put(username, updated);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<User> snapshotUsers() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}
