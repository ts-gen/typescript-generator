
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;


public class TsPropertyDefinition {

    private final String propertyName;
    private final TsExpression expression;

    public TsPropertyDefinition(String propertyName, TsExpression expression) {
        this.propertyName = propertyName;
        this.expression = expression;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public TsExpression getExpression() {
        return expression;
    }

    public String format(Settings settings) {
        return Emitter.quoteIfNeeded(propertyName, settings) + ": " + expression.format(settings);
    }

}
