
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TsType;


public class TsTypeReferenceExpression extends TsExpression {

    private final TsType.ReferenceType type;

    public TsTypeReferenceExpression(TsType.ReferenceType type) {
        this.type = type;
    }

    public TsType.ReferenceType getType() {
        return type;
    }

    @Override
    public String format(Settings settings) {
        return type.format(settings);
    }

}
