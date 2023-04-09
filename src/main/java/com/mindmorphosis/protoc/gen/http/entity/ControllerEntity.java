package com.mindmorphosis.protoc.gen.http.entity;

import com.mindmorphosis.protoc.gen.http.Template;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class ControllerEntity {

    private String pack;

    private String classname;

    /**
     * 导入的包
     */
    private Set<String> imports;

    /**
     * 类型属性(自动注入)
     */
    private Set<String> properties;

    private List<ControllerMethodEntity> methods;

    public ControllerEntity() {
        this.pack = "";
        this.classname = "";
        this.imports = new LinkedHashSet<>();
        this.properties = new LinkedHashSet<>();
        this.methods = new LinkedList<>();
    }

    public void addImport(String classPath) {
        this.imports.add(String.format(Template.IMPORT, classPath));
    }

    public void addProperty(String className, String propertyName) {
        this.properties.add(String.format(Template.DEPENDENCY_INJECTION, className, propertyName));
    }

    public void addMethod(ControllerMethodEntity method) {
        this.methods.add(method);
    }

    public String generate() {
        List<String> propertyContentLine = new LinkedList<>();
        properties.forEach(property -> {
            List<String> lines = Arrays.asList(property.split("\n"));
            propertyContentLine.addAll(lines);
            propertyContentLine.add("");
        });
        String propertyContent = propertyContentLine.stream().map(s -> "    " + s).collect(Collectors.joining("\n"));

        List<String> methodContentLine = new LinkedList<>();
        methods.forEach(method -> {
            List<String> lines = Arrays.asList(method.generate().split("\n"));
            methodContentLine.addAll(lines);
            methodContentLine.add("");
        });
        String methodContent = methodContentLine.stream().map(s -> "    " + s).collect(Collectors.joining("\n"));

        String classContent = "\n" + propertyContent + "\n\n" + methodContent;

        StringBuilder content = new StringBuilder();
        content.append(String.format(Template.PACKAGE, pack));
        content.append("\n\n");
        content.append(String.join("\n", imports));
        content.append("\n\n");
        content.append(String.format(Template.CONTROLLER_CLASS, classname, classContent));

        return content.toString();
    }
}
