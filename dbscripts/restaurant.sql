create table if not exists baklava.victualsAndDrinks (
	victualsAndDrinksId int(5) not null primary key auto_increment,
	name varchar(100) not null ,
	description varchar(100) not null,
	price int(6) not null 
);

create table if not exists baklava.restaurants (
	restaurantId int(5) not null primary key auto_increment,
	ime varchar(30) not null,
	description varchar(100) not null
);

create table if not exists baklava.victualMenu (
	restaurantId int(5) not null,
	victualId int(5) not null,
	constraint `victual_id_in_victual_menu`
		foreign key (victualId) references baklava.victualsAndDrinks (victualsAndDrinksId),
	constraint `restaurant_id_in_victual_menu`
		foreign key (restaurantId) references baklava.restaurants (restaurantId)
);

create table if not exists baklava.drinkMenu (
	restaurantId int(5) not null,
	drinkId int(5) not null,
	constraint `drink_id_in_drink_menu`
		foreign key (drinkId) references baklava.victualsAndDrinks (victualsAndDrinksId),
	constraint `restaurant_id_in_drink_menu`
		foreign key (restaurantId) references baklava.restaurants (restaurantId)
);

-- veza za menadzere restorana

create table if not exists baklava.restaurantManagers (
	restaurantId int(5) not null,
	userId int(5) not null,
	constraint `user_id_in_restaurant_managers`
		foreign key (userId) references baklava.users (userId),
	constraint `restaurant_id_in_restaurant_managers`
		foreign key (restaurantId) references baklava.restaurants (restaurantId)
);
