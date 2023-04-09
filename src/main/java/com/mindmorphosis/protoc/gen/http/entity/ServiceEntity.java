package com.mindmorphosis.protoc.gen.http.entity;

import com.mindmorphosis.protoc.gen.http.Template;
import com.mindmorphosis.protoc.gen.http.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class ServiceEntity {

    public String pack;

    public String classname;

    public Set<String> imports;

    public List<ControllerMethodEntity> methods;

    public ServiceEntity() {
        this.pack = "";
        this.classname = "";
        this.imports = new LinkedHashSet<>();
        this.methods = new LinkedList<>();
    }

    public void addImport(String classPath) {
        this.imports.add(String.format(Template.IMPORT, classPath));
    }

    public String generate() {
        List<String> methodContentLine = new LinkedList<>();
        methods.forEach(method -> {
            int lastIndex = method.getInputClassFullName().lastIndexOf(".");
            String inputParameterName = (lastIndex == -1) ?
                    method.getInputClassFullName() : method.getInputClassFullName().substring(lastIndex + 1);
            inputParameterName = StringUtil.toCamelCase(inputParameterName);
            methodContentLine.add(String.format(
                    Template.SERVICE_METHOD,
                    method.getOutputClassFullName(),
                    method.getMethodName(),
                    method.getInputClassFullName() + " " + inputParameterName
                    ));
            methodContentLine.add("");
        });
        String classContent = methodContentLine.stream().map(s -> "    " + s).collect(Collectors.joining("\n"));

        StringBuilder content = new StringBuilder();
        content.append(String.format(Template.PACKAGE, pack));
        content.append("\n\n");
        content.append(String.join("\n", imports));
        content.append("\n\n");
        content.append(String.format(Template.SERVICE_INTERFACE, classname, classContent));
        content.append("\n");

        return content.toString();
    }
}
