
package io.github.tsgen.typescript.generator.ext;

import io.github.tsgen.typescript.generator.Extension;
import io.github.tsgen.typescript.generator.TsParameter;
import io.github.tsgen.typescript.generator.TsType;
import io.github.tsgen.typescript.generator.compiler.ModelCompiler;
import io.github.tsgen.typescript.generator.compiler.Symbol;
import io.github.tsgen.typescript.generator.compiler.SymbolTable;
import io.github.tsgen.typescript.generator.compiler.TsModelTransformer;
import io.github.tsgen.typescript.generator.emitter.EmitterExtensionFeatures;
import io.github.tsgen.typescript.generator.emitter.TsArrowFunction;
import io.github.tsgen.typescript.generator.emitter.TsAssignmentExpression;
import io.github.tsgen.typescript.generator.emitter.TsBeanModel;
import io.github.tsgen.typescript.generator.emitter.TsBinaryExpression;
import io.github.tsgen.typescript.generator.emitter.TsBinaryOperator;
import io.github.tsgen.typescript.generator.emitter.TsCallExpression;
import io.github.tsgen.typescript.generator.emitter.TsConstructorModel;
import io.github.tsgen.typescript.generator.emitter.TsExpression;
import io.github.tsgen.typescript.generator.emitter.TsExpressionStatement;
import io.github.tsgen.typescript.generator.emitter.TsHelper;
import io.github.tsgen.typescript.generator.emitter.TsIdentifierReference;
import io.github.tsgen.typescript.generator.emitter.TsIfStatement;
import io.github.tsgen.typescript.generator.emitter.TsMemberExpression;
import io.github.tsgen.typescript.generator.emitter.TsMethodModel;
import io.github.tsgen.typescript.generator.emitter.TsModel;
import io.github.tsgen.typescript.generator.emitter.TsModifierFlags;
import io.github.tsgen.typescript.generator.emitter.TsNewExpression;
import io.github.tsgen.typescript.generator.emitter.TsObjectLiteral;
import io.github.tsgen.typescript.generator.emitter.TsParameterModel;
import io.github.tsgen.typescript.generator.emitter.TsPrefixUnaryExpression;
import io.github.tsgen.typescript.generator.emitter.TsPropertyDefinition;
import io.github.tsgen.typescript.generator.emitter.TsPropertyModel;
import io.github.tsgen.typescript.generator.emitter.TsReturnStatement;
import io.github.tsgen.typescript.generator.emitter.TsStatement;
import io.github.tsgen.typescript.generator.emitter.TsStringLiteral;
import io.github.tsgen.typescript.generator.emitter.TsSuperExpression;
import io.github.tsgen.typescript.generator.emitter.TsSwitchCaseClause;
import io.github.tsgen.typescript.generator.emitter.TsSwitchStatement;
import io.github.tsgen.typescript.generator.emitter.TsTypeReferenceExpression;
import io.github.tsgen.typescript.generator.emitter.TsUnaryOperator;
import io.github.tsgen.typescript.generator.emitter.TsVariableDeclarationStatement;
import io.github.tsgen.typescript.generator.util.Utils;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class JsonDeserializationExtension extends Extension {

    public static final String CFG_USE_JSON_DESERIALIZATION_IN_JAXRS_APPLICATION_CLIENT = "useJsonDeserializationInJaxrsApplicationClient";

    private boolean useJsonDeserializationInJaxrsApplicationClient = false;

    public JsonDeserializationExtension() {
    }

    public JsonDeserializationExtension(boolean useJsonDeserializationInJaxrsApplicationClient) {
        this.useJsonDeserializationInJaxrsApplicationClient = useJsonDeserializationInJaxrsApplicationClient;
    }

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        features.worksWithPackagesMappedToNamespaces = true;
        return features;
    }

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        if (configuration.containsKey(CFG_USE_JSON_DESERIALIZATION_IN_JAXRS_APPLICATION_CLIENT)) {
            useJsonDeserializationInJaxrsApplicationClient = Boolean.parseBoolean(configuration.get(CFG_USE_JSON_DESERIALIZATION_IN_JAXRS_APPLICATION_CLIENT));
        }
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeSymbolResolution, new TsModelTransformer() {
            @Override
            public TsModel transformModel(Context context, TsModel model) {
                model = createDeserializationMethods(context.getSymbolTable(), model);
                if (useJsonDeserializationInJaxrsApplicationClient) {
                    model = useDeserializationMethodsInJaxrs(context.getSymbolTable(), model);
                }
                return model;
            }
        }));
    }

    private static TsModel createDeserializationMethods(SymbolTable symbolTable, TsModel tsModel) {
        tsModel.getHelpers().add(TsHelper.loadFromResource("/helpers/jsonDeserialization.ts"));
        final List<TsBeanModel> beans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            if (bean.isDataClass()) {
                final List<TsMethodModel> methods = new ArrayList<>(bean.getMethods());
                final TsMethodModel deserializationMethod = createDeserializationMethod(symbolTable, tsModel, bean);
                methods.add(0, deserializationMethod);
                if (!bean.getTypeParameters().isEmpty()) {
                    final TsMethodModel genericFunctionConstructor = createDeserializationGenericFunctionConstructor(symbolTable, tsModel, bean);
                    methods.add(0, genericFunctionConstructor);
                }
                if (bean.getTaggedUnionAlias() != null) {
                    final TsMethodModel unionDeserializationMethod = createDeserializationMethodForTaggedUnion(symbolTable, tsModel, bean);
                    methods.add(1, unionDeserializationMethod);
                }
                beans.add(bean.withMethods(methods));
            } else {
                beans.add(bean);
            }
        }
        return tsModel.withBeans(beans);
    }

    private static TsMethodModel createDeserializationMethod(SymbolTable symbolTable, TsModel tsModel, TsBeanModel bean) {
        final Symbol beanIdentifier = symbolTable.getSymbol(bean.getOrigin());
        List<TsType.GenericVariableType> typeParameters = getTypeParameters(bean.getOrigin());

        final TsType.ReferenceType dataType = typeParameters.isEmpty()
                ? new TsType.ReferenceType(beanIdentifier)
                : new TsType.GenericReferenceType(beanIdentifier, typeParameters);
        final List<TsParameterModel> parameters = new ArrayList<>();
        parameters.add(new TsParameterModel("data", dataType));
        parameters.addAll(getConstructorFnOfParameters(typeParameters));
        parameters.add(new TsParameterModel("target", dataType.optional()));

        final List<TsStatement> body = new ArrayList<>();
        body.add(ifUndefinedThenReturnItStatement("data"));
        body.add(new TsVariableDeclarationStatement(
                /*const*/ true,
                "instance",
                /*type*/ null,
                new TsBinaryExpression(
                        new TsIdentifierReference("target"),
                        TsBinaryOperator.BarBar,
                        new TsNewExpression(new TsTypeReferenceExpression(new TsType.ReferenceType(beanIdentifier)), typeParameters, getConstructorParameters(bean))
                )
        ));
        if (bean.getParent() != null) {
            body.add(new TsExpressionStatement(
                    new TsCallExpression(
                            new TsMemberExpression(new TsSuperExpression(), "fromData"),
                            new TsIdentifierReference("data"),
                            new TsIdentifierReference("instance")
                    )
            ));
        }
        for (TsPropertyModel property : bean.getProperties()) {
            final Map<String, TsType> inheritedProperties = ModelCompiler.getInheritedProperties(symbolTable, tsModel, Utils.listFromNullable(bean.getParent()));
            if (!inheritedProperties.containsKey(property.getName())) {
                body.add(new TsExpressionStatement(new TsAssignmentExpression(
                        new TsMemberExpression(new TsIdentifierReference("instance"), property.name),
                        getPropertyCopy(symbolTable, tsModel, bean, property)
                )));
            }
        }
        body.add(new TsReturnStatement(new TsIdentifierReference("instance")));

        return new TsMethodModel(
                "fromData",
                TsModifierFlags.None.setStatic(),
                typeParameters,
                parameters,
                dataType,
                body,
                null
        );
    }

    private static List<TsIdentifierReference> getConstructorParameters(TsBeanModel bean) {
        TsConstructorModel constructor = bean.getConstructor();
        if (constructor == null) {
            return null;
        }
        List<TsIdentifierReference> parameters = new ArrayList<>();
        for (TsParameterModel parameter : constructor.getParameters()) {
            parameters.add(new TsIdentifierReference(parameter.name));
        }
        return parameters;
    }

    private static TsMethodModel createDeserializationGenericFunctionConstructor(SymbolTable symbolTable, TsModel tsModel, TsBeanModel bean) {
        final Symbol beanIdentifier = symbolTable.getSymbol(bean.getOrigin());
        List<TsType.GenericVariableType> typeParameters = getTypeParameters(bean.getOrigin());
        final TsType.ReferenceType dataType = new TsType.GenericReferenceType(beanIdentifier, typeParameters);

        final List<TsParameterModel> constructorFnOfParameters = getConstructorFnOfParameters(typeParameters);
        final List<TsExpression> arguments = new ArrayList<>();
        arguments.add(new TsIdentifierReference("data"));
        for (TsParameterModel constructorFnOfParameter : constructorFnOfParameters) {
            arguments.add(new TsIdentifierReference(constructorFnOfParameter.name));
        }
        final List<TsStatement> body = new ArrayList<>();
        body.add(new TsReturnStatement(
                new TsArrowFunction(
                        Arrays.asList(new TsParameter("data", null)),
                        new TsCallExpression(
                                new TsMemberExpression(new TsTypeReferenceExpression(new TsType.ReferenceType(beanIdentifier)), "fromData"),
                                null,
                                arguments
                        )
                )
        ));

        return new TsMethodModel(
                "fromDataFn",
                TsModifierFlags.None.setStatic(),
                typeParameters,
                constructorFnOfParameters,
                new TsType.FunctionType(Arrays.asList(new TsParameter("data", dataType)), dataType),
                body,
                null
        );
    }

    private static List<TsType.GenericVariableType> getTypeParameters(Class<?> cls) {
        final List<TsType.GenericVariableType> typeParameters = new ArrayList<>();
        for (TypeVariable<?> typeParameter : cls.getTypeParameters()) {
            typeParameters.add(new TsType.GenericVariableType(typeParameter.getName()));
        }
        return typeParameters;
    }

    private static List<TsParameterModel> getConstructorFnOfParameters(List<TsType.GenericVariableType> typeParameters) {
        final List<TsParameterModel> parameters = new ArrayList<>();
        for (TsType.GenericVariableType typeParameter : typeParameters) {
            parameters.add(new TsParameterModel(
                    "constructorFnOf" + typeParameter.name,
                    new TsType.FunctionType(Arrays.asList(new TsParameter("data", typeParameter)), typeParameter)
            ));
        }
        return parameters;
    }

    private static TsIfStatement ifUndefinedThenReturnItStatement(String identifier) {
        return new TsIfStatement(
                new TsPrefixUnaryExpression(TsUnaryOperator.Exclamation, new TsIdentifierReference(identifier)),
                Arrays.<TsStatement>asList(new TsReturnStatement(new TsIdentifierReference(identifier)))
        );
    }

    private static TsExpression getPropertyCopy(SymbolTable symbolTable, TsModel tsModel, TsBeanModel bean, TsPropertyModel property) {
        final TsExpression copyFunction = getCopyFunctionForTsType(symbolTable, tsModel, property.getTsType());
        if (copyFunction instanceof TsCallExpression) {
            final TsCallExpression callExpression = (TsCallExpression) copyFunction;
            if (callExpression.getExpression() instanceof TsIdentifierReference) {
                final TsIdentifierReference reference = (TsIdentifierReference) callExpression.getExpression();
                if (reference.getIdentifier().equals("__identity")) {
                    // function degenerates to the same value (data.property)
                    return new TsMemberExpression(new TsIdentifierReference("data"), property.name);
                }
            }
        }
        return new TsCallExpression(
                copyFunction,
                new TsMemberExpression(new TsIdentifierReference("data"), property.name)
        );
    }

    private static TsExpression getCopyFunctionForTsType(SymbolTable symbolTable, TsModel tsModel, TsType tsType) {
        if (tsType instanceof TsType.GenericReferenceType) {
            final TsType.GenericReferenceType genericReferenceType = (TsType.GenericReferenceType) tsType;
            // Class.fromDataFn<T1...>(constructorFnOfT1...)
            final List<TsExpression> arguments = new ArrayList<>();
            for (TsType typeArgument : genericReferenceType.typeArguments) {
                arguments.add(getCopyFunctionForTsType(symbolTable, tsModel, typeArgument));
            }
            return new TsCallExpression(
                    new TsMemberExpression(new TsTypeReferenceExpression(new TsType.ReferenceType(genericReferenceType.symbol)), "fromDataFn"),
                    genericReferenceType.typeArguments,
                    arguments
            );
        }
        if (tsType instanceof TsType.ReferenceType) {
            final TsType.ReferenceType referenceType = (TsType.ReferenceType) tsType;
            final TsBeanModel referencedBean = tsModel.getBean(symbolTable.getSymbolClass(referenceType.symbol));
            if (referencedBean != null && referencedBean.isClass()) {
                if (referencedBean.getTaggedUnionAlias() != null) {
                    // Class.fromDataUnion (tagged union)
                    return new TsMemberExpression(new TsTypeReferenceExpression(new TsType.ReferenceType(referencedBean.getName())), "fromDataUnion");
                } else {
                    // Class.fromData
                    return new TsMemberExpression(new TsTypeReferenceExpression(referenceType), "fromData");
                }
            }
        }
        if (tsType instanceof TsType.BasicArrayType) {
            // __getCopyArrayFn
            final TsType.BasicArrayType arrayType = (TsType.BasicArrayType) tsType;
            return new TsCallExpression(
                    new TsIdentifierReference("__getCopyArrayFn"),
                    getCopyFunctionForTsType(symbolTable, tsModel, arrayType.elementType)
            );
        }
        if (tsType instanceof TsType.IndexedArrayType) {
            // __getCopyObjectFn
            final TsType.IndexedArrayType objectType = (TsType.IndexedArrayType) tsType;
            return new TsCallExpression(
                    new TsIdentifierReference("__getCopyObjectFn"),
                    getCopyFunctionForTsType(symbolTable, tsModel, objectType.elementType)
            );
        }
        if (tsType instanceof TsType.GenericVariableType) {
            // constructorFnOfT
            final TsType.GenericVariableType genericVariableType = (TsType.GenericVariableType) tsType;
            return new TsIdentifierReference("constructorFnOf" + genericVariableType.name);
        }
        // __identity
        return new TsCallExpression(
                new TsIdentifierReference("__identity"),
                Arrays.asList(tsType),
                Collections.<TsExpression>emptyList()
        );
    }

    private static TsMethodModel createDeserializationMethodForTaggedUnion(SymbolTable symbolTable, TsModel tsModel, TsBeanModel bean) {
        final List<TsSwitchCaseClause> caseClauses = new ArrayList<>();
        for (Class<?> cls : bean.getTaggedUnionClasses()) {
            final TsBeanModel tuBean = tsModel.getBean(cls);
            caseClauses.add(new TsSwitchCaseClause(
                    new TsStringLiteral(tuBean.getDiscriminantLiteral()),
                    Arrays.<TsStatement>asList(new TsReturnStatement(
                            new TsCallExpression(
                                    new TsMemberExpression(new TsTypeReferenceExpression(new TsType.ReferenceType(symbolTable.getSymbol(cls))), "fromData"),
                                    new TsIdentifierReference("data")
                            )
                    ))
            ));
        }

        final List<TsStatement> body = new ArrayList<>();
        body.add(ifUndefinedThenReturnItStatement("data"));
        body.add(new TsSwitchStatement(
                new TsMemberExpression(new TsIdentifierReference("data"), bean.getDiscriminantProperty()),
                caseClauses,
                null
        ));
        final TsType.ReferenceType unionType = new TsType.ReferenceType(bean.getTaggedUnionAlias().getName());
        return new TsMethodModel(
                "fromDataUnion",
                TsModifierFlags.None.setStatic(),
                null, //typeParameters,
                Arrays.asList(new TsParameterModel("data", unionType)),
                unionType,
                body,
                null
        );
    }

    private TsModel useDeserializationMethodsInJaxrs(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            if (bean.isJaxrsApplicationClientBean()) {
                final List<TsMethodModel> methods = new ArrayList<>();
                for (TsMethodModel method : bean.getMethods()) {
                    final TsMethodModel changedMethod = addCopyFnToJaxrsMethod(symbolTable, tsModel, method);
                    methods.add(changedMethod != null ? changedMethod : method);
                }
                beans.add(bean.withMethods(methods));
            } else {
                beans.add(bean);
            }
        }
        return tsModel.withBeans(beans);
    }

    private static TsMethodModel addCopyFnToJaxrsMethod(SymbolTable symbolTable, TsModel tsModel, TsMethodModel method) {
        final TsType returnType = method.getReturnType();
        if (!(returnType instanceof TsType.GenericReferenceType)) return null;
        final TsType.GenericReferenceType genericReferenceReturnType = (TsType.GenericReferenceType) returnType;
        if (genericReferenceReturnType.symbol != symbolTable.getSyntheticSymbol("RestResponse")) return null;
        final List<TsType> typeArguments = genericReferenceReturnType.typeArguments;
        if (typeArguments == null || typeArguments.size() != 1) return null;
        final TsType returnDataType = typeArguments.get(0);
        final List<TsStatement> body = method.getBody();
        if (body == null || body.size() != 1) return null;
        final TsStatement statement = body.get(0);
        if (!(statement instanceof TsReturnStatement)) return null;
        final TsReturnStatement returnStatement = (TsReturnStatement) statement;
        final TsExpression returnExpression = returnStatement.getExpression();
        if (returnExpression == null) return null;
        if (!(returnExpression instanceof TsCallExpression)) return null;
        final TsCallExpression callExpression = (TsCallExpression) returnExpression;
        final List<TsExpression> arguments = callExpression.getArguments();
        if (arguments == null || arguments.isEmpty()) return null;
        final TsExpression firstArgument = arguments.get(0);
        if (!(firstArgument instanceof TsObjectLiteral)) return null;
        final TsObjectLiteral objectLiteral = (TsObjectLiteral) firstArgument;

        // todo create changed method instead of modifying existing
        final int index = Math.max(objectLiteral.getPropertyDefinitions().size() - 1, 0);
        final TsExpression copyFunction = returnDataType == TsType.Void
                ? TsIdentifierReference.Undefined
                : getCopyFunctionForTsType(symbolTable, tsModel, returnDataType);
        objectLiteral.getPropertyDefinitions().add(index, new TsPropertyDefinition("copyFn", copyFunction));
        return method;
    }

}
