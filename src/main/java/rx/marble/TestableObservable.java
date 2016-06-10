package rx.marble;

import rx.Notification;
import java.util.List;

interface TestableObservable<T> {

    List<SubscriptionLog> getSubscriptions();

    List<Recorded<Notification<T>>> getMessages();
}

