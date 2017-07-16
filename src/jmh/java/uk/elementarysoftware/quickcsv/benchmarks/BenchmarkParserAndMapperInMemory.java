package uk.elementarysoftware.quickcsv.benchmarks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import uk.elementarysoftware.quickcsv.api.CSVParser;
import uk.elementarysoftware.quickcsv.api.CSVParserBuilder;

@BenchmarkMode(Mode.AverageTime)
@Fork(1)
@Warmup(iterations = 3, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 7000, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class BenchmarkParserAndMapperInMemory {
    
    private static final String TEST_FILE = "src/test/resources/cities-unix.txt"; 
    private static final String TEST_FILE_QUOTED = "src/test/resources/cities-unix-quoted.txt"; 
    
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        
        byte[] content = loadFile(prepareFile(100, TEST_FILE));
        
        byte[] quotedContent = loadFile(prepareFile(100, TEST_FILE_QUOTED));
        
        private File prepareFile(int sizeMultiplier, String testFile) {
            try {
                byte[] content= FileUtils.readFileToByteArray(new File(testFile));
                File result = File.createTempFile("csv", "large");
                for (int i = 0; i < sizeMultiplier; i++) {
                    FileUtils.writeByteArrayToFile(result, content, true);
                }
                return result;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private byte[] loadFile(File file) {
            try {
                return FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
    
    @Benchmark
    public void benchmarkParallelParser(BenchmarkState state, Blackhole bh) {
        CSVParser<City> parser = CSVParserBuilder.aParser(City.MAPPER).build();
        Stream<City> stream = parser.parse(new ByteArrayInputStream(state.content));
        stream.forEach(c -> bh.consume(c));
    }

    @Benchmark
    public void benchmarkParallelParserWithHeader(BenchmarkState state, Blackhole bh) {
        CSVParser<City> parser = CSVParserBuilder
                .aParser(City.EnumMapper.MAPPER, City.EnumMapper.Fields.class)
                .usingExplicitHeader("Country", "City", "AccentCity", "Region", "Population", "Latitude", "Longitude")
                .build();
        Stream<City> stream = parser.parse(new ByteArrayInputStream(state.content));
        stream.forEach(c -> bh.consume(c));
    }
    
    @Benchmark
    public void benchmarkSequentialParser(BenchmarkState state, Blackhole bh) {
        CSVParser<City> parser = CSVParserBuilder.aParser(City.MAPPER).build();
        Stream<City> stream = parser.parse(new ByteArrayInputStream(state.content));
        stream.sequential().forEach(c -> bh.consume(c));
    }
    

    @Benchmark
    public void benchmarkSequentialParserWithQuotes(BenchmarkState state, Blackhole bh) {
        CSVParser<City> parser = CSVParserBuilder.aParser(City.MAPPER).build();
        Stream<City> stream = parser.parse(new ByteArrayInputStream(state.quotedContent));
        stream.sequential().forEach(c -> bh.consume(c));
    }
    
    @Benchmark
    public void benchmarkOpenCSVParser(BenchmarkState state, Blackhole bh) {
        OpenCSVParser parser = new OpenCSVParser();
        Stream<City> stream = parser.parse(new ByteArrayInputStream(state.content));
        stream.forEach(c -> bh.consume(c));
    }
    
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(".*" + BenchmarkParserAndMapperInMemory.class.getSimpleName()+".*")
            //.addProfiler(LinuxPerfAsmProfiler.class)
            //.addProfiler(StackProfiler.class)
            .build();
        new Runner(opt).run();
    }

}