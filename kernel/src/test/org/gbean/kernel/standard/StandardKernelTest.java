/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.kernel.standard;

import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;
import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceState;
import org.gbean.kernel.StaticServiceFactory;
import org.gbean.kernel.StringServiceName;
import org.gbean.kernel.ServiceNotFoundException;
import org.gbean.kernel.StartStrategies;
import org.gbean.kernel.StopStrategies;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class StandardKernelTest extends TestCase {
    private static final Object SERVICE = new Object();
    private static final String KERNEL_NAME = "test";
    private final Kernel kernel = new StandardKernel(KERNEL_NAME);
    private final StringServiceName serviceName = new StringServiceName("Service");
//    private final StringServiceName ownedServiceName = new StringServiceName("OwnedService");
    private final MockServiceFactory serviceFactory = new MockServiceFactory();
    private final ClassLoader classLoader = new URLClassLoader(new URL[0]);

    public void testInitialState() throws Exception {
        assertEquals(KERNEL_NAME, kernel.getKernelName());
        assertTrue(kernel.isRunning());

        assertFalse(kernel.isRegistered(serviceName));
        try {
            kernel.getServiceFactory(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getClassLoaderFor(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getServiceState(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getService(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getServiceStartTime(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.isServiceEnabled(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startService(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startService(serviceName, StartStrategies.ASYNCHRONOUS);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startServiceRecursive(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startServiceRecursive(serviceName, StartStrategies.ASYNCHRONOUS);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.stopService(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.stopService(serviceName, StopStrategies.ASYNCHRONOUS);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }

        kernel.destroy();
        assertEquals(KERNEL_NAME, kernel.getKernelName());
        assertFalse(kernel.isRunning());

        assertFalse(kernel.isRegistered(serviceName));
        try {
            kernel.getServiceFactory(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getClassLoaderFor(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getServiceState(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getService(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.getServiceStartTime(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.isServiceEnabled(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startService(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startService(serviceName, StartStrategies.ASYNCHRONOUS);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startServiceRecursive(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.startServiceRecursive(serviceName, StartStrategies.ASYNCHRONOUS);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.stopService(serviceName);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
        try {
            kernel.stopService(serviceName, StopStrategies.ASYNCHRONOUS);
            fail("expected exception");
        } catch (ServiceNotFoundException e) {
            // expected
            assertSame(serviceName, e.getServiceName());
        }
    }

    public void testSimpleLifecycle() throws Exception {
        kernel.registerService(serviceName, serviceFactory, classLoader);
        assertTrue(kernel.isRegistered(serviceName));
        assertSame(serviceFactory, kernel.getServiceFactory(serviceName));
        assertSame(classLoader, kernel.getClassLoaderFor(serviceName));
        assertSame(ServiceState.STOPPED, kernel.getServiceState(serviceName));
        assertNull(kernel.getService(serviceName));
        assertEquals(0, kernel.getServiceStartTime(serviceName));
        assertTrue(kernel.isServiceEnabled(serviceName));
    }

    private static class MockServiceFactory extends StaticServiceFactory {
        private boolean restartable = true;

        public MockServiceFactory() throws NullPointerException {
            super(SERVICE);
        }

        public boolean isRestartable() {
            return restartable;
        }
    }
}
