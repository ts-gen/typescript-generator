
package io.github.tsgen.typescript.generator;

import io.github.tsgen.typescript.generator.util.DeprecationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DeprecationUtilsTest {

    @Deprecated
    private String a;

    @Deprecated(since = "2.28")
    private String b;

    @Deprecated(forRemoval = true)
    private String c;

    @Deprecated(since = "2.28", forRemoval = true)
    private String d;

    @Test
    public void test() {
        Assertions.assertEquals("@deprecated", getDeprecationText("a"));
        Assertions.assertEquals("@deprecated since 2.28", getDeprecationText("b"));
        Assertions.assertEquals("@deprecated for removal", getDeprecationText("c"));
        Assertions.assertEquals("@deprecated since 2.28, for removal", getDeprecationText("d"));
    }

    private static String getDeprecationText(String fieldName) {
        try {
            final Deprecated deprecated = DeprecationUtilsTest.class.getDeclaredField(fieldName).getAnnotation(Deprecated.class);
            return DeprecationUtils.convertToComment(deprecated);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
