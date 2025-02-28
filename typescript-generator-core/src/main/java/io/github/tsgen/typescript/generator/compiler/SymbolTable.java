
package io.github.tsgen.typescript.generator.compiler;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TypeScriptGenerator;
import io.github.tsgen.typescript.generator.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;


/**
 * Name table.
 */
public class SymbolTable {

    private final Settings settings;
    private final LinkedHashMap<Pair<Class<?>, String>, Symbol> symbols = new LinkedHashMap<>();
    private final LinkedHashMap<String, Symbol> syntheticSymbols = new LinkedHashMap<>();
    private CustomTypeNamingFunction customTypeNamingFunction;

    public SymbolTable(Settings settings) {
        this.settings = settings;
    }

    public Symbol getSymbol(Class<?> cls) {
        return getSymbol(cls, null);
    }

    public Symbol getSymbol(Class<?> cls, String suffix) {
        final String suffixString = suffix != null ? suffix : "";
        final Pair<Class<?>, String> key = Pair.<Class<?>, String>of(cls, suffixString);
        if (!symbols.containsKey(key)) {
            symbols.put(key, new Symbol("$" + cls.getName().replace('.', '$') + suffixString + "$"));
        }
        return symbols.get(key);
    }

    public Symbol hasSymbol(Class<?> cls, String suffix) {
        return symbols.get(Pair.<Class<?>, String>of(cls, suffix));
    }

    public Class<?> getSymbolClass(Symbol symbol) {
        for (Map.Entry<Pair<Class<?>, String>, Symbol> entry : symbols.entrySet()) {
            if (entry.getValue() == symbol) {
                return entry.getKey().getValue1();
            }
        }
        return null;
    }

    public Symbol getSyntheticSymbol(String name) {
        if (!syntheticSymbols.containsKey(name)) {
            syntheticSymbols.put(name, new Symbol(name));
        }
        return syntheticSymbols.get(name);
    }

    public Symbol getSyntheticSymbol(String name, String suffix) {
        return getSyntheticSymbol(name + (suffix != null ? suffix : ""));
    }

    public Symbol addSuffixToSymbol(Symbol symbol, String suffix) {
        // try symbols
        for (Map.Entry<Pair<Class<?>, String>, Symbol> entry : symbols.entrySet()) {
            if (entry.getValue() == symbol) {
                return getSymbol(entry.getKey().getValue1(), entry.getKey().getValue2() + suffix);
            }
        }
        // syntheticSymbols
        return getSyntheticSymbol(symbol.getFullName() + suffix);
    }

    public void resolveSymbolNames() {
        final Map<String, List<Class<?>>> names = new LinkedHashMap<>();
        for (Map.Entry<Pair<Class<?>, String>, Symbol> entry : symbols.entrySet()) {
            final Class<?> cls = entry.getKey().getValue1();
            final String suffix = entry.getKey().getValue2();
            final Symbol symbol = entry.getValue();
            if (!symbol.isResolved()) {
                setSymbolQualifiedName(symbol, cls, suffix);
            }
            final String fullName = symbol.getFullName();
            if (!names.containsKey(fullName)) {
                names.put(fullName, new ArrayList<>());
            }
            names.get(fullName).add(cls);
        }
        reportConflicts(names);
    }

    private static void reportConflicts(Map<String, List<Class<?>>> names) {
        boolean conflict = false;
        for (Map.Entry<String, List<Class<?>>> entry : names.entrySet()) {
            final String name = entry.getKey();
            final List<Class<?>> classes = entry.getValue();
            if (classes.size() > 1) {
                TypeScriptGenerator.getLogger().warning(String.format("Multiple classes are mapped to '%s' name. Conflicting classes: %s", name, classes));
                conflict = true;
            }
        }
        if (conflict) {
            throw new NameConflictException("Multiple classes are mapped to the same name. You can use 'customTypeNaming' or 'customTypeNamingFunction' settings to resolve conflicts or exclude conflicting class if it was added accidentally.");
        }
    }

    private void setSymbolQualifiedName(Symbol symbol, Class<?> cls, String suffix) {
        final String module;
        final String namespacedName;
        final Pair<String/*module*/, String/*namespacedName*/> fullNameFromDependency = settings.getModuleDependencies().getFullName(cls);
        if (fullNameFromDependency != null) {
            module = fullNameFromDependency.getValue1();
            namespacedName = fullNameFromDependency.getValue2();
        } else {
            module = null;
            namespacedName = getMappedNamespacedName(cls);
        }
        final String suffixString = suffix != null ? suffix : "";
        symbol.setFullName(module, namespacedName + suffixString);
    }

    public String getMappedNamespacedName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        final String customName = settings.customTypeNaming.get(cls.getName());
        if (customName != null) {
            return customName;
        }
        if (settings.customTypeNamingFunctionImpl != null || settings.customTypeNamingFunction != null) {
            try {
                final CustomTypeNamingFunction function;
                if (settings.customTypeNamingFunctionImpl != null) {
                    function = settings.customTypeNamingFunctionImpl;
                } else {
                    function = getCustomTypeNamingFunction();
                }

                final Object getNameResult = function.getName(cls.getName(), cls.getSimpleName());
                if (getNameResult instanceof String) {
                    return (String) getNameResult;
                }
            } catch (ScriptException e) {
                throw new RuntimeException("Evaluating 'customTypeNamingFunction' failed.", e);
            }
        }
        String simpleName = cls.getSimpleName();
        if (settings.removeTypeNamePrefix != null && simpleName.startsWith(settings.removeTypeNamePrefix)) {
            simpleName = simpleName.substring(settings.removeTypeNamePrefix.length(), simpleName.length());
        }
        if (settings.removeTypeNameSuffix != null && simpleName.endsWith(settings.removeTypeNameSuffix)) {
            simpleName = simpleName.substring(0, simpleName.length() - settings.removeTypeNameSuffix.length());
        }
        if (settings.addTypeNamePrefix != null) {
            simpleName = settings.addTypeNamePrefix + simpleName;
        }
        if (settings.addTypeNameSuffix != null) {
            simpleName = simpleName + settings.addTypeNameSuffix;
        }

        if (settings.mapPackagesToNamespaces) {
            final String classNameDotted = cls.getName().replace('$', '.');
            final String[] parts = classNameDotted.split(Pattern.quote("."));
            final List<String> safeParts = new ArrayList<>();
            for (String part : Arrays.asList(parts).subList(0, parts.length - 1)) {
                safeParts.add(isReservedWord(part) ? "_" + part : part);
            }
            safeParts.add(simpleName);
            return String.join(".", safeParts);
        } else {
            return simpleName;
        }
    }

    // https://github.com/Microsoft/TypeScript/blob/master/doc/spec-ARCHIVED.md#221-reserved-words
    private static final Set<String> Keywords = new LinkedHashSet<>(Arrays.asList(
        "break",             "case",              "catch",             "class",
        "const",             "continue",          "debugger",          "default",
        "delete",            "do",                "else",              "enum",
        "export",            "extends",           "false",             "finally",
        "for",               "function",          "if",                "import",
        "in",                "instanceof",        "new",               "null",
        "return",            "super",             "switch",            "this",
        "throw",             "true",              "try",               "typeof",
        "var",               "void",              "while",             "with",

        "implements",        "interface",         "let",               "package",
        "private",           "protected",         "public",            "static",
        "yield"
    ));

    public static boolean isReservedWord(String word) {
        return Keywords.contains(word);
    }

    private CustomTypeNamingFunction getCustomTypeNamingFunction() throws ScriptException {
        if (customTypeNamingFunction == null) {
            final ScriptEngine engine = GraalJSScriptEngine.create(
                    Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build(),
                    Context.newBuilder("js").allowHostAccess(HostAccess.ALL)
            );
            engine.eval("var getName = " + settings.customTypeNamingFunction);
            final Invocable invocable = (Invocable) engine;
            customTypeNamingFunction = invocable.getInterface(CustomTypeNamingFunction.class);
        }
        return customTypeNamingFunction;
    }

    /**
     * Function for getting custom TypeScript names for Java classes.
     * If the function returns null, the default is used.
     * Used by parameter {@link Settings#customTypeNamingFunction} (JavaScript) and {@link Settings#customTypeNamingFunctionImpl} (Java).
     */
    public static interface CustomTypeNamingFunction {
        public Object getName(String className, String classSimpleName);
    }

    public Symbol getSymbolIfImported(Class<?> cls) {
        final Pair<String/*module*/, String/*namespacedName*/> fullNameFromDependency = settings.getModuleDependencies().getFullName(cls);
        if (fullNameFromDependency != null) {
            final Symbol symbol = new Symbol(null);
            symbol.setFullName(fullNameFromDependency.getValue1(), fullNameFromDependency.getValue2());
            return symbol;
        } else {
            return null;
        }
    }

    public static class NameConflictException extends RuntimeException {
        
        private static final long serialVersionUID = 1L;

        public NameConflictException() {
        }

        public NameConflictException(String message) {
            super(message);
        }

        public NameConflictException(String message, Throwable cause) {
            super(message, cause);
        }

        public NameConflictException(Throwable cause) {
            super(cause);
        }

    }

}
