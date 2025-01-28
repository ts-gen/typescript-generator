
package io.github.tsgen.typescript.generator.emitter;


public enum TsAccessibilityModifier {

    Public, Private, Protected;

    public String format() {
        return name().toLowerCase();
    }

}
