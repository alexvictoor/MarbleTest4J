package io.reactivex.marble;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Created by Alexandre Victoor on 23/04/2017.
 */
class ObserverAdapter<T> implements Subscriber<T> {

    private Observer<? super T> observer;

    ObserverAdapter(Observer<? super T> observer) {
        this.observer = observer;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        observer.onSubscribe(new Disposable() {

            private boolean disposed = false;

            @Override
            public void dispose() {
                disposed = true;
                subscription.cancel();
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        });
        subscription.request(Long.MAX_VALUE);

    }

    @Override
    public void onNext(T t) {
        observer.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        observer.onError(t);
    }

    @Override
    public void onComplete() {
        observer.onComplete();
    }
}
