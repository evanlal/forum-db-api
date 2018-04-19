package uk.ac.bris.cs.databases.cwk3;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final String STMT = "SELECT name, username, stuId FROM Person WHERE username = ?";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setString(1,username);
            ResultSet r = p.executeQuery();

            if (!r.next()) {
                return Result.failure("user not found");
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
            return Result.failure("name cannot be empty");
        }

        if (username == null || username.isEmpty()) {
            return Result.failure("username cannot be empty");
        }

        if (studentId.isEmpty()) {
            return Result.failure("studentId cannot be empty");
        }

        // check if user exists
        final String STMT_1 = "SELECT username FROM Person WHERE username = ?";

        try (PreparedStatement p = c.prepareStatement(STMT_1)) {
            p.setString(1, username);
            ResultSet r = p.executeQuery();

            while(r.next()) {
                if (r.getString(1) != null) {
                    return Result.failure("username exists");
                }
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
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
                SimpleForumSummaryView view = new SimpleForumSummaryView(r.getLong("forum_id"),
                        r.getString("title"));
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

            while(r.next()) {
                if (r.getString(1) != null) {
                    return Result.failure("forum exists");
                }
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
        final String STMT = "SELECT forum_id, title FROM Forum";
        final String STMT_2 =
                "SELECT topic_id, forum_id, title FROM Topic " +
                "WHERE forum_id = ? " +
                "ORDER BY last_post_time DESC " +
                "LIMIT 1";

        try (PreparedStatement p = c.prepareStatement(STMT);
        PreparedStatement p2 = c.prepareStatement(STMT_2)) {

            ResultSet r = p.executeQuery();

            List<ForumSummaryView> viewList = new ArrayList<>();

            while(r.next()) {
                // create topic view
                SimpleTopicSummaryView topicSummaryView = null;
                p2.setString(1, r.getString("forum_id"));
                ResultSet r2 = p2.executeQuery();
                if (r2.next()) {
                    topicSummaryView = new SimpleTopicSummaryView(
                            r2.getLong("topic_id"),
                            r2.getLong("forum_id"),
                            r2.getString("title")
                    );
                }

                // create forum view
                ForumSummaryView view = new ForumSummaryView(
                        r.getLong("forum_id"),
                        r.getString("title"),
                        topicSummaryView);
                viewList.add(view);
            }

            return Result.success(viewList);

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }
    
    @Override
    public Result<ForumView> getForum(long id) {
        final String STMT = "SELECT Forum.title, Topic.topic_id, Topic.title FROM Topic " +
                "JOIN Forum ON Forum.forum_id = Topic.forum_id " +
                "WHERE Topic.forum_id = ? " +
                "ORDER BY Topic.title ASC";

        try (PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, id);
            ResultSet r = p.executeQuery();

            if (!r.isBeforeFirst()) {
                return Result.failure("forum not exists");
            }

            String forumTitle = "";
            List<SimpleTopicSummaryView> topicSummaryViews = new ArrayList<>();

            while(r.next()) {
                forumTitle = r.getString("Forum.title");
                SimpleTopicSummaryView topicSummaryView = new SimpleTopicSummaryView(
                        r.getLong("Topic.topic_id"),
                        id,
                        r.getString("Topic.title")
                );

                topicSummaryViews.add(topicSummaryView);
            }

            ForumView forumView = new ForumView(id, forumTitle,topicSummaryViews);
            return Result.success(forumView);

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }


    }   

    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
        final String STMT = "SELECT Topic.title, Post.post_number, Person.name, Post.text, Post.posted_at FROM Post " +
                "JOIN Topic ON Topic.topic_id = Post.topic_id " +
                "JOIN Person ON Person.id = Post.person_id " +
                "WHERE Post.topic_id = ? " +
                "ORDER BY Post.posted_at ASC";

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            ResultSet r = p.executeQuery();

            if(!r.isBeforeFirst()) {
                return Result.failure("topic does not exists");
            }

            // get topic title
            r.next();
            String topicTitle = r.getString("Topic.title");
            List<SimplePostView> simplePostViews = new ArrayList<>();

            do {
                SimplePostView simplePostView = new SimplePostView(
                        r.getInt("Post.post_number"),
                        r.getString("Person.name"),
                        r.getString("Post.text"),
                        r.getTimestamp("Post.posted_at").toString()
                );
                simplePostViews.add(simplePostView);
            } while (r.next());

            return Result.success(new SimpleTopicView(topicId, topicTitle, simplePostViews));
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }    
    
    @Override
    public Result<PostView> getLatestPost(long topicId) {
        // check if topic exists
        try {
            if (!HelperStatements.itemExists(c, "Topic", "topic_id", topicId)) {
                return Result.failure("topic not exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // find latest post
        final String STMT_1 = "SELECT Post.post_id, Post.forum_id, Post.post_number, Post.posted_at, Post.text, " +
                "Person.name, Person.username FROM Post " +
                "JOIN Person ON Person.id = person_id " +
                "WHERE Post.topic_id = ? " +
                "ORDER BY Post.posted_at DESC " +
                "LIMIT 1";
        final String STMT_2 = "SELECT Count(*) AS likes FROM PostLikes WHERE post_id = ?";

        try(PreparedStatement p = c.prepareStatement(STMT_1);
        PreparedStatement p2 = c.prepareStatement(STMT_2)) {
            p.setLong(1, topicId);
            ResultSet r = p.executeQuery();

            if(r.next()) {
                Long postId = r.getLong("post_id");

                // count likes
                p2.setLong(1, postId);
                ResultSet r2 = p2.executeQuery();
                int likes = 0;
                if (r2.next()) {
                    likes = r2.getInt("likes");
                }

                // create PostView
                PostView postView = new PostView(r.getLong("Post.forum_id"),
                        topicId,
                        r.getInt("Post.post_number"),
                        r.getString("Person.name"),
                        r.getString("Person.username"),
                        r.getString("Post.test"),
                        r.getString("Post.posted_at"),
                        likes
                        );
                return Result.success(postView);
            } else {
                return Result.failure("no latest post found");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result createPost(long topicId, String username, String text) {
        if (username == null || username.isEmpty()) {
            return Result.failure("username cannot be empty");
        }
        if (text == null || text.isEmpty()) {
            return Result.failure("text cannot be empty");
        }

        // check if topic exists
        try {
            if (!HelperStatements.itemExists(c, "Topic", "topic_id", topicId)) {
                return Result.failure("topic not exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // get total posts
        int total_posts = countPostsInTopic(topicId).getValue();

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

        // create post
        final String STMT_2 = "INSERT INTO Post(topic_id, person_id, post_number, posted_at, text) VALUES(?, ?, ?, ?, ?)";

        try(PreparedStatement p = c.prepareStatement(STMT_2)) {
            p.setLong(1, topicId);
            p.setLong(2, personId);
            p.setInt(3, total_posts +1);
            p.setTimestamp(4, Timestamp.from(Instant.now()));
            p.setString(5, text);
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
            return Result.failure("username cannot be empty");
        }
        if (title == null || title.isEmpty()) {
            return Result.failure("title cannot be empty");
        }
        if (text == null || text.isEmpty()) {
            return Result.failure("text cannot be empty");
        }

        // check if forum exists
        try {
            if (!HelperStatements.itemExists(c, "Forum", "forum_id", forumId)) {
                return Result.failure("forum not exists.");
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

        // create topic
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
        try {
            if (!HelperStatements.itemExists(c, "Topic", "topic_id", topicId)) {
                return Result.failure("topic not exists.");
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // get total posts
        int total_posts = 0;
        final String STMT_1 = "SELECT COUNT(*) AS totalPosts FROM Post WHERE topic_id = ?";
        try(PreparedStatement p = c.prepareStatement(STMT_1)) {
            p.setLong(1,topicId);
            ResultSet r = p.executeQuery();

            if(r.next()) {
                return Result.success(r.getInt("totalPosts"));
            }

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        return Result.success(0);
    }

    /* B.1 */
       
    @Override
    public Result likeTopic(String username, long topicId, boolean like) {
        return HelperStatements.like(c, "TopicLikes", "topic_id", "Topic", topicId, username, like);
    }
    
    @Override
    public Result likePost(String username, long topicId, int post, boolean like) {
        // get post id
        Long postId;
        final String STMT = "SELECT post_id FROM Post WHERE topic_id = ? AND post_number = ?";

        try(PreparedStatement p = c.prepareStatement(STMT)) {
            p.setLong(1, topicId);
            p.setInt(2, post);
            ResultSet r = p.executeQuery();

            if (r.next()) {
                postId = r.getLong("post_id");
            } else {
                return Result.failure("post not exists");
            }

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        return HelperStatements.like(c, "PostLikes", "post_id", "Post", postId, username, like);
    }

    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<TopicView> getTopic(long topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
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
