package com.elementarysoftware.quickcsv.integration;

import static org.junit.Assert.*;

import java.io.File;
import java.util.stream.Stream;

import org.junit.Test;

import com.elementarysoftware.quickcsv.api.CSVParserBuilder;
import com.elementarysoftware.quickcsv.parser.simple.StraightForwardParser;
import com.elementarysoftware.quickcsv.sampledomain.City;

public class IntegrationTest {
    
    File inputDos = new File("src/test/resources/cities-dos.txt");
    File inputUnix = new File("src/test/resources/cities-unix.txt");
    
    int[] bufferSizesToTest = new int[] {1024, 11_111, 1_000_000};
    
    
    @Test
    public void testMultiThreaded() throws Exception {
        Stream<City> s1 = new StraightForwardParser().parse(inputDos).map(City.MAPPER);
        Object[] expected = s1.toArray();
        for (int i = 0; i < bufferSizesToTest.length; i++) {
            Stream<City> s2 = CSVParserBuilder.aParser().usingBufferSize(bufferSizesToTest[i]).build().parse(inputDos).map(City.MAPPER);
            assertArrayEquals(expected, s2.toArray());
        }
    }
    
    @Test
    public void testSingleThreaded() throws Exception {
        Stream<City> s1 = new StraightForwardParser().parse(inputDos).map(City.MAPPER);
        Stream<City> s2 = CSVParserBuilder.aParser().build().parse(inputDos).sequential().map(City.MAPPER);
        assertArrayEquals(s1.toArray(), s2.sequential().toArray());
    }
    
    @Test
    public void testDosVsUnix() throws Exception {
        Stream<City> s1 = CSVParserBuilder.aParser().build().parse(inputUnix).map(City.MAPPER);
        Stream<City> s2 = CSVParserBuilder.aParser().build().parse(inputDos).map(City.MAPPER);
        assertArrayEquals(s1.toArray(), s2.sequential().toArray());
    }
    
}