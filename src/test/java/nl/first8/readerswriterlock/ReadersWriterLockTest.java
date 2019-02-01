package nl.first8.readerswriterlock;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.FutureTask;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.Test;

public class ReadersWriterLockTest {

    @Test
    public void checkReadLockIsBlockedWriteLock() {
        // Given an acquired write lock.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireWriteLock();

        // When we subsequently call acquire a read lock.
        final FutureTask<Void> task = new FutureTask<>(() -> {
            lock.acquireReadLock();
            return null;
        });
        new Thread(task).start();
        
        // Then we expect the acquire read lock call to block.
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        
        // Until the write lock is released.
        lock.releaseWriteLock();
        Awaitility.await().atMost(Duration.ONE_SECOND).until(task::isDone);
    }

    @Test
    public void checkWriteLockIsBlockedReadersLock() {
        // Given an acquired read lock.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireReadLock();

        // When we subsequently call acquire a write lock.
        final FutureTask<Void> task = new FutureTask<>(() -> {
            lock.acquireWriteLock();
            return null;
        });
        new Thread(task).start();

        // Then we expect the acquire read lock call to block.
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.releaseReadLock();
        Awaitility.await().atMost(Duration.ONE_SECOND).until(task::isDone);
    }

    @Test
    public void checkWriteLockIsBlockedWriteLock() {
        // Given an acquired write lock.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireWriteLock();

        // When we subsequently call acquire a write lock.
        final FutureTask<Void> task = new FutureTask<>(() -> {
            lock.acquireWriteLock();
            return null;
        });
        new Thread(task).start();

        // Then we expect the acquire read lock call to block.
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.releaseWriteLock();
        Awaitility.await().atMost(Duration.ONE_SECOND).until(task::isDone);
    }

    @Test
    public void checkWriteLockIsBlockedByMultipleReaders() {
        // Given multiple acquired readers locks.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireReadLock();
        lock.acquireReadLock();

        // When we subsequently call acquire a readers lock.
        final FutureTask<Void> task = new FutureTask<>(() -> {
            lock.acquireWriteLock();
            return null;
        });
        new Thread(task).start();

        // Then we expect that the call to acquire the write lock blocks.
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.acquireReadLock();
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.releaseReadLock();
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.releaseReadLock();
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.releaseReadLock();

        // Until the number of readers is 0.
        Awaitility.await().atMost(Duration.ONE_SECOND).until(task::isDone);
    }

    @Test
    public void checkWriteLockIsBlockedByMultipleReadersInOtherOrder() {
        // Given multiple acquired readers locks.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireReadLock();
        lock.acquireReadLock();

        // When we subsequently call acquire a readers lock.
        final FutureTask<Void> task = new FutureTask<>(() -> {
            lock.acquireWriteLock();
            return null;
        });
        new Thread(task).start();

        // Then we expect that the call to acquire the write lock blocks.
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.releaseReadLock();
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.acquireReadLock();
        lock.releaseReadLock();
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());
        lock.releaseReadLock();

        // Until the number of readers is 0.
        Awaitility.await().atMost(Duration.ONE_SECOND).until(task::isDone);
    }

    // Check exception handling

    @Test
    public void checkSpuriousWakeUpInAcquireReadersLock() {
        // Given an acquired write lock.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireWriteLock();

        // When we subsequently call acquire a readers lock.
        final FutureTask<Void> task = new FutureTask<>(() -> {
            lock.acquireReadLock();
            return null;
        });
        final Thread thread = new Thread(task);
        thread.start();

        // And when we simulate a spurious wake up, by interrupting the thread.
        thread.interrupt();
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());

        // Until the write lock is released.
        lock.releaseWriteLock();
        Awaitility.await().atMost(Duration.ONE_SECOND).until(task::isDone);
    }

    @Test
    public void checkSpuriousWakeUpInAcquireWriteLock() {
        // Given an acquired readers lock.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireReadLock();

        // When we subsequently call acquire a write lock.
        final FutureTask<Void> task = new FutureTask<>(() -> {
            lock.acquireWriteLock();
            return null;
        });
        final Thread thread = new Thread(task);
        thread.start();

        // And when we simulate a spurious wake up, by interrupting the thread.
        thread.interrupt();

        // Then we expect that the call to acquire the write lock keep blocking.
        Awaitility.await().pollDelay(Duration.ONE_MILLISECOND)
                .until(() -> !task.isDone());

        // Until the readers lock is released.
        lock.releaseReadLock();
        Awaitility.await().atMost(Duration.ONE_SECOND).until(task::isDone);
    }

    @Test
    public void checkExceptionWhenOnlyReleasingReadLock() {
        // Check that incorrect usage results in exceptions.
        assertThrows(//
                ReadersWriterLock.ReleasingInvalidLockException.class, //
                () -> new ReadersWriterLock().releaseReadLock());
    }

    @Test
    public void checkExceptionWhenOnlyReleasingWriteLock() {
        // Check that incorrect usage results in exceptions.
        assertThrows(//
                ReadersWriterLock.ReleasingInvalidLockException.class, //
                () -> new ReadersWriterLock().releaseWriteLock());
    }

    @Test
    public void checkExceptionWhenReleasingTooMuch() {
        // Check that incorrect usage results in exceptions.
        final ReadersWriterLock lock = new ReadersWriterLock();
        lock.acquireReadLock();
        lock.releaseReadLock();
        assertThrows(//
                ReadersWriterLock.ReleasingInvalidLockException.class, //
                () -> lock.releaseReadLock());
    }
}