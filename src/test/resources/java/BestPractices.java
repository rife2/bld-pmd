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

package java;/**
 * BestPractices class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class BestPractices {
    private final String ip = "127.0.0.1";     // not recommended
    private StringBuffer buffer;    // potential memory leak as an instance variable;
    private String[] x;

    void bar(int a) {
        switch (a) {
            case 1:  // do something
                break;
            default:  // the default case should be last, by convention
                break;
            case 2:
                break;
        }
    }

    public void foo(String[] param) {
        // Don't do this, make a copy of the array at least
        this.x = param;
    }

    private void greet(String name) {
        name = name.trim();
        System.out.println("Hello " + name);
    }


}
