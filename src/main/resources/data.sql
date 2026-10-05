INSERT IGNORE INTO roles (id,codigo,descripcion) VALUES
('00000000-0000-4000-8000-000000000001','ESTUDIANTE','Estudiante'),
('00000000-0000-4000-8000-000000000002','ADMINISTRADOR','Administrador');
INSERT IGNORE INTO categorias (id,codigo,nombre,activa) VALUES
('00000000-0000-4000-8000-000000000003','OTROS','Otros',TRUE),
('00000000-0000-4000-8000-000000000004','CALCULADORAS','Calculadoras',TRUE),
('00000000-0000-4000-8000-000000000005','CAMARAS','Cámaras',TRUE),
('00000000-0000-4000-8000-000000000006','LIBROS','Libros',TRUE),
('00000000-0000-4000-8000-000000000007','HERRAMIENTAS','Herramientas',TRUE),
('00000000-0000-4000-8000-000000000008','ELECTRONICA','Electrónica',TRUE);

INSERT IGNORE INTO universidades (codigo,nombre,sigla) VALUES
('UPC','Universidad Peruana de Ciencias Aplicadas','UPC'),
('PUCP','Pontificia Universidad Católica del Perú','PUCP'),
('ULIMA','Universidad de Lima','ULima'),
('UNI','Universidad Nacional de Ingeniería','UNI'),
('UNMSM','Universidad Nacional Mayor de San Marcos','UNMSM'),
('UTP','Universidad Tecnológica del Perú','UTP'),
('UPN','Universidad Privada del Norte','UPN'),
('USIL','Universidad San Ignacio de Loyola','USIL');
INSERT IGNORE INTO dominios_universidad (dominio,universidad_codigo) VALUES
('upc.edu.pe','UPC'),('pucp.edu.pe','PUCP'),('pucp.pe','PUCP'),
('aloe.ulima.edu.pe','ULIMA'),('ulima.edu.pe','ULIMA'),
('uni.pe','UNI'),('uni.edu.pe','UNI'),('unmsm.edu.pe','UNMSM'),
('utp.edu.pe','UTP'),('upn.pe','UPN'),('upn.edu.pe','UPN'),('usil.pe','USIL'),('epg.usil.pe','USIL');
