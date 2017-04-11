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

-- tabela za goste, za menadzere i za radnike -- posebno, kasnije

-- user types
insert into baklava.usertypes values('guest');
insert into baklava.usertypes values('system-manager');
insert into baklava.usertypes values('rest-manager');
insert into baklava.usertypes values('waiter');
insert into baklava.usertypes values('bartender');
insert into baklava.usertypes values('chef');
insert into baklava.usertypes values('bidder');

-- test korisnici
insert into baklava.users(name, surname, email, password, type) 
	values('gost', 'gostić', 'gost@gost.com', 'password', 'guest');
insert into baklava.users(name, surname, email, password, type) 
	values('sistem', 'menadzer', 'system@manager.com', 'password', 'system-manager');
insert into baklava.users(name, surname, email, password, type) 
	values('restoran', 'menadzer', 'restoran@manager.com', 'password', 'rest-manager');
insert into baklava.users(name, surname, email, password, type) 
	values('waiter', 'waiter', 'waiter@waiter.com', 'password', 'waiter');
