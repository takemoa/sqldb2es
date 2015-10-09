UPDATE `employees`.`salaries`
SET `salary` = 42000
WHERE `emp_no` = 11827 AND salary = 42925;

INSERT INTO `employees`.`dept_emp`
(`emp_no`,
`dept_no`,
`from_date`,
`to_date`)
VALUES
(11827,
'd005',
'2000-07-19',
'9999-07-19');

UPDATE `employees`.`employees` SET `last_update_date` = CURRENT_TIMESTAMP() WHERE emp_no = 11827;
