
package io.github.tsgen.typescript.generator;

import io.github.tsgen.typescript.generator.compiler.ModelCompiler;
import io.github.tsgen.typescript.generator.emitter.Emitter;
import io.github.tsgen.typescript.generator.emitter.InfoJsonEmitter;
import io.github.tsgen.typescript.generator.emitter.NpmPackageJson;
import io.github.tsgen.typescript.generator.emitter.NpmPackageJsonEmitter;
import io.github.tsgen.typescript.generator.emitter.TsModel;
import io.github.tsgen.typescript.generator.parser.GsonParser;
import io.github.tsgen.typescript.generator.parser.Jackson2Parser;
import io.github.tsgen.typescript.generator.parser.JsonbParser;
import io.github.tsgen.typescript.generator.parser.Model;
import io.github.tsgen.typescript.generator.parser.ModelParser;
import io.github.tsgen.typescript.generator.parser.RestApplicationParser;
import io.github.tsgen.typescript.generator.util.Utils;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TypeScriptGenerator {

    public static final String Version = getVersion();

    private static Logger logger = new Logger();

    private final Settings settings;
    private TypeProcessor commonTypeProcessor = null;
    private ModelParser modelParser = null;
    private ModelCompiler modelCompiler = null;
    private Emitter emitter = null;
    private InfoJsonEmitter infoJsonEmitter = null;
    private NpmPackageJsonEmitter npmPackageJsonEmitter = null;

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        TypeScriptGenerator.logger = logger;
    }

    public TypeScriptGenerator() {
        this (new Settings());
    }

    public TypeScriptGenerator(Settings settings) {
        this.settings = settings;
        settings.validate();
    }

    public static void printVersion() {
        TypeScriptGenerator.getLogger().info("Running TypeScriptGenerator version " + Version);
    }

    public String generateTypeScript(Input input) {
        final StringWriter stringWriter = new StringWriter();
        generateTypeScript(input, Output.to(stringWriter));
        return stringWriter.toString();
    }

    public void generateTypeScript(Input input, Output output) {
        final Model model = getModelParser().parseModel(input.getSourceTypes());
        final TsModel tsModel = getModelCompiler().javaToTypeScript(model);
        generateTypeScript(tsModel, output);
        generateInfoJson(tsModel, output);
        generateNpmPackageJson(output);
    }

    private void generateTypeScript(TsModel tsModel, Output output) {
        getEmitter().emit(tsModel, output.getWriter(), output.getName(), output.shouldCloseWriter());
    }

    private void generateInfoJson(TsModel tsModel, Output output) {
        if (settings.generateInfoJson) {
            if (output.getName() == null) {
                throw new RuntimeException("Generating info JSON can only be used when output is specified using file name");
            }
            final File outputFile = new File(output.getName());
            final Output out = Output.to(new File(outputFile.getParent(), "typescript-generator-info.json"));
            getInfoJsonEmitter().emit(tsModel, out.getWriter(), out.getName(), out.shouldCloseWriter());
        }
    }

    private void generateNpmPackageJson(Output output) {
        if (settings.generateNpmPackageJson) {
            if (output.getName() == null) {
                throw new RuntimeException("Generating NPM package.json can only be used when output is specified using file name");
            }
            final File outputFile = new File(output.getName());
            final Output npmOutput = Output.to(new File(outputFile.getParent(), "package.json"));
            final NpmPackageJson npmPackageJson = new NpmPackageJson();
            npmPackageJson.name = settings.npmName;
            npmPackageJson.version = settings.npmVersion;
            npmPackageJson.types = outputFile.getName();
            npmPackageJson.dependencies = new LinkedHashMap<>();
            npmPackageJson.devDependencies = new LinkedHashMap<>();
            npmPackageJson.peerDependencies = new LinkedHashMap<>();
            if (settings.moduleDependencies != null) {
                for (ModuleDependency dependency : settings.moduleDependencies) {
                    if (dependency.peerDependency) {
                        npmPackageJson.peerDependencies.put(dependency.npmPackageName, dependency.npmVersionRange);
                    } else {
                        npmPackageJson.dependencies.put(dependency.npmPackageName, dependency.npmVersionRange);
                    }
                }
            }
            if (settings.outputFileType == TypeScriptFileType.implementationFile) {
                npmPackageJson.types = Utils.replaceExtension(outputFile, ".d.ts").getName();
                npmPackageJson.main = Utils.replaceExtension(outputFile, ".js").getName();
                npmPackageJson.dependencies.putAll(settings.npmPackageDependencies);
                npmPackageJson.devDependencies.putAll(settings.npmDevDependencies);
                npmPackageJson.peerDependencies.putAll(settings.npmPeerDependencies);
                final String typescriptVersion = settings.npmTypescriptVersion != null ? settings.npmTypescriptVersion : settings.typescriptVersion;
                npmPackageJson.devDependencies.put("typescript", typescriptVersion);
                final String npmBuildScript = settings.npmBuildScript != null
                        ? settings.npmBuildScript
                        : "tsc --module umd --moduleResolution node --typeRoots --target es5 --lib es6 --declaration --sourceMap $outputFile";
                final String build = npmBuildScript.replaceAll(Pattern.quote("$outputFile"), outputFile.getName());
                npmPackageJson.scripts = Collections.singletonMap("build", build);
            }
            if (npmPackageJson.dependencies.isEmpty()) {
                npmPackageJson.dependencies = null;
            }
            if (npmPackageJson.devDependencies.isEmpty()) {
                npmPackageJson.devDependencies = null;
            }
            if (npmPackageJson.peerDependencies.isEmpty()) {
                npmPackageJson.peerDependencies = null;
            }
            getNpmPackageJsonEmitter().emit(npmPackageJson, npmOutput.getWriter(), npmOutput.getName(), npmOutput.shouldCloseWriter());
        }
    }

    public TypeProcessor getCommonTypeProcessor() {
        if (commonTypeProcessor == null) {
            final List<RestApplicationParser.Factory> restFactories = settings.getRestApplicationParserFactories();
            final ModelParser.Factory modelParserFactory = getModelParserFactory();
            final List<TypeProcessor> specificTypeProcessors = Stream
                    .concat(
                            restFactories.stream().map(RestApplicationParser.Factory::getSpecificTypeProcessor),
                            Stream.of(modelParserFactory.getSpecificTypeProcessor())
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            commonTypeProcessor = createTypeProcessor(specificTypeProcessors);
        }
        return commonTypeProcessor;
    }

    private TypeProcessor createTypeProcessor(List<TypeProcessor> specificTypeProcessors) {
        final List<TypeProcessor> processors = new ArrayList<>();
        processors.add(new ExcludingTypeProcessor(settings.getExcludeFilter()));
        if (settings.customTypeProcessor != null) {
            processors.add(settings.customTypeProcessor);
        }
        processors.add(new CustomMappingTypeProcessor(settings.getValidatedCustomTypeMappings()));
        processors.addAll(specificTypeProcessors);
        processors.add(new DefaultTypeProcessor(settings.getLoadedDataLibraries()));
        return new TypeProcessor.Chain(processors);
    }

    public ModelParser getModelParser() {
        if (modelParser == null) {
            modelParser = createModelParser();
        }
        return modelParser;
    }

    private ModelParser createModelParser() {
        final List<RestApplicationParser.Factory> factories = settings.getRestApplicationParserFactories();
        final List<RestApplicationParser> restApplicationParsers = factories.stream()
                .map(factory -> factory.create(settings, getCommonTypeProcessor()))
                .collect(Collectors.toList());
        return getModelParserFactory().create(settings, getCommonTypeProcessor(), restApplicationParsers);
    }

    private ModelParser.Factory getModelParserFactory() {
        switch (settings.jsonLibrary) {
            case jackson2:
                return new Jackson2Parser.Jackson2ParserFactory();
            case jaxb:
                return new Jackson2Parser.JaxbParserFactory();
            case gson:
                return new GsonParser.Factory();
            case jsonb:
                return new JsonbParser.Factory();
            default:
                throw new RuntimeException();
        }
    }

    public ModelCompiler getModelCompiler() {
        if (modelCompiler == null) {
            modelCompiler = new ModelCompiler(settings, getCommonTypeProcessor());
        }
        return modelCompiler;
    }

    public Emitter getEmitter() {
        if (emitter == null) {
            emitter = new Emitter(settings);
        }
        return emitter;
    }

    public InfoJsonEmitter getInfoJsonEmitter() {
        if (infoJsonEmitter == null) {
            infoJsonEmitter = new InfoJsonEmitter();
        }
        return infoJsonEmitter;
    }

    public NpmPackageJsonEmitter getNpmPackageJsonEmitter() {
        if (npmPackageJsonEmitter == null) {
            npmPackageJsonEmitter = new NpmPackageJsonEmitter();
        }
        return npmPackageJsonEmitter;
    }

    private static String getVersion() {
        try {
            final InputStream inputStream = TypeScriptGenerator.class.getResourceAsStream(
                    "/META-INF/maven/io.github.tsgen.typescript-generator/typescript-generator-core/pom.properties");
            if (inputStream != null) {
                final Properties properties = new Properties();
                properties.load(inputStream);
                return (String) properties.get("version");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
