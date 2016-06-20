package rx.marble;


public class ExceptionHelper {


    public static String findCallerInStackTrace(Class callee) {
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        StackTraceElement current = null;
        for (StackTraceElement element : stackTrace) {
            current = element;
            if (!element.getClassName().endsWith(callee.getSimpleName())
                    && !element.getClassName().endsWith(ExceptionHelper.class.getSimpleName())) {
                break;
            }
        }
        return current.getClassName()
                + "." + current.getMethodName()
                + "(" + current.getFileName() + ":" + current.getLineNumber() + ")";
    }
}
