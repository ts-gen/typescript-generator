package io.github.tsgen.typescript.generator.ext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.tsgen.typescript.generator.ClassMapping;
import io.github.tsgen.typescript.generator.Input;
import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TestUtils;
import io.github.tsgen.typescript.generator.TypeScriptFileType;
import io.github.tsgen.typescript.generator.TypeScriptGenerator;
import io.github.tsgen.typescript.generator.TypeScriptOutputKind;
import io.github.tsgen.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OnePossiblePropertyValueAssigningExtensionTest {
    private static final String BASE_PATH = "/ext/OnePossiblePropertyValueAssigningExtensionTest-";

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "discriminator")
    abstract static class BaseClass {

        @JsonProperty
        private Long field1;

        @JsonProperty
        private OneValueEnum field2;
    }

    static class SubClass extends BaseClass {

        @JsonProperty
        private String testField1;
    }

    static class OtherSubClass extends BaseClass {

        @JsonProperty
        private String testField2;

        @JsonProperty
        private OneValueEnum enumField1;

        @JsonProperty
        private TwoValueEnum enumField2;
    }

    enum OneValueEnum {
        MY_VALUE
    }

    enum TwoValueEnum {
        ONE,
        TWO
    }

    @Test
    public void testGeneration() {
        Settings settings = createBaseSettings(new OnePossiblePropertyValueAssigningExtension());
        String result = generateTypeScript(settings, SubClass.class, OtherSubClass.class);

        String expected = readResource("all.ts");

        Assertions.assertEquals(expected, result);
    }

    private static Settings createBaseSettings(OnePossiblePropertyValueAssigningExtension extension) {
        Settings settings = TestUtils.settings();
        settings.sortDeclarations = true;
        settings.extensions.add(extension);
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.mapClasses = ClassMapping.asClasses;
        return settings;
    }

    private static String generateTypeScript(Settings settings, Type... types) {
        TypeScriptGenerator typeScriptGenerator = new TypeScriptGenerator(settings);
        String result = typeScriptGenerator.generateTypeScript(Input.from(types));
        return Utils.normalizeLineEndings(result, "\n");
    }

    private String readResource(String suffix) {
        return Utils.readString(getClass().getResourceAsStream(BASE_PATH + suffix), "\n");
    }

}
