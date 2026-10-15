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

package org.codehaus.janino;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A type with type VARIABLES, e.g. "{@code E}" or "{@code List<K>}" — the declared signature of a field or method
 * of a generic class, BEFORE the type variables are substituted with actual type arguments. Used by JANINO's
 * "generics support": The declared signature is loaded from the "Signature" attribute (or reflection), and then the
 * type variables are replaced with the actual type arguments of the receiver, e.g. for
 * "{@code List<String> l = ...; l.get(i);}", "{@code E get(int)}" becomes "{@code String}".
 * <p>
 *   Opposed to the runtime type system ({@link IClass} and friends), this structure contains NO erased
 *   representation; unresolvable cases are represented by {@link #ERASED}.
 * </p>
 */
public abstract
class GenericType {

    private GenericType() {}

    /** A type without type variables. */
    public static final
    class Raw extends GenericType {

        /** The (erased) class. */
        public final IClass iClass;

        public Raw(IClass iClass) { this.iClass = iClass; }

        @Override public String toString() { return this.iClass.toString(); }
    }

    /** A reference to a type variable of the declaring class (or of the method). */
    public static final
    class TypeVariable extends GenericType {

        /** The simple name of the type variable, e.g. "{@code E}". */
        public final String name;

        public TypeVariable(String name) { this.name = name; }

        @Override public String toString() { return this.name; }
    }

    /** An array type whose component type may contain type variables. */
    public static final
    class ArrayOf extends GenericType {

        /** The component type. */
        public final GenericType componentType;

        public ArrayOf(GenericType componentType) { this.componentType = componentType; }

        @Override public String toString() { return this.componentType + "[]"; }
    }

    /** A wildcard whose bound must survive until a functional target is instantiated. */
    public static final
    class Wildcard extends GenericType {
        public final boolean upperBound;
        public final GenericType bound;

        public Wildcard(boolean upperBound, GenericType bound) {
            this.upperBound = upperBound;
            this.bound = bound;
        }

        @Override public String toString() { return "? " + (this.upperBound ? "extends " : "super ") + this.bound; }
    }

    /** A parameterized type whose type arguments may contain type variables, e.g. "{@code Map<K, V>}". */
    public static final
    class Parameterized extends GenericType {

        /** The raw type. */
        public final IClass rawType;

        /** The type arguments. */
        public final GenericType[] typeArguments;

        @Nullable public final GenericType ownerType;

        public Parameterized(IClass rawType, GenericType[] typeArguments) { this(rawType, typeArguments, null); }

        public Parameterized(IClass rawType, GenericType[] typeArguments, @Nullable GenericType ownerType) {
            this.rawType       = rawType;
            this.typeArguments = typeArguments;
            this.ownerType     = ownerType;
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder(this.rawType.toString()).append('<');
            for (int i = 0; i < this.typeArguments.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(this.typeArguments[i]);
            }
            return sb.append('>').toString();
        }
    }

    /**
     * Special value indicating that the generic signature could not be represented (e.g. a malformed signature or an
     * unsupported construct); consumers fall back to the erased type.
     */
    public static final GenericType ERASED = new GenericType() {
        @Override public String toString() { return "(erased)"; }
    };
}
