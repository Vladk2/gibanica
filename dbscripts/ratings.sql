CREATE TABLE IF NOT EXISTS baklava.restaurantsRating(
  userId INT(5) NOT NULL,
  restaurantId INT(5) NOT NULL,
  rating INT(1) DEFAULT 0,
  serviceRating INT(1) DEFAULT 0,
  mealRating INT(1) DEFAULT 0,
  CONSTRAINT `restaurantRating_user`
  FOREIGN KEY (userId) REFERENCES baklava.users (userId),
  CONSTRAINT `restaurantRating_restaurant`
  FOREIGN KEY (restaurantId) REFERENCES baklava.restaurants (restaurantId),
  PRIMARY KEY (userId, restaurantId)
);