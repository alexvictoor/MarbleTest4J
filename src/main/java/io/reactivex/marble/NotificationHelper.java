package io.reactivex.marble;


import io.reactivex.Notification;
import io.reactivex.Observer;

public class NotificationHelper {

    static <T> void accept(Notification<T> notification, Observer<? super T> observer) {
        if (notification.isOnComplete()) {
            observer.onComplete();
        } else if (notification.isOnError()) {
            observer.onError(notification.getError());
        } else {
            observer.onNext(notification.getValue());
        }
    }
}
