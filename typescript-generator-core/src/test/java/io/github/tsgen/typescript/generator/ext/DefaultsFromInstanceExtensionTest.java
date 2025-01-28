
package io.github.tsgen.typescript.generator.ext;

import io.github.tsgen.typescript.generator.ClassMapping;
import io.github.tsgen.typescript.generator.Input;
import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TestUtils;
import io.github.tsgen.typescript.generator.TypeScriptFileType;
import io.github.tsgen.typescript.generator.TypeScriptGenerator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DefaultsFromInstanceExtensionTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.quotes = "'";
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.extensions.add(new DefaultsFromInstanceExtension());
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.class));
        Assertions.assertTrue(output.contains("text0: string;"));
        Assertions.assertTrue(output.contains("text1: string = 'hello';"));
        Assertions.assertTrue(output.contains("number0: number;"));
        Assertions.assertTrue(output.contains("number1: number = 42;"));
        Assertions.assertTrue(output.contains("number2: number = 42;"));
        Assertions.assertTrue(output.contains("list: string[];"));
        Assertions.assertTrue(output.contains("text2: string = 'hello2';"));
    }

    public static class A {

        public String text0 = null;
        public String text1 = "hello";
        public Long number0 = null;
        public Long number1 = 42L;
        public long number2 = 42L;
        public List<String> list = Arrays.asList("a", "b");

        public String getText2() {
            return "hello2";
        }

    }

}
