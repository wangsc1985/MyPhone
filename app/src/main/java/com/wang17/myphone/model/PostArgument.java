package com.wang17.myphone.model;

public class PostArgument {
    public String name;
    public String value;

    public PostArgument(String name, Object value) {
        this.name = name;
        this.value = value.toString();
    }
}
