package org.reactivestreams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexandre Victoor on 05/06/2016.
 */
public class Parser {


    public static <T> List<Recorded<T>> parseMarbles(String marbles,
                                                                         Map<String, T> values,
                                                                         Exception errorValue,
                                                                         long frameTimeFactor,
                                                                         boolean materializeInnerObservables) {

        if (marbles.indexOf('!') != -1) {
            throw new IllegalArgumentException("Conventional marble diagrams cannot have the unsubscription marker '!'");
        }

        int len = marbles.length();
        List<Recorded<T>> testMessages = new ArrayList<>();
        int subIndex = marbles.indexOf('^');
        long frameOffset = subIndex == -1 ? 0 : (subIndex * -frameTimeFactor);

        long groupStart = -1;

        for (int i = 0; i < len; i++) {
            long frame = i * frameTimeFactor + frameOffset;
            Notification<T> notification = null;
            char c = marbles.charAt(i);
            switch (c) {
                case '-':
                case ' ':
                    break;
                case '(':
                    groupStart = frame;
                    break;
                case ')':
                    groupStart = -1;
                    break;
                case '|':
                    notification = Notification.createOnComplete();
                    break;
                case '^':
                    break;
                case '#':
                    notification = Notification.createOnError(errorValue);
                    break;
                default:
                    T value;
                    if (values == null) {
                        value = (T)String.valueOf(c);
                    } else {
                        value = values.get(String.valueOf(c));
                        if (materializeInnerObservables && value instanceof TestablePublisher) {
                            value = (T)((TestablePublisher)value).getMessages();
                        }
                    }
                    notification = Notification.createOnNext(value);
                    break;
            }

            if (notification != null) {
                long messageFrame = groupStart > -1 ? groupStart : frame;
                testMessages.add(new Recorded<>(messageFrame, notification));
            }
        }
        return testMessages;
    }

    public static <T> List<Recorded<T>> parseMarbles(String marbles, Map<String, T> values, long frameTimeFactor) {
        return parseMarbles(marbles, values, null, frameTimeFactor);
    }

    public static <T> List<Recorded<T>> parseMarbles(String marbles, Map<String, T> values, Exception errorValue, long frameTimeFactor) {
        return parseMarbles(marbles, values, errorValue, frameTimeFactor, false);
    }

    public static List<Recorded<String>> parseMarbles(String marbles, long frameTimeFactor) {
        return parseMarbles(marbles, null, frameTimeFactor);
    }

    public static SubscriptionLog parseMarblesAsSubscriptions(String marbles, long frameTimeFactor) {
        int len = marbles.length();
        long groupStart = -1;
        long subscriptionFrame = Long.MAX_VALUE;
        long unsubscriptionFrame = Long.MAX_VALUE;

        for (int i = 0; i < len; i++) {
            long frame = i * frameTimeFactor;
            char c = marbles.charAt(i);
            switch (c) {
                case '-':
                case ' ':
                    break;
                case '(':
                    groupStart = frame;
                    break;
                case ')':
                    groupStart = -1;
                    break;
                case '^':
                    if (subscriptionFrame != Long.MAX_VALUE) {
                        throw new IllegalArgumentException("Found a second subscription point \'^\' in a " +
                                "subscription marble diagram. There can only be one.");
                    }
                    subscriptionFrame = groupStart > -1 ? groupStart : frame;
                    break;
                case '!':
                    if (unsubscriptionFrame != Long.MAX_VALUE) {
                        throw new IllegalArgumentException("Found a second subscription point \'^\' in a " +
                                "subscription marble diagram. There can only be one.");
                    }
                    unsubscriptionFrame = groupStart > -1 ? groupStart : frame;
                    break;
                default:
                    throw new IllegalArgumentException("There can only be \'^\' and \'!\' markers in a " +
                            "subscription marble diagram. Found instead \'' + c + '\'.");
            }
        }

        if (unsubscriptionFrame < 0) {
            return new SubscriptionLog(subscriptionFrame);
        }
        return new SubscriptionLog(subscriptionFrame, unsubscriptionFrame);

    }

}
