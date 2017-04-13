package io.reactivex.marble;

import java.util.List;

interface TestableObservable<T> {

    List<SubscriptionLog> getSubscriptions();

    List<Recorded<T>> getMessages();
}

