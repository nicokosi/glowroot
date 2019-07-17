/*
 * Copyright 2018 the original author or authors.
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
package org.glowroot.agent.plugin.servlet;

import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindReturn;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.servlet._.ServletMessageSupplier;
import org.glowroot.agent.plugin.servlet._.ServletPluginProperties;

public class ServletInputStreamAspect {

    @Pointcut(className = "javax.servlet.ServletInputStream", methodName = "readLine",
            methodParameterTypes = {"byte[]", "int", "int"},
            nestingGroup = "servlet-read-request", timerName = "servlet read request")
    public static class ReadLineAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return ReadByteAdvice.isEnabled();
        }
        @OnReturn
        public static void onReturn(@BindReturn int numRead, ThreadContext context,
                @BindParameter byte /*@Nullable*/ [] bytes, @BindParameter int off) {
            ReadBytesOffAndLenAdvice.onReturn(numRead, context, bytes, off);
        }
    }

    @Pointcut(className = "java.io.InputStream",
            subTypeRestriction = "javax.servlet.ServletInputStream", methodName = "read",
            methodParameterTypes = {"byte[]", "int", "int"},
            nestingGroup = "servlet-read-request", timerName = "servlet read request")
    public static class ReadBytesOffAndLenAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return ReadByteAdvice.isEnabled();
        }
        @OnReturn
        public static void onReturn(@BindReturn int numRead, ThreadContext context,
                @BindParameter byte /*@Nullable*/ [] bytes, @BindParameter int off) {
            if (numRead == -1) {
                return;
            }
            if (bytes == null) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.appendRequestBodyBytes(bytes, off, numRead);
            }
        }
    }

    @Pointcut(className = "java.io.InputStream",
            subTypeRestriction = "javax.servlet.ServletInputStream", methodName = "read",
            methodParameterTypes = {"byte[]"}, nestingGroup = "servlet-read-request",
            timerName = "servlet read request")
    public static class ReadBytesAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return ReadByteAdvice.isEnabled();
        }
        @OnReturn
        public static void onReturn(@BindReturn int numRead, ThreadContext context,
                @BindParameter byte /*@Nullable*/ [] bytes) {
            if (numRead == -1) {
                return;
            }
            if (bytes == null) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.appendRequestBodyBytes(bytes, 0, numRead);
            }
        }
    }

    @Pointcut(className = "java.io.InputStream",
            subTypeRestriction = "javax.servlet.ServletInputStream", methodName = "read",
            methodParameterTypes = {}, nestingGroup = "servlet-read-request",
            timerName = "servlet read request")
    public static class ReadByteAdvice {
        @IsEnabled
        public static boolean isEnabled() {
            return ServletPluginProperties.captureRequestBodyNumBytes() != 0;
        }
        @OnReturn
        public static void onReturn(@BindReturn int b, ThreadContext context) {
            if (b == -1) {
                return;
            }
            ServletMessageSupplier messageSupplier =
                    (ServletMessageSupplier) context.getServletRequestInfo();
            if (messageSupplier != null) {
                messageSupplier.appendRequestBodyByte((byte) b);
            }
        }
    }
}
