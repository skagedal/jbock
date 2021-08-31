package net.jbock.parse;

import net.jbock.util.ExToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegularParserTest {

    @Test
    void testZeroParamsFail() {
        RegularParser<String> parser = RegularParser.create(Map.of(), Map.of(), 0);
        assertThrows(ExToken.class, () -> parser.parse(List.of("1")));
    }

    @Test
    void testZeroParamsSuccessEmpty() throws ExToken {
        RegularParser<String> parser = RegularParser.create(Map.of(), Map.of(), 0);
        parser.parse(List.of());
        assertTrue(parser.rest().toList().isEmpty());
        assertTrue(parser.option("a").findAny().isEmpty());
        assertTrue(parser.param(0).isEmpty());
    }

    @Test
    void testZeroParamsSuccessEscape() throws ExToken {
        RegularParser<String> parser = RegularParser.create(Map.of(), Map.of(), 0);
        parser.parse(List.of("--"));
        assertTrue(parser.option("a").findAny().isEmpty());
        assertTrue(parser.param(0).isEmpty());
    }

    @Test
    void testOneParamsFail() {
        RegularParser<String> parser = RegularParser.create(Map.of(), Map.of(), 1);
        assertThrows(ExToken.class, () -> parser.parse(List.of("1", "2")));
    }

    @Test
    void testOneParamSuccess() throws ExToken {
        RegularParser<String> parser = RegularParser.create(Map.of(), Map.of(), 1);
        parser.parse(List.of("1"));
        assertTrue(parser.option("a").findAny().isEmpty());
        assertEquals(Optional.of("1"), parser.param(0));
    }

    @Test
    void testOneUnixOptionAttached() throws ExToken {
        Map<String, String> optionNames = Map.of("-a", "A");
        Map<String, OptionState> optionStates = Map.of("A", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("-a1"));
        assertEquals(List.of("1"), parser.option("A").toList());
    }

    @Test
    void testOneUnixOptionDetached() throws ExToken {
        Map<String, String> optionNames = Map.of("-a", "A");
        Map<String, OptionState> optionStates = Map.of("A", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("-a", "1"));
        assertEquals(List.of("1"), parser.option("A").toList());
    }

    @Test
    void testOneGnuOptionAttached() throws ExToken {
        Map<String, String> optionNames = Map.of("--alpha", "A");
        Map<String, OptionState> optionStates = Map.of("A", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("--alpha=1"));
        assertEquals(List.of("1"), parser.option("A").toList());
    }

    @Test
    void testOneGnuOptionDetached() throws ExToken {
        Map<String, String> optionNames = Map.of("--alpha", "A");
        Map<String, OptionState> optionStates = Map.of("A", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("--alpha", "1"));
        assertEquals(List.of("1"), parser.option("A").toList());
    }

    @Test
    void testTwoUnixOptionsAttached() throws ExToken {
        Map<String, String> optionNames = Map.of("-a", "A", "-b", "B");
        Map<String, OptionState> optionStates = Map.of(
                "A", new OptionStateModeFlag(),
                "B", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("-a", "-b1"));
        assertTrue(parser.option("A").findAny().isPresent());
        assertEquals(List.of("1"), parser.option("B").toList());
    }

    @Test
    void testTwoUnixOptionsDetached() throws ExToken {
        Map<String, String> optionNames = Map.of("-a", "A", "-b", "B");
        Map<String, OptionState> optionStates = Map.of(
                "A", new OptionStateModeFlag(),
                "B", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("-a", "-b", "1"));
        assertTrue(parser.option("A").findAny().isPresent());
        assertEquals(List.of("1"), parser.option("B").toList());
    }

    @Test
    void testTwoUnixOptionsClusteringAttached() throws ExToken {
        Map<String, String> optionNames = Map.of("-a", "A", "-b", "B");
        Map<String, OptionState> optionStates = Map.of(
                "A", new OptionStateModeFlag(),
                "B", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("-ab1"));
        assertTrue(parser.option("A").findAny().isPresent());
        assertEquals(List.of("1"), parser.option("B").toList());
    }

    @Test
    void testTwoUnixOptionsClusteringDetached() throws ExToken {
        Map<String, String> optionNames = Map.of("-a", "A", "-b", "B");
        Map<String, OptionState> optionStates = Map.of(
                "A", new OptionStateModeFlag(),
                "B", new OptionStateNonRepeatable());
        RegularParser<String> parser = RegularParser.create(optionNames, optionStates, 0);
        parser.parse(List.of("-ab", "1"));
        assertTrue(parser.option("A").findAny().isPresent());
        assertEquals(List.of("1"), parser.option("B").toList());
    }
}