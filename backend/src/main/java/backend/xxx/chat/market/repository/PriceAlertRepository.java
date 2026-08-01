package backend.xxx.chat.market.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    @Query("""
            select alert
            from PriceAlert alert
            join fetch alert.asset
            left join fetch alert.pair
            where lower(alert.user.username) = lower(:username)
            order by alert.createdAt desc, alert.id desc
            """)
    List<PriceAlert> findAllByUser_UsernameIgnoreCaseOrderByCreatedAtDescIdDesc(
            @Param("username") String username
    );

    @Query("""
            select alert
            from PriceAlert alert
            join fetch alert.asset
            left join fetch alert.pair
            where lower(alert.user.username) = lower(:username)
                and alert.id = :id
            """)
    Optional<PriceAlert> findByUser_UsernameIgnoreCaseAndId(
            @Param("username") String username,
            @Param("id") Long id
    );

    @Query("""
            select alert
            from PriceAlert alert
            join fetch alert.user
            join fetch alert.asset
            join fetch alert.pair
            where alert.active = true
            """)
    List<PriceAlert> findAllByActiveTrueWithDetails();

    @Query("""
            select alert
            from PriceAlert alert
            join fetch alert.user
            join fetch alert.asset
            join fetch alert.pair pair
            where pair.id = :pairId
                and alert.active = true
            """)
    List<PriceAlert> findAllByPair_IdAndActiveTrueWithDetails(@Param("pairId") Long pairId);

    @Query("""
            select alert
            from PriceAlert alert
            join fetch alert.user
            join fetch alert.asset
            join fetch alert.pair pair
            where pair.id in :pairIds
                and alert.active = true
            """)
    List<PriceAlert> findAllByPairIdInAndActiveTrueWithDetails(
            @Param("pairIds") Collection<Long> pairIds
    );

    @Query("""
            select alert
            from PriceAlert alert
            join fetch alert.user
            join fetch alert.asset
            join fetch alert.pair
            where alert.id in :ids
                and alert.active = true
            """)
    List<PriceAlert> findAllByIdInAndActiveTrueWithDetails(
            @Param("ids") Collection<Long> ids
    );

    @Query("""
            select alert
            from PriceAlert alert
            join fetch alert.user
            join fetch alert.asset
            join fetch alert.pair
            where alert.id = :id
                and alert.active = true
            """)
    Optional<PriceAlert> findByIdAndActiveTrueWithDetails(@Param("id") Long id);
}
