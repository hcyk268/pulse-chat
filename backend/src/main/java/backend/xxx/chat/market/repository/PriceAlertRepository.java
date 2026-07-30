package backend.xxx.chat.market.repository;

import java.util.List;
import java.util.Optional;

import backend.xxx.chat.market.model.PriceAlert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    @EntityGraph(attributePaths = {"asset", "pair"})
    List<PriceAlert> findAllByUser_UsernameIgnoreCaseOrderByCreatedAtDescIdDesc(String username);

    @EntityGraph(attributePaths = {"asset", "pair"})
    Optional<PriceAlert> findByUser_UsernameIgnoreCaseAndId(String username, Long id);
}
