
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TsType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class TsCallExpression extends TsExpression {

    private final TsExpression expression;
    private final List<TsType> typeArguments;
    private final List<TsExpression> arguments;

    public TsCallExpression(TsExpression expression, TsExpression... arguments) {
        this(expression, null, Arrays.asList(arguments));
    }

    public TsCallExpression(TsExpression expression, List<TsType> typeArguments, List<TsExpression> arguments) {
        this.expression = expression;
        this.typeArguments = typeArguments != null ? typeArguments : Collections.<TsType>emptyList();
        this.arguments = arguments;
    }

    public TsExpression getExpression() {
        return expression;
    }

    public List<TsType> getTypeArguments() {
        return typeArguments;
    }

    public List<TsExpression> getArguments() {
        return arguments;
    }

    @Override
    public String format(Settings settings) {
        final String typeArgumentsString = typeArguments.isEmpty() ? "" : "<" + Emitter.formatList(settings, typeArguments) + ">";
        return expression.format(settings) + typeArgumentsString + "(" + Emitter.formatList(settings, arguments) + ")";
    }

}
