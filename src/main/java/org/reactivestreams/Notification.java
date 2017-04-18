package org.reactivestreams;

/**
 * Copy/pasted from rxjava Notification class
 */
public final class Notification<T> {

    private final Kind kind;
    private final Throwable throwable;
    private final T value;

    private static final Notification<Void> ON_COMPLETED = new Notification<Void>(Kind.OnCompleted, null, null);

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnNext}, and assigns it a value.
     *
     * @param <T> the actual value type held by the Notification
     * @param t
     *          the item to assign to the notification as its value
     * @return an {@code OnNext} variety of {@code Notification}
     */
    public static <T> Notification<T> createOnNext(T t) {
        return new Notification<T>(Kind.OnNext, t, null);
    }

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnError}, and assigns it an exception.
     *
     * @param <T> the actual value type held by the Notification
     * @param e
     *          the exception to assign to the notification
     * @return an {@code OnError} variety of {@code Notification}
     */
    public static <T> Notification<T> createOnError(Throwable e) {
        return new Notification<T>(Kind.OnError, null, e);
    }

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnCompleted}.
     *
     * @param <T> the actual value type held by the Notification
     * @return an {@code OnCompleted} variety of {@code Notification}
     */
    @SuppressWarnings("unchecked")
    public static <T> Notification<T> createOnCompleted() {
        return (Notification<T>) ON_COMPLETED;
    }

    /**
     * Creates and returns a {@code Notification} of variety {@code Kind.OnCompleted}.
     *
     * @param <T> the actual value type held by the Notification
     * @param type the type token to help with type inference of {@code <T>}
     * @deprecated this method does the same as {@link #createOnCompleted()} and does not use the passed in type hence it's useless.
     * @return an {@code OnCompleted} variety of {@code Notification}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> Notification<T> createOnCompleted(Class<T> type) {
        return (Notification<T>) ON_COMPLETED;
    }

    private Notification(Kind kind, T value, Throwable e) {
        this.value = value;
        this.throwable = e;
        this.kind = kind;
    }

    /**
     * Retrieves the exception associated with this (onError) notification.
     *
     * @return the Throwable associated with this (onError) notification
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Retrieves the item associated with this (onNext) notification.
     *
     * @return the item associated with this (onNext) notification
     */
    public T getValue() {
        return value;
    }

    /**
     * Indicates whether this notification has an item associated with it.
     *
     * @return a boolean indicating whether or not this notification has an item associated with it
     */
    public boolean hasValue() {
        return isOnNext() && value != null;
// isn't "null" a valid item?
    }

    /**
     * Indicates whether this notification has an exception associated with it.
     *
     * @return a boolean indicating whether this notification has an exception associated with it
     */
    public boolean hasThrowable() {
        return isOnError() && throwable != null;
    }

    /**
     * Retrieves the kind of this notification: {@code OnNext}, {@code OnError}, or {@code OnCompleted}
     *
     * @return the kind of the notification: {@code OnNext}, {@code OnError}, or {@code OnCompleted}
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Indicates whether this notification represents an {@code onError} event.
     *
     * @return a boolean indicating whether this notification represents an {@code onError} event
     */
    public boolean isOnError() {
        return getKind() == Kind.OnError;
    }

    /**
     * Indicates whether this notification represents an {@code onCompleted} event.
     *
     * @return a boolean indicating whether this notification represents an {@code onCompleted} event
     */
    public boolean isOnCompleted() {
        return getKind() == Kind.OnCompleted;
    }

    /**
     * Indicates whether this notification represents an {@code onNext} event.
     *
     * @return a boolean indicating whether this notification represents an {@code onNext} event
     */
    public boolean isOnNext() {
        return getKind() == Kind.OnNext;
    }

    /**
     * Forwards this notification on to a specified {@link Subscriber}.
     * @param subscriber the target subscriber to call onXXX methods on based on the kind of this Notification instance
     */
    public void accept(Subscriber<? super T> subscriber) {
        if (kind == Kind.OnNext) {
            subscriber.onNext(getValue());
        } else if (kind == Kind.OnCompleted) {
            subscriber.onComplete();
        } else {
            subscriber.onError(getThrowable());
        }
    }

    /**
     * Specifies the kind of the notification: an element, an error or a completion notification.
     */
    public enum Kind {
        OnNext, OnError, OnCompleted
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(64).append('[').append(super.toString())
                .append(' ').append(getKind());
        if (hasValue()) {
            str.append(' ').append(getValue());
        }
        if (hasThrowable()) {
            str.append(' ').append(getThrowable().getMessage());
        }
        str.append(']');
        return str.toString();
    }

    @Override
    public int hashCode() {
        int hash = getKind().hashCode();
        if (hasValue()) {
            hash = hash * 31 + getValue().hashCode();
        }
        if (hasThrowable()) {
            hash = hash * 31 + getThrowable().hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        Notification<?> notification = (Notification<?>) obj;
        return notification.getKind() == getKind() && (value == notification.value || (value != null && value.equals(notification.value))) && (throwable == notification.throwable || (throwable != null && throwable.equals(notification.throwable)));

    }
}
