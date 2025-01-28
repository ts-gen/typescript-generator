
package io.github.tsgen.typescript.generator.compiler;

import io.github.tsgen.typescript.generator.emitter.TsBeanModel;
import io.github.tsgen.typescript.generator.emitter.TsModel;
import io.github.tsgen.typescript.generator.emitter.TsPropertyModel;
import io.github.tsgen.typescript.generator.parser.BeanModel;
import io.github.tsgen.typescript.generator.parser.Model;
import io.github.tsgen.typescript.generator.parser.PropertyModel;
import java.util.Objects;


@FunctionalInterface
public interface TsModelTransformer {

    public TsModel transformModel(Context context, TsModel model);

    public static class Context {

        private final SymbolTable symbolTable;
        private final Model model;

        public Context(SymbolTable symbolTable, Model model) {
            this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable");
            this.model = Objects.requireNonNull(model, "model");
        }

        public SymbolTable getSymbolTable() {
            return symbolTable;
        }

        public BeanModel getBeanModelOrigin(TsBeanModel tsBean) {
            final BeanModel bean = model.getBean(tsBean.getOrigin());
            return bean;
        }

        public PropertyModel getPropertyModelOrigin(TsBeanModel tsBean, TsPropertyModel tsProperty) {
            final BeanModel bean = getBeanModelOrigin(tsBean);
            if (bean == null) {
                return null;
            }
            final PropertyModel property = bean.getProperty(tsProperty.getName());
            return property;
        }

    }

}
