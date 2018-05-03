DROP TABLE IF EXISTS PostLikes;
DROP TABLE IF EXISTS TopicLikes;
DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS Topic;
DROP TABLE IF EXISTS Forum;
DROP TABLE IF EXISTS Person;

CREATE TABLE Person (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	username VARCHAR(10) NOT NULL UNIQUE,
	stuId VARCHAR(10) NULL
);

CREATE TABLE Forum (
	forum_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	title VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE Topic (
	topic_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	forum_id INTEGER NOT NULL,
	person_id INTEGER NOT NULL,
	title VARCHAR(255) NOT NULL,
	text VARCHAR(2048) NOT NULL,
	CONSTRAINT fk_topic_nonexistent_forumid FOREIGN KEY (forum_id) REFERENCES Forum(forum_id),
	CONSTRAINT fk_topic_nonexistent_personid FOREIGN KEY (person_id) REFERENCES Person(id)
);

CREATE TABLE Post (
	post_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	topic_id INTEGER NOT NULL,
	person_id INTEGER NOT NULL,
	posted_at DATETIME NOT NULL,
	text VARCHAR(2048) NOT NULL,
	total_likes INTEGER DEFAULT 0,
	CONSTRAINT fk_post_nonexistent_topicid FOREIGN KEY (topic_id) REFERENCES Topic(topic_id),
	CONSTRAINT fk_post_nonexistent_personid FOREIGN KEY (person_id) REFERENCES Person(id)
);

CREATE TABLE TopicLikes (
	topic_id INTEGER NOT NULL,
	person_id INTEGER NOT NULL,
	CONSTRAINT pk_topiclikes_already_liked PRIMARY KEY (topic_id, person_id),
	CONSTRAINT fk_topiclikes_nonexistent_topicid FOREIGN KEY (topic_id) REFERENCES Topic(topic_id),
	CONSTRAINT fk_topiclikes_nonexistent_personid FOREIGN KEY (person_id) REFERENCES Person(id)
);

CREATE TABLE PostLikes (
	post_id INTEGER NOT NULL,
	person_id INTEGER NOT NULL,
	CONSTRAINT pk_postlikes_already_liked PRIMARY KEY (post_id, person_id),
	CONSTRAINT fk_postlikes_nonexistent_postid FOREIGN KEY (post_id) REFERENCES Post (post_id),
	CONSTRAINT fk_postlikes_nonexist_personid FOREIGN KEY (person_id) REFERENCES Person (id)
);
