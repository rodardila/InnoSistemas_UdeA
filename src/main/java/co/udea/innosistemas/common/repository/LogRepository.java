package co.udea.innosistemas.common.repository;

import co.udea.innosistemas.common.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface LogRepository extends JpaRepository<Log, Integer> {

    /**
     * Encuentra todos los logs de un usuario específico
     */
    List<Log> findByUserIdOrderByTimestampDesc(Integer userId);

    /**
     * Encuentra logs por evento específico
     */
    List<Log> findByEventNameOrderByTimestampDesc(String eventName);

    /**
     * Encuentra logs de eventos relacionados con teams
     */
    @Query("SELECT l FROM Log l WHERE l.eventName LIKE 'TEAM_%' ORDER BY l.timestamp DESC")
    List<Log> findTeamRelatedLogsOrderByTimestampDesc();

    /**
     * Encuentra logs en un rango de fechas
     */
    List<Log> findByTimestampBetweenOrderByTimestampDesc(OffsetDateTime start, OffsetDateTime end);

    /**
     * Encuentra logs de un usuario para eventos específicos de teams
     */
    @Query("SELECT l FROM Log l WHERE l.user.id = :userId AND l.eventName LIKE 'TEAM_%' ORDER BY l.timestamp DESC")
    List<Log> findUserTeamLogsOrderByTimestampDesc(@Param("userId") Integer userId);
}