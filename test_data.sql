INSERT INTO Person(name, username, stuId) VALUES("evan", "userone", 0123);
INSERT INTO Person(name, username, stuId) VALUES("george", "usertwo", 0123);
INSERT INTO Person(name, username, stuId) VALUES("pete", "userthree", 0123);
INSERT INTO Person(name, username, stuId) VALUES("james", "userfour", 0123);


INSERT INTO Forum(title) VALUES("Sample Forum One");
INSERT INTO Forum(title) VALUES("Sample Forum Two");
INSERT INTO Forum(title) VALUES("Sample Forum Three");


INSERT INTO Topic(forum_id, person_id, title, text)
VALUES(1, 1, "A topic", "Some text");
INSERT INTO Topic(forum_id, person_id, title, text)
VALUES(1, 2, "A topic 2", "Some text 2");
INSERT INTO Topic(forum_id, person_id, title, text)
VALUES(1, 3, "A topic 3", "Some text 3");
INSERT INTO Topic(forum_id, person_id, title, text)
VALUES(2, 1, "A topic", "Some text");
INSERT INTO Topic(forum_id, person_id, title, text)
VALUES(2, 2, "A topic 2", "Some text 2");
INSERT INTO Topic(forum_id, person_id, title, text)
VALUES(3, 3, "A topic 3", "Some text 3");


INSERT INTO Post(topic_id, person_id, post_number, posted_at, text)
VALUES (1, 2, 1, NOW(), "text");
INSERT INTO Post(topic_id, person_id, post_number, posted_at, text)
VALUES (1, 3, 2, NOW(), "text");
INSERT INTO Post(topic_id, person_id, post_number, posted_at, text)
VALUES (1, 4, 3, NOW(), "text");

INSERT INTO Post(topic_id, person_id, post_number, posted_at, text)
VALUES (2, 3, 1, NOW(), "text");
