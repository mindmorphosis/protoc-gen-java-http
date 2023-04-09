package com.mindmorphosis.protoc.gen.http.entity;

import lombok.Data;

import java.util.Map;
@Data
public class HttpEntity {

    private String type;

    private String url;

    private Map<String, String> parameters;
}
