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

/**
 * Performance class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class Performance {
    public static void main(String[] as) {
        for (int i = 0; i < 10; i++) {
            Performance p = new Performance(); // Avoid this whenever you can it's really expensive
        }
    }


    private boolean checkTrimEmpty(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    void foo() {
        StringBuffer sb = new StringBuffer();
        sb.append("a");     // avoid this

        String foo = " ";
        StringBuffer buf = new StringBuffer();
        buf.append("Hello"); // poor
        buf.append(foo);
        buf.append("World");

        buf.append("Hello").append(" ").append("World");

        StringBuffer sbuf = new StringBuffer("tmp = " + System.getProperty("java.io.tmpdir"));
// poor
    }
}
