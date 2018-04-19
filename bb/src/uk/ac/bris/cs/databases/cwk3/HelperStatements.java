package uk.ac.bris.cs.databases.cwk3;

import uk.ac.bris.cs.databases.api.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HelperStatements {
    /**
     * Check if an item exists in a table
     * @param c
     * @param table Table to check
     * @param column Column to check
     * @param id id to check
     * @return
     * @throws SQLException
     */
    static boolean itemExists(Connection c, String table, String column, Long id) throws SQLException {
        // check if topic exists
        String STMT_1 = "SELECT * FROM " +
                table +
                " WHERE " +
                column +
                " = ?";
        PreparedStatement p = c.prepareStatement(STMT_1);
        p.setLong(1, id);
        ResultSet r = p.executeQuery();

        return r.next();
    }

    /**
     * Get a person's id
     * @param c
     * @param username
     * @return
     * @throws SQLException
     */
    static long getPersonId(Connection c, String username) throws SQLException {
        String STMT_1 = "SELECT id FROM Person WHERE name = ?";


        PreparedStatement p = c.prepareStatement(STMT_1);
        p.setString(1, username);
        ResultSet r = p.executeQuery();

        if (r.next()) {
            return r.getLong("id");
        }

        return -1;
    }

    /**
     * like an item
     * @param c
     * @param table Table of likes, currenty TopicLikes or PostLikes
     * @param column
     * @param itemTable Table of the item, currently Topic or Post
     * @param itemId
     * @param username
     * @param like
     * @return
     */
    static Result like(Connection c, String table, String column, String itemTable, Long itemId, String username, boolean like) {
        if (table == null || table.isEmpty()) {
            return Result.failure("empty table");
        }
        if (column == null || column.isEmpty()) {
            return Result.failure("empty column");
        }
        if (username == null || username.isEmpty()) {
            return Result.failure("empty username");
        }

        // check if item exists
        try {
            if (!HelperStatements.itemExists(c, itemTable, column, itemId)) {
                return Result.failure("item not exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // retrieve person id if exists
        Long personId;
        try {
            personId = HelperStatements.getPersonId(c, username);
            if (personId == -1) {
                return Result.failure("user not exists");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // perform action
        String STMT;
        if (like) {
            STMT = "INSERT INTO " +
                    table  +
                    "(" + column.toLowerCase() + ", person_id) " +
                    "VALUES (?, ?)";
        } else {
            STMT = "DELETE FROM " + table + " " +
                    "WHERE " + column + " = ? AND person_id = ?";
        }

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, itemId);
            p.setLong(2, personId);
            p.executeQuery();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }

        return Result.success("success");
    }
}
