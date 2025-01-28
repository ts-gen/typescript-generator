
package io.github.tsgen.typescript.generator;

import io.github.tsgen.typescript.generator.compiler.Symbol;
import io.github.tsgen.typescript.generator.compiler.SymbolTable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public interface TypeProcessor {

    /**
     * @return <code>null</code> if this processor didn't process passed java type
     */
    public Result processType(Type javaType, Context context);

    public default Result processTypeInTemporaryContext(Type type, Object typeContext, Settings settings) {
        return processType(type, new Context(new SymbolTable(settings), this, typeContext));
    }

    public default List<Class<?>> discoverClassesUsedInType(Type type, Object typeContext, Settings settings) {
        final TypeProcessor.Result result = processTypeInTemporaryContext(type, typeContext, settings);
        return result != null ? result.getDiscoveredClasses() : Collections.emptyList();
    }

    public default boolean isTypeExcluded(Type type, Object typeContext, Settings settings) {
        final TypeProcessor.Result result = processTypeInTemporaryContext(type, typeContext, settings);
        return result != null && result.tsType == TsType.Any;
    }

    public static class Context {

        private final SymbolTable symbolTable;
        private final TypeProcessor typeProcessor;
        private final Object typeContext;
        private final boolean insideCollection;

        public Context(SymbolTable symbolTable, TypeProcessor typeProcessor, Object typeContext) {
            this(symbolTable, typeProcessor, typeContext, false);
        }

        public Context(SymbolTable symbolTable, TypeProcessor typeProcessor, Object typeContext, boolean insideCollection) {
            this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable");
            this.typeProcessor = Objects.requireNonNull(typeProcessor, "typeProcessor");
            this.typeContext = typeContext;
            this.insideCollection = insideCollection;
        }

        public Symbol getSymbol(Class<?> cls) {
            return symbolTable.getSymbol(cls);
        }

        public Symbol getSymbolIfImported(Class<?> cls) {
            return symbolTable.getSymbolIfImported(cls);
        }

        public Result processType(Type javaType) {
            return typeProcessor.processType(javaType, this);
        }

        public Result processTypeInsideCollection(Type javaType) {
            return typeProcessor.processType(javaType, this.withInsideCollection());
        }

        public Object getTypeContext() {
            return typeContext;
        }

        public boolean isInsideCollection() {
            return insideCollection;
        }

        public Context withTypeContext(Object typeContext) {
            return new Context(symbolTable, typeProcessor, typeContext, insideCollection);
        }

        public Context withInsideCollection() {
            return new Context(symbolTable, typeProcessor, typeContext, true);
        }

    }

    public static class Result {

        private final TsType tsType;
        private final List<Class<?>> discoveredClasses;

        public Result(TsType tsType, List<Class<?>> discoveredClasses) {
            this.tsType = tsType;
            this.discoveredClasses = discoveredClasses;
        }

        public Result(TsType tsType, Class<?>... discoveredClasses) {
            this.tsType = tsType;
            this.discoveredClasses = Arrays.asList(discoveredClasses);
        }

        public TsType getTsType() {
            return tsType;
        }

        public List<Class<?>> getDiscoveredClasses() {
            return discoveredClasses;
        }

    }

    public static class Chain implements TypeProcessor {

        private final List<TypeProcessor> processors;

        public Chain(List<TypeProcessor> processors) {
            this.processors = processors;
        }

        public Chain(TypeProcessor... processors) {
            this.processors = Arrays.asList(processors);
        }

        @Override
        public Result processType(Type javaType, Context context) {
            for (TypeProcessor processor : processors) {
                final Result result = processor.processType(javaType, context);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

    }

}
