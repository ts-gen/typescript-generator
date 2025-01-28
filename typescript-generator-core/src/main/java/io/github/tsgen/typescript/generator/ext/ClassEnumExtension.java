
package io.github.tsgen.typescript.generator.ext;

import io.github.tsgen.typescript.generator.Extension;
import io.github.tsgen.typescript.generator.compiler.EnumKind;
import io.github.tsgen.typescript.generator.compiler.EnumMemberModel;
import io.github.tsgen.typescript.generator.compiler.ModelCompiler;
import io.github.tsgen.typescript.generator.compiler.TsModelTransformer;
import io.github.tsgen.typescript.generator.emitter.EmitterExtensionFeatures;
import io.github.tsgen.typescript.generator.emitter.TsBeanModel;
import io.github.tsgen.typescript.generator.emitter.TsEnumModel;
import io.github.tsgen.typescript.generator.emitter.TsModel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ClassEnumExtension extends Extension {

    public static final String CFG_CLASS_ENUM_PATTERN = "classEnumPattern";

    private String classEnumPattern = "ClassEnum";

    @Override
    public EmitterExtensionFeatures getFeatures() {
        return new EmitterExtensionFeatures();
    }

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        if (configuration.containsKey(CFG_CLASS_ENUM_PATTERN)) {
            classEnumPattern = configuration.get(CFG_CLASS_ENUM_PATTERN);
        }
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, new TsModelTransformer() {
            @Override
            public TsModel transformModel(Context context, TsModel model) {
                List<TsBeanModel> beans = model.getBeans();
                List<TsBeanModel> classEnums = new ArrayList<>();
                for (TsBeanModel bean : beans) {
                    if (bean.getName().getSimpleName().contains(classEnumPattern)) {
                        classEnums.add(bean);
                    }
                }

                List<TsEnumModel> stringEnums = new ArrayList<>();
                for (TsBeanModel tsBeanModel : classEnums) {
                    List<EnumMemberModel> members = new ArrayList<>();
                    for (Field declaredField : tsBeanModel.getOrigin().getDeclaredFields()) {
                        if (declaredField.getType().getName().equals(tsBeanModel.getOrigin().getName())) {
                            members.add(new EnumMemberModel(declaredField.getName(), declaredField.getName(), declaredField, null));
                        }
                    }
                    TsEnumModel temp = new TsEnumModel(
                            tsBeanModel.getOrigin(),
                            tsBeanModel.getName(),
                            EnumKind.StringBased,
                            members,
                            null,
                            false
                    );
                    stringEnums.add(temp);
                }

                stringEnums.addAll(model.getEnums());
                return model.withEnums(stringEnums).withoutBeans(classEnums);
            }
        }));
    }
}
