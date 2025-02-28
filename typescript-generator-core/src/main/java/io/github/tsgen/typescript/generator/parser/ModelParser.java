
package io.github.tsgen.typescript.generator.parser;

import io.github.tsgen.typescript.generator.OptionalProperties;
import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TsType;
import io.github.tsgen.typescript.generator.TypeProcessor;
import io.github.tsgen.typescript.generator.TypeScriptGenerator;
import io.github.tsgen.typescript.generator.compiler.EnumKind;
import io.github.tsgen.typescript.generator.compiler.EnumMemberModel;
import io.github.tsgen.typescript.generator.util.AnnotationGetter;
import io.github.tsgen.typescript.generator.util.GenericsResolver;
import io.github.tsgen.typescript.generator.util.PropertyMember;
import io.github.tsgen.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class ModelParser {

    protected final Settings settings;
    private final Javadoc javadoc;
    private final DeprecationEnricher deprecationEnricher;
    private final Queue<SourceType<? extends Type>> typeQueue;
    private final TypeProcessor commonTypeProcessor;
    private final List<RestApplicationParser> restApplicationParsers;
        
    public static abstract class Factory {

        public TypeProcessor getSpecificTypeProcessor() {
            return null;
        }

        public abstract ModelParser create(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers);

    }

    public ModelParser(Settings settings, TypeProcessor commonTypeProcessor, List<RestApplicationParser> restApplicationParsers) {
        this.settings = settings;
        this.javadoc = new Javadoc(settings);
        this.deprecationEnricher = new DeprecationEnricher();
        this.typeQueue = new LinkedList<>();
        this.restApplicationParsers = restApplicationParsers;
        this.commonTypeProcessor = commonTypeProcessor;
    }

    public Model parseModel(Type type) {
        return parseModel(Arrays.asList(new SourceType<>(type)));
    }

    public Model parseModel(List<SourceType<Type>> types) {
        typeQueue.addAll(types);
        Model model = parseQueue();
        if (!settings.ignoreSwaggerAnnotations) {
            model = Swagger.enrichModel(model);
        }
        model = javadoc.enrichModel(model);
        model = deprecationEnricher.enrichModel(model);
        return model;
    }

    private Model parseQueue() {
        final Collection<Type> parsedTypes = new ArrayList<>();  // do not use hashcodes, we can only count on `equals` since we use custom `ParameterizedType`s
        final List<BeanModel> beans = new ArrayList<>();
        final List<EnumModel> enums = new ArrayList<>();
        SourceType<? extends Type> sourceType;
        while ((sourceType = typeQueue.poll()) != null) {
            if (parsedTypes.contains(sourceType.type)) {
                continue;
            }
            parsedTypes.add(sourceType.type);

            // REST resource
            boolean parsedByRestApplicationParser = false;
            for (RestApplicationParser restApplicationParser : restApplicationParsers) {
                final JaxrsApplicationParser.Result jaxrsResult = restApplicationParser.tryParse(sourceType);
                if (jaxrsResult != null) {
                    typeQueue.addAll(jaxrsResult.discoveredTypes);
                    parsedByRestApplicationParser = true;
                }
            }
            if (parsedByRestApplicationParser) {
                continue;
            }

            final TypeProcessor.Result result = commonTypeProcessor.processTypeInTemporaryContext(sourceType.type, null, settings);
            if (result != null) {
                if (sourceType.type instanceof Class<?> && result.getTsType() instanceof TsType.ReferenceType) {
                    final Class<?> cls = (Class<?>) sourceType.type;
                    final TsType.ReferenceType referenceType = (TsType.ReferenceType) result.getTsType();
                    if (!referenceType.symbol.isResolved()) {
                        TypeScriptGenerator.getLogger().verbose("Parsing '" + cls.getName() + "'" +
                                (sourceType.usedInClass != null ? " used in '" + sourceType.usedInClass.getSimpleName() + "." + sourceType.usedInMember + "'" : ""));
                        final DeclarationModel model = parseClass(sourceType.asSourceClass());
                        if (model instanceof EnumModel) {
                            enums.add((EnumModel) model);
                        } else if (model instanceof BeanModel) {
                            beans.add((BeanModel) model);
                        } else {
                            throw new RuntimeException();
                        }
                    }
                }
                for (Class<?> cls : result.getDiscoveredClasses()) {
                    typeQueue.add(new SourceType<>(cls, sourceType.usedInClass, sourceType.usedInMember));
                }
            }
        }
        final List<RestApplicationModel> restModels = restApplicationParsers.stream()
                .map(RestApplicationParser::getModel)
                .collect(Collectors.toList());
        return new Model(beans, enums, restModels);
    }

    protected abstract DeclarationModel parseClass(SourceType<Class<?>> sourceClass);

    protected static PropertyMember wrapMember(TypeParser typeParser, Member propertyMember, Integer creatorIndex, AnnotationGetter annotationGetter,
            String propertyName, Class<?> sourceClass) {
        if (propertyMember instanceof Field) {
            final Field field = (Field) propertyMember;
            return new PropertyMember(field, typeParser.getFieldType(field), field.getAnnotatedType(), annotationGetter);
        }
        if (propertyMember instanceof Method) {
            final Method method = (Method) propertyMember;
            if (creatorIndex != null) {
                return new PropertyMember(method, typeParser.getMethodParameterTypes(method).get(creatorIndex), method.getAnnotatedParameterTypes()[creatorIndex], annotationGetter);
            } else {
                switch (method.getParameterCount()) {
                    case 0:
                        return new PropertyMember(method, typeParser.getMethodReturnType(method), method.getAnnotatedReturnType(), annotationGetter);
                    case 1:
                        return new PropertyMember(method, typeParser.getMethodParameterTypes(method).get(0), method.getAnnotatedParameterTypes()[0], annotationGetter);
                }
            }
        }
        if (propertyMember instanceof Constructor) {
            final Constructor<?> constructor = (Constructor<?>) propertyMember;
            if (creatorIndex != null) {
                return new PropertyMember(constructor, typeParser.getConstructorParameterTypes(constructor).get(creatorIndex), constructor.getAnnotatedParameterTypes()[creatorIndex], annotationGetter);
            }
        }
        TypeScriptGenerator.getLogger().verbose(String.format(
                "Unexpected member '%s' in property '%s' in class '%s'",
                propertyMember != null ? propertyMember.getClass().getName() : null,
                propertyName,
                sourceClass.getName()));
        return null;
    }

    protected boolean isAnnotatedPropertyIncluded(Function<Class<? extends Annotation>, Annotation> getAnnotationFunction, String propertyDescription) {
        boolean isIncluded = settings.includePropertyAnnotations.isEmpty()
                || Utils.hasAnyAnnotation(getAnnotationFunction, settings.includePropertyAnnotations);
        if (!isIncluded) {
            TypeScriptGenerator.getLogger().verbose("Skipping '" + propertyDescription + "' because it doesn't have any annotation from 'includePropertyAnnotations'");
            return false;
        }
        boolean isExcluded = Utils.hasAnyAnnotation(getAnnotationFunction, settings.excludePropertyAnnotations);
        if (isExcluded) {
            TypeScriptGenerator.getLogger().verbose("Skipping '" + propertyDescription + "' because it has some annotation from 'excludePropertyAnnotations'");
            return false;
        }
        return true;
    }

    protected boolean isPropertyOptional(PropertyMember propertyMember) {
        if (settings.optionalProperties == OptionalProperties.all) {
            return true;
        }
        if (settings.optionalProperties == null || settings.optionalProperties == OptionalProperties.useSpecifiedAnnotations) {
            if (!settings.optionalAnnotations.isEmpty()) {
                return Utils.hasAnyAnnotation(propertyMember::getAnnotation, settings.optionalAnnotations);
            }
            if (settings.primitivePropertiesRequired && Utils.isPrimitiveType(propertyMember.getType())) {
                return false;
            }
            if (!settings.requiredAnnotations.isEmpty()) {
                return !Utils.hasAnyAnnotation(propertyMember::getAnnotation, settings.requiredAnnotations);
            }
        }
        return false;
    }

    protected static DeclarationModel parseEnum(SourceType<Class<?>> sourceClass) {
        final List<EnumMemberModel> values = new ArrayList<>();
        if (sourceClass.type.isEnum()) {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) sourceClass.type;
            final Map<String, Field> fields = Stream.of(enumClass.getDeclaredFields()).collect(Utils.toMap(field -> field.getName(), field -> field));
            for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                values.add(new EnumMemberModel(enumConstant.name(), enumConstant.name(), fields.get(enumConstant.name()), null));
            }
        }
        return new EnumModel(sourceClass.type, EnumKind.StringBased, values, null);
    }

    protected void addBeanToQueue(SourceType<? extends Type> sourceType) {
        typeQueue.add(sourceType);
    }

    protected PropertyModel processTypeAndCreateProperty(String name, Type type, Object typeContext, boolean optional, PropertyAccess access, Class<?> usedInClass, Member originalMember, PropertyModel.PullProperties pullProperties, List<String> comments) {
        final Type resolvedType = GenericsResolver.resolveType(usedInClass, type, originalMember.getDeclaringClass());
        final List<Class<?>> classes = commonTypeProcessor.discoverClassesUsedInType(resolvedType, typeContext, settings);
        for (Class<?> cls : classes) {
            typeQueue.add(new SourceType<>(cls, usedInClass, name));
        }
        return new PropertyModel(name, resolvedType, optional, access, originalMember, pullProperties, typeContext, comments);
    }

    public static boolean containsProperty(List<PropertyModel> properties, String propertyName) {
        for (PropertyModel property : properties) {
            if (property.getName().equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

}
