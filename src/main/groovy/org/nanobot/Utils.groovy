package org.nanobot

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer

class Utils {
    private static JSON = new JsonSlurper()
    private static XML = new XmlParser()

    static def parseJSON(String input) {
        return JSON.parseText(input)
    }

    static def parseXML(String input) {
        return XML.parseText(input)
    }

    static def encodeJSON(Object object, boolean pretty = true) {
        def builder = new JsonBuilder()
        builder(object)

        if (pretty)
            return builder.toPrettyString()
        else
            return builder.toString()
    }

    static Script parseConfig(File file) {
        def cc = new CompilerConfiguration()
        def scc = new SecureASTCustomizer()
        scc.with {
            closuresAllowed = false
            methodDefinitionAllowed = false
            importsWhitelist = []
            staticImportsWhitelist = []
            starImportsWhitelist = []
            constantTypesClassesWhiteList = [
                    Integer,
                    GString,
                    String,
                    Float,
                    Double,
                    Long,
                    Float.TYPE,
                    Double.TYPE,
                    Long.TYPE,
                    int,
                    long,
                    float,
                    double,
                    Object,
                    Boolean.TYPE,
                    Boolean,
                    boolean
            ].asImmutable()
        }

        cc.addCompilationCustomizers(scc)

        return new GroovyShell(cc).parse(file.newReader(), "NanoBotConfig")
    }

    static def runScript(String text, binding) {
        if (binding instanceof Map)
            binding = binding as Binding
        def shell = new GroovyShell(binding)

        return shell.parse(text).run()
    }
}