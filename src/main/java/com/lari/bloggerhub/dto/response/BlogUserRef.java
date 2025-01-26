package com.lari.bloggerhub.dto.response;

import java.util.StringJoiner;

public class BlogUserRef {
    String id;
    String username;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BlogUserRef.class.getSimpleName() + "[", "]")
                .add("userId='" + id + "'")
                .add("username='" + username + "'")
                .toString();
    }
}
