-- Mysql -h<host> -uroot -p < thisscript

CREATE SCHEMA IF NOT EXISTS `RAPLA_DB` DEFAULT CHARACTER SET latin1;
USE `RAPLA_DB`;

-- -----------------------------------------------------
-- Table `RAPLA_DB`.`ALLOCATION`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`ALLOCATION` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`ALLOCATION` (
  `APPOINTMENT_ID` INT NOT NULL ,
  `RESOURCE_ID` INT NOT NULL,
  `OPTIONAL` INT
  );

CREATE INDEX `INDEX_1` ON `RAPLA_DB`.`ALLOCATION` (`APPOINTMENT_ID` ASC) ;


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`APPOINTMENT`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`APPOINTMENT` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`APPOINTMENT` (
   `ID` INT NOT NULL
  ,`EVENT_ID` INT NOT NULL
  ,`APPOINTMENT_START`   DATETIME       NOT NULL
  ,`APPOINTMENT_END`     DATETIME       NOT NULL
  ,`REPETITION_TYPE`     VARCHAR(15)    NULL DEFAULT NULL
  ,`REPETITION_NUMBER`   INT            NULL DEFAULT NULL
  ,`REPETITION_END`      DATETIME       NULL DEFAULT NULL
  ,`REPETITION_INTERVAL` INT            NULL DEFAULT NULL  
  ,PRIMARY KEY (`ID`) );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`APPOINTMENT_EXCEPTION`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`APPOINTMENT_EXCEPTION` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`APPOINTMENT_EXCEPTION` (
  `APPOINTMENT_ID` INT NOT NULL ,
  `EXCEPTION_DATE` DATETIME NOT NULL );

CREATE INDEX `Index_1` ON `RAPLA_DB`.`APPOINTMENT_EXCEPTION` (`APPOINTMENT_ID` ASC) ;


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`CATEGORY`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`CATEGORY` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`CATEGORY` (
  `ID` INT NOT NULL ,
  `PARENT_ID` INT NULL DEFAULT NULL ,
  `CATEGORY_KEY` VARCHAR(50) NOT NULL ,
  `LABEL` VARCHAR(250) NULL DEFAULT NULL ,
  `DEFINITION` TEXT NULL DEFAULT NULL ,
  `PARENT_ORDER` INT NULL DEFAULT NULL ,
  PRIMARY KEY (`ID`) );

CREATE INDEX `INDEX_2` ON `RAPLA_DB`.`CATEGORY` (`PARENT_ID` ASC) ;


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`DYNAMIC_TYPE`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`DYNAMIC_TYPE` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`DYNAMIC_TYPE` (
  `ID` INT NOT NULL ,
  `TYPE_KEY` VARCHAR(50) NOT NULL ,
  `DEFINITION` TEXT NOT NULL ,
  PRIMARY KEY (`ID`) );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`EVENT`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`EVENT` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`EVENT` (
  `ID` INT NOT NULL ,
  `TYPE_KEY` VARCHAR(50) NOT NULL ,
  `OWNER_ID` INT NOT NULL ,
  `CREATION_TIME` DATETIME ,
  `LAST_CHANGED` DATETIME ,
  `LAST_CHANGED_BY` INT NULL DEFAULT NULL ,
  PRIMARY KEY (`ID`) );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`EVENT_ATTRIBUTE_VALUE`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`EVENT_ATTRIBUTE_VALUE` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`EVENT_ATTRIBUTE_VALUE` (
  `EVENT_ID` INT NOT NULL ,
  `ATTRIBUTE_KEY` VARCHAR(20) NOT NULL ,
  `VALUE` VARCHAR(1000) NULL DEFAULT NULL );

CREATE INDEX `INDEX_1` ON `RAPLA_DB`.`EVENT_ATTRIBUTE_VALUE` (`EVENT_ID` ASC) ;


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`PERIOD`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`PERIOD` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`PERIOD` (
  `ID` INT NOT NULL ,
  `NAME` VARCHAR(255) NOT NULL ,
  `PERIOD_START` DATETIME NOT NULL ,
  `PERIOD_END` DATETIME NOT NULL ,
  PRIMARY KEY (`ID`) );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`PERMISSION`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`PERMISSION` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`PERMISSION` (
  `RESOURCE_ID` INT NOT NULL ,
  `USER_ID` INT NULL DEFAULT NULL ,
  `GROUP_ID` INT NULL DEFAULT NULL ,
  `ACCESS_LEVEL` INT NOT NULL ,
  `MIN_ADVANCE` INT NULL DEFAULT NULL ,
  `MAX_ADVANCE` INT NULL DEFAULT NULL ,
  `START_DATE` DATETIME NULL DEFAULT NULL ,
  `END_DATE` DATETIME NULL DEFAULT NULL );

CREATE INDEX `INDEX_1` ON `RAPLA_DB`.`PERMISSION` (`RESOURCE_ID` ASC) ;


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`PREFERENCE`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`PREFERENCE` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`PREFERENCE` (
  `USER_ID` INT NULL DEFAULT NULL ,
  `ROLE` VARCHAR(200) NOT NULL ,
  `STRING_VALUE` VARCHAR(1000) NULL DEFAULT NULL ,
  `XML_VALUE` MEDIUMTEXT NULL DEFAULT NULL );

CREATE INDEX `INDEX_1` ON `RAPLA_DB`.`PREFERENCE` (`USER_ID` ASC) ;


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`RAPLA_USER`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`RAPLA_USER` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`RAPLA_USER` (
  `ID` INT NOT NULL ,
  `USERNAME` VARCHAR(30) NOT NULL ,
  `PASSWORD` VARCHAR(130) NULL DEFAULT NULL ,
  `NAME` VARCHAR(200) NOT NULL ,
  `EMAIL` VARCHAR(150) NOT NULL ,
  `ISADMIN` INT NOT NULL ,
  PRIMARY KEY (`ID`) );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`RAPLA_USER_GROUP`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`RAPLA_USER_GROUP` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`RAPLA_USER_GROUP` (
  `USER_ID` INT NOT NULL ,
  `CATEGORY_ID` INT NOT NULL );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`RAPLA_RESOURCE`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`RAPLA_RESOURCE` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`RAPLA_RESOURCE` (
  `ID` INT NOT NULL ,
  `TYPE_KEY` VARCHAR(100) NOT NULL ,
  `IGNORE_CONFLICTS` INT NOT NULL ,
  `OWNER_ID` INT,
  `CREATION_TIME` DATETIME ,
  `LAST_CHANGED` DATETIME,
  `LAST_CHANGED_BY` INT NULL DEFAULT NULL ,
  PRIMARY KEY (`ID`) );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`RESOURCE_GROUP`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`RESOURCE_GROUP` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`RESOURCE_GROUP` (
  `ID` INT NOT NULL ,
  `GROUP_ID` INT NOT NULL );


-- -----------------------------------------------------
-- Table `RAPLA_DB`.`RESOURCE_ATTRIBUTE_VALUE`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `RAPLA_DB`.`RESOURCE_ATTRIBUTE_VALUE` ;

CREATE  TABLE IF NOT EXISTS `RAPLA_DB`.`RESOURCE_ATTRIBUTE_VALUE` (
  `RESOURCE_ID` INT NOT NULL ,
  `ATTRIBUTE_KEY` VARCHAR(25) NULL DEFAULT NULL ,
  `VALUE` VARCHAR(1000) NULL DEFAULT NULL );

DROP USER 'rapla'@'%';
CREATE USER 'rapla'@'%' IDENTIFIED BY 'raplapw';
GRANT ALL PRIVILEGES ON `RAPLA_DB`.* TO 'rapla'@'%';