CREATE TABLE IF NOT EXISTS baklava.workingday (
  dayId int(5) NOT NULL PRIMARY KEY AUTO_INCREMENT ,
  date DATE NOT NULL,
  startTime TIME NOT NULL,
  endTime TIME NOT NULL,
  sectorName VARCHAR(25),
   userId int(5) NOT NULL,
    CONSTRAINT `user_in_workingday`
    FOREIGN KEY (userId) REFERENCES baklava.users (userId)


);


INSERT INTO baklava.workingDay (date, startTime, endTime) values ("2017-05-11", "08:00:00", "16:00:00");
INSERT INTO baklava.workingDay (date, startTime, endTime) values ("2017-05-11", "16:00:00", "00:00:00");

