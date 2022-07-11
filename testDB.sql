CREATE DATABASE IF NOT EXISTS `tiw_app` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `tiw_app`;
-- MySQL dump 10.13  Distrib 8.0.28, for Linux (x86_64)
--
-- Host: localhost    Database: tiw_app
-- ------------------------------------------------------
-- Server version	8.0.28

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `accounts`
--

DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accounts`
(
    `id`      bigint         NOT NULL,
    `ownerId` bigint         NOT NULL,
    `balance` float unsigned NOT NULL,
    PRIMARY KEY (`id`),
    KEY `ownerId_idx` (`ownerId`),
    CONSTRAINT `ownerId` FOREIGN KEY (`ownerId`) REFERENCES `users` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accounts`
--

LOCK TABLES `accounts` WRITE;
/*!40000 ALTER TABLE `accounts`
    DISABLE KEYS */;
INSERT INTO `accounts`
VALUES (1, 1, 490),
       (2, 1, 500),
       (3, 2, 110),
       (4, 3, 50);
/*!40000 ALTER TABLE `accounts`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contacts`
--

DROP TABLE IF EXISTS `contacts`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contacts`
(
    `ownerId`   bigint NOT NULL,
    `contactId` bigint NOT NULL,
    PRIMARY KEY (`ownerId`, `contactId`),
    KEY `fk_contacts_2_idx` (`contactId`),
    CONSTRAINT `fk_contacts_1` FOREIGN KEY (`ownerId`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_contacts_2` FOREIGN KEY (`contactId`) REFERENCES `users` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contacts`
--

LOCK TABLES `contacts` WRITE;
/*!40000 ALTER TABLE `contacts`
    DISABLE KEYS */;
INSERT INTO `contacts`
VALUES (1, 2);
/*!40000 ALTER TABLE `contacts`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transfers`
--

DROP TABLE IF EXISTS `transfers`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfers`
(
    `id`          bigint         NOT NULL,
    `date`        timestamp      NOT NULL,
    `amount`      float unsigned NOT NULL,
    `toId`        bigint         NOT NULL,
    `toBalance`   float unsigned NOT NULL,
    `fromId`      bigint         NOT NULL,
    `fromBalance` float unsigned NOT NULL,
    `causal`      varchar(1024)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `toId_idx` (`toId`),
    KEY `fromId_idx` (`fromId`),
    CONSTRAINT `fromId` FOREIGN KEY (`fromId`) REFERENCES `accounts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `toId` FOREIGN KEY (`toId`) REFERENCES `accounts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transfers`
--

LOCK TABLES `transfers` WRITE;
/*!40000 ALTER TABLE `transfers`
    DISABLE KEYS */;
INSERT INTO `transfers`
VALUES (1, '2022-07-11 22:43:36', 10, 2, 500, 1, 500, 'Intra-account transfer'),
       (2, '2022-07-11 22:45:19', 15, 2, 510, 1, 490, 'asdf'),
       (3, '2022-07-12 00:30:29', 15, 1, 475, 2, 525, 'Rettifica'),
       (4, '2022-07-12 00:45:46', 10, 3, 100, 2, 510, 'Buona fortuna per l\'esame di TIW!');
/*!40000 ALTER TABLE `transfers`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users`
(
    `id`       bigint       NOT NULL,
    `username` varchar(128) NOT NULL,
    `password` varchar(512) NOT NULL,
    `email`    varchar(128) NOT NULL,
    `name`     varchar(128) NOT NULL,
    `surname`  varchar(128) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `id_UNIQUE` (`id`),
    UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users`
    DISABLE KEYS */;
INSERT INTO `users`
VALUES (1, 'alexbradd',
        'd74ff0ee8da3b9806b18c877dbf29bbde50b5bd8e4dad7a3a725000feb82e8f1f991e16947615c7c3491edb613a668abd3125903da2aab4c5e2815a802df76532e14141725d1d5427efb66385fcc14c8e8cc95bda6ae78d3e732bfe85e2fa295:f991e16947615c7c3491edb613a668abd3125903da2aab4c5e2815a802df76532e14141725d1d5427efb66385fcc14c8e8cc95bda6ae78d3e732bfe85e2fa295',
        'my.fake.mail@polimi.it', 'Alexandru', 'Bradatan'),
       (2, 'mario-rossi',
        'd74ff0ee8da3b9806b18c877dbf29bbde50b5bd8e4dad7a3a725000feb82e8f1e9c405a790a6490160678cde601092335b2a3782ec4bd5dc7454908bab69faac5e7534eaf04d19a5d52f8e94199b81cbc649da0ddbdb0c81459d274d068c4670:e9c405a790a6490160678cde601092335b2a3782ec4bd5dc7454908bab69faac5e7534eaf04d19a5d52f8e94199b81cbc649da0ddbdb0c81459d274d068c4670',
        'mario.rossi@mail.com', 'Mario', 'Rossi'),
       (3, 'refactored-disco',
        'd74ff0ee8da3b9806b18c877dbf29bbde50b5bd8e4dad7a3a725000feb82e8f10ffd4eeed188c3d016bf384c5e4c495318739e1f7b00141b3c3c756ce01e2a3ee51e19230fbbddedd64b6b05aa8be37afffa9a7100b67305c83d4d8182686c29:0ffd4eeed188c3d016bf384c5e4c495318739e1f7b00141b3c3c756ce01e2a3ee51e19230fbbddedd64b6b05aa8be37afffa9a7100b67305c83d4d8182686c29',
        'refactored@disco.com', 'Maria', 'Mattei');
/*!40000 ALTER TABLE `users`
    ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2022-07-12  0:49:19
