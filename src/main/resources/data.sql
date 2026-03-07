-- Employee data source: https://www.kaggle.com/datasets/williamlucas0/employee-sample-data
INSERT INTO employee (EMPLOYEE_ID, FIRST_NAME, LAST_NAME, TITLE, DEPARTMENT, BUSINESS_UNIT, gender, ethnicity, age, hire_Date, annual_Salary, termination_date) SELECT EMPLOYEE_ID, FIRST_NAME, LAST_NAME, TITLE, DEPARTMENT, BUSINESS_UNIT, gender, ethnicity, age, hire_Date, annual_Salary, termination_date FROM CSVREAD('ops/data/db/employee_data_load.csv');

UPDATE EMPLOYEE t
SET t.MANAGER_ID = (SELECT c.MANAGER_ID FROM CSVREAD('ops/data/db/employee_data_load.csv') c WHERE CAST(c.EMPLOYEE_ID AS BIGINT)= t.EMPLOYEE_ID)
WHERE t.EMPLOYEE_ID IN (SELECT CAST(c.EMPLOYEE_ID AS BIGINT) FROM CSVREAD('ops/data/db/employee_data_load.csv') c);  