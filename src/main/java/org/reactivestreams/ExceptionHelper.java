package org.reactivestreams;


public class ExceptionHelper {


    public static String findCallerInStackTrace(Class... callees) {
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        StackTraceElement current = null;
        for (StackTraceElement element : stackTrace) {
            current = element;
            boolean elementFromCallees = false;
            String elementClassName = element.getClassName();
            for (Class callee : callees) {
                elementFromCallees |= elementClassName.endsWith(callee.getSimpleName());
            }
            if (!elementFromCallees
                    && !elementClassName.endsWith(ExceptionHelper.class.getSimpleName())) {
                break;
            }
        }
        return current.getClassName()
                + "." + current.getMethodName()
                + "(" + current.getFileName() + ":" + current.getLineNumber() + ")";
    }
}
