package com.amplitude.util;


/**
 * Copy from https://github.com/google/dagger/blob/master/java/dagger/internal/DoubleCheck.java
 */
public class DoubleCheck<T> implements Provider<T> {
    private static final Object UNINITIALIZED = new Object();

    private volatile Provider<T> provider;
    private volatile Object instance = UNINITIALIZED;

    private DoubleCheck(Provider<T> provider) {
        assert provider != null;
        this.provider = provider;
    }

    @SuppressWarnings("unchecked") // cast only happens when result comes from the provider
    @Override
    public T get() {
        Object result = instance;
        if (result == UNINITIALIZED) {
            synchronized (this) {
                result = instance;
                if (result == UNINITIALIZED) {
                    result = provider.get();
                    instance = reentrantCheck(instance, result);
                    /* Null out the reference to the provider. We are never going to need it again, so we
                     * can make it eligible for GC. */
                    provider = null;
                }
            }
        }
        return (T) result;
    }

    /**
     * Checks to see if creating the new instance has resulted in a recursive call. If it has, and the
     * new instance is the same as the current instance, return the instance. However, if the new
     * instance differs from the current instance, an {@link IllegalStateException} is thrown.
     */
    public static Object reentrantCheck(Object currentInstance, Object newInstance) {
        boolean isReentrant = !(currentInstance == UNINITIALIZED);

        if (isReentrant && currentInstance != newInstance) {
            throw new IllegalStateException("Scoped provider was invoked recursively returning "
                    + "different results: " + currentInstance + " & " + newInstance + ". This is likely "
                    + "due to a circular dependency.");
        }
        return newInstance;
    }

    /** Returns a {@link Provider} that caches the value from the given delegate provider. */
    public static <P extends Provider<T>, T> Provider<T> provider(P delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
        if (delegate instanceof DoubleCheck) {
            /* This should be a rare case, but if we have a scoped @Binds that delegates to a scoped
             * binding, we shouldn't cache the value again. */
            return delegate;
        }
        return new DoubleCheck<T>(delegate);
    }
}
