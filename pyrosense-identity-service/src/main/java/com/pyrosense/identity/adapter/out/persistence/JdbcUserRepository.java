package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.UserRepository;
import com.pyrosense.identity.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbc;

    public JdbcUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User save(User user) {
        int updated = jdbc.update("""
            UPDATE users SET email = ?, full_name = ?, status = ?,
                failed_login_attempts = ?, last_login_at = ?, locked_until = ?, updated_at = NOW()
            WHERE id = ?
            """,
                user.getEmail(), user.getFullName(), user.getStatus().name(),
                user.getFailedLoginAttempts(), toTimestamp(user.getLastLoginAt()),
                toTimestamp(user.getLockedUntil()),
                user.getId().value().toString()
        );

        if (updated == 0) {
            jdbc.update("""
                INSERT INTO users (id, email, full_name, status, failed_login_attempts,
                    last_login_at, locked_until, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                    user.getId().value().toString(), user.getEmail(), user.getFullName(),
                    user.getStatus().name(), user.getFailedLoginAttempts(),
                    toTimestamp(user.getLastLoginAt()), toTimestamp(user.getLockedUntil()),
                    toTimestamp(user.getCreatedAt())
            );
        }

        saveMemberships(user);
        return user;
    }

    private void saveMemberships(User user) {
        for (Membership m : user.getMemberships()) {
            int membershipUpdated = jdbc.update("""
                UPDATE memberships SET roles = ?, active = ?
                WHERE id = ?
                """,
                    rolesToString(m.getRoles()), m.isActive(), m.getId().toString()
            );

            if (membershipUpdated == 0) {
                jdbc.update("""
                    INSERT INTO memberships (id, user_id, tenant_id, roles, active, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                        m.getId().toString(), m.getUserId().value().toString(),
                        m.getTenantId().value().toString(), rolesToString(m.getRoles()),
                        m.isActive(), toTimestamp(m.getCreatedAt())
                );
            }
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        List<User> users = jdbc.query(
                "SELECT * FROM users WHERE id = ?",
                userRowMapper(), id.value().toString()
        );
        if (users.isEmpty()) return Optional.empty();
        return Optional.of(loadMemberships(users.get(0)));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        List<User> users = jdbc.query(
                "SELECT * FROM users WHERE email = ?",
                userRowMapper(), email
        );
        if (users.isEmpty()) return Optional.empty();
        return Optional.of(loadMemberships(users.get(0)));
    }

    @Override
    public List<User> findByTenantId(TenantId tenantId) {
        List<User> users = jdbc.query("""
            SELECT DISTINCT u.* FROM users u
            INNER JOIN memberships m ON m.user_id = u.id
            WHERE m.tenant_id = ? AND m.active = TRUE
            """,
                userRowMapper(), tenantId.value().toString()
        );
        users.replaceAll(this::loadMemberships);
        return users;
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email
        );
        return count != null && count > 0;
    }

    private User loadMemberships(User user) {
        List<Membership> memberships = jdbc.query(
                "SELECT * FROM memberships WHERE user_id = ?",
                membershipRowMapper(), user.getId().value().toString()
        );
        return User.reconstitute(
                user.getId(), user.getEmail(), user.getFullName(), user.getStatus(),
                memberships, user.getFailedLoginAttempts(),
                user.getLastLoginAt(), user.getLockedUntil(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> User.reconstitute(
                new UserId(UUID.fromString(rs.getString("id"))),
                rs.getString("email"),
                rs.getString("full_name"),
                UserStatus.valueOf(rs.getString("status")),
                Collections.emptyList(),
                rs.getInt("failed_login_attempts"),
                toInstant(rs.getTimestamp("last_login_at")),
                toInstant(rs.getTimestamp("locked_until")),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<Membership> membershipRowMapper() {
        return (rs, rowNum) -> Membership.reconstitute(
                UUID.fromString(rs.getString("id")),
                new UserId(UUID.fromString(rs.getString("user_id"))),
                new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                stringToRoles(rs.getString("roles")),
                rs.getBoolean("active"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private static String rolesToString(Set<Role> roles) {
        return roles.stream().map(Role::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private static Set<Role> stringToRoles(String rolesStr) {
        if (rolesStr == null || rolesStr.isBlank()) return EnumSet.noneOf(Role.class);
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        for (String r : rolesStr.split(",")) {
            roles.add(Role.valueOf(r.trim()));
        }
        return roles;
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
