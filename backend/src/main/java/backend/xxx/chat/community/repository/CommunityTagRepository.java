package backend.xxx.chat.community.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.community.model.CommunityTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityTagRepository extends JpaRepository<CommunityTag, Long> {

    List<CommunityTag> findAllByActiveTrueOrderByNameAsc();

    Optional<CommunityTag> findBySlugAndActiveTrue(String slug);

    List<CommunityTag> findBySlugInAndActiveTrue(Collection<String> slugs);
}
