/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util.service;

import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.util.FilterIterator;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * A registry for service provider objects.
 * <p/>
 * Service providers are looked up from the classpath, under the path
 * {@code META-INF/services/}&lt;full-class-name&gt;.
 * <p/>
 * For example:<br/>
 * {@code META-INF/services/com.company.package.spi.MyService}.
 * <p/>
 * The file should contain a list of fully-qualified concrete class names,
 * one per line.
 * <p/>
 * The <em>full-class-name</em> represents an interface or (typically) an
 * abstract class, and is the same class used as the category for this registry.
 * Note that only one instance of a concrete subclass may be registered with a
 * specific category at a time.
 * <p/>
 * <small>Implementation detail: This class is a clean room implementation of
 * a service registry and does not use the proprietary {@code sun.misc.Service}
 * class that is referred to in the <em>JAR File specification</em>.
 * This class should work on any Java platform.
 * </small>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: com/twelvemonkeys/util/service/ServiceRegistry.java#2 $
 * @see RegisterableService
 * @see <a href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider">JAR File Specification</a>
 */
public class ServiceRegistry {
    // TODO: Security issues?
    // TODO: Application contexts? Probably use instance per thread group..

    /**
     * "META-INF/services/"
     */
    public static final String SERVICES = "META-INF/services/";

    // Class to CategoryRegistry mapping
    private final Map<Class<?>, CategoryRegistry> categoryMap;

    /**
     * Creates a {@code ServiceRegistry} instance with a set of categories
     * taken from the {@code pCategories} argument.
     * <p/>
     * The categories are constant during the lifetime of the registry, and may
     * not be changed after initial creation.
     *
     * @param pCategories an {@code Iterator} containing
     *                    {@code Class} objects that defines this registry's categories.
     * @throws IllegalArgumentException if {@code pCategories} is {@code null}.
     * @throws ClassCastException       if {@code pCategories} contains anything
     *                                  but {@code Class} objects.
     */
    public ServiceRegistry(final Iterator<? extends Class<?>> pCategories) {
        Validate.notNull(pCategories, "categories");

        Map<Class<?>, CategoryRegistry> map = new LinkedHashMap<Class<?>, CategoryRegistry>();

        while (pCategories.hasNext()) {
            putCategory(map, pCategories.next());
        }

        // NOTE: Categories are constant for the lifetime of a registry
        categoryMap = Collections.unmodifiableMap(map);
    }

    private <T> void putCategory(Map<Class<?>, CategoryRegistry> pMap, Class<T> pCategory) {
        CategoryRegistry<T> registry = new CategoryRegistry<T>(pCategory);
        pMap.put(pCategory, registry);
    }

    /**
     * Registers all provider implementations for this {@code ServiceRegistry}
     * found in the application classpath.
     *
     * @throws ServiceConfigurationError if an error occurred during registration
     */
    public void registerApplicationClasspathSPIs() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Iterator<Class<?>> categories = categories();

        while (categories.hasNext()) {
            Class<?> category = categories.next();

            try {
                // Find all META-INF/services/ + name on class path
                String name = SERVICES + category.getName();
                Enumeration<URL> spiResources = loader.getResources(name);

                while (spiResources.hasMoreElements()) {
                    URL resource = spiResources.nextElement();
                    registerSPIs(resource, category, loader);
                }
            }
            catch (IOException e) {
                throw new ServiceConfigurationError(e);
            }
        }
    }

    /**
     * Registers all SPIs listed in the given resource.
     *
     * @param pResource the resource to load SPIs from
     * @param pCategory the category class
     * @param pLoader   the class loader to use
     */
    <T> void registerSPIs(final URL pResource, final Class<T> pCategory, final ClassLoader pLoader) {
        Properties classNames = new Properties();

        try {
            classNames.load(pResource.openStream());
        }
        catch (IOException e) {
            throw new ServiceConfigurationError(e);
        }

        if (!classNames.isEmpty()) {
            @SuppressWarnings({"unchecked"})
            CategoryRegistry<T> registry = categoryMap.get(pCategory);

            Set providerClassNames = classNames.keySet();

            for (Object providerClassName : providerClassNames) {
                String className = (String) providerClassName;
                try {
                    @SuppressWarnings({"unchecked"})
                    Class<T> providerClass = (Class<T>) Class.forName(className, true, pLoader);
                    T provider = providerClass.newInstance();
                    registry.register(provider);
                }
                catch (ClassNotFoundException e) {
                    throw new ServiceConfigurationError(e);
                }
                catch (IllegalAccessException e) {
                    throw new ServiceConfigurationError(e);
                }
                catch (InstantiationException e) {
                    throw new ServiceConfigurationError(e);
                }
                catch (IllegalArgumentException e) {
                    throw new ServiceConfigurationError(e);
                }
            }
        }
    }

    /**
     * Returns an {@code Iterator} containing all providers in the given
     * category.
     * <p/>
     * The iterator supports removal.
     * <p/>
     * <small>
     * NOTE: Removing a provider from the iterator, deregisters the current
     * provider (as returned by the last invocation of {@code next()}) from
     * {@code pCategory}, it does <em>not</em> remove the provider
     * from other categories in the registry.
     * </small>
     *
     * @param pCategory the category class
     * @return an {@code Iterator} containing all providers in the given
     *         category.
     * @throws IllegalArgumentException if {@code pCategory} is not a valid
     *                                  category in this registry
     */
    protected <T> Iterator<T> providers(Class<T> pCategory) {
        return getRegistry(pCategory).providers();
    }

    /**
     * Returns an {@code Iterator} containing all categories in this registry.
     * <p/>
     * The iterator does not support removal.
     *
     * @return an {@code Iterator} containing all categories in this registry.
     */
    protected Iterator<Class<?>> categories() {
        return categoryMap.keySet().iterator();
    }

    /**
     * Returns an {@code Iterator} containing all categories in this registry
     * the given {@code pProvider} <em>may be registered with</em>.
     * <p/>
     * The iterator does not support removal.
     *
     * @param pProvider the provider instance
     * @return an {@code Iterator} containing all categories in this registry
     *         the given {@code pProvider} may be registered with
     */
    protected Iterator<Class<?>> compatibleCategories(final Object pProvider) {
        return new FilterIterator<Class<?>>(categories(),
                                  new FilterIterator.Filter<Class<?>>() {
                                      public boolean accept(Class<?> pElement) {
                                          return pElement.isInstance(pProvider);
                                      }
                                  });
    }

    /**
     * Returns an {@code Iterator} containing all categories in this registry
     * the given {@code pProvider} <em>is currently registered with</em>.
     * <p/>
     * The iterator supports removal.
     * <p/>
     * <small>
     * NOTE: Removing a category from the iterator, de-registers
     * {@code pProvider} from the current category (as returned by the last
     * invocation of {@code next()}), it does <em>not</em> remove the category
     * itself from the registry.
     * </small>
     *
     * @param pProvider the provider instance
     * @return an {@code Iterator} containing all categories in this registry
     *         the given {@code pProvider} may be registered with
     */
    protected Iterator<Class<?>> containingCategories(final Object pProvider) {
        // TODO: Is removal using the iterator really a good idea?
        return new FilterIterator<Class<?>>(categories(),
                                  new FilterIterator.Filter<Class<?>>() {
                                      public boolean accept(Class<?> pElement) {
                                          return getRegistry(pElement).contains(pProvider);
                                      }
                                  }) {
            Class<?> current;

            public Class next() {
                return (current = super.next());
            }

            public void remove() {
                if (current == null) {
                    throw new IllegalStateException("No current element");
                }
                
                getRegistry(current).deregister(pProvider);
                current = null;
            }
        };
    }

    /**
     * Gets the category registry for the given category.
     *
     * @param pCategory the category class
     * @return the {@code CategoryRegistry} for the given category
     */
    private <T> CategoryRegistry<T> getRegistry(final Class<T> pCategory) {
        @SuppressWarnings({"unchecked"})
        CategoryRegistry<T> registry = categoryMap.get(pCategory);
        if (registry == null) {
            throw new IllegalArgumentException("No such category: " + pCategory.getName());
        }
        return registry;
    }

    /**
     * Registers the given provider for all categories it matches.
     *
     * @param pProvider the provider instance
     * @return {@code true} if {@code pProvider} is now registered in
     *         one or more categories it was not registered in before.
     * @see #compatibleCategories(Object)
     */
    public boolean register(final Object pProvider) {
        Iterator<Class<?>> categories = compatibleCategories(pProvider);
        boolean registered = false;
        while (categories.hasNext()) {
            Class<?> category = categories.next();
            if (registerImpl(pProvider, category) && !registered) {
                registered = true;
            }
        }
        return registered;
    }

    private <T> boolean registerImpl(final Object pProvider, final Class<T> pCategory) {
        return getRegistry(pCategory).register(pCategory.cast(pProvider));
    }

    /**
     * Registers the given provider for the given category.
     *
     * @param pProvider the provider instance
     * @param pCategory the category class
     * @return {@code true} if {@code pProvider} is now registered in
     *         the given category
     */
    public <T> boolean register(final T pProvider, final Class<? super T> pCategory) {
        return registerImpl(pProvider, pCategory);
    }

    /**
     * De-registers the given provider from all categories it's currently
     * registered in.
     *
     * @param pProvider the provider instance
     * @return {@code true} if {@code pProvider} was previously registered in
     *         any category and is now de-registered.
     * @see #containingCategories(Object)
     */
    public boolean deregister(final Object pProvider) {
        Iterator<Class<?>> categories = containingCategories(pProvider);

        boolean deregistered = false;
        while (categories.hasNext()) {
            Class<?> category = categories.next();
            if (deregister(pProvider, category) && !deregistered) {
                deregistered = true;
            }
        }

        return deregistered;
    }

    /**
     * Deregisters the given provider from the given category.
     *
     * @param pProvider the provider instance
     * @param pCategory the category class
     * @return {@code true} if {@code pProvider} was previously registered in
     *         the given category
     */
    public boolean deregister(final Object pProvider, final Class<?> pCategory) {
        return getRegistry(pCategory).deregister(pProvider);
    }

    /**
     * Keeps track of each individual category.
     */
    class CategoryRegistry<T> {
        private final Class<T> category;
        private final Map<Class, T> providers = new LinkedHashMap<Class, T>();

        CategoryRegistry(Class<T> pCategory) {
            Validate.notNull(pCategory, "category");
            category = pCategory;
        }

        private void checkCategory(final Object pProvider) {
            if (!category.isInstance(pProvider)) {
                throw new IllegalArgumentException(pProvider + " not instance of category " + category.getName());
            }
        }

        public boolean register(final T pProvider) {
            checkCategory(pProvider);

            // NOTE: We only register the new instance, if we don't already have an instance of pProvider's class.
            if (!contains(pProvider)) {
                providers.put(pProvider.getClass(), pProvider);
                processRegistration(pProvider);
                return true;
            }

            return false;
        }

        void processRegistration(final T pProvider) {
            if (pProvider instanceof RegisterableService) {
                RegisterableService service = (RegisterableService) pProvider;
                service.onRegistration(ServiceRegistry.this, category);
            }
        }

        public boolean deregister(final Object pProvider) {
            checkCategory(pProvider);

            // NOTE: We remove any provider of the same class, this may or may
            // not be the same instance as pProvider.
            T oldProvider = providers.remove(pProvider.getClass());

            if (oldProvider != null) {
                processDeregistration(oldProvider);
                return true;
            }

            return false;
        }

        void processDeregistration(final T pOldProvider) {
            if (pOldProvider instanceof RegisterableService) {
                RegisterableService service = (RegisterableService) pOldProvider;
                service.onDeregistration(ServiceRegistry.this, category);
            }
        }

        public boolean contains(final Object pProvider) {
            return providers.containsKey(pProvider != null ? pProvider.getClass() : null);
        }

        public Iterator<T> providers() {
            // NOTE: The iterator must support removal because deregistering
            // using the deregister method will result in
            // ConcurrentModificationException in the iterator..
            // We wrap the iterator to track deregistration right.
            final Iterator<T> iterator = providers.values().iterator();
            return new Iterator<T>() {
                T current;

                public boolean hasNext() {
                    return iterator.hasNext();

                }

                public T next() {
                    return (current = iterator.next());
                }

                public void remove() {
                    iterator.remove();
                    processDeregistration(current);
                }
            };
        }
    }

    @SuppressWarnings({"UnnecessaryFullyQualifiedName"})
    public static void main(String[] pArgs) {
        abstract class Spi {}
        class One extends Spi {}
        class Two extends Spi {}

        ServiceRegistry testRegistry = new ServiceRegistry(
                Arrays.<Class<?>>asList(
                        java.nio.charset.spi.CharsetProvider.class,
                        java.nio.channels.spi.SelectorProvider.class,
                        javax.imageio.spi.ImageReaderSpi.class,
                        javax.imageio.spi.ImageWriterSpi.class,
                        Spi.class
                ).iterator()
        );

        testRegistry.registerApplicationClasspathSPIs();

        One one = new One();
        Two two = new Two();
        testRegistry.register(one, Spi.class);
        testRegistry.register(two, Spi.class);
        testRegistry.deregister(one);
        testRegistry.deregister(one, Spi.class);
        testRegistry.deregister(two, Spi.class);
        testRegistry.deregister(two);

        Iterator<Class<?>> categories = testRegistry.categories();
        System.out.println("Categories: ");
        while (categories.hasNext()) {
            Class<?> category = categories.next();
            System.out.println("  " + category.getName() + ":");

            Iterator<?> providers = testRegistry.providers(category);
            Object provider = null;
            while (providers.hasNext()) {
                provider = providers.next();
                System.out.println("    " + provider);
                if (provider instanceof javax.imageio.spi.ImageReaderWriterSpi) {
                    System.out.println("    - " + ((javax.imageio.spi.ImageReaderWriterSpi) provider).getDescription(null));
                }
                //                javax.imageio.spi.ImageReaderWriterSpi provider = (javax.imageio.spi.ImageReaderWriterSpi) providers.next();
                //                System.out.println("    " + provider);
                //                System.out.println("    " + provider.getVendorName());
                //                System.out.println("    Formats:");
                //
                //                System.out.print("      ");
                //                String[] formatNames = provider.getFormatNames();
                //                for (int i = 0; i < formatNames.length; i++) {
                //                    if (i != 0) {
                //                        System.out.print(", ");
                //                    }
                //                    System.out.print(formatNames[i]);
                //                }
                //                System.out.println();

                // Don't remove last one, it's removed later to exercise more code :-)
                if (providers.hasNext()) {
                    providers.remove();
                }
            }

            // Remove the last item from all categories
            if (provider != null) {
                Iterator containers = testRegistry.containingCategories(provider);
                int count = 0;
                while (containers.hasNext()) {
                    if (category == containers.next()) {
                        containers.remove();
                        count++;
                    }
                }

                if (count != 1) {
                    System.err.println("Removed " + provider + " from " + count + " categories");
                }
            }

            // Remove all using providers iterator
            providers = testRegistry.providers(category);
            if (!providers.hasNext()) {
                System.out.println("All providers successfully deregistered");
            }
            while (providers.hasNext()) {
                System.err.println("Not removed: " + providers.next());
            }
        }
    }

    //*/
}
