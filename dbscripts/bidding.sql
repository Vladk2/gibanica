
CREATE TABLE IF NOT EXISTS baklava.requests (
  requestId	INT(5)		NOT NULL PRIMARY KEY AUTO_INCREMENT,
  fromDate	DATE		NOT NULL,
  dueDate	DATE		NOT NULL,
  isActive	BOOLEAN		NOT NULL,
  restaurantId	INT(5)		NOT NULL,
  CONSTRAINT `restId_in_requests`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId)		
);

CREATE TABLE IF NOT EXISTS baklava.requestedFood (
  foodId        INT(5)		NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name		VARCHAR(50)	NOT NULL,
  amount	INT(5)		NOT NULL,
  requestId	INT(5)		NOT NULL,
  CONSTRAINT `requestId_in_requestedFood`
  FOREIGN KEY (requestId) REFERENCES baklava.requests (requestId)		
);


CREATE TABLE IF NOT EXISTS baklava.offers (
  offerId	INT(5)		NOT NULL PRIMARY KEY AUTO_INCREMENT,
  requestId	INT(5)		NOT NULL,
  price		DOUBLE		NOT NULL,
  dueDate	DATE		NOT NULL,
  message	VARCHAR(200)    NOT NULL,
  userId	INT(5)		NOT NULL,
  CONSTRAINT `requestId_in_offers`
  FOREIGN KEY (requestId) REFERENCES baklava.requests (requestId),
  CONSTRAINT `userId_in_offers`
  FOREIGN KEY (userId) REFERENCES baklava.users (userId)
);

CREATE TABLE IF NOT EXISTS baklava.notifications (
  notificationId  INT(5)	NOT NULL PRIMARY KEY AUTO_INCREMENT,
  userId	  INT(5)	NOT NULL,
  message	  VARCHAR(50)   NOT NULL,
  seen		  BOOLEAN	NOT NULL,
  CONSTRAINT `userId_in_notifications`
  FOREIGN KEY (userId) REFERENCES baklava.users (userId)	
);
