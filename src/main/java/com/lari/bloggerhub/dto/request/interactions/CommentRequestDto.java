package com.lari.bloggerhub.dto.request.interactions;

import java.util.StringJoiner;

public class CommentRequestDto {

    String content;
    String postId;
    String parentId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CommentRequestDto.class.getSimpleName() + "[", "]")
                .add("content='" + content + "'")
                .add("postId='" + postId + "'")
                .add("parentId='" + parentId + "'")
                .toString();
    }
}
