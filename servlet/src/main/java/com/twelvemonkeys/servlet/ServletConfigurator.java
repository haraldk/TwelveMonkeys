package com.twelvemonkeys.servlet;

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.FilterIterator;
import com.twelvemonkeys.util.convert.ConversionException;
import com.twelvemonkeys.util.convert.Converter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * ServletConfigurator
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ServletConfigurator.java,v 1.0 Apr 30, 2010 2:51:38 PM haraldk Exp$
 * @see com.twelvemonkeys.servlet.InitParam
 */
final class ServletConfigurator {
    // TODO: Rethink @InitParam? Allow annotation of method parameters instead? Allows setLocation(@InitParam int x, @InitParam int y)
    // TODO: At least allow field injection
    // TODO: defaultValue, required

    private ServletConfigurator() {
    }

    public static void configure(final Servlet pServlet, final ServletConfig pConfig) throws ServletConfigException {
        new Configurator(pServlet, pConfig.getServletName()).configure(ServletUtil.asMap(pConfig));
    }

    public static void configure(final Filter pFilter, final FilterConfig pConfig) throws ServletConfigException {
        new Configurator(pFilter, pConfig.getFilterName()).configure(ServletUtil.asMap(pConfig));
    }

    private static class Configurator {
        private final Object servletOrFilter;
        private final String name;

        private Configurator(final Object servletOrFilter, final String name) {
            this.servletOrFilter = servletOrFilter;
            this.name = name;
        }

        private void configure(final Map<String, String> pMapping) throws ServletConfigException {
            // Loop over methods with InitParam annotations
            for (Method method : annotatedMethods(servletOrFilter.getClass(), InitParam.class)) {
                assertAcceptableMethod(method);

                // Get value or default, throw exception if missing required value
                Object value = getConfiguredValue(method, pMapping);

                if (value != null) {
                    // Inject value to this method
                    try {
                        method.invoke(servletOrFilter, value);
                    }
                    catch (IllegalAccessException e) {
                        // We know the method is accessible, so this should never happen
                        throw new Error(e);
                    }
                    catch (InvocationTargetException e) {
                        throw new ServletConfigException(String.format("Could not configure %s: %s", name, e.getCause().getMessage()), e.getCause());
                    }
                }
            }

            // TODO: Loop over fields with InitParam annotations

            // TODO: Log warning for mappings not present among InitParam annotated methods?
        }

        private Object getConfiguredValue(final Method method, final Map<String, String> mapping) throws ServletConfigException {
            InitParam initParam = method.getAnnotation(InitParam.class);
            String paramName = getParameterName(method, initParam);

            // Get parameter value
            String stringValue = mapping.get(paramName);

            if (stringValue == null && initParam.name().equals(InitParam.UNDEFINED)) {
                stringValue = mapping.get(StringUtil.camelToLisp(paramName));
            }

            if (stringValue == null) {
                // InitParam support required = true and throw exception if not present in map
                if (initParam.required()) {
                    throw new ServletConfigException(
                            String.format(
                                    "Could not configure %s: Required init-parameter \"%s\" of type %s is missing",
                                    name, paramName, method.getParameterTypes()[0]
                            )
                    );
                }
                else if (!initParam.defaultValue().equals(InitParam.UNDEFINED)) {
                    // Support default values
                    stringValue = initParam.defaultValue();
                }
            }

            // Convert value based on method arguments...
            return stringValue == null ? null : convertValue(method, stringValue);
        }

        private Object convertValue(final Method method, final String stringValue) throws ServletConfigException {
            // We know it's a single parameter method
            Class<?> type = method.getParameterTypes()[0];

            try {
                return String.class.equals(type) ? stringValue : Converter.getInstance().toObject(stringValue, type);
            }
            catch (ConversionException e) {
                throw new ServletConfigException(e);
            }
        }

        private String getParameterName(final Method method, final InitParam initParam) throws ServletConfigException {
            String paramName = initParam.name();

            if (paramName.equals(InitParam.UNDEFINED)) {
                String methodName = method.getName();
                if (methodName.startsWith("set") && methodName.length() > 3) {
                    paramName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                }
                else {
                    throw new ServletConfigException(
                            String.format(
                                    "Could not configure %s: InitParam annotated method must either specify name or follow Bean standard for properties (ie. setFoo => 'foo'): %s",
                                    name, method
                            )
                    );
                }
            }

            return paramName;
        }

        private void assertAcceptableMethod(final Method method) throws ServletConfigException {
            // Try to use setAccessible, if not public
            boolean isAccessible = Modifier.isPublic(method.getModifiers());

            if (!isAccessible) {
                try {
                    method.setAccessible(true);
                    isAccessible = true;
                }
                catch (SecurityException ignore) {
                    // Won't be accessible, we'll fail below
                }
            }

            if (!isAccessible || method.getReturnType() != Void.TYPE || method.getParameterTypes().length != 1) {
                throw new ServletConfigException(
                        String.format(
                                "Could not configure %s: InitParam annotated method must be public void and have a single parameter argument list: %s",
                                name, method
                        )
                );
            }
        }


        /**
         * Gets all methods annotated with the given annotations.
         *
         * @param pClass the class to get annotated methods from
         * @param pAnnotations the annotations to test for
         * @return an iterable that allows iterating over all methods with the given annotations.
         */
        private Iterable<Method> annotatedMethods(final Class<?> pClass, final Class<? extends Annotation>... pAnnotations) {
            return new Iterable<Method>() {
                public Iterator<Method> iterator() {
                    Set<Method> methods = new LinkedHashSet<Method>();

                    Class<?> cl = pClass;
                    while (cl.getSuperclass() != null) { // There's no annotations of interest on java.lang.Object
                        methods.addAll(Arrays.asList(cl.getDeclaredMethods()));

                        // TODO: What about interface methods? Do we really want them?
                        Class<?>[] interfaces = cl.getInterfaces();
                        for (Class<?> i : interfaces) {
                            methods.addAll(Arrays.asList(i.getDeclaredMethods()));
                        }

                        cl = cl.getSuperclass();
                    }

                    return new FilterIterator<Method>(methods.iterator(), new FilterIterator.Filter<Method>() {
                        public boolean accept(final Method pMethod) {
                            for (Class<? extends Annotation> annotation : pAnnotations) {
                                if (!pMethod.isAnnotationPresent(annotation) || isOverriddenWithAnnotation(pMethod, annotation)) {
                                    return false;
                                }
                            }

                            return true;
                        }

                        /**
                         * @param pMethod the method to test for override
                         * @param pAnnotation the annotation that must be present
                         * @return {@code true} iff the method is overridden in a subclass, and has annotation
                         * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/classes.html#8.4.8">The Java Language Specification: Classes: Inheritance, Overriding, and Hiding</a>
                         */
                        private boolean isOverriddenWithAnnotation(final Method pMethod, final Class<? extends Annotation> pAnnotation) {
                            if (Modifier.isPrivate(pMethod.getModifiers())) {
                                return false;
                            }

                            Class cl = pClass;

                            // Loop down up from subclass to superclass declaring the method
                            while (cl != null && !pMethod.getDeclaringClass().equals(cl)) {
                                try {
                                    Method override = cl.getDeclaredMethod(pMethod.getName(), pMethod.getParameterTypes());

                                    // Overridden, test if it has the annotation present
                                    if (override.isAnnotationPresent(pAnnotation)) {
                                        return true;
                                    }

                                }
                                catch (NoSuchMethodException ignore) {
                                }

                                cl = cl.getSuperclass();
                            }

                            return false;
                        }
                    });
                }
            };
        }
    }
}
