INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Cerrar la llave mientras te cepillas',
       'Ahorra agua cerrando la llave mientras te lavas los dientes.',
       'AGUA',
       10,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Cerrar la llave mientras te cepillas'
);

INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Reutilizar agua para plantas',
       'Usa agua reutilizable para regar una planta con ayuda de un adulto.',
       'AGUA',
       15,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Reutilizar agua para plantas'
);

INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Apagar luces innecesarias',
       'Apaga las luces de habitaciones que no se estén usando.',
       'ENERGIA',
       10,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Apagar luces innecesarias'
);

INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Desconectar cargadores',
       'Desconecta cargadores que no estén siendo utilizados.',
       'ENERGIA',
       15,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Desconectar cargadores'
);

INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Separar residuos reciclables',
       'Clasifica papel, plástico o cartón en el contenedor correcto.',
       'RECICLAJE',
       20,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Separar residuos reciclables'
);

INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Reutilizar una caja o botella',
       'Convierte una caja o botella limpia en un objeto útil o decorativo.',
       'RECICLAJE',
       20,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Reutilizar una caja o botella'
);

INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Observar insectos sin dañarlos',
       'Observa insectos en el jardín o parque sin tocarlos ni hacerles daño.',
       'BIODIVERSIDAD',
       15,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Observar insectos sin dañarlos'
);

INSERT INTO misiones (titulo, descripcion, categoria, semillas_recompensa, activa)
SELECT 'Cuidar una planta',
       'Riega o limpia cuidadosamente una planta con ayuda de un adulto.',
       'BIODIVERSIDAD',
       20,
       true
WHERE NOT EXISTS (
    SELECT 1 FROM misiones WHERE titulo = 'Cuidar una planta'
);
