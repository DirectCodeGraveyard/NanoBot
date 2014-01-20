package org.nanobot.test

import org.junit.Test
import org.nanobot.Utils

import static groovy.util.GroovyTestCase.assertEquals

class UtilsTest {
    @Test
    void encodeJSON() {
        def expected = "{\n" +
                "    \"string\": \"String\",\n" +
                "    \"array\": [\n" +
                "        \"Array\",\n" +
                "        \"Of\",\n" +
                "        \"Stuff\"\n" +
                "    ],\n" +
                "    \"object\": {\n" +
                "        \"objects\": \"consist\",\n" +
                "        \"of\": \"keys\",\n" +
                "        \"and\": \"values\"\n" +
                "    }\n" +
                "}"
        def actual = Utils.encodeJSON([
                string: "String",
                array: [
                        "Array",
                        "Of",
                        "Stuff"
                ],
                object: [
                        objects: "consist",
                        of: "keys",
                        and: "values"
                ]
        ])
        assertEquals(expected, actual)
    }

    @Test
    void parseJSON() {
        def expected = [
                string: "String",
                array: [
                        "Array",
                        "Of",
                        "Stuff"
                ],
                object: [
                        objects: "consist",
                        of: "keys",
                        and: "values"
                ]
        ]
        def actual = Utils.parseJSON(
                "{\n" +
                "    \"string\": \"String\",\n" +
                "    \"array\": [\n" +
                "        \"Array\",\n" +
                "        \"Of\",\n" +
                "        \"Stuff\"\n" +
                "    ],\n" +
                "    \"object\": {\n" +
                "        \"objects\": \"consist\",\n" +
                "        \"of\": \"keys\",\n" +
                "        \"and\": \"values\"\n" +
                "    }\n" +
                "}"
        )
        assertEquals(expected, actual)
    }

    @Test
    void parseXML() {
        def expected = 'root[attributes={}; value=[test[attributes={}; value=[Works]]]]'
        def actual = Utils.parseXML("<root><test>Works</test></root>")
        assertEquals(expected, actual.toString())
    }
}
