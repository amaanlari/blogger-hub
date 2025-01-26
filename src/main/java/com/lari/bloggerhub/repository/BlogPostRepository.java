package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.BlogPost;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BlogPostRepository extends MongoRepository<BlogPost, String> {

  List<BlogPost> findAllByCreatedBy_Username(String username);

  List<BlogPost> findAllByCreatedBy_Id(String userId);
}
