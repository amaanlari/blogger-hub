package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.Comments;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentsRepository extends MongoRepository<Comments, String> {
    long countCommentsByPostId(String postId);

    void deleteCommentsByParentId(String parentId);

    Object findCommentsByPostId(String postId);
}
