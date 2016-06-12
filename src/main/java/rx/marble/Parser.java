package rx.marble;

import rx.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexandre Victoor on 05/06/2016.
 */
public class Parser {


    public static <T> List<Recorded<Notification<T>>> parseMarbles(String marbles,
                                                                   Map<String, T> values,
                                                                   Exception errorValue,
                                                                   long frameTimeFactor,
                                                                   boolean materializeInnerObservables)
    {

        if (marbles.indexOf('!') != -1)
        {
            throw new RuntimeException("Conventional marble diagrams cannot have the unsubscription marker '!'");
        }

        int len = marbles.length();
        List<Recorded<Notification<T>>> testMessages = new ArrayList<>();
        int subIndex = marbles.indexOf('^');
        long frameOffset = subIndex == -1 ? 0 : (subIndex * -frameTimeFactor);

        long groupStart = -1;

        for (int i = 0; i < len; i++)
        {
            long frame = i * frameTimeFactor + frameOffset;
            Notification<T> notification = null;
            char c = marbles.charAt(i);
            switch (c)
            {
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
                    notification = Notification.createOnCompleted();
                    break;
                case '^':
                    break;
                case '#':
                    notification = Notification.createOnError(errorValue);
                    break;
                default:
                    T value;
                    if (values == null)
                    {
                        value = (T)String.valueOf(c);
                    }
                    else
                    {
                        value = values.get(String.valueOf(c));
                        if (materializeInnerObservables)
                        {
                            /*if ((typeof(T) == typeof(object)) && ReflectionHelper.IsTestableObservable(value))
                            {
                                value = (T) ReflectionHelper.RetrieveNotificationsFromTestableObservable(value);
                            }*/
                        }
                    }
                    notification = Notification.createOnNext(value);
                    break;
            }

            if (notification != null)
            {
                long messageFrame = groupStart > -1 ? groupStart : frame;
                testMessages.add(new Recorded<>(messageFrame, notification));
            }
        }
        return testMessages;
    }

    public static <T> List<Recorded<Notification<T>>> parseMarbles(String marbles, Map<String, T> values, long frameTimeFactor) {
        return parseMarbles(marbles, values, null, frameTimeFactor);
    }

    public static <T> List<Recorded<Notification<T>>> parseMarbles(String marbles, Map<String, T> values, Exception errorValue, long frameTimeFactor) {
        return parseMarbles(marbles, values, errorValue, frameTimeFactor, false);
    }

    public static List<Recorded<Notification<String>>> parseMarbles(String marbles, long frameTimeFactor) {
        return parseMarbles(marbles, null, frameTimeFactor);
    }
}
