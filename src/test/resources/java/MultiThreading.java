/*
 *  Copyright 2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java;

import java.text.SimpleDateFormat;

/**
 * MultiThreading class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class MultiThreading {
    private static final SimpleDateFormat sdf = new SimpleDateFormat();
    /*volatile */ Object baz = null; // fix for Java5 and later: volatile
    private volatile String var1; // not suggested

    void bar() {
        sdf.format("bar"); // poor, no thread-safety
    }

    Object obj() {
        if (baz == null) { // baz may be non-null yet not fully created
            synchronized (this) {
                if (baz == null) {
                    baz = new Object();
                }
            }
        }
        return baz;
    }
}
