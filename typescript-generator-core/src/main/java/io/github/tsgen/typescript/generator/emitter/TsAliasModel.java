
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.TsType;
import io.github.tsgen.typescript.generator.compiler.Symbol;
import java.util.Collections;
import java.util.List;


public class TsAliasModel extends TsDeclarationModel {
    
    private final List<TsType.GenericVariableType> typeParameters;
    private final TsType definition;

    public TsAliasModel(Class<?> origin, Symbol name, List<TsType.GenericVariableType> typeParameters, TsType definition, List<String> comments) {
        super(origin, null, name, comments);
        this.typeParameters = typeParameters != null ? typeParameters : Collections.emptyList();
        this.definition = definition;
    }

    public List<TsType.GenericVariableType> getTypeParameters() {
        return typeParameters;
    }

    public TsType getDefinition() {
        return definition;
    }

}
