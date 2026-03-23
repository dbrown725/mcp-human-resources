-- Employee data source: https://www.kaggle.com/datasets/williamlucas0/employee-sample-data
-- Address data source: https://www.mockaroo.com/
INSERT INTO address (ADDRESS_ID, STREET_ADDRESS, CITY, STATE, POSTAL_CODE, IS_REMOTE)
SELECT ADDRESS_ID, STREET_ADDRESS, CITY, STATE, POSTAL_CODE, IS_REMOTE
FROM CSVREAD('ops/data/db/address_data_load.csv');

ALTER SEQUENCE address_sequence RESTART WITH 10900;

INSERT INTO employee (EMPLOYEE_ID, FIRST_NAME, LAST_NAME, TITLE, DEPARTMENT, BUSINESS_UNIT, gender, ethnicity, age, hire_Date, annual_Salary, termination_date, ADDRESS_ID)
SELECT EMPLOYEE_ID, FIRST_NAME, LAST_NAME, TITLE, DEPARTMENT, BUSINESS_UNIT, gender, ethnicity, age, hire_Date, annual_Salary, termination_date, ADDRESS_ID
FROM CSVREAD('ops/data/db/employee_data_load.csv');

UPDATE EMPLOYEE t
SET t.MANAGER_ID = (SELECT c.MANAGER_ID FROM CSVREAD('ops/data/db/employee_data_load.csv') c WHERE CAST(c.EMPLOYEE_ID AS BIGINT)= t.EMPLOYEE_ID)
WHERE t.EMPLOYEE_ID IN (SELECT CAST(c.EMPLOYEE_ID AS BIGINT) FROM CSVREAD('ops/data/db/employee_data_load.csv') c); 

-- Benefits plan seed data (table created by JPA from BenefitsConfig entity)
INSERT INTO BENEFITS_CONFIG (PLAN_ID, PLAN_NAME, PLAN_TYPE, PROVIDER, COVERAGE_LEVEL, MONTHLY_PREMIUM, DEDUCTIBLE, OUT_OF_POCKET_MAX, ENROLLMENT_STATUS, DESCRIPTION)
VALUES
(1, 'Titanium Health PPO', 'Medical', 'BlueShield', 'Individual', 150.00, 500.00, 3000.00, TRUE, 'Low deductible premium plan with wide provider access.'),
(2, 'Silver Health HMO', 'Medical', 'Aetna', 'Individual', 95.00, 1500.00, 5000.00, TRUE, 'Mid-tier HMO plan with in-network focus and moderate costs.'),
(3, 'Bronze Basic EPO', 'Medical', 'UnitedHealth', 'Family', 65.00, 3000.00, 8000.00, TRUE, 'Budget-friendly EPO plan with higher deductible for cost-conscious employees.'),
(4, 'Basic Dental', 'Dental', 'Delta Dental', 'Family', 45.00, 50.00, 1500.00, TRUE, 'Covers 100% of preventative care and 50% of major procedures.'),
(5, 'Premium Dental Plus', 'Dental', 'MetLife', 'Individual', 30.00, 25.00, 1000.00, TRUE, 'Enhanced dental plan with orthodontic coverage and low deductible.'),
(6, 'Vision Plus', 'Vision', 'VSP', 'Individual', 12.00, 10.00, 500.00, TRUE, 'Annual eye exam included with $150 frame allowance.');

ALTER SEQUENCE benefits_config_sequence RESTART WITH 100;

-- Employee benefit enrollments (table created by JPA from EmployeeBenefit entity)
-- Enroll ~85% of employees in a Medical plan (distributed across 3 medical plans)
INSERT INTO EMPLOYEE_BENEFIT (EMPLOYEE_ID, PLAN_ID, ENROLLMENT_DATE)
SELECT e.EMPLOYEE_ID,
       CASE MOD(e.EMPLOYEE_ID, 3) WHEN 0 THEN 1 WHEN 1 THEN 2 ELSE 3 END,
       e.HIRE_DATE
FROM EMPLOYEE e
WHERE MOD(e.EMPLOYEE_ID, 7) != 0;

-- Enroll ~60% of employees in a Dental plan (distributed across 2 dental plans)
INSERT INTO EMPLOYEE_BENEFIT (EMPLOYEE_ID, PLAN_ID, ENROLLMENT_DATE)
SELECT e.EMPLOYEE_ID,
       CASE MOD(e.EMPLOYEE_ID, 2) WHEN 0 THEN 4 ELSE 5 END,
       e.HIRE_DATE
FROM EMPLOYEE e
WHERE MOD(e.EMPLOYEE_ID, 5) > 1;

-- Enroll ~33% of employees in Vision plan
INSERT INTO EMPLOYEE_BENEFIT (EMPLOYEE_ID, PLAN_ID, ENROLLMENT_DATE)
SELECT e.EMPLOYEE_ID, 6, e.HIRE_DATE
FROM EMPLOYEE e
WHERE MOD(e.EMPLOYEE_ID, 3) = 0;