DO $$
BEGIN
  DROP ROLE "isyfo-analyse";
  CREATE USER "isyfo-analyse";
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role isyfo-analyse -- it already exists';
END
$$;
