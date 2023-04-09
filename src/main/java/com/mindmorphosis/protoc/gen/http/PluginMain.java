package com.mindmorphosis.protoc.gen.http;

import com.google.api.HttpRule;
import com.google.common.base.Strings;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos;
import com.google.api.AnnotationsProto;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mindmorphosis.protoc.gen.http.entity.ControllerEntity;
import com.mindmorphosis.protoc.gen.http.entity.ControllerMethodEntity;
import com.mindmorphosis.protoc.gen.http.entity.HttpEntity;
import com.mindmorphosis.protoc.gen.http.entity.ServiceEntity;
import com.mindmorphosis.protoc.gen.http.util.StringUtil;

public class PluginMain {
    public static void main(String[] args) throws IOException, Descriptors.DescriptorValidationException {
        ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
        extensionRegistry.add(AnnotationsProto.http);
        PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(System.in, extensionRegistry);

        for (String fileToGenerate : request.getFileToGenerateList()) {
            Descriptors.FileDescriptor descriptor = getFileDescriptor(request, fileToGenerate);
            if (descriptor == null) {
                throw new RuntimeException("无法解析");
            }

            String pack = descriptor.getOptions().getJavaPackage();
            String classname = descriptor.getOptions().getJavaOuterClassname();

            pack = (Strings.isNullOrEmpty(pack)) ? descriptor.getPackage() : pack;
            if (Strings.isNullOrEmpty(pack)) {
                throw new RuntimeException("包路径为空");
            }

            ControllerEntity controllerEntity = buildController(descriptor, pack, classname);
            ServiceEntity serviceEntity = buildService(pack, classname, controllerEntity.getMethods());

            generateController(descriptor, pack, classname, controllerEntity);
            generateService(descriptor, pack, classname, serviceEntity);
        }
    }

    public static void generateController(Descriptors.FileDescriptor descriptor,
                                          String pack, String protoEntityClassname,
                                          ControllerEntity controllerEntity) throws IOException {

        PluginProtos.CodeGeneratorResponse.Builder responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder();
        PluginProtos.CodeGeneratorResponse.File.Builder fileBuilder = PluginProtos.CodeGeneratorResponse.File.newBuilder()
                .setName(pack.replace(".", File.separator) +
                        File.separator +
                        StringUtil.toPascalCase(protoEntityClassname) +
                        "Controller.java")
                .setContent(String.format(Template.PROTO_GEN_CONTENT, descriptor.getName(), controllerEntity.generate()));

        responseBuilder.addFile(fileBuilder);
        PluginProtos.CodeGeneratorResponse response = responseBuilder.build();
        response.writeTo(System.out);
    }

    public static void generateService(Descriptors.FileDescriptor descriptor,
                                       String pack, String protoEntityClassname,
                                       ServiceEntity serviceEntity) throws IOException {

        PluginProtos.CodeGeneratorResponse.Builder responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder();
        PluginProtos.CodeGeneratorResponse.File.Builder fileBuilder = PluginProtos.CodeGeneratorResponse.File.newBuilder()
                .setName(pack.replace(".", File.separator) +
                        File.separator +
                        StringUtil.toPascalCase(protoEntityClassname) +
                        "Service.java")
                .setContent(String.format(Template.PROTO_GEN_CONTENT, descriptor.getName(), serviceEntity.generate()));

        responseBuilder.addFile(fileBuilder);
        PluginProtos.CodeGeneratorResponse response = responseBuilder.build();
        response.writeTo(System.out);
    }

    public static ServiceEntity buildService(String pack, String protoEntityClassname,
                                             List<ControllerMethodEntity> methods) {

        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.setPack(pack);
        serviceEntity.setClassname(protoEntityClassname + "Service");
        serviceEntity.setMethods(methods);

        return serviceEntity;
    }

    public static ControllerEntity buildController(Descriptors.FileDescriptor descriptor,
                                                   String pack, String protoEntityClassname) {

        ControllerEntity controllerEntity = new ControllerEntity();
        controllerEntity.setPack(pack);
        controllerEntity.setClassname(protoEntityClassname + "Controller");

        controllerEntity.addImport("org.springframework.beans.factory.annotation.Autowired");
        controllerEntity.addImport("org.springframework.web.bind.annotation.*");

        controllerEntity.addProperty(
                pack + "." + protoEntityClassname + "Service",
                "__" + StringUtil.toCamelCase(protoEntityClassname + "Service"));

        List<Descriptors.ServiceDescriptor> services = descriptor.getServices();
        for (Descriptors.ServiceDescriptor service : services) {
            List<Descriptors.MethodDescriptor> methods = service.getMethods();
            for (Descriptors.MethodDescriptor method : methods) {
                if (!method.getOptions().hasExtension(AnnotationsProto.http)) {
                    continue;
                }
                ControllerMethodEntity controllerMethodEntity = buildControllerMethod(method, pack, protoEntityClassname);
                controllerEntity.addMethod(controllerMethodEntity);
            }
        }

        return controllerEntity;
    }

    public static ControllerMethodEntity buildControllerMethod(Descriptors.MethodDescriptor method,
                                                               String pack, String protoEntityClassname) {

        HttpRule rule = method.getOptions().getExtension(AnnotationsProto.http);
        String bodyType = rule.getBody();
        if (!rule.getGet().isBlank() && !bodyType.isBlank()) {
            throw new RuntimeException("HTTP GET 请求不应该包含请求体");
        }

        ControllerMethodEntity methodEntity = new ControllerMethodEntity();
        String classPrefix = pack + "." + protoEntityClassname;
        String inputClassFullName = getTypeClassname(method.getInputType(), classPrefix);
        String outputClassFullName = getTypeClassname(method.getOutputType(), classPrefix);

        methodEntity.setMethodName(StringUtil.toCamelCase(method.getName()));
        methodEntity.setServiceName(StringUtil.toCamelCase(protoEntityClassname + "Service"));
        methodEntity.setInputClassFullName(inputClassFullName);
        methodEntity.setOutputClassFullName(outputClassFullName);
        methodEntity.setHttpEntity(buildHttp(method, pack));

        if (!bodyType.isBlank()) {
            if (bodyType.equals("*")) {
                methodEntity.setBody(inputClassFullName);
            } else {
                List<Descriptors.FieldDescriptor> fields = method.getInputType().getFields();
                for (Descriptors.FieldDescriptor field : fields) {
                    if (Strings.isNullOrEmpty(field.getName())) {
                        continue;
                    }
                    if (field.getName().equals(bodyType)) {
                        methodEntity.setBody(
                                String.join(".", classPrefix, StringUtil.toPascalCase(field.getName())),
                                StringUtil.toCamelCase(field.getName())
                        );
                        break;
                    }
                }
            }
            if (Strings.isNullOrEmpty(methodEntity.getBodyClassFullName())) {
                throw new RuntimeException("未找到匹配的body类型");
            }
        }

        return methodEntity;
    }

    public static HttpEntity buildHttp(Descriptors.MethodDescriptor method, String pack) {
        HttpRule rule = method.getOptions().getExtension(AnnotationsProto.http);
        String httpType = "";
        String fullUrl = "";
        if (!rule.getGet().isBlank()) {
            httpType = "Get";
            fullUrl = rule.getGet();
        } else if (!rule.getDelete().isBlank()) {
            httpType = "Delete";
            fullUrl = rule.getDelete();
        } else if (!rule.getPost().isBlank()) {
            httpType = "Post";
            fullUrl = rule.getPost();
        } else if (!rule.getPut().isBlank()) {
            httpType = "Put";
            fullUrl = rule.getPut();
        } else if (!rule.getPatch().isBlank()) {
            httpType = "Patch";
            fullUrl = rule.getPatch();
        }

        if (Strings.isNullOrEmpty(fullUrl)) {
            throw new RuntimeException("空的URL");
        }

        // 处理url中'&'前面的部分
        // 规则: '&'为url中的特殊符号, 代表参数, 正常部分不会出现'&'
        //       当url出现'&'时表示'&'之前为路径之后为参数
        String url = null;
        int index = fullUrl.indexOf('&');  // 查找 & 字符的位置
        if (index == -1) {  // 如果不存在 & 字符，则返回整个字符串
            url = fullUrl;
        } else {
            url = fullUrl.substring(0, index);  // 返回 & 字符之前的部分
        }

        // 检查完整url中所有参数名称和参数类型
        Set<String> parameterNames = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("\\{([^}]+)}");
        Matcher matcher = pattern.matcher(fullUrl);
        while (matcher.find()) {
            String name = matcher.group(1);
            boolean res = parameterNames.add(name);
            if (!res) {
                throw new RuntimeException(String.format("出现重复的URL参数 %s", name));
            }
        }
        Map<String, String> parameters = buildUrlParameter(method.getInputType(), pack, parameterNames);

        HttpEntity httpEntity = new HttpEntity();
        httpEntity.setType(httpType);
        httpEntity.setUrl(url);
        httpEntity.setParameters(parameters);

        return httpEntity;
    }

    public static Map<String, String> buildUrlParameter(
            Descriptors.Descriptor type, String pack, Set<String> parameterNames) {

        Map<String, String> urlParameters = new LinkedHashMap<>();
        List<Descriptors.FieldDescriptor> fields = type.getFields();
        for (Descriptors.FieldDescriptor field : fields) {
            // 解析url与类型关系
            String name = field.getName();
            if (Strings.isNullOrEmpty(name)) {
                continue;
            }
            if (!parameterNames.contains(name)) {
                continue;
            }
            String urlClassname = switch (field.getJavaType()) {
                case INT -> "Integer";
                case LONG -> "Long";
                case FLOAT -> "Float";
                case DOUBLE -> "Double";
                case BOOLEAN -> "Boolean";
                case STRING -> "String";
                case BYTE_STRING -> "com.google.protobuf.ByteString";
                case MESSAGE, ENUM -> {
                    if (isImportType(field.getFile())) {
                        // 获取消息类型所在的包名
                        String packageName = field.getMessageType().getFile().getOptions().getJavaPackage();
                        packageName = (Strings.isNullOrEmpty(packageName)) ?
                                field.getMessageType().getFile().getPackage() : packageName;
                        if (Strings.isNullOrEmpty(packageName)) {
                            throw new RuntimeException("缺少引入类型");
                        }
                        // 获取完整的消息类型路径
                        yield packageName + "." + field.getMessageType().getName();
                    }
                    yield pack + "." + field.getMessageType().getName();
                }
            };
            urlParameters.put(field.getName(), urlClassname);
        }
        return urlParameters;
    }

    public static String getTypeClassname(Descriptors.Descriptor type, String classPrefix) {
        if (isImportType(type.getFile())) {
            // 获取消息类型所在的包名
            String packageName = type.getFile().getOptions().getJavaPackage();
            packageName = (Strings.isNullOrEmpty(packageName)) ? type.getFile().getPackage() : packageName;
            if (Strings.isNullOrEmpty(packageName)) {
                throw new RuntimeException("缺少引入类型");
            }
            // 获取完整的消息类型路径
            return packageName + "." + StringUtil.toPascalCase(type.getName());
        }
        return classPrefix + "." + StringUtil.toPascalCase(type.getName());
    }

    public static boolean isImportType(Descriptors.FileDescriptor file) {
        boolean isImported = false;
        String fileName = file.getName(); // 获取字段所在的 .proto 文件名
        for (String dependency : file.toProto().getDependencyList()) {
            if (dependency.endsWith(fileName)) {
                isImported = true;
                break;
            }
        }
        return isImported;
    }

    private static Descriptors.FileDescriptor getFileDescriptor(
            PluginProtos.CodeGeneratorRequest request, String fileToGenerate)
            throws Descriptors.DescriptorValidationException {

        for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : request.getProtoFileList()) {
            if (fileDescriptorProto.getName().equals(fileToGenerate)) {
                return Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0]);
            }
        }
        return null;
    }
}
