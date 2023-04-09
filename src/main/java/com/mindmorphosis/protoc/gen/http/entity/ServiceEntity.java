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
        Set<String> importContentLine = new LinkedHashSet<>(imports);

        List<String> methodContentLine = new LinkedList<>();
        methods.forEach(method -> {
            // 如果是同一个proto文件生成的类型则直接import后使用短类型名
            String inputClassname = method.getInputClassFullName();
            int inputLastIndex = method.getInputClassFullName().lastIndexOf(".");
            if (inputLastIndex != -1) {
                int packIndex = method.getInputClassFullName().lastIndexOf(".", inputLastIndex - 1);
                String inputClassPrefix = (packIndex == -1) ?
                        method.getInputClassFullName() : method.getInputClassFullName().substring(0, packIndex);
                if (inputClassPrefix.equals(pack)) {
                    importContentLine.add(String.format(Template.IMPORT, method.getInputClassFullName()));
                    inputClassname = method.getInputClassFullName().substring(inputLastIndex + 1);
                }
            }

            int index = inputClassname.lastIndexOf(".");
            String inputParameterName = (index == -1) ?
                    StringUtil.toCamelCase(inputClassname) : StringUtil.toCamelCase(inputClassname.substring(index + 1));

            // 同上
            String outputClassname = method.getOutputClassFullName();
            int outputLastIndex = method.getOutputClassFullName().lastIndexOf(".");
            if (outputLastIndex != -1) {
                int packIndex = method.getOutputClassFullName().lastIndexOf(".", outputLastIndex - 1);
                String outputClassPrefix = (packIndex == -1) ?
                        method.getOutputClassFullName() : method.getOutputClassFullName().substring(0, packIndex);
                if (outputClassPrefix.equals(pack)) {
                    importContentLine.add(String.format(Template.IMPORT, method.getOutputClassFullName()));
                    outputClassname = method.getOutputClassFullName().substring(outputLastIndex + 1);
                }
            }

            methodContentLine.add(String.format(
                    Template.SERVICE_METHOD,
                    outputClassname,
                    method.getMethodName(),
                    inputClassname + " " + inputParameterName
            ));
            methodContentLine.add("");
        });
        String classContent = methodContentLine.stream().map(s -> "    " + s).collect(Collectors.joining("\n"));

        StringBuilder content = new StringBuilder();
        content.append(String.format(Template.PACKAGE, pack));
        content.append("\n\n");
        if (!importContentLine.isEmpty()) {
            content.append(String.join("\n", importContentLine));
            content.append("\n\n");
        }
        content.append(String.format(Template.SERVICE_INTERFACE, classname, classContent));

        return content.toString();
    }
}
