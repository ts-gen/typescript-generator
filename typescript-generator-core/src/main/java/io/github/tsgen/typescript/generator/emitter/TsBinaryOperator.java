
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;


public enum TsBinaryOperator implements Emittable {
    
    BarBar("||");

    private final String formatted;

    private TsBinaryOperator(String formatted) {
        this.formatted = formatted;
    }

    @Override
    public String format(Settings settings) {
        return formatted;
    }

}
