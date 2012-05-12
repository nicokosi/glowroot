/**
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.informantproject.test.plugin;

import org.informantproject.api.ContextMap;
import org.informantproject.api.Message;
import org.informantproject.api.Metric;
import org.informantproject.api.PluginServices;
import org.informantproject.api.Stopwatch;
import org.informantproject.api.Supplier;
import org.informantproject.api.weaving.Aspect;
import org.informantproject.api.weaving.InjectTraveler;
import org.informantproject.api.weaving.IsEnabled;
import org.informantproject.api.weaving.OnAfter;
import org.informantproject.api.weaving.OnBefore;
import org.informantproject.api.weaving.Pointcut;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
@Aspect
public class LevelOneAspect {

    private static final PluginServices pluginServices = PluginServices
            .get("org.informantproject:informant-integration-tests");

    @Pointcut(typeName = "org.informantproject.test.api.LevelOne", methodName = "call",
            methodArgs = { "java.lang.String", "java.lang.String" }, metricName = "level one")
    public static class LevelOneAdvice {

        private static final Metric metric = pluginServices.getMetric(LevelOneAdvice.class);

        @IsEnabled
        public static boolean isEnabled() {
            return pluginServices.isEnabled();
        }

        @OnBefore
        public static Stopwatch onBefore(final String arg1, final String arg2) {
            Supplier<Message> messageSupplier = new Supplier<Message>() {
                @Override
                public Message get() {
                    String traceDescription = pluginServices.getStringProperty(
                            "alternateDescription").or("Level One");
                    if (pluginServices.getBooleanProperty("starredDescription")) {
                        traceDescription += "*";
                    }
                    ContextMap context = ContextMap.of("arg1", arg1, "arg2", arg2);
                    ContextMap nestedContext = ContextMap.of("nestedkey11", arg1, "nestedkey12",
                            arg2, "subnested1",
                            ContextMap.of("subnestedkey1", arg1, "subnestedkey2", arg2));
                    context.put("nested1", nestedContext);
                    context.put("nested2", ContextMap.of("nestedkey21", arg1, "nestedkey22", arg2));
                    return Message.withContext(traceDescription, context);
                }
            };
            return pluginServices.startTrace(messageSupplier, metric);
        }

        @OnAfter
        public static void onAfter(@InjectTraveler Stopwatch stopwatch) {
            stopwatch.stop();
        }
    }
}
