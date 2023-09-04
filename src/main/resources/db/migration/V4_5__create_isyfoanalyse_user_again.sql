DO $$
BEGIN
  REVOKE SELECT ON ALL TABLES IN SCHEMA public FROM "isyfo-analyse";
  DROP ROLE "isyfo-analyse";
  CREATE USER "isyfo-analyse";
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role isyfo-analyse -- it already exists';
END
$$;
