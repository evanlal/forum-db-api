package uk.ac.bris.cs.databases.cwk3;

import uk.ac.bris.cs.databases.api.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HelperStatements {

    /**
     * Checks if a topic exists.
     * @param topicId
     * @param c
     * @return Success with title if exists, failure otherwise.
     * Fatal on database errors.
     */
    static Result<String> topicExists(Long topicId, Connection c) {
        final String STMT = "SELECT title FROM Topic WHERE topic_id = ?";

        try (PreparedStatement p = c.prepareStatement(STMT)){
            p.setLong(1, topicId);
            ResultSet r = p.executeQuery();

            if (r.next()) {
                return Result.success(r.getString("title"));
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        return Result.failure("Topic does not exist.");
    }

    /**
     * Checks if a forum exists.
     * @param forumId
     * @param c
     * @return Success with title if exists, failure otherwise.
     * Fatal on database errors.
     */
    static Result<String> forumExists(Long forumId, Connection c) {
        final String STMT = "SELECT title FROM  Forum WHERE forum_id = ?";

        try (PreparedStatement p = c.prepareStatement(STMT)){
            p.setLong(1, forumId);
            ResultSet r = p.executeQuery();

            if (r.next()) {
                return Result.success(r.getString("title"));
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        return Result.failure("Forum does not exist.");
    }

    /**
     * Checks if a person exists.
     * @param username
     * @param c
     * @return Success with userId if exists, failure otherwise.
     * Fatal on database errors.
     */
    static Result<Long> getPersonId(String username, Connection c) {
        final String STMT = "SELECT id FROM Person WHERE name = ?";

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();

            if (r.next()) {
                return Result.success(r.getLong("id"));
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        return Result.failure("User does not exist.");
    }
}
