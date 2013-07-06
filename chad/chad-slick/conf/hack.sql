/* Added this to deal with the bug in evolutions-creation in play-slick when
defining models as classes instead of objects.
Should be fixed in a release soon */

/*
CREATE TABLE `SPEAKERS` (
    `name` varchar(254) NOT NULL,
      `bio` varchar(254) NOT NULL,
        `id` bigint(20) NOT NULL AUTO_INCREMENT,
          PRIMARY KEY (`id`)
        ) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8
*/

CREATE TABLE `TALKS` (
    `description` varchar(254) NOT NULL,
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `speakerId` bigint(20) NOT NULL REFERENCES SPEAKERS(id),
     PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8
