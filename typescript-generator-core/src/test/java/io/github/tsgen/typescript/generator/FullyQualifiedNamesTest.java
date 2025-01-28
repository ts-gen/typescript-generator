
package io.github.tsgen.typescript.generator;

import io.github.tsgen.typescript.generator.p2.D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
public class FullyQualifiedNamesTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.mapPackagesToNamespaces = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(D.class));
        final String expected = ""
                + "namespace io.github.tsgen.typescript.generator.p2 {\n"
                + "\n"
                + "    export class D {\n"
                + "        a: io.github.tsgen.typescript.generator.p1.A;\n"
                + "        b: io.github.tsgen.typescript.generator.p2.B;\n"
                + "        c: io.github.tsgen.typescript.generator.p1.C;\n"
                + "        e: io.github.tsgen.typescript.generator.p1.E;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace io.github.tsgen.typescript.generator.p1 {\n"
                + "\n"
                + "    export class A {\n"
                + "        sa: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace io.github.tsgen.typescript.generator.p2 {\n"
                + "\n"
                + "    export class B extends io.github.tsgen.typescript.generator.p1.A {\n"
                + "        sb: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace io.github.tsgen.typescript.generator.p1 {\n"
                + "\n"
                + "    export class C extends io.github.tsgen.typescript.generator.p2.B {\n"
                + "        sc: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace io.github.tsgen.typescript.generator.p1 {\n"
                + "\n"
                + "    export type E = \"Left\" | \"Right\";\n"
                + "\n"
                + "}";
        Assertions.assertEquals(expected.trim(), output.trim());
    }

    @Test
    public void testNested() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.mapPackagesToNamespaces = true;
        settings.sortTypeDeclarations = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Outer.Inner.class, Outer.class));
        final String expected = ""
                + "namespace io.github.tsgen.typescript.generator.FullyQualifiedNamesTest {\n"
                + "\n"
                + "    export class Outer {\n"
                + "        outer: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace io.github.tsgen.typescript.generator.FullyQualifiedNamesTest.Outer {\n"
                + "\n"
                + "    export class Inner {\n"
                + "        inner: string;\n"
                + "    }\n"
                + "\n"
                + "}\n";
        Assertions.assertEquals(expected.trim(), output.trim());
    }

    private static class Outer {
        public String outer;
        private static class Inner {
            public String inner;
        }
    }

}
