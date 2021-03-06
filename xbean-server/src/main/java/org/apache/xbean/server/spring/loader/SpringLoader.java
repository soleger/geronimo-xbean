/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.server.spring.loader;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.xbean.kernel.Kernel;
import org.apache.xbean.kernel.ServiceFactory;
import org.apache.xbean.kernel.ServiceName;
import org.apache.xbean.kernel.StringServiceName;
import org.apache.xbean.server.loader.Loader;
import org.apache.xbean.server.spring.configuration.SpringConfigurationServiceFactory;
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext;
import org.apache.xbean.spring.context.SpringApplicationContext;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;


/**
 * SpringLoader loads Spring xml configurations into a Kernel.  This service uses the XBean version of
 * FileSystemXmlApplicationContext so custom XML extensions are available.  This loader also support the specification
 * of SpringXmlPreprocessors and BeanFactoryPostProcessors to apply to the configuration.
 *
 * @org.apache.xbean.XBean namespace="http://xbean.apache.org/schemas/server" element="spring-loader"
 *     description="Loads Spring xml configurations into a Kernel"
 *
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class SpringLoader implements Loader {
    private Kernel kernel;
    private File baseDir = new File(".").getAbsoluteFile();
    private List beanFactoryPostProcessors = Collections.EMPTY_LIST;
    private List xmlPreprocessors = Collections.EMPTY_LIST;

    /**
     * Creates an empty SpringLoader.  Note this loader is not usable until a kernel is specified.
     */
    public SpringLoader() {
    }

    /**
     * {@inheritDoc}
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Sets the kernel in which configurations are loaded.
     * @param kernel the kernel in which configurations are loaded
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * Gets the BeanFactoryPostProcessors to apply to the configuration.
     * @return the BeanFactoryPostProcessors to apply to the configuration
     */
    public List getBeanFactoryPostProcessors() {
        return beanFactoryPostProcessors;
    }

    /**
     * Sets the BeanFactoryPostProcessors to apply to the configuration.
     * @param beanFactoryPostProcessors the BeanFactoryPostProcessors to apply to the configuration
     */
    public void setBeanFactoryPostProcessors(List beanFactoryPostProcessors) {
        this.beanFactoryPostProcessors = beanFactoryPostProcessors;
    }

    /**
     * Gets the base directory from which configuration locations are resolved.
     * @return the base directory from which configuration locations are resolved
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * Sets the base directory from which configuration locations are resolved.
     * @param baseDir the base directory from which configuration locations are resolved
     */
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Gets the SpringXmlPreprocessors applied to the configuration.
     * @return the SpringXmlPreprocessors applied to the configuration
     */
    public List getXmlPreprocessors() {
        return xmlPreprocessors;
    }

    /**
     * Sets the SpringXmlPreprocessors applied to the configuration.
     * @param xmlPreprocessors the SpringXmlPreprocessors applied to the configuration
     */
    public void setXmlPreprocessors(List xmlPreprocessors) {
        this.xmlPreprocessors = xmlPreprocessors;
    }

    /**
     * Loads the specified configuration into the kernel.  The location specifies a file relative to the baseDir using
     * baseDir.toURI().resolve(location).getPath() + ".xml".  This service uses the XBean version of
     * FileSystemXmlApplicationContext so custom XML extensions are available.
     * @param location the location of the configuration file relative to the base directory without the .xml extension
     * @return the name of the SpringConfiguration service for this location
     * @throws Exception if a problem occurs while loading the Spring configuration file
     */
    public ServiceName load(String location) throws Exception {
        String resolvedLocation = baseDir.toURI().resolve(location).getPath();
        String configLocation = "/" + resolvedLocation + ".xml";
        SpringApplicationContext applicationContext = new FileSystemXmlApplicationContext(
                new String[] {configLocation},
                false,
                xmlPreprocessors);

        for (Iterator iter = beanFactoryPostProcessors.iterator(); iter.hasNext();) {
            BeanFactoryPostProcessor processor = (BeanFactoryPostProcessor) iter.next();
            applicationContext.addBeanFactoryPostProcessor(processor);
        }
        applicationContext.setDisplayName(location);

        ClassLoader classLoader = applicationContext.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = SpringLoader.class.getClassLoader();
        }

        ServiceName serviceName = new StringServiceName("configuration:" + location);
        ServiceFactory springConfigurationServiceFactory = new SpringConfigurationServiceFactory(applicationContext);
        kernel.registerService(serviceName, springConfigurationServiceFactory);
        return serviceName;
    }
}
