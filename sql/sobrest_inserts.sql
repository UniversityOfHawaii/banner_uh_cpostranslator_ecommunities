-------------------------------------------------------------------------------------------------------------
--
-- File Name: sobrest_inserts.sql
-- Created:   18-May-17
--
-- Purpose:   Insert needed records into SOBREST for the CPOS process. Defaults the term and dummy passwords
--            in order to do the insert.
--
-- Notes:     1.  Run as SATURN.
--            2.  This will insert all values for a MEP'ed instance.
--            3.  Update the value of the SOBREST_API_CODE pattern from STVREST entries.
--
-- Audit Log 1.0:
--
--    1. Initial release of this script.                                                      KAH 18-May-2017
--    2. Added to logic to change the URL and port pending which environment is used.             09-DEC-2019
--    3. Updated user name.
--
-- Audit Log 1.0 End
-------------------------------------------------------------------------------------------------------------
--
PROMPT
PROMPT Start of sobrest_inserts.sql
PROMPT
--
PROMPT
PROMPT Data before inserts:
PROMPT
SELECT x.* FROM sobrest x;
--
PROMPT
PROMPT Deleting data
PROMPT
DELETE FROM  sobrest 
WHERE sobrest_api_code in ( SELECT 'S' || gtvvpdi_code 
                            FROM GTVVPDI );
--
PROMPT
PROMPT Start data inserts:
PROMPT
--
SET SCAN OFF;
--
DECLARE
  restUrlBase  VARCHAR2(100);
BEGIN
--
-- Change URL and port pending environment
--
  CASE UPPER(sys_context('USERENV','DB_NAME'))
    WHEN 'BANDEV'   THEN restUrlBase := 'https://test-degreeaudit.url.institution.edu:12345';
    WHEN 'BANPPRD'  THEN restUrlBase := 'https://test-degreeaudit.url.institution.edu:54321';
    WHEN 'PROD'     THEN restUrlBase := 'https://prod-degreeaudit.url.institution.edu:44444';
    ELSE                 restUrlBase := 'FIXME';
  END CASE;
--
-- Insert for only missing STAR STVREST codes S%
--
  FOR c1 IN ( SELECT *
              FROM   GTVVPDI
              WHERE  EXISTS (SELECT 'Y'
                             FROM   stvrest 
                             WHERE  stvrest_code =  'S' || gtvvpdi_code)
              AND    NOT EXISTS ( SELECT 'Y'
                                  FROM   sobrest
                                  WHERE  sobrest_api_code = 'S' || gtvvpdi_code )
            )
--
  LOOP
    BEGIN
      INSERT INTO sobrest
        (sobrest_api_code,
         sobrest_url,
         sobrest_username,
         sobrest_password,
         sobrest_user_id,
         sobrest_activity_date,
         sobrest_vpdi_code)
      VALUES
        ('S' || c1.gtvvpdi_code,
         restUrlBase || '/cpos/v1/runAudit?campus='||UPPER(c1.gtvvpdi_code)||'&term=202010',
         'starcpos_user',
         sokrest.f_encrypt('i_need_a_pass_' || LOWER(c1.gtvvpdi_code)),
         USER,
         SYSDATE,
         c1.gtvvpdi_code);
    END;
  END LOOP;
--
END;
/
--
SET SCAN ON;
--
PROMPT
PROMPT Data after inserts:
PROMPT
SELECT x.* FROM sobrest x;
--
PROMPT
PROMPT SOBREST Inserts Completed
PROMPT
--
COMMIT;
