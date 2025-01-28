
package io.github.tsgen.typescript.generator.emitter;

import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TsObjectLiteral extends TsExpression {

    private final List<TsPropertyDefinition> propertyDefinitions;

    public TsObjectLiteral(TsPropertyDefinition... propertyDefinitions) {
        this(Utils.removeNulls(Arrays.asList(propertyDefinitions)));
    }

    public TsObjectLiteral(List<TsPropertyDefinition> propertyDefinitions) {
        this.propertyDefinitions = propertyDefinitions;
    }

    public List<TsPropertyDefinition> getPropertyDefinitions() {
        return propertyDefinitions;
    }

    @Override
    public String format(Settings settings) {
        final List<String> props = new ArrayList<>();
        for (TsPropertyDefinition property : propertyDefinitions) {
            props.add(property.format(settings));
        }
        if (props.isEmpty()) {
            return "{}";
        } else {
            return "{ " + String.join(", ", props) + " }";
        }
    }

}
