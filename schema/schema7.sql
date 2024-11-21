/**
  1 => urgent need
  2 => need
  3 => available
  4 => oversupply
 */
update item_status set sort_order = 4 where name = 'Oversupply';
insert into item_status(name, sort_order) values ('Need', 2);
update item_status set name = 'Available' where name = 'Requested';
