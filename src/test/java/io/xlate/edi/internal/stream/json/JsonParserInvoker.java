package io.xlate.edi.internal.stream.json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

interface JsonParserInvoker {

    @SuppressWarnings("unchecked")
    default <T> T invoke(Object instance, String methodName, Class<T> returnType) throws Throwable {
        Class<?> clazz = instance.getClass();

        try {
            Method method = Stream.of(clazz.getMethods(), clazz.getDeclaredMethods())
                .flatMap(Arrays::stream)
                .filter(m -> m.getName().equals(methodName))
                .filter(m -> m.getParameterCount() == 0)
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(methodName));

            return (T) method.invoke(instance);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
