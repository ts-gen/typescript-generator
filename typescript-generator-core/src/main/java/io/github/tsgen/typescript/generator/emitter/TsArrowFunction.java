
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TsParameter;
import java.util.List;


public class TsArrowFunction extends TsExpression {

    private final List<TsParameter> parameters;
    
    // ConciseBody = FunctionBody | Expression;
    private final TsExpression expression;
//    private final List<TsStatement> body;

    public TsArrowFunction(List<TsParameter> parameters, TsExpression expression) {
        this.parameters = parameters;
        this.expression = expression;
    }

    public List<TsParameter> getParameters() {
        return parameters;
    }

    public TsExpression getExpression() {
        return expression;
    }

    @Override
    public String format(Settings settings) {
        return Emitter.formatParameterList(parameters) + " => " + expression.format(settings);
    }
    
}
