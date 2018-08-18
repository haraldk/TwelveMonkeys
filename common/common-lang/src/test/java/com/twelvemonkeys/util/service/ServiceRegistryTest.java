/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util.service;

import com.twelvemonkeys.util.CollectionUtil;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * ServiceRegistryTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ServiceRegistryTest.java,v 1.0 25.01.12 16:16 haraldk Exp$
 */
public class ServiceRegistryTest {

    private final TestRegistry registry = new TestRegistry();

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        new ServiceRegistry(null);
    }

    @Test
    public void testCreateEmptyIterator() {
        // A completely useless registry...
        ServiceRegistry registry = new ServiceRegistry(Collections.<Class<?>>emptyList().iterator());
        registry.registerApplicationClasspathSPIs();

        while (registry.categories().hasNext()) {
            fail("No categories");
        }
    }

    @Test(expected = ServiceConfigurationError.class)
    public void testCreateBadConfig() {
        @SuppressWarnings("unchecked") 
        ServiceRegistry registry = new ServiceRegistry(Arrays.asList(BadSPI.class).iterator());
        registry.registerApplicationClasspathSPIs();

        // DONE: Test non-class

        // TODO: Test class not implementing SPI category
        // TODO: Test class that throws exception in constructor
        // TODO: Test class that has no public no-args constructor
        // TODO: Test IOException
        // Some of these can be tested using stubs, via the package protected registerSPIs method
    }

    @Test
    public void testCategories() {
        // Categories
        Iterator<Class<?>> categories = registry.categories();
        assertTrue(categories.hasNext());
        Class<?> category = categories.next();
        assertEquals(DummySPI.class, category);
        assertFalse(categories.hasNext());
    }

    @Test
    public void testProviders() {
        // Providers
        Iterator<DummySPI> providers = registry.providers(DummySPI.class);
        List<DummySPI> providerList = new ArrayList<DummySPI>();
        CollectionUtil.addAll(providerList, providers);

        assertEquals(2, providerList.size());

        // Order should be as in configuration file
        assertNotNull(providerList.get(0));
        assertEquals(DummySPIImpl.class, providerList.get(0).getClass());
        assertNotNull(providerList.get(1));
        assertEquals(DummySPIToo.class, providerList.get(1).getClass());
    }

    @Test
    public void testCompatibleCategoriesNull() {
        // Compatible categories
        Iterator<Class<?>> categories = registry.compatibleCategories(null);
        assertFalse(categories.hasNext());
    }

    @Test
    public void testCompatibleCategoriesImpl() {
        Iterator<Class<?>> categories = registry.compatibleCategories(new DummySPIImpl());
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testCompatibleCategoriesToo() {
        Iterator<Class<?>> categories = registry.compatibleCategories(new DummySPIToo());
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testCompatibleCategoriesNonRegistered() {
        Iterator<Class<?>> categories = registry.compatibleCategories(new DummySPI() {});
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testCompatibleCategoriesUnknownType() {
        Iterator<Class<?>> categories = registry.compatibleCategories(new Object());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testContainingCategoriesNull() {
        // Containing categories
        Iterator<Class<?>> categories = registry.containingCategories(null);
        assertFalse(categories.hasNext());
    }

    @Test
    public void testContainingCategoriesKnownInstanceImpl() {
        Iterator<DummySPI> providers = registry.providers(DummySPI.class);
        assertTrue(providers.hasNext()); // Sanity check

        Iterator<Class<?>> categories = registry.containingCategories(providers.next());
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testContainingCategoriesKnownInstanceToo() {
        Iterator<DummySPI> providers = registry.providers(DummySPI.class);
        providers.next();
        assertTrue(providers.hasNext()); // Sanity check

        Iterator<Class<?>> categories = registry.containingCategories(providers.next());
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testContainingCategoriesNewInstanceRegisteredImpl() {
        // NOTE: Currently we match based on type, rather than instance, but it does make sense...
        Iterator<Class<?>> categories = registry.containingCategories(new DummySPIImpl());
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testContainingCategoriesNewInstanceRegisteredToo() {
        // NOTE: Currently we match based on type, rather than instance, but it does make sense...
        Iterator<Class<?>> categories = registry.containingCategories(new DummySPIToo());
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testContainingCategoriesCompatibleNonRegisteredType() {
        Iterator<Class<?>> categories = registry.containingCategories(new DummySPI() {});
        assertFalse(categories.hasNext());
    }

    @Test
    public void testContainingCategoriesUnknownType() {
        Iterator<Class<?>> categories = registry.containingCategories(new Object());
        assertFalse(categories.hasNext());
    }

    @Test
    public void testRegister() {
        // Register
        DummySPI dummy = new DummySPI() {};
        assertTrue(registry.register(dummy));

        // Should now have category
        Iterator<Class<?>> categories = registry.containingCategories(dummy);
        assertTrue(categories.hasNext());
        assertEquals(DummySPI.class, categories.next());
        assertFalse(categories.hasNext());

        // Should now be in providers
        Iterator<DummySPI> providers = registry.providers(DummySPI.class);
        List<DummySPI> providerList = new ArrayList<DummySPI>();
        CollectionUtil.addAll(providerList, providers);

        assertEquals(3, providerList.size());

        assertNotNull(providerList.get(1));
        assertSame(dummy, providerList.get(2));
    }

    @Test
    public void testRegisterAlreadyRegistered() {
        Iterator<DummySPI> providers = registry.providers(DummySPI.class);
        assertTrue(providers.hasNext()); // Sanity check

        assertFalse(registry.register(providers.next()));
    }

    @Test
    public void testRegisterNull() {
        assertFalse(registry.register(null));
    }

    @Test
    public void testRegisterIncompatible() {
        assertFalse(registry.register(new Object()));
    }
    
    @Test
    public void testDeregisterNull() {
        assertFalse(registry.deregister(null));
    }

    @Test
    public void testDeregisterIncompatible() {
        assertFalse(registry.deregister(new Object()));
    }

    @Test
    public void testDeregisterCompatibleNonRegistered() {
        DummySPI dummy = new DummySPI() {};
        assertFalse(registry.deregister(dummy));
    }

    @Test
    public void testDeregister() {
        Iterator<DummySPI> providers = registry.providers(DummySPI.class);
        assertTrue(providers.hasNext()); // Sanity check
        DummySPI instance = providers.next();
        assertTrue(registry.deregister(instance));

        // Test no longer in registry
        providers = registry.providers(DummySPI.class);
        int count = 0;
        while (providers.hasNext()) {
            DummySPI next = providers.next();
            assertNotSame(instance, next);
            count++;
        }

        assertEquals(1, count);
    }

    // TODO: Test register with category
    // TODO: Test register with unknown category
    // TODO: Test register with null category

    // TODO: Test de-register with category
    // TODO: Test de-register with unknown category
    // TODO: Test de-register with null category


    private static class TestRegistry extends ServiceRegistry {
        @SuppressWarnings("unchecked")
        public TestRegistry() {
            super(Arrays.asList(DummySPI.class).iterator());
            registerApplicationClasspathSPIs();
        }
    }
    
    public static class BadSPI {}
}
