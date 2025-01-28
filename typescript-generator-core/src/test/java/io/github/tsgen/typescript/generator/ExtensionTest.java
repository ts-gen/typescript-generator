package io.github.tsgen.typescript.generator;

import io.github.tsgen.typescript.generator.compiler.ModelCompiler;
import io.github.tsgen.typescript.generator.compiler.ModelCompiler.TransformationPhase;
import io.github.tsgen.typescript.generator.compiler.ModelTransformer;
import io.github.tsgen.typescript.generator.compiler.SymbolTable;
import io.github.tsgen.typescript.generator.emitter.EmitterExtensionFeatures;
import io.github.tsgen.typescript.generator.emitter.TsModel;
import io.github.tsgen.typescript.generator.parser.BeanModel;
import io.github.tsgen.typescript.generator.parser.Jackson2Parser;
import io.github.tsgen.typescript.generator.parser.Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExtensionTest {

    @Test
    public void testBeforeTsExtension() throws Exception {
        final Settings settings = TestUtils.settings();

        settings.extensions.add(new Extension() {

            @Override
            public EmitterExtensionFeatures getFeatures() {
                return new EmitterExtensionFeatures();
            }

            @Override
            public List<TransformerDefinition> getTransformers() {
                return Collections.singletonList(new TransformerDefinition(TransformationPhase.BeforeTsModel, new ModelTransformer() {
                    @Override
                    public Model transformModel(SymbolTable symbolTable, Model model) {
                        List<BeanModel> beans = new ArrayList<>(model.getBeans());

                        BeanModel implementationBean = model.getBean(Implementation.class);
                        BeanModel beanWithComments = implementationBean.withComments(Collections.singletonList("My new comment"));

                        beans.remove(implementationBean);
                        beans.add(beanWithComments);

                        return new Model(beans, model.getEnums(), model.getRestApplications());
                    }
                }));
            }
        });

        final Jackson2Parser jacksonParser = new Jackson2Parser(settings, new DefaultTypeProcessor());
        final Model model = jacksonParser.parseModel(Implementation.class);
        final ModelCompiler modelCompiler = new TypeScriptGenerator(settings).getModelCompiler();

        final TsModel result = modelCompiler.javaToTypeScript(model);

        Assertions.assertEquals(1, result.getBean(Implementation.class).getComments().size());
        Assertions.assertTrue(result.getBean(Implementation.class).getComments().get(0).contains("My new comment"));
    }

    private static class Implementation { }

}
