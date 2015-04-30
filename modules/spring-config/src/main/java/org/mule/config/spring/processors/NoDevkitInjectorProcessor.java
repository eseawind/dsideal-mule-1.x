/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.util.ClassUtils;

import java.beans.PropertyDescriptor;

import org.springframework.beans.PropertyValues;

/**
 * A {@link SelectiveInjectorProcessor} used to keep
 * mule from injecting dependencies into components
 * built with Devkit, by skipping injection of objects
 * which are instances of {@link org.mule.api.devkit.ProcessAdapter}
 *
 * @since 3.7.0
 */
public final class NoDevkitInjectorProcessor extends SelectiveInjectorProcessor
{

    private static final String PROCESS_ADAPTER_CLASS_NAME = "org.mule.api.devkit.ProcessAdapter";

    private Class<?> exclusionClass;

    public NoDevkitInjectorProcessor()
    {
        try
        {
            // fetch class by name to avoid circular dependency between spring module
            // and devkit-support
            exclusionClass = ClassUtils.loadClass(PROCESS_ADAPTER_CLASS_NAME, getClass());
        }
        catch (ClassNotFoundException e)
        {
            exclusionClass = null;
        }
    }

    @Override
    protected boolean shouldInject(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName)
    {
        if (exclusionClass != null)
        {
            return !exclusionClass.isInstance(bean);
        }

        return true;
    }
}
