package backend.xxx.chat.community.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.community.model.CommunityCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCategoryRepository extends JpaRepository<CommunityCategory, Long> {

    List<CommunityCategory> findAllByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<CommunityCategory> findBySlugAndActiveTrue(String slug);
}
