package ru.hogwarts.school;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserInfo> findUsersByName(String firstName, String lastName) {
        String sql = "SELECT DISTINCT u.id, u.first_name, u.last_name FROM users u WHERE u.first_name LIKE ? AND u.last_name LIKE ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new UserInfo(rs.getString("id"), rs.getString("first_name"), rs.getString("last_name")),
                firstName + "%", lastName + "%");
    }

    public Optional<UserInfo> findUserById(String userId) {
        String sql = "SELECT id, first_name, last_name FROM users WHERE id = ?";
        try {
            UserInfo user = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                            new UserInfo(rs.getString("id"), rs.getString("first_name"), rs.getString("last_name")),
                    userId);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}