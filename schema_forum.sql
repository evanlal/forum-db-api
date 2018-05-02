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
	CONSTRAINT topic_nonexist_forum FOREIGN KEY (forum_id) REFERENCES Forum(forum_id),
	CONSTRAINT topic_nonexist_person FOREIGN KEY (person_id) REFERENCES Person(id)
);

CREATE TABLE Post (
	post_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	topic_id INTEGER NOT NULL,
	person_id INTEGER NOT NULL,
	posted_at TIMESTAMP NOT NULL,
	text VARCHAR(2048) NOT NULL,
	total_likes INTEGER DEFAULT 0,
	CONSTRAINT post_nonexist_topic FOREIGN KEY (topic_id) REFERENCES Topic(topic_id),
	CONSTRAINT post_nonexist_person FOREIGN KEY (person_id) REFERENCES Person(id)
);

CREATE TABLE TopicLikes (
	topic_id INTEGER NOT NULL,
	person_id INTEGER NOT NULL,
	PRIMARY KEY (topic_id, person_id),
	CONSTRAINT topiclike_nonexist_topic FOREIGN KEY (topic_id) REFERENCES Topic(topic_id),
	CONSTRAINT topiclike_nonexist_person FOREIGN KEY (person_id) REFERENCES Person(id),
	CONSTRAINT topic_already_liked UNIQUE (topic_id, person_id)
);

CREATE TABLE PostLikes (
	post_id INTEGER NOT NULL,
	person_id INTEGER NOT NULL,
	PRIMARY KEY (post_id, person_id),
	CONSTRAINT postlike_nonexist_post FOREIGN KEY (post_id) REFERENCES Post (post_id),
	CONSTRAINT postlike_nonexist_person FOREIGN KEY (person_id) REFERENCES Person (id),
	CONSTRAINT post_already_liked UNIQUE (post_id, person_id)
);
