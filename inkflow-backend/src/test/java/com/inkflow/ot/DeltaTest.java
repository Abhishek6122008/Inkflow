package com.inkflow.ot;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeltaTest {

    // ── compose ────────────────────────────────────────────────────────────

    private static Delta ins(String s)                              { return new Delta().insert(s, null); }
    private static Delta ins(String s, Map<String, Object> attrs)  { return new Delta().insert(s, attrs); }
    private static Delta ret(int n)                                { return new Delta().retain(n, null); }
    private static Delta ret(int n, Map<String, Object> attrs)     { return new Delta().retain(n, attrs); }

    @Test
    void compose_insertsStack() {
        Delta result = ins("Hello\n").compose(ret(5, null).insert(" World", null));
        assertEquals(List.of(Map.of("insert", "Hello World\n")), result.toJson());
    }

    @Test
    void compose_deleteReducesContent() {
        Delta result = ins("Hello\n").compose(ret(2).delete(3));
        assertEquals(List.of(Map.of("insert", "He\n")), result.toJson());
    }

    @Test
    void compose_attributeApplied() {
        Delta result = ins("Hi\n").compose(ret(2, Map.of("bold", true)));
        assertEquals(List.of(
                Map.of("insert", "Hi", "attributes", Map.of("bold", true)),
                Map.of("insert", "\n")
        ), result.toJson());
    }

    // ── transform ──────────────────────────────────────────────────────────

    @Test
    void transform_concurrentInsertsAtSamePosition_priorityWins() {
        Delta a = ins("X");
        Delta b = ins("Y");
        Delta bPrime = a.transform(b, true);
        Delta result = ins("Hello\n").compose(a).compose(bPrime);
        assertEquals(List.of(Map.of("insert", "XYHello\n")), result.toJson());
    }

    @Test
    void transform_helloWorld_scenario() {
        // Mirrors the live two-client STOMP test result
        Delta opA = ins("Hello");
        Delta opB = ins("World");
        Delta result = ins("\n").compose(opA).compose(opA.transform(opB, true));
        assertEquals(List.of(Map.of("insert", "HelloWorld\n")), result.toJson());
    }

    @Test
    void transform_deleteAndAttributeRetain() {
        Delta opA = ret(1).delete(2);
        Delta opB = ret(4, null).retain(2, Map.of("bold", true));
        Delta result = ins("ABCDEF\n").compose(opA).compose(opA.transform(opB, true));
        // "AD" stays plain; opB bolded positions 4-5 ("EF") of the original,
        // which after opA's delete of positions 1-2 ("BC") land at positions 2-3.
        assertEquals(List.of(
                Map.of("insert", "AD"),
                Map.of("insert", "EF", "attributes", Map.of("bold", true)),
                Map.of("insert", "\n")
        ), result.toJson());
    }

    // ── push / trim ────────────────────────────────────────────────────────

    @Test
    void push_adjacentPlainInsertsWithSameAttributesMerge() {
        Delta d = new Delta().insert("A", null).insert("B", null);
        assertEquals(List.of(Map.of("insert", "AB")), d.toJson());
    }

    @Test
    void trim_removesTrailingPlainRetain() {
        Delta d = ins("Hi").retain(10, null);
        assertEquals(List.of(Map.of("insert", "Hi")), d.trim().toJson());
    }
}
