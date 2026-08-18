package backend.xxx.chat.community.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.community.model.Community;
import backend.xxx.chat.community.model.CommunityStatus;
import backend.xxx.chat.community.model.CommunityVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    @Query("""
            select community.slug
            from Community community
            where community.slug = :baseSlug
                or community.slug like concat(:baseSlug, '-%')
            """)
    List<String> findSlugsByBase(@Param("baseSlug") String baseSlug);

    @Query("""
            from Community community
            join fetch community.owner
            left join fetch community.category
            left join fetch community.avatarAsset
            left join fetch community.coverAsset
            left join fetch community.defaultChannel
            where community.slug = :slug
            """)
    Optional<Community> findBySlugWithDetails(@Param("slug") String slug);

    @Query("""
            from Community community
            join fetch community.owner
            left join fetch community.category
            left join fetch community.avatarAsset
            left join fetch community.coverAsset
            left join fetch community.defaultChannel
            where community.id = :communityId
            """)
    Optional<Community> findByIdWithDetails(@Param("communityId") Long communityId);

    @Query("""
            select community
            from Community community
            join fetch community.owner
            left join fetch community.category category
            left join fetch community.avatarAsset
            left join fetch community.coverAsset
            left join fetch community.defaultChannel
            where community.status = :status
                and (:visibility is null or community.visibility = :visibility)
                and (:categorySlug is null or category.slug = :categorySlug)
                and (:tagSlug is null or exists (
                    select 1
                    from CommunityTagLink link
                    where link.community = community
                        and link.tag.slug = :tagSlug
                        and link.tag.active = true
                ))
                and (:searchPattern is null
                    or lower(community.name) like :searchPattern
                    or lower(community.description) like :searchPattern
                )
            order by community.memberCount desc, community.id desc
            """)
    List<Community> findDiscoverable(
            @Param("status") CommunityStatus status,
            @Param("visibility") CommunityVisibility visibility,
            @Param("categorySlug") String categorySlug,
            @Param("tagSlug") String tagSlug,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
