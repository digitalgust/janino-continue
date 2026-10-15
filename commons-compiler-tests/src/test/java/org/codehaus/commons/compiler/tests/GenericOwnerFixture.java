/*
 * Janino - An embedded Java[TM] compiler
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

// SUPPRESS CHECKSTYLE JavadocMethod|MethodName:9999

package org.codehaus.commons.compiler.tests;

/** Compiled by javac; exercises both ReflectionIClass and native ClassFileIClass signature readers. */
public class GenericOwnerFixture {
    public static class Outer<T> {
        public T value;
        public Outer(T value) { this.value = value; }
        public class Inner<U extends T> {
            public U inner;
            public Inner(U value) { this.inner = value; }
            public T outer() { return Outer.this.value; }
            public U get() { return this.inner; }
        }
    }
    public Outer<Number>.Inner<Integer> value = create();
    public static Outer<Number>.Inner<Integer> create() {
        Outer<Number> outer = new Outer<Number>(8);
        return outer.new Inner<Integer>(3);
    }
    public static class N extends Number implements Runnable {
        public int intValue() { return 2; }
        public long longValue() { return 2; }
        public float floatValue() { return 2; }
        public double doubleValue() { return 2; }
        public void run() {}
    }
    public static class Bounded<T extends Number & Runnable> {
        private final T value;
        public Bounded(T value) { this.value = value; }
        public T get() { return this.value; }
    }
    public static Bounded<?> bounded() { return new Bounded<N>(new N()); }
}
