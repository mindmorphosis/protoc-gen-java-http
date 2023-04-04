package com.mindmorphosis.protoc.gen.http;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.compiler.PluginProtos;
import java.io.IOException;

public class PluginMain {
    public static void main(String[] args) {
        try {
            // 1. 从输入流中读取CodeGeneratorRequest
            PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(System.in);

            // 2. 为每个要生成的.proto文件创建CodeGeneratorResponse
            PluginProtos.CodeGeneratorResponse.Builder responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder();

            for (String fileToGenerate : request.getFileToGenerateList()) {
                Descriptors.FileDescriptor fileDescriptor = getFileDescriptor(request, fileToGenerate);

                // 3. 生成.proto文件
                String protoContent = generateProtoContent(fileDescriptor);

                // 4. 将生成的.proto文件添加到CodeGeneratorResponse
                PluginProtos.CodeGeneratorResponse.File.Builder fileBuilder = PluginProtos.CodeGeneratorResponse.File.newBuilder()
                        .setName(fileToGenerate + ".proto")
                        .setContent(protoContent);
                responseBuilder.addFile(fileBuilder);
            }

            // 5. 将CodeGeneratorResponse写入输出流
            PluginProtos.CodeGeneratorResponse response = responseBuilder.build();
            response.writeTo(System.out);
        } catch (IOException e) {
            System.err.println("Error reading CodeGeneratorRequest or writing CodeGeneratorResponse: " + e.getMessage());
            System.exit(1);
        }
    }
    // 获取生成的文件
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
            content.append("package").append(fileDescriptor.getPackage()).append(";\n\n");
            for(Descriptors.Descriptor message :fileDescriptor.getMessageTypes()){
                for(Descriptors.FieldDescriptor fieldDescriptor : message.getFields()){
                    content.append("    ").append(fieldDescriptor.getType().toString().toLowerCase()).append(" ")
                            .append(fileDescriptor.getName()).append(";").append("\n");

                }
            }
            content.append("}\n\n");
        }
        return content.toString();
    }
}
