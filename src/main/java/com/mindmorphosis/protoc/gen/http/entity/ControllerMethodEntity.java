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
     * 如果body是子类, 则保存set是的函数名称
     */
    private String bodySetterName;

    /**
     * 是否有body
     */
    private boolean hasBody = false;

    /**
     * body是否为入参
     */
    private boolean inputBody = false;

    private HttpEntity httpEntity;

    public void setBody(String bodyClassname, String bodySetterName, String bodyParameterName) {
        this.bodyClassFullName = bodyClassname;
        this.bodyParameterName = bodyParameterName;
        this.bodySetterName = bodySetterName;
        this.hasBody = true;
        this.inputBody = false;
    }

    public void setBody(String bodyClassname, String bodySetterName) {
        this.bodyClassFullName = bodyClassname;
        this.bodyParameterName = "body";
        this.bodySetterName = bodySetterName;
        this.hasBody = true;
        this.inputBody = true;
    }

    public String generate() {
        List<String> controllerParameters = new LinkedList<>();
        httpEntity.getParameters().forEach((urlParameter, urlClassname) -> {
            controllerParameters.add(String.format(
                    Template.ANNOTATIONS_PATH_VARIABLE, urlParameter, urlClassname, urlParameter + "_"));
        });
        String bodyContent = "";
        // 有body并且body不为空
        boolean emptyBody = !hasBody || bodyClassFullName.equals("com.google.protobuf.Empty");
        if (hasBody && !emptyBody) {
            bodyContent = String.format(Template.ANNOTATIONS_REQUEST_BODY, bodyClassFullName, "_" + bodyParameterName);
            controllerParameters.add(bodyContent);
        }
        String fullParameter = String.join(", ", controllerParameters);

        // 处理六种情况
        // 1. 没有body, 没有url参数
        // 2. 没有body, 有url参数
        // 3. 有body, body是*, 没有url参数, body的名字应该是: _body
        // 4. 有body, body是*, 有url参数, body的名字应该是: _body
        // 5. 有body, body是子类型, 没有url参数, body的名字应该是: _bodyParameterName
        // 6. 有body, body是子类型, 有url参数, body的名字应该是: _bodyParameterName

        List<String> methodContentLine = new LinkedList<>();

        String inputParameterName = "_input";
        boolean hasServiceInput = !inputClassFullName.equals("com.google.protobuf.Empty");
        boolean hasServiceOutput = !outputClassFullName.equals("com.google.protobuf.Empty");

        if (hasServiceInput) {
            if (hasBody && !emptyBody && inputBody) {
                // 有body，并且是*body(body就是入参)
                if (httpEntity.getParameters().size() > 0) {
                    methodContentLine.add(String.format("%s _input = _body.toBuilder()", bodyClassFullName));
                } else {
                    inputParameterName = "_body";
                }
            } else {
                // 没body，或者子类body
                methodContentLine.add(String.format("%s _input = %s.newBuilder()", inputClassFullName, inputClassFullName));
                if (hasBody && !emptyBody) {
                    methodContentLine.add(String.format("        .set%s(_%s)", bodySetterName, bodyParameterName));
                }
            }

            // set url参数
            httpEntity.getParameters().forEach((urlParameter, urlClassname) -> {
                methodContentLine.add(String.format(
                        "        .set%s(%s_)", StringUtil.toPascalCase(urlParameter), urlParameter));
            });

            // 有url参数或者(有body, body是子类) 构建新的实体
            if (httpEntity.getParameters().size() > 0 || !inputBody) {
                methodContentLine.add("        .build();");
            }
        }

        methodContentLine.add(String.format(
                Template.CONTROLLER_RETURN,
                hasServiceOutput ? "return " : "",
                "__" + serviceName,
                methodName,
                hasServiceInput ? inputParameterName : ""));

        String methodContent = methodContentLine.stream().map(s -> "    " + s).collect(Collectors.joining("\n"));

        return String.format(Template.CONTROLLER_METHOD,
                httpEntity.getType(),
                httpEntity.getUrl(),
                hasServiceOutput ? outputClassFullName : "void",
                methodName,
                fullParameter,
                methodContent);
    }
}
