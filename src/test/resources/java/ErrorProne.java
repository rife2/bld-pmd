/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java;

/**
 * ErrorProne class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class ErrorProne {
    static int x;

    public ErrorProne(int y) {
        x = y; // unsafe

        // unusual use of branching statement in a loop
        for (int i = 0; i < 10; i++) {
            if (i * i <= 25) {
                continue;
            }
            break;
        }
    }

    void bar() {
        try {
            // do something
        } catch (NullPointerException npe) {
        }
    }

    void foo() {
        try {
            // do something
        } catch (Throwable th) { // should not catch Throwable
            th.printStackTrace();
        }
    }
}
