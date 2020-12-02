-------------------------------------------------------------------------------------------------------------
--
-- File Name: stvrest_inserts.sql
-- Created:   18-May-17
-- Version:   1.0
--
-- Purpose:   Insert needed records into STVREST for the CPOS process.
--
-- Notes:     1.  Run as SATURN.
--            2.  Change value of IS_MEP if you are a MEP'ed institution.
--
-- Audit Log 1.0:
--
--    1. Initial release of this script.                                                      KAH 18-MAY-2017
--    2. Added MEPD vs non-MEP isntall flags.                                                     01-DEC-2020
--    3. Added ability to change the prefixs as desired.
--
-- Audit Log 1.0 End
-------------------------------------------------------------------------------------------------------------
--
PROMPT
PROMPT Start of stvrest_inserts.sql
PROMPT
--
PROMPT
PROMPT Data before inserts:
PROMPT
SELECT x.* FROM stvrest x;
--
PROMPT
PROMPT Start data inserts:
PROMPT
--
DECLARE
  IS_MEP        BOOLEAN     := TRUE;   -- Change to false if NON-MEP institution.
  lv_prefix_mep VARCHAR2(1) := 'E';    -- Prefix for all MEP codes; STVREST_CODE is 4 charactes max.
  lv_prefix_non VARCHAR2(4) := 'EXDA'; -- Prefix for non-mep code; (EX)ternal (D)egree (A)udit System

BEGIN
--
  IF ( IS_MEP ) THEN
    FOR c1 IN (SELECT *
               FROM   GTVVPDI
               WHERE NOT EXISTS (SELECT 'Y' 
                                 FROM   stvrest 
                                 WHERE  stvrest_code =  lv_prefix_mep || gtvvpdi_code)
              )
    LOOP
--
      INSERT INTO stvrest
        (stvrest_code,
         stvrest_desc,
         stvrest_system_req_ind,
         stvrest_user_id,
         stvrest_activity_date,
         stvrest_vpdi_code)
      VALUES
        (lv_prefix_mep || c1.gtvvpdi_code,
         'External Degree Audit API for ' || c1.gtvvpdi_code,
         'Y',
         USER,
         SYSDATE,
         c1.gtvvpdi_code);
    END LOOP;
  ELSE
      INSERT INTO stvrest
        (stvrest_code,
         stvrest_desc,
         stvrest_system_req_ind,
         stvrest_user_id,
         stvrest_activity_date)
      VALUES
        (lv_prefix_non,
         'External Degree Audit API',
         'Y',
         USER,
         SYSDATE);
  END IF;
--
END;
/
--
PROMPT
PROMPT Data after inserts:
PROMPT
SELECT x.* FROM stvrest x;
--
PROMPT
PROMPT STVREST Inserts Completed
PROMPT
--
COMMIT;
