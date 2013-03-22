DROP TABLE IF EXISTS `plot_events`;
DROP TABLE IF EXISTS `role_events`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `actors`;
DROP TABLE IF EXISTS `codes`;
DROP TABLE IF EXISTS `movies`;

CREATE TABLE IF NOT EXISTS `movies` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code_version` char(4) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `imdb_url` varchar(255) DEFAULT NULL,
  `summary` text DEFAULT NULL,
  `length` int DEFAULT NULL,
  `import_date` datetime NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `codes` (
  `code` int NOT NULL,
  `time_stamp` int NOT NULL,
  `movie_id` int NOT NULL,
  PRIMARY KEY (`code`,`time_stamp`,`movie_id`),
  FOREIGN KEY (`movie_id`) REFERENCES `movies`(`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `actors` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `imdb_url` varchar(255) DEFAULT NULL,
  `picture_url` varchar(255) DEFAULT NULL,
  `bio` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `imdb_url` varchar(255) DEFAULT NULL,
  `actor` int NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`actor`) REFERENCES `actors`(`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `role_events` (
  `id` int NOT NULL AUTO_INCREMENT,
  `time_stamp` int NOT NULL,
  `movie` int NOT NULL,
  `role` int NOT NULL,
  `blurb` text NOT NULL,
  PRIMARY KEY (`id`),
  INDEX (`movie`, `time_stamp`),
  FOREIGN KEY (`movie`) REFERENCES `movies`(`id`),
  FOREIGN KEY (`role`) REFERENCES `roles`(`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `plot_events` (
  `id` int NOT NULL AUTO_INCREMENT,
  `time_stamp` int NOT NULL,
  `movie` int NOT NULL,
  `plot` text NOT NULL,
  PRIMARY KEY (`id`),
  INDEX (`movie`, `time_stamp`),
  FOREIGN KEY (`movie`) REFERENCES `movies`(`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `comments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `time_stamp` int NOT NULL,
  `movie` int NOT NULL,
  `comment` text NOT NULL,
  PRIMARY KEY (`id`),
  INDEX (`movie`, `time_stamp`),
  FOREIGN KEY (`movie`) REFERENCES `movies`(`id`)
) DEFAULT CHARSET=utf8;

