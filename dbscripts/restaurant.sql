CREATE TABLE IF NOT EXISTS baklava.restaurants (
  restaurantId INT(5)       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name         VARCHAR(30)  NOT NULL,
  description  VARCHAR(100) NOT NULL,
  address      VARCHAR(50)  NOT NULL,
  tel          VARCHAR(20)  NOT NULL,
  size         INT(3)       NOT NULL
);

CREATE TABLE IF NOT EXISTS baklava.victualsAndDrinks (
  victualsAndDrinksId INT(5)       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                VARCHAR(100) NOT NULL,
  description         VARCHAR(100) NOT NULL,
  price               DOUBLE       NOT NULL,
  type                VARCHAR(10)  NOT NULL,
  restaurantId        INT(5)       NOT NULL,
  CONSTRAINT `restaurant_id_in_menu`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);

CREATE TABLE IF NOT EXISTS baklava.seatConfig (
  seatId INT(12)       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  posX         VARCHAR(3)  NOT NULL,
  posY         VARCHAR(3)  NOT NULL,
  sectorColor  VARCHAR(15) NOT NULL,
  restaurantId INT(5)      NOT NULL,
  CONSTRAINT `restaurant_id_in_seat_config`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);

CREATE TABLE IF NOT EXISTS baklava.sectorNames (
  sectorName   VARCHAR(25) NOT NULL,
  sectorColor  VARCHAR(15) NOT NULL,
  restaurantId INT(5)      NOT NULL,
  CONSTRAINT `restaurant_id_in_sectornames`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
);

-- veza za menadzere restorana

CREATE TABLE IF NOT EXISTS baklava.restaurantManagers (
  restaurantId INT(5) NOT NULL,
  userId       INT(5) NOT NULL,
  CONSTRAINT `user_id_in_restaurant_managers`
  FOREIGN KEY (userId) REFERENCES baklava.users (userId),
  CONSTRAINT `restaurant_id_in_restaurant_managers`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)
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
