
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;


public enum TsUnaryOperator implements Emittable {

    Exclamation("!");

    private final String formatted;

    private TsUnaryOperator(String formatted) {
        this.formatted = formatted;
    }

    @Override
    public String format(Settings settings) {
        return formatted;
    }

}
