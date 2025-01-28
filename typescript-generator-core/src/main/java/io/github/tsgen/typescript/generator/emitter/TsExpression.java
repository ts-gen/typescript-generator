
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;


public abstract class TsExpression implements Emittable {

    @Override
    public abstract String format(Settings settings);

}
