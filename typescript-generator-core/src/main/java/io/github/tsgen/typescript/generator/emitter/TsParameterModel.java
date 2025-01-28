
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.TsParameter;
import io.github.tsgen.typescript.generator.TsType;
import io.github.tsgen.typescript.generator.util.Utils;
import java.util.List;


public class TsParameterModel extends TsParameter {

    protected final List<TsDecorator> decorators;
    private final TsAccessibilityModifier accessibilityModifier;

    public TsParameterModel(String name, TsType tsType) {
        this(null, name, tsType);
    }

    public TsParameterModel(TsAccessibilityModifier accessibilityModifier, String name, TsType tsType) {
        this(null, accessibilityModifier, name, tsType);
    }

    private TsParameterModel(List<TsDecorator> decorators, TsAccessibilityModifier accessibilityModifier, String name, TsType tsType) {
        super(name, tsType);
        this.decorators = Utils.listFromNullable(decorators);
        this.accessibilityModifier = accessibilityModifier;
    }

    public List<TsDecorator> getDecorators() {
        return decorators;
    }

    public TsAccessibilityModifier getAccessibilityModifier() {
        return accessibilityModifier;
    }

    public TsParameterModel withTsType(TsType tsType) {
        return new TsParameterModel(decorators, accessibilityModifier, name, tsType);
    }

    public TsParameterModel withDecorators(List<TsDecorator> decorators) {
        return new TsParameterModel(decorators, accessibilityModifier, name, tsType);
    }

}
