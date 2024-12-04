alter table site add column publicly_visible boolean not null default false;
alter table site add column contact_name varchar(256);
alter table site add column contact_email varchar(256);
alter table site add column website varchar(256);
alter table site add column facebook varchar(256);
alter table site add column hours varchar(512);

/* A single sequence for generating all public facing IDs */
create sequence wss_id;

-- use 'wss_id' as a public facing ID value for
-- data integrations. This allows us to set up test
-- data more easily
alter table site add column wss_id integer not null unique
  default nextval('wss_id');
alter table item add column wss_id integer not null unique
  default nextval('wss_id');

-- move 'state' to be a column of county table
alter table county add column state varchar(2) not null default 'NC';
alter table site drop column state;
