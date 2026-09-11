package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.Comments;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CommentsRepository extends MongoRepository<Comments, String> {
    long countCommentsByPostId(String postId);

    void deleteCommentsByParentId(String parentId);

    /**
     * All comments on a post, oldest first.
     *
     * <p>The return type was previously a bare {@code Object}, which made Spring Data treat this as
     * a single-result query: a post with no comments returned {@code null}, one comment returned the
     * bare document rather than a list, and two or more threw
     * {@code IncorrectResultSizeDataAccessException} ("returned non unique result") — surfacing as a
     * 500 and making the endpoint unusable on any post with a real conversation. Declaring the
     * collection return type is what makes zero, one and many behave consistently.
     */
    List<Comments> findCommentsByPostId(String postId, Sort sort);
}
