-- Creamos la tabla temporal de nombres

DROP TEMPORARY TABLE IF EXISTS temp_nombres;

CREATE TEMPORARY TABLE temp_nombres(nombre VARCHAR(30) UNIQUE);

INSERT INTO temp_nombres(nombre) WITH RECURSIVE nombres AS (
    SELECT 
    1 as n,		-- Caso base: empezar en 1
    'nombre001' as nombre 	-- Caso base: cadena "nombre001"
    
    UNION ALL
    
    SELECT 
    n + 1,
    CONCAT('nombre', LPAD(n + 1, 3, '0'))  -- Rellena con ceros a la izquierda hasta 3 dígitos y Creamos el nombre

    FROM nombres
    WHERE n < 100           -- Condición de parada: hasta 100, contando que el caso base es 1, y por eso no es <=
) SELECT nombre FROM nombres;



-- Creamos la tabla temporal de apellidos

DROP TEMPORARY TABLE IF EXISTS temp_apellidos;

CREATE TEMPORARY TABLE temp_apellidos(apellido VARCHAR(30) UNIQUE);

INSERT INTO temp_apellidos(apellido) WITH RECURSIVE apellidos AS (
    SELECT 
    1 as n,		-- Caso base: empezar en 1
    'ape001' as apellido 	-- Caso base: cadena "ape001"
    
    UNION ALL
    
    SELECT 
    n + 1,
    CONCAT('ape', LPAD(n + 1, 3, '0'))  -- Rellena con ceros a la izquierda hasta 3 dígitos y Creamos el apellido

    FROM apellidos
    WHERE n < 100           -- Condición de parada: hasta 100, contando que el caso base es 1, y por eso no es <=
) SELECT apellido FROM apellidos;



-- Creamos la tabla temporal de numeros

DROP TEMPORARY TABLE IF EXISTS temp_numeros;

CREATE TEMPORARY TABLE temp_numeros(num TINYINT UNIQUE);

INSERT INTO temp_numeros(num) WITH RECURSIVE numeros AS (
    SELECT 
    1 as n		-- Caso base: empezar en 1
    
    UNION
    
    SELECT 
    n + 1

    FROM numeros
    WHERE n < 30           -- Condición de parada: hasta 30, contando que el caso base es 1, y por eso no es <=
) SELECT n FROM numeros;



-- Creamos las credenciales

INSERT INTO credencial_acceso(hashPassword, salt, ultimoCambio, requireReset) WITH RECURSIVE datos_credenciales AS (
	SELECT
    1 as n,		-- Caso base, empieza por 1.
	SHA2(CONCAT('password', 1, UUID()), 256) as hashPass,	-- Los hashes usan UUID() para que en cada iteración sean diferentes, evitando que tengan todos el mismo hash
    MD5(CONCAT('salt', 1, UUID())) as sal,
    DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 2000) DAY) as fecha,
    IF(RAND() < 0.1, 1, 0) as requiereReset
    
    UNION ALL
    
    SELECT
	n + 1,
	SHA2(CONCAT(hashPass, n + 1, UUID()), 256),		
    MD5(CONCAT(sal, n + 1, UUID())),			
    DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 2000) DAY),
    IF(RAND() < 0.1, 1, 0)
    
    FROM datos_credenciales
    WHERE n < 300000
) SELECT hashPass, sal, fecha, requiereReset FROM datos_credenciales;

-- Creamos los usuarios

INSERT INTO usuarios (username, email, activo, fechaRegistro, credencial_id)
SELECT 
    CONCAT(temp_nombres.nombre, '_', temp_apellidos.apellido, '_', temp_numeros.num) as usuario,
    CONCAT(temp_nombres.nombre, '_', temp_apellidos.apellido, '_', temp_numeros.num, IF(RAND(10) < 0.5, '@gmail.com', '@hotmail.com')),
    IF(RAND() < 0.1, 1, 0) as activo,
    DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 2000) DAY) as fecha,
    ROW_NUMBER() OVER () + 1 as credencial_id	-- Al ya tener un usuario generado en el archivo "01_esquema", tuve que agregarle un + 1 para que funcione correctamente 
FROM temp_nombres
CROSS JOIN temp_apellidos  
CROSS JOIN temp_numeros;


-- ULTIMA VERIFICACIÓN: Verificamos las cantidades de los datos, deberían ser 300.000 en cada tabla.
SELECT
    (SELECT COUNT(*) FROM usuarios) AS usuarios,
    (SELECT COUNT(*) FROM credencial_acceso) AS credenciales;

