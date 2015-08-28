USE employees;

ALTER TABLE `employees`.`employees` 
ADD COLUMN `last_update_date` TIMESTAMP NULL AFTER `hire_date`;
