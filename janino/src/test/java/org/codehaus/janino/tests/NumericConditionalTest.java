package org.codehaus.janino.tests;

import org.codehaus.janino.SimpleCompiler;
import org.junit.Assert;
import org.junit.Test;

/** Numeric conditional narrowing must be a non-diagnostic representability check. */
public class NumericConditionalTest {
    private void check(String type, String value, String constant, Class<?> expected) throws Exception {
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.setSourceVersion(8);
        compiler.setTargetVersion(8);
        compiler.cook("public class Case {"
            + "public static Object left(boolean b){" + type + " x=" + value + ";return b?" + constant + ":x;}"
            + "public static Object right(boolean b){" + type + " x=" + value + ";return b?x:" + constant + ";}}");
        Class<?> result = compiler.getClassLoader().loadClass("Case");
        for (String method : new String[] {"left", "right"}) {
            for (boolean condition : new boolean[] {false, true}) {
                Object actual = result.getMethod(method, boolean.class).invoke(null, condition);
                Assert.assertEquals(type + " / " + constant, expected, actual.getClass());
                boolean constantBranch = method.equals("left") == condition;
                Object expectedValue = new org.codehaus.janino.ExpressionEvaluator(
                    constantBranch ? constant : value, double.class, new String[0], new Class<?>[0]
                ).evaluate(new Object[0]);
                double actualValue = actual instanceof Character
                    ? ((Character) actual).charValue() : ((Number) actual).doubleValue();
                Assert.assertEquals(((Number) expectedValue).doubleValue(), actualValue, 0.0);
            }
        }
    }

    @Test public void outOfRangeIntConstantsUseIntPromotion() throws Exception {
        check("char", "'A'", "-1", Integer.class);
        check("char", "'A'", "65536", Integer.class);
        check("byte", "(byte)7", "128", Integer.class);
        check("byte", "(byte)7", "-129", Integer.class);
        check("short", "(short)7", "32768", Integer.class);
        check("short", "(short)7", "-32769", Integer.class);
    }

    @Test public void representableIntConstantsRetainNarrowType() throws Exception {
        check("char", "'A'", "0", Character.class);
        check("char", "'A'", "65535", Character.class);
        check("byte", "(byte)7", "-128", Byte.class);
        check("byte", "(byte)7", "127", Byte.class);
        check("short", "(short)7", "-32768", Short.class);
        check("short", "(short)7", "32767", Short.class);
    }

    @Test public void boxedOperandsFollowNumericConditionalRules() throws Exception {
        check("Character", "Character.valueOf('A')", "-1", Integer.class);
        check("Character", "Character.valueOf('A')", "65535", Character.class);
        check("Byte", "Byte.valueOf((byte)7)", "127", Byte.class);
        check("Short", "Short.valueOf((short)7)", "32767", Short.class);
    }

    @Test public void otherNumericConstantsUseBinaryNumericPromotion() throws Exception {
        check("char", "'A'", "1L", Long.class);
        check("byte", "(byte)7", "1.0f", Float.class);
        check("short", "(short)7", "1.0d", Double.class);
    }
}
