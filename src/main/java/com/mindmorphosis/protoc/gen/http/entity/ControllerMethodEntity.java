package com.mindmorphosis.protoc.gen.http.entity;

import com.mindmorphosis.protoc.gen.http.Template;
import com.mindmorphosis.protoc.gen.http.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ControllerMethodEntity {

    /**
     * Controller方法名称
     */
    private String methodName;

    /**
     * 注入的Service接口成员变量名称
     */
    private String serviceName;

    /**
     * 入参类型
     */
    private String inputClassFullName;

    /**
     * 出参类型
     */
    private String outputClassFullName;

    /**
     * body类型
     */
    private String bodyClassFullName;

    /**
     * body名称
     */
    private String bodyParameterName;

    /**
     * 是否有body
     */
    private boolean hasBody = false;

    /**
     * body是否为入参
     */
    private boolean inputBody = false;

    private HttpEntity httpEntity;

    public void setBody(String bodyClassname, String bodyParameterName) {
        this.bodyClassFullName = bodyClassname;
        this.bodyParameterName = bodyParameterName;
        this.hasBody = true;
        this.inputBody = false;
    }

    public void setBody(String bodyClassname) {
        this.bodyClassFullName = bodyClassname;
        this.bodyParameterName = "body";
        this.hasBody = true;
        this.inputBody = true;
    }

    public String generate() {
        // 处理六种情况
        // 1. 没有body, 没有url参数
        // 2. 没有body, 有url参数
        // 3. 有body, body是*, 没有url参数, body的名字应该是: _body
        // 4. 有body, body是*, 有url参数, body的名字应该是: _body
        // 5. 有body, body是input, 没有url参数, body的名字应该是: _bodyParameterName
        // 6. 有body, body是input, 有url参数, body的名字应该是: _bodyParameterName
        List<String> controllerParameters = new LinkedList<>();
        httpEntity.getParameters().forEach((urlParameter, urlClassname) -> {
            controllerParameters.add(String.format(
                    Template.ANNOTATIONS_PATH_VARIABLE, urlParameter, urlClassname, urlParameter + "_"));
        });
        String bodyContent = "";
        if (hasBody && !bodyClassFullName.equals("com.google.protobuf.Empty")) {
            bodyContent = String.format(Template.ANNOTATIONS_REQUEST_BODY, bodyClassFullName, "_" + bodyParameterName);
            controllerParameters.add(bodyContent);
        }
        String fullParameter = String.join(", ", controllerParameters);

        List<String> methodContentLine = new LinkedList<>();
        if (hasBody && bodyClassFullName.equals("com.google.protobuf.Empty")){
            methodContentLine.add(String.format(
                    Template.CONTROLLER_VOID,
                    "__" + serviceName,
                    methodName));
        }else{
            if (httpEntity.getParameters().isEmpty()) {
                // 没有url参数
                methodContentLine.add(String.format(
                        Template.CONTROLLER_RETURN,
                        "__" + serviceName,
                        methodName,
                        // 有body时service(传入body), 没有body时service()
                        (hasBody) ? "_" + bodyParameterName : ""));
            } else {
                // 有url参数
                methodContentLine.add(String.format(
                        // 有body时body.toBuilder(), 没有body时class.newBuilder()
                        "%s _input = %s.%sBuilder()",
                        inputClassFullName,
                        (hasBody) ? "_" + bodyParameterName : inputClassFullName,
                        (hasBody) ? "to" : "new"));
                httpEntity.getParameters().forEach((urlParameter, urlClassname) -> {
                    methodContentLine.add(String.format(
                            "        .set%s(%s_)", StringUtil.toPascalCase(urlParameter), urlParameter));
                });
                methodContentLine.add("        .build();");
                methodContentLine.add(String.format(
                        Template.CONTROLLER_RETURN,
                        "__" + serviceName,
                        methodName,
                        "_input"));
            }
        }


        String methodContent = methodContentLine.stream().map(s -> "    " + s).collect(Collectors.joining("\n"));

        return String.format(Template.CONTROLLER_METHOD,
                httpEntity.getType(),
                httpEntity.getUrl(),
                outputClassFullName,
                methodName,
                fullParameter,
                methodContent);
    }
}
