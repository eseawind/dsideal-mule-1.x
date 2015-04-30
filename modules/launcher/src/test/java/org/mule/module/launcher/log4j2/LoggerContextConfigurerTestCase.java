/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.ClassUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.ConfigurationMonitor;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.FileConfigurationMonitor;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LoggerContextConfigurerTestCase extends AbstractMuleTestCase
{

    private static final String CURRENT_DIRECTORY = ".";
    private static final String INTERVAL_PROPERTY = "interval";
    private static final String SHUTDOWN_HOOK_PROPERTY = "isShutdownHookEnabled";
    private static final int MONITOR_INTERVAL = 60000;
    private static final String CONVERTER_COMPONENT = "Converter";

    private LoggerContextConfigurer contextConfigurer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleLoggerContext context;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS, extraInterfaces = {Reconfigurable.class})
    private DefaultConfiguration configuration;

    private Object converter;

    @Before
    public void before()
    {
        contextConfigurer = new LoggerContextConfigurer();
        when(context.isStandlone()).thenReturn(true);
        when(context.getConfiguration()).thenReturn(configuration);

        converter = null;

        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                converter = invocation.getArguments()[1];
                return null;
            }
        }).when(configuration).addComponent(eq("Converter"), anyObject());

        when(configuration.getComponent(CONVERTER_COMPONENT)).thenAnswer(
                new Answer<Object>()
                {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable
                    {
                        return converter;
                    }
                }
        );
    }

    @Test
    public void disableShutdownHook() throws Exception
    {
        contextConfigurer.configure(context);
        assertThat((boolean) ClassUtils.getFieldValue(context.getConfiguration(), SHUTDOWN_HOOK_PROPERTY, true), is(false));
    }

    @Test
    public void configurationMonitor() throws Exception
    {
        when(context.getConfigFile()).thenReturn(new File(CURRENT_DIRECTORY).toURI());
        contextConfigurer.configure(context);
        ArgumentCaptor<ConfigurationMonitor> captor = ArgumentCaptor.forClass(ConfigurationMonitor.class);
        verify(configuration).setConfigurationMonitor(captor.capture());

        assertThat(captor.getValue() instanceof FileConfigurationMonitor, is(true));
        FileConfigurationMonitor monitor = (FileConfigurationMonitor) captor.getValue();
        assertThat(MONITOR_INTERVAL, equalTo(ClassUtils.getFieldValue(monitor, INTERVAL_PROPERTY, true)));
    }

    @Test
    public void forceConsoleLog()
    {
        withForceConsoleLog(new Runnable()
        {
            @Override
            public void run()
            {
                contextConfigurer.configure(context);
                ArgumentCaptor<ConsoleAppender> appenderCaptor = ArgumentCaptor.forClass(ConsoleAppender.class);
                verify(context.getConfiguration()).addAppender(appenderCaptor.capture());

                Appender forcedConsoleAppender = appenderCaptor.getValue();

                assertThat(forcedConsoleAppender, notNullValue());
                assertThat(forcedConsoleAppender.getName(), equalTo("Forced-Console"));
                assertThat(forcedConsoleAppender.isStarted(), is(true));

                LoggerConfig rootLogger = ((AbstractConfiguration) context.getConfiguration()).getRootLogger();
                verify(rootLogger).addAppender(forcedConsoleAppender, Level.INFO, null);
            }
        });
    }

    @Test
    public void forceConsoleLogWithAppenderAlreadyPresent()
    {
        withForceConsoleLog(new Runnable()
        {
            @Override
            public void run()
            {
                LoggerConfig rootLogger = ((AbstractConfiguration) context.getConfiguration()).getRootLogger();
                Collection<Appender> appenders = new ArrayList<>();
                appenders.add(ConsoleAppender.createAppender(mock(Layout.class), null, null, "Console", null, null));
                when(rootLogger.getAppenders().values()).thenReturn(appenders);

                contextConfigurer.configure(context);
                verify(context.getConfiguration(), never()).addAppender(any(ConsoleAppender.class));
                verify(rootLogger, never()).addAppender(any(ConsoleAppender.class), same(Level.INFO), any(Filter.class));
            }
        });
    }

    private void withForceConsoleLog(Runnable assertion)
    {
        System.setProperty(MuleProperties.MULE_FORCE_CONSOLE_LOG, "");
        try
        {
            assertion.run();
        }
        finally
        {
            System.clearProperty(MuleProperties.MULE_FORCE_CONSOLE_LOG);
        }
    }
}
