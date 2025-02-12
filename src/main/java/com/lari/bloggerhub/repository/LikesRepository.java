package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.Likes;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LikesRepository extends MongoRepository<Likes, String> {

    Likes findByPostId(String postId);

    List<Likes> getLikesByUserId(String userId);

    List<Likes> getLikesByPostId(String blogPostId);
}
