package org.reactivestreams;

import java.util.List;

public interface TestablePublisher<T> extends Publisher<T> {

    List<SubscriptionLog> getSubscriptions();

    List<Recorded<T>> getMessages();
}

