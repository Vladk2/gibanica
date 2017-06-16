create database if not exists baklava;
create table if not exists baklava.usertypes(
	type varchar(15) not null primary key
);
create table if not exists baklava.users(
	userId int(5) not null auto_increment primary key,
	name varchar(60) not null,
	surname varchar(60) not null,
	email varchar(60) unique not null,
	password varchar(200) not null,
	verified boolean not null,
	type varchar(15) not null,
	constraint `usertype_constraint`
		foreign key (type) references baklava.usertypes (type)
);

CREATE TABLE IF NOT EXISTS baklava.restaurants (
  restaurantId INT(5)       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name         VARCHAR(30)  NOT NULL,
  description  VARCHAR(100) NOT NULL,
  address      VARCHAR(50)  NOT NULL,
  tel          VARCHAR(20)  NOT NULL,
  size         INT(3)       NOT NULL
);


create table if not exists baklava.workers(
	userId int(5) not null,
	birthDate varchar(20) not null,
	clothNo varchar(20) not null,
	shoesNo varchar(20) not null,
	restaurantId INT(5)  NOT NULL,
	constraint `user_id_in_workers`
		foreign key (userId) references baklava.users (userId),
	CONSTRAINT `restaurant_id_in_workers`
  		FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);
-- tabela za goste, za menadzere i za radnike -- posebno, kasnije

CREATE TABLE IF NOT EXISTS baklava.victualsanddrinks (
  victualsAndDrinksId INT(5)       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                VARCHAR(100) NOT NULL,
  description         VARCHAR(100) NOT NULL,
  price               DOUBLE       NOT NULL,
  type                VARCHAR(10)  NOT NULL,
  restaurantId        INT(5)       NOT NULL,
  CONSTRAINT `restaurant_id_in_menu`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);

CREATE TABLE IF NOT EXISTS baklava.seatconfig (
  posX         VARCHAR(3)  NOT NULL,
  posY         VARCHAR(3)  NOT NULL,
  sectorColor  VARCHAR(15) NOT NULL,
  restaurantId INT(5)      NOT NULL,
  CONSTRAINT `restaurant_id_in_seat_config`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);

CREATE TABLE IF NOT EXISTS baklava.sectornames (
  sectorName   VARCHAR(25) NOT NULL,
  sectorColor  VARCHAR(15) NOT NULL,
  restaurantId INT(5)      NOT NULL,
  CONSTRAINT `restaurant_id_in_sectornames`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);

-- veza za menadzere restorana

CREATE TABLE IF NOT EXISTS baklava.restaurantmanagers (
  restaurantId INT(5) NOT NULL,
  userId       INT(5) NOT NULL,
  CONSTRAINT `user_id_in_restaurant_managers`
  FOREIGN KEY (userId) REFERENCES baklava.users (userId),
  CONSTRAINT `restaurant_id_in_restaurant_managers`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);


-- user types
insert into baklava.usertypes values('guest');
insert into baklava.usertypes values('system-manager');
insert into baklava.usertypes values('rest-manager');
insert into baklava.usertypes values('waiter');
insert into baklava.usertypes values('bartender');
insert into baklava.usertypes values('chef');
insert into baklava.usertypes values('bidder');

-- test korisnici
insert into baklava.users(name, surname, email, password, verified, type)
	values('gost', 'gostić', 'gost@gost.com', 'password', 1, 'guest');
insert into baklava.users(name, surname, email, password, verified, type)
	values('sistem', 'menadzer', 'system@manager.com', 'password', 1, 'system-manager');
insert into baklava.users(name, surname, email, password, verified, type)
	values('restoran', 'menadzer', 'restoran@manager.com', 'password', 1, 'rest-manager');
insert into baklava.users(name, surname, email, password, verified, type)
	values('waiter', 'waiter', 'waiter@waiter.com', 'password', 1, 'waiter');
