package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.document.BlogPost;
import com.lari.bloggerhub.dto.response.BlogUserRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BlogPostRepository extends MongoRepository<BlogPost, String> {

  List<BlogPost> findAllByCreatedBy(BlogUserRef userRef);

  List<BlogPost> findAllByCreatedBy_Id(String userId);

  BlogPost getByBlogPostId(String blogPostId);

  /**
   * Case-insensitive substring search across title and description, for the explore/search feed.
   *
   * <p>Uses {@code Containing} (a regex scan) rather than a Mongo text index on purpose. A text
   * index matches whole stemmed words only, so a search-as-you-type box would return nothing for
   * "reac" until the user finished typing "react" — the opposite of what the debounced search input
   * on the explore page needs. At this collection's scale the regex scan is not a concern; if the
   * post count ever makes it one, the fix is a text index plus a separate "search" mode, not a
   * change here.
   */
  Page<BlogPost> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
      String title, String description, Pageable pageable);
}
