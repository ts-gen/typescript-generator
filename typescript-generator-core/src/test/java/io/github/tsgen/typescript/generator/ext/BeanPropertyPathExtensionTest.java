package io.github.tsgen.typescript.generator.ext;

import io.github.tsgen.typescript.generator.DefaultTypeProcessor;
import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TypeProcessor;
import io.github.tsgen.typescript.generator.compiler.ModelCompiler;
import io.github.tsgen.typescript.generator.emitter.EmitterExtension;
import io.github.tsgen.typescript.generator.emitter.TsModel;
import io.github.tsgen.typescript.generator.parser.Jackson2Parser;
import io.github.tsgen.typescript.generator.parser.Model;
import io.github.tsgen.typescript.generator.util.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BeanPropertyPathExtensionTest {

    static class ClassA {
        public String field1;
        public ClassB field2;
        public ClassC field3;
    }

    static class ClassB {
        public int field1;
    }

    static class ClassC extends ClassB {
        public int field4;
    }

    @Test
    public void basicTest() throws Exception {
        final StringBuilder data = new StringBuilder();
        final EmitterExtension.Writer writer = new EmitterExtension.Writer() {
            @Override
            public void writeIndentedLine(String line) {
                data.append(line + "\n");
            }
        };
        final Settings settings = new Settings();
        settings.sortDeclarations = true;
        final TypeProcessor typeProcessor = new DefaultTypeProcessor();
        final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(ClassA.class);
        final TsModel tsModel = new ModelCompiler(settings, typeProcessor).javaToTypeScript(model);
        new BeanPropertyPathExtension().emitElements(writer, settings, false, tsModel);
        String dataStr = data.toString();
        final String expected = Utils.readString(getClass().getResourceAsStream("/ext/expected.ts"), "\n");
        Assertions.assertEquals(expected.trim(), dataStr.trim());
    }
}
