package nl.first8.readerswriterlock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Do not use in production, created as example to show condition variables.
 * 
 * Lock to make sure either a single writer has access, or potentially multiple
 * readers.
 */
public final class ReadersWriterLock {
    /**
     * A lock that can be released while waiting on a condition.
     */
    private final Lock lock = new ReentrantLock();
    /**
     * Used to signal/await the release of the writers lock.
     */
    private final Condition onChange = lock.newCondition();
    /**
     * The numbers of readers holding a lock. The number of times
     * acquireReadersLock() was called minus the number of time
     * releaseReadersLock() was called.
     * 
     * -1 means locked for writing.
     */
    private int activeReaders = 0;

    /**
     * Returns when no writer is active. After returning from this method and
     * before releasing the lock, no writer locks can be acquired. Only more
     * read locks can be acquired, until the all read locks have been released.
     */
    public void acquireReadLock() {
        lock.lock();
        try {
            while (activeReaders < 0) {
                // Temporarily release the lock, until we can continue.
                try {
                    onChange.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Thread.interrupted();
                }
            }
            activeReaders++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases a read lock and returns immediately.
     */
    public void releaseReadLock() {
        // Decrease number of readers
        lock.lock();
        if (activeReaders == 0) {
            throw new ReleasingInvalidLockException(
                    "Unable to release readers lock.");
        }
        --activeReaders;
        // Signal to threads that have called await() to wake up and re-check
        // their condition.
        onChange.signal();
        lock.unlock();
    }

    /**
     * Blocks until all read locks are released. After execution of this method,
     * all attempts to acquire another lock, read- or write-lock alike, will
     * block until releaseWriteLock is called.
     */
    public void acquireWriteLock() {
        lock.lock();
        try {
            // Check the 'condition', verify there are no readers
            while (activeReaders != 0) {
                try {
                    // Temporarily release the lock and block until some other
                    // thread signals a change that might interest us.
                    onChange.await();
                    // After returning from wait, the lock is re-acquired
                    // automatically.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Thread.interrupted();
                }
            }
            // Now that there are no (longer any) read or write locks.
            activeReaders = -1;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns immediately and releases the write lock.
     */
    public void releaseWriteLock() {
        lock.lock();
        if (activeReaders != -1) {
            throw new ReleasingInvalidLockException(
                    "Unable to release writer lock, not locked for writing.");
        }
        activeReaders = 0;
        // Signal to threads that have called await() to wake up and re-check
        // their condition.
        onChange.signal();
        lock.unlock();
    }

    static class ReleasingInvalidLockException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ReleasingInvalidLockException(final String message) {
            super(message);
        }
    }
}
