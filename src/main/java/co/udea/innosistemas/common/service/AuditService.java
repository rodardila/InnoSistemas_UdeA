package co.udea.innosistemas.common.service;

import co.udea.innosistemas.common.model.Log;
import co.udea.innosistemas.common.repository.LogRepository;
import co.udea.innosistemas.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final LogRepository logRepository;

    /**
     * Registra un evento de creación de equipo
     */
    @Transactional
    public void logTeamCreated(User user, Integer teamId, String teamName) {
        String description = String.format("Created team '%s' with ID: %d", teamName, teamId);
        saveLog(user, "TEAM_CREATED", description);
    }

    /**
     * Registra un evento de actualización de equipo
     */
    @Transactional
    public void logTeamUpdated(User user, Integer teamId, String teamName, String changes) {
        String description = String.format("Updated team '%s' (ID: %d). Changes: %s", teamName, teamId, changes);
        saveLog(user, "TEAM_UPDATED", description);
    }

    /**
     * Registra cuando un usuario se une a un equipo
     */
    @Transactional
    public void logUserJoinedTeam(User user, Integer teamId, String teamName) {
        String description = String.format("User joined team '%s' (ID: %d)", teamName, teamId);
        saveLog(user, "TEAM_JOINED", description);
    }

    /**
     * Registra cuando un usuario abandona un equipo
     */
    @Transactional
    public void logUserLeftTeam(User user, Integer teamId, String teamName) {
        String description = String.format("User left team '%s' (ID: %d)", teamName, teamId);
        saveLog(user, "TEAM_LEFT", description);
    }

    /**
     * Registra cuando se agrega un miembro a un equipo (por admin)
     */
    @Transactional
    public void logMemberAdded(User admin, User addedUser, Integer teamId, String teamName) {
        String description = String.format("Admin added user '%s' to team '%s' (ID: %d)",
                addedUser.getName(), teamName, teamId);
        saveLog(admin, "TEAM_MEMBER_ADDED", description);
    }

    /**
     * Registra cuando se remueve un miembro de un equipo (por admin)
     */
    @Transactional
    public void logMemberRemoved(User admin, User removedUser, Integer teamId, String teamName) {
        String description = String.format("Admin removed user '%s' from team '%s' (ID: %d)",
                removedUser.getName(), teamName, teamId);
        saveLog(admin, "TEAM_MEMBER_REMOVED", description);
    }

    /**
     * Registra un evento genérico de equipo
     */
    @Transactional
    public void logTeamEvent(User user, String eventName, String description) {
        saveLog(user, eventName, description);
    }

    /**
     * Obtiene el historial de logs de un usuario
     */
    public List<Log> getUserLogs(Integer userId) {
        return logRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Obtiene logs relacionados con equipos
     */
    public List<Log> getTeamLogs() {
        return logRepository.findTeamRelatedLogsOrderByTimestampDesc();
    }

    /**
     * Obtiene logs de un usuario relacionados con equipos
     */
    public List<Log> getUserTeamLogs(Integer userId) {
        return logRepository.findUserTeamLogsOrderByTimestampDesc(userId);
    }

    /**
     * Obtiene logs en un rango de fechas
     */
    public List<Log> getLogsByDateRange(OffsetDateTime start, OffsetDateTime end) {
        return logRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    /**
     * Método privado para guardar un log
     */
    private void saveLog(User user, String eventName, String description) {
        try {
            Log logEntry = Log.builder()
                    .user(user)
                    .eventName(eventName)
                    .eventDescription(description)
                    .timestamp(OffsetDateTime.now())
                    .build();

            logRepository.save(logEntry);
            log.debug("Audit log saved: {} - {} - {}", eventName, user.getEmail(), description);
        } catch (Exception e) {
            log.error("Failed to save audit log: {} - {} - {}", eventName, user.getEmail(), description, e);
            // No lanzamos la excepción para no afectar el flujo principal
        }
    }
}