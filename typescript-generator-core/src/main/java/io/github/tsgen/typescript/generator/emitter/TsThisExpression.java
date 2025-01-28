
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;


public class TsThisExpression extends TsExpression {

    @Override
    public String format(Settings settings) {
        return "this";
    }

}
