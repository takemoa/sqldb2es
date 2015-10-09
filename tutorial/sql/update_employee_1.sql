INSERT INTO `employees`.`titles`
(`emp_no`,
`title`,
`from_date`,
`to_date`)
VALUES
(11827,
'Engineer',
'2000-07-19',
null);

UPDATE `employees`.`employees` SET `last_update_date` = CURRENT_TIMESTAMP() WHERE emp_no = 11827;
