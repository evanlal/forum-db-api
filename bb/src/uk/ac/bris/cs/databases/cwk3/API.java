package uk.ac.bris.cs.databases.cwk3;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.WinNT;
import uk.ac.bris.cs.databases.api.*;

/**
 *
 * @author csxdb
 */
public class API implements APIProvider {

    private final Connection c;
    
    public API(Connection c) {
        this.c = c;
    }

    /* A.1 */
    
    @Override
    public Result<Map<String, String>> getUsers() {
        final String STMT = "SELECT name, username FROM Person";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            ResultSet r = p.executeQuery();
            Map<String,String> map = new HashMap<>();

            while (r.next()) {
                map.put(r.getString("name"), r.getNString("username"));
            }

            return Result.success(map);

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<PersonView> getPersonView(String username) {
        if (username == null || username.isEmpty()) {
            return Result.failure("Username cannot be empty.");
        }

        final String STMT = "SELECT name, username, stuId FROM Person WHERE username = ?";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setString(1,username);
            ResultSet r = p.executeQuery();

            if (!r.next()) {
                return Result.failure("User not found");
            }

            PersonView personView = new PersonView(
                        r.getString("name"),
                        r.getString("username"),
                        r.getString("stuId"));

            return Result.success(personView);

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }
    
    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        if (name == null || name.isEmpty()) {
            return Result.failure("Name cannot be empty.");
        }

        if (username == null || username.isEmpty()) {
            return Result.failure("Username cannot be empty.");
        }

        if (studentId.isEmpty()) {
            return Result.failure("StudentId cannot be empty.");
        }

        // check if user exists
        Result<Long> personExists = HelperStatements.getPersonId(username, c);
        if (personExists.isSuccess()) {
            return Result.failure("User already exists.");
        }

        // create user
        final String STMT_2 = "INSERT INTO Person (name, username, stuId) VALUES (?, ?, ?)";

        try (PreparedStatement p = c.prepareStatement(STMT_2)) {
            p.setString(1, name);
            p.setString(2, username);
            p.setString(3, studentId);

            p.executeQuery();
            c.commit();
            return Result.success();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }
    }
    
    /* A.2 */

    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
        final String STMT = "SELECT forum_id, title FROM Forum";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            ResultSet r = p.executeQuery();

            List<SimpleForumSummaryView> viewList = new ArrayList<>();
            while(r.next()) {
                SimpleForumSummaryView view = new SimpleForumSummaryView(
                        r.getLong("forum_id"),
                        r.getString("title")
                );

                viewList.add(view);
            }

            return Result.success(viewList);

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result createForum(String title) {
        if (title == null || title.isEmpty()) {
            return Result.failure("name cannot be empty");
        }

        // check if forum exists
        final String STMT_1 = "SELECT title FROM Forum WHERE title = ?";

        try (PreparedStatement p = c.prepareStatement(STMT_1)) {
            p.setString(1, title);
            ResultSet r = p.executeQuery();

            if (r.next() ) {
                return Result.failure("A forum with the same title already exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // create forum
        final String STMT_2 = "INSERT INTO Forum (title) VALUES (?)";

        try (PreparedStatement p = c.prepareStatement(STMT_2)) {
            p.setString(1, title);

            p.executeQuery();
            c.commit();
            return Result.success();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }
    }
 
    /* A.3 */
 
    @Override
    public Result<List<ForumSummaryView>> getForums() {
        // Get all forums
        final String STMT_1 = "SELECT forum_id, title FROM Forum";

        // Get last topic in which a post was made for a forum
        final String STMT_2 =
                "SELECT Topic.topic_id, Topic.title, MAX(Post.posted_at) AS lastpost FROM Topic " +
                        "JOIN Post ON Topic.topic_id = Post.topic_id " +
                        "WHERE Topic.forum_id = ? " +
                        "GROUP BY Topic.topic_id, Topic.title " +
                        "ORDER BY lastpost DESC " +
                        "LIMIT 1";

        try (PreparedStatement p = c.prepareStatement(STMT_1);
        PreparedStatement p2 = c.prepareStatement(STMT_2)) {

            ResultSet r = p.executeQuery();
            List<ForumSummaryView> forumSummaryViews = new ArrayList<>();

            // For each forum get the last topic in which a post was made
            while(r.next()) {
                Long forumId = r.getLong("forum_id");
                String forumTitle = r.getString("title");

                p2.setLong(1, forumId);
                ResultSet r2 = p2.executeQuery();

                SimpleTopicSummaryView topicSummaryView = null;
                if (r2.next()) {
                    topicSummaryView = new SimpleTopicSummaryView(
                            r2.getLong("Topic.topic_id"),
                            forumId,
                            r2.getString("Topic.title")
                    );
                }


                ForumSummaryView forumView = new ForumSummaryView(
                        forumId,
                        forumTitle,
                        topicSummaryView
                );

                forumSummaryViews.add(forumView);
            }

            return Result.success(forumSummaryViews);

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }
    
    @Override
    public Result<ForumView> getForum(long id) {
        // Check if forum exists
        Result forumExists =   HelperStatements.forumExists(id, c);
        if (!forumExists.isSuccess()) {
            return forumExists;
        }
        String forumName = forumExists.getValue().toString();

        // Get topics
        final String STMT = "SELECT topic_id, title FROM Topic " +
                "WHERE forum_id = ? " +
                "ORDER BY title ASC";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, id);
            ResultSet r = p.executeQuery();

            List<SimpleTopicSummaryView> topicSummaryViews = new ArrayList<>();
            while(r.next()) {
                SimpleTopicSummaryView topicSummaryView = new SimpleTopicSummaryView(
                        r.getLong("topic_id"),
                        id,
                        r.getString("title")
                );

                topicSummaryViews.add(topicSummaryView);
            }

            ForumView forumView = new ForumView(id, forumName,topicSummaryViews);
            return Result.success(forumView);

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
        // Check if topic exists
        Result topicExists =   HelperStatements.topicExists(topicId, c);
        if (!topicExists.isSuccess()) {
            return topicExists;
        }
        String topicTitle = topicExists.getValue().toString();

        // Get the posts of a topic in the order they were created
        final String STMT = "SELECT Post.post_number, Person.name, Post.text, Post.posted_at FROM Post " +
                "JOIN Person ON Person.id = Post.person_id " +
                "WHERE Post.topic_id = ? " +
                "ORDER BY Post.posted_at ASC";

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            ResultSet r = p.executeQuery();

            List<SimplePostView> simplePostViews = new ArrayList<>();
            while (r.next()){
                SimplePostView simplePostView = new SimplePostView(
                        r.getInt("Post.post_number"),
                        r.getString("Person.name"),
                        r.getString("Post.text"),
                        r.getTimestamp("Post.posted_at").toString()
                );
                simplePostViews.add(simplePostView);
            }

            return Result.success(new SimpleTopicView(topicId, topicTitle, simplePostViews));

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }    
    
    @Override
    public Result<PostView> getLatestPost(long topicId) {
        // check if topic exists
        Result topicExists =   HelperStatements.topicExists(topicId, c);
        if (!topicExists.isSuccess()) {
            return topicExists;
        }

        // TODO CHECK
        final String STMT = "SELECT Post.post_id, Post.forum_id, Post.post_number, Post.posted_at, Post.text, " +
                "Post.total_likes, Person.name, Person.username FROM Post " +
                "JOIN Person ON Post.person_id = Person.id " +
                "WHERE Post.topic_id = ? " +
                "ORDER BY Post.posted_at DESC " +
                "LIMIT 1";

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            ResultSet r = p.executeQuery();

            if(r.next()) {
                PostView postView = new PostView(r.getLong("Post.forum_id"),
                        topicId,
                        r.getInt("Post.post_number"),
                        r.getString("Person.name"),
                        r.getString("Person.username"),
                        r.getString("Post.test"),
                        r.getString("Post.posted_at"),
                        r.getInt("Post.total_likes")
                        );
                return Result.success(postView);
            }
            return Result.failure("There are no posts in this tipic.");

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result createPost(long topicId, String username, String text) {
        if (username == null || username.isEmpty()) {
            return Result.failure("Username cannot be empty.");
        }
        if (text == null || text.isEmpty()) {
            return Result.failure("Text cannot be empty.");
        }

        // Don't check if topic exists, if wrong topicId it will violate the foreign constraint
        // with the appropriate constraint error message

        // Retrieve person id if exists
        Result<Long> personExists = HelperStatements.getPersonId(username, c);
        if (!personExists.isSuccess()) {
            return personExists;
        }
        Long personId = personExists.getValue();

        // Create post
        final String STMT = "INSERT INTO Post(topic_id, person_id, posted_at, text) VALUES(?, ?, ?, ?)";

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            p.setLong(2, personId);
            p.setTimestamp(3, Timestamp.from(Instant.now()));
            p.setString(4, text);
            p.executeQuery();
            c.commit();

            return Result.success();

        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }

            return Result.fatal(e.getMessage());
        }

    }
     
    @Override
    public Result createTopic(long forumId, String username, String title, String text) {
        if (username == null || username.isEmpty()) {
            return Result.failure("Username cannot be empty.");
        }
        if (title == null || title.isEmpty()) {
            return Result.failure("Title cannot be empty.");
        }
        if (text == null || text.isEmpty()) {
            return Result.failure("Text cannot be empty.");
        }

        // Don't check if forum exists, if wrong forumId it will violate the foreign constraint
        // with the appropriate constraint error message

        // Check if person exists and retrieve person id
        Result<Long> personExists = HelperStatements.getPersonId(username, c);
        if (!personExists.isSuccess()) {
            return personExists;
        }
        Long personId = personExists.getValue();

        // Create topic
        String STMT_3 = "INSERT INTO Topic(forum_id, person_id, title, text) VALUES(?, ?, ?, ?)";

        try(PreparedStatement p = c.prepareStatement(STMT_3)) {
            p.setLong(1, forumId);
            p.setLong(2, personId);
            p.setString(3, title);
            p.setString(4, text);
            p.executeQuery();
            c.commit();

            return Result.success();

        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }

            return Result.fatal(e.getMessage());
        }
    }
    
    @Override
    public Result<Integer> countPostsInTopic(long topicId) {
        // check if topic exists
        Result topicExists =   HelperStatements.topicExists(topicId, c);
        if (!topicExists.isSuccess()) {
            return topicExists;
        }

        // Get post count
        // TODO: check
        final String STMT_1 = "SELECT COUNT(*) AS totalPosts FROM Post WHERE topic_id = ?";
        try(PreparedStatement p = c.prepareStatement(STMT_1)) {
            p.setLong(1,topicId);
            ResultSet r = p.executeQuery();

            r.next();
            return Result.success(r.getInt("totalPosts"));

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    /* B.1 */
       
    @Override
    public Result likeTopic(String username, long topicId, boolean like) {
        if (username == null || username.isEmpty()) {
            return Result.failure("Username cannot be empty.");
        }

        // Retrieve person id if exists
        Result<Long> personExists = HelperStatements.getPersonId(username, c);
        if (!personExists.isSuccess()) {
            return personExists;
        }
        Long personId = personExists.getValue();

        // Perform like/unlike action
        String STMT;
        if (like) {
            STMT = "INSERT INTO TopicLikes(topic_id, person_id) VALUES (?,?)";
        } else {
            STMT = "DELETE FROM TopicLikes WHERE topic_id = ? AND person_id = ?";
        }

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
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

        return Result.success();
    }
    
    @Override
    public Result likePost(String username, long topicId, int post, boolean like) {
        // Get post id if exists
        final String STMT_1 = "SELECT post_id FROM Post WHERE topic_id = ? " +
                "ORDER BY posted_at ASC " +
                "LIMIT 1 OFFSET ?";

        Long postId;
        try(PreparedStatement p = c.prepareStatement(STMT_1)) {
            p.setLong(1, topicId);
            p.setInt(2, post);
            ResultSet r = p.executeQuery();

            if (!r.next()) {
                return Result.failure("Post does not exist.");
            }

            postId = r.getLong("post_id");

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // Retrieve person id
        Result<Long> personExists = HelperStatements.getPersonId(username, c);
        if (!personExists.isSuccess()) {
            return personExists;
        }
        Long personId = personExists.getValue();

        // Perform like action
        String STMT_3A;
        String STMT_3B;
        if (like) {
            STMT_3A = "INSERT INTO PostLikes(post_id, person_id) VALUES (?,?)";
            STMT_3B = "UPDATE Post SET total_likes = total_likes + 1 WHERE post_id = ?";
        } else {
            STMT_3A = "DELETE FROM PostLikes WHERE post_id = ? AND person_id = ?";
            STMT_3B = "UPDATE Post SET total_likes = total_likes - 1 WHERE post_id = ?";
        }

        try(PreparedStatement p = c.prepareStatement(STMT_3A);
            PreparedStatement p2 = c.prepareStatement(STMT_3B)) {
            p.setLong(1, postId);
            p.setLong(2, personId);

            // If the insert/delete affected any rows, update the post total likes
            int rowsAffected = p.executeUpdate();
            if (rowsAffected == 1) {
                p2.setLong(1, postId);
                p2.executeQuery();
            }
            c.commit();

            return Result.success();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        // check if topic exists
        Result topicExists =   HelperStatements.topicExists(topicId, c);
        if (!topicExists.isSuccess()) {
            return topicExists;
        }

        final String STMT = "SELECT Person.name, Person.username, Person.stuId FROM TopicLikes " +
                "JOIN Person ON Person.id = TopicLikes.person_id " +
                "WHERE TopicLikes.topic_id = ?";

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            ResultSet r = p.executeQuery();

            List<PersonView> personViews = new ArrayList<>();
            while(r.next()) {
                PersonView personView = new PersonView(
                        r.getString("name"),
                        r.getString("username"),
                        r.getString("stuId")
                );

                personViews.add(personView);
            }

            return Result.success(personViews);
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<TopicView> getTopic(long topicId) {
        // Check if topic exists and
        // get topic title, forum id, and forum title
        final String STMT_1 = "SELECT Topic.title, Topic.forum_id, Forum.title FROM Topic " +
                "JOIN Forum ON Topic.forum_id = Forum.forum_id " +
                "WHERE Topic.topic_id = ?";


        Long forumId;
        String forumName;
        String topicTitle;

        try (PreparedStatement p = c.prepareStatement(STMT_1)) {
            p.setLong(1,topicId);
            ResultSet r = p.executeQuery();

            if (!r.next()) {
                return Result.failure("Topic does not exist.");
            }

            topicTitle = r.getString("Topic.title");
            forumId = r.getLong("Topic.forum_id");
            forumName = r.getString("Forum.title");

        } catch(SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // Construct post views
        final String STMT_2 =
                "SELECT Post.text, Post.posted_at, Post.total_likes, Person.name, Person.username FROM Post " +
                        "JOIN Person ON Post.person_id = Person.id " +
                        "WHERE Post.topic_id = ? " +
                        "ORDER BY posted_at ASC";

        List<PostView> postViews = new ArrayList<>();

        try(PreparedStatement p = c.prepareStatement(STMT_2)) {
            p.setLong(1, topicId);
            ResultSet r = p.executeQuery();

            int postNumber = 0;
            while(r.next()) {
                postNumber++;

                PostView postView = new PostView(
                        forumId,
                        topicId,
                        postNumber,
                        r.getString("Person.name"),
                        r.getString("Person.username"),
                        r.getString("text"),
                        r.getTimestamp("Post.posted_at").toString(),
                        r.getInt("Post.total_likes")
                );

                postViews.add(postView);
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // TODO: 5/1/18 what if empty
        // Construct topic view
        TopicView topicView = new TopicView(forumId, topicId, forumName, topicTitle, postViews);

        return Result.success(topicView);
    }

    /* B.2 */

    @Override
    public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedPersonView> getAdvancedPersonView(String username) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedForumView> getAdvancedForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
