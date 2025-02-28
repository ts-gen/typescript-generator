
package io.github.tsgen.typescript.generator.library;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.hash.HashCode;
import com.google.common.net.HostAndPort;
import com.google.common.net.InternetDomainName;
import io.github.tsgen.typescript.generator.Input;
import io.github.tsgen.typescript.generator.Jackson2ConfigurationResolved;
import io.github.tsgen.typescript.generator.Logger;
import io.github.tsgen.typescript.generator.Settings;
import io.github.tsgen.typescript.generator.TestUtils;
import io.github.tsgen.typescript.generator.TypeScriptGenerator;
import io.github.tsgen.typescript.generator.util.Utils;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
public class GuavaTest {

    @Test
    public void testGuavaInJackson() throws JsonProcessingException {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.registerModule(new GuavaModule());
        final String json = objectMapper.writeValueAsString(new GuavaSerializedClasses());
//        System.out.println(json);
    }

    @Test
    public void testGuava() {
        TypeScriptGenerator.setLogger(new Logger(Logger.Level.Verbose));
        final Settings settings = TestUtils.settings();
        settings.jackson2Configuration = new Jackson2ConfigurationResolved();
        settings.additionalDataLibraries = Arrays.asList("guava");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(GuavaSerializedClasses.class));
        Assertions.assertTrue(output.contains("rangeSet: GuavaRangeSet<string>"));
        Assertions.assertTrue(output.contains("type GuavaRangeSet<C>"));
        Assertions.assertTrue(output.contains("range: GuavaRange<Named>"));
        Assertions.assertTrue(output.contains("type GuavaRange<C>"));
        Assertions.assertTrue(output.contains("table: GuavaTable<boolean>"));
        Assertions.assertTrue(output.contains("type GuavaTable<V>"));
        Assertions.assertTrue(output.contains("hostAndPort: string"));
        Assertions.assertTrue(output.contains("internetDomainName: string"));
        Assertions.assertTrue(output.contains("cacheBuilderSpec: string"));
        Assertions.assertTrue(output.contains("cacheBuilder: string"));
        Assertions.assertTrue(output.contains("hashCode: string"));
        Assertions.assertTrue(output.contains("fluentIterable: string[]"));
        Assertions.assertTrue(output.contains("multimap: GuavaMultimap<number>"));
        Assertions.assertTrue(output.contains("type GuavaMultimap<V>"));
    }

    private static class GuavaSerializedClasses {
        public ImmutableRangeSet<String> rangeSet = ImmutableRangeSet.of(Range.closedOpen("a", "d"));
        public Range<Named> range = Range.closedOpen(new Named("a"), new Named("f"));
        public Table<Character, Integer, Boolean> table = ImmutableTable.<Character, Integer, Boolean>builder()
                .put(Tables.immutableCell('a', 1, false))
                .put(Tables.immutableCell('b', 3, true))
                .build();
        public NamedTable namedTable = new NamedTable();
        public HostAndPort hostAndPort = HostAndPort.fromParts("habarta.cz", 80);
        public InternetDomainName internetDomainName = InternetDomainName.from("habarta.cz");
        public CacheBuilderSpec cacheBuilderSpec = CacheBuilderSpec.parse("initialCapacity=5,expireAfterWrite=60s");
        public CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                .initialCapacity(5)
                .expireAfterWrite(60, TimeUnit.SECONDS);
        public HashCode hashCode = HashCode.fromInt(45);
        public FluentIterable<String> fluentIterable = FluentIterable.of("a", "b", "c");
        public Multimap<String, Integer> multimap = ImmutableMultimap.of("a", 1, "b", 2);
    }

    private static class Named implements Comparable<Named> {
        public final String name;

        public Named(String name) {
            this.name = name;
        }

        @Override
        public int compareTo(Named o) {
            return this.name.compareTo(o.name);
        }
    }

    private static class NamedTable extends ForwardingTable<String, String, Named> {
        @Override
        protected Table<String, String, Named> delegate() {
            return ImmutableTable.<String, String, Named>builder()
                .put(Tables.immutableCell("a", "1", new Named("a1")))
                .put(Tables.immutableCell("b", "3", new Named("b3")))
                .build();
        }
    }

}
