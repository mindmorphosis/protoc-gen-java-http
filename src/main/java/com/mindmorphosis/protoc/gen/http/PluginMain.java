package com.mindmorphosis.protoc.gen.http;

import com.google.api.HttpRule;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos;
import com.google.api.AnnotationsProto;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginMain {
    public static void main(String[] args) {
        try {
            ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
            extensionRegistry.add(AnnotationsProto.http);
            PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(System.in, extensionRegistry);
            PluginProtos.CodeGeneratorResponse.Builder responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder();

            for (String fileToGenerate : request.getFileToGenerateList()) {
                Descriptors.FileDescriptor fileDescriptor = getFileDescriptor(request, fileToGenerate);

                String protoContent = generateProtoContent(fileDescriptor);

                PluginProtos.CodeGeneratorResponse.File.Builder fileBuilder = PluginProtos.CodeGeneratorResponse.File.newBuilder()
                        .setName(fileDescriptor.getName() + ".java")
                        .setContent(protoContent);
                responseBuilder.addFile(fileBuilder);
            }

            PluginProtos.CodeGeneratorResponse response = responseBuilder.build();
            response.writeTo(System.out);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading CodeGeneratorRequest or writing CodeGeneratorResponse: " + e.getMessage());
            System.exit(1);
        }
    }
    private static Descriptors.FileDescriptor getFileDescriptor(PluginProtos.CodeGeneratorRequest request, String fileToGenerate) {
        for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : request.getProtoFileList()) {
            if (fileDescriptorProto.getName().equals(fileToGenerate)) {
                try {
                    return Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0]);
                } catch (Descriptors.DescriptorValidationException e) {
                    System.err.println("Error building FileDescriptor from FileDescriptorProto: " + e.getMessage());
                    System.exit(1);
                }
            }
        }
        return null;
    }

    private static String generateProtoContent(Descriptors.FileDescriptor fileDescriptor) {
        StringBuilder content = new StringBuilder();
        if(!fileDescriptor.getPackage().isEmpty()){

            // package packageName;
            String packageGen = String.format("package %s;\n\n",fileDescriptor.getPackage());
            content.append(packageGen);

            // import package
            content.append("import org.springframework.web.bind.annotation.*;\n");
            content.append("import ").append(fileDescriptor.getPackage()).append(".*;\n\n");

            // serviceInterface
            for (Descriptors.ServiceDescriptor services : fileDescriptor.getServices()){
                String serviceName = genServiceName(services.getName());
                content.append("interface ").append(serviceName).append(" {\n");
                // type method
                for (Descriptors.MethodDescriptor method : services.getMethods()){
                    content.append("    ").append(genInterfaceMethod(method));
                }
                content.append("}\n\n");
            }

            // controller annotate
            content.append("@ResponseBody\n");
            content.append("@RestController\n");
            content.append("public class").append(" ").append(fileDescriptor.getFullName().split("\\.")[0]).append("Controller ").append("{\n");

            // genAutowired
            for (Descriptors.ServiceDescriptor services : fileDescriptor.getServices()){
                content.append(genAutowired(services)).append("\n");
            }

            // controller method
            for (Descriptors.ServiceDescriptor services : fileDescriptor.getServices()){
                for (Descriptors.MethodDescriptor method : services.getMethods()){
                    HttpRule rule = method.getOptions().getExtension(AnnotationsProto.http);
                    //gen http annotate
                    String annotate = "    @%sMapping(\"%s\")";
                    if(!rule.getGet().equals("")){
                        annotate = String.format(annotate, "Get", rule.getGet());
                    }else if (!rule.getDelete().equals("")){
                        annotate = String.format(annotate, "Delete", rule.getDelete());
                    }else if (!rule.getPost().equals("")){
                        annotate = String.format(annotate, "Post", rule.getPost());
                    }else if(!rule.getPut().equals("")){
                        annotate = String.format(annotate, "Put", rule.getPut());
                    }
                    content.append(annotate).append("\n");
                }
            }
            content.append("\n}");
        }
        return content.toString();
    }

    // 生成方法
    private static String genInterfaceMethod(Descriptors.MethodDescriptor m) {
        String url = getMethodUrl(m);
        StringBuilder parameterList = new StringBuilder();
        if (!url.equals("")){
            Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
            Matcher matcher = pattern.matcher(url);
            while (matcher.find()){
                String parameterName = matcher.group(1);
                for (Descriptors.FieldDescriptor field : m.getInputType().getFields()) {
                    if (field.getName().equals(parameterName)){
                        if (parameterList.length()!=0){
                            parameterList.append(", ");
                        }
                        parameterList.append(field.getType().getJavaType().toString().toLowerCase()).append(" ").append(field.getName());
                    }
                }
            }
        }
        if(parameterList.length()!=0){
            parameterList.append(", ");
        }
        parameterList.append(m.getInputType().getName()).append(" ").append(m.getInputType().getName().toLowerCase());
        return String.format("    %s %s ( %s );\n", m.getOutputType().getName(), m.getName(), parameterList) + "\n";
    }

    private static String genAutowired(Descriptors.ServiceDescriptor services){
        return String.format("    @Autowired\n    %s;\n",genServiceName(services.getName()));
    }

    private static String genServiceName(String name){
        return name+"_Service";
    }

    // 获取rpc方法中的url参数
    private static String getMethodUrl(Descriptors.MethodDescriptor m) {
        HttpRule rule = m.getOptions().getExtension(AnnotationsProto.http);
        if (!rule.getPut().equals("")) {
            return rule.getPut();
        }
        if (!rule.getPost().equals("")) {
            return rule.getPost();
        }
        if (!rule.getGet().equals("")) {
            return rule.getGet();
        }
        if (!rule.getDelete().equals("")) {
            return rule.getDelete();
        }
        return "";
    }
}
