-- Employee data source: https://www.kaggle.com/datasets/williamlucas0/employee-sample-data
INSERT INTO employee (EMPLOYEE_ID, FIRST_NAME, LAST_NAME, TITLE, DEPARTMENT, BUSINESS_UNIT, gender, ethnicity, age, hire_Date, annual_Salary) SELECT EMPLOYEE_ID, FIRST_NAME, LAST_NAME, TITLE, DEPARTMENT, BUSINESS_UNIT, gender, ethnicity, age, hire_Date, annual_Salary FROM CSVREAD('~/Documents/projects/mcp-human-resources/employee_data_load.csv');

UPDATE EMPLOYEE t
SET t.MANAGER_ID = (SELECT c.MANAGER_ID FROM CSVREAD('/home/davidbrown/Documents/projects/mcp-human-resources/employee_data_load.csv') c WHERE CAST(c.EMPLOYEE_ID AS BIGINT)= t.EMPLOYEE_ID)
WHERE t.EMPLOYEE_ID IN (SELECT CAST(c.EMPLOYEE_ID AS BIGINT) FROM CSVREAD('/home/davidbrown/Documents/projects/mcp-human-resources/employee_data_load.csv') c);  