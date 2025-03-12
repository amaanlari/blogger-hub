package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.BlogPost;
import com.lari.bloggerhub.dto.response.BlogUserRef;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BlogPostRepository extends MongoRepository<BlogPost, String> {

  List<BlogPost> findAllByCreatedBy(BlogUserRef userRef);

  List<BlogPost> findAllByCreatedBy_Id(String userId);

  BlogPost getByBlogPostId(String blogPostId);
}
