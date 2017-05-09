create table if not exists baklava.restaurants (
	restaurantId int(5) not null PRIMARY KEY auto_increment,
	name varchar(30) not null,
	description varchar(100) not null,
	address varchar(50) not null,
	tel varchar(20) not null,
	size int(3) not null
);

create table if not exists baklava.victualsanddrinks (
	victualsAndDrinksId int(5) not null primary key auto_increment,
	name varchar(100) not null ,
	description varchar(100) not null,
	price int(6) not null,
	type varchar(10) not null,
	restaurantId int(5) not null,
	constraint `restaurant_id_in_menu`
		foreign key (restaurantId) references baklava.restaurants (restaurantId)
);

create table if not exists baklava.seatconfig (
  posX varchar(3) not null,
  posY varchar(3) not null,
  sectorColor varchar(15) not null,
  restaurantId int(5) not null,
  constraint `restaurant_id_in_seat_config`
    foreign key (restaurantId) references baklava.restaurants (restaurantId)
);

create table if not exists baklava.sectornames (
  sectorName varchar(25) not null,
  sectorColor varchar(15) not null,
  restaurantId int(5) not null,
  constraint `restaurant_id_in_sectornames`
  foreign key (restaurantId) references baklava.restaurants (restaurantId)
);

-- veza za menadzere restorana

create table if not exists baklava.restaurantmanagers (
	restaurantId int(5) not null,
	userId int(5) not null,
	constraint `user_id_in_restaurant_managers`
		foreign key (userId) references baklava.users (userId),
	constraint `restaurant_id_in_restaurant_managers`
		foreign key (restaurantId) references baklava.restaurants (restaurantId)
);
