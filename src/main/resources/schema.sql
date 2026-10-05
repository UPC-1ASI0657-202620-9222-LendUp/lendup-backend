CREATE TABLE IF NOT EXISTS `roles` (
  `id` CHAR(36) NOT NULL,
  `codigo` VARCHAR(20) NOT NULL,
  `descripcion` VARCHAR(150) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roles_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `usuarios` (
  `id` CHAR(36) NOT NULL,
  `firebase_uid` VARCHAR(128) NOT NULL,
  `correo_institucional` VARCHAR(254) NOT NULL,
  `rol_id` CHAR(36) NOT NULL,
  `estado_verificacion` VARCHAR(20) NOT NULL DEFAULT 'NO_VERIFICADO',
  `verificado_en` DATETIME(6),
  `version_terminos_aceptada` VARCHAR(50),
  `version_descargo_aceptada` VARCHAR(50),
  `aceptados_en` DATETIME(6),
  `creado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `verificacion_solicitada_en` DATETIME(6),
  `verificacion_referencia` VARCHAR(255),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_usuarios_firebase_uid` (`firebase_uid`),
  UNIQUE KEY `uq_usuarios_correo_institucional` (`correo_institucional`),
  KEY `idx_usuarios_0` (`rol_id`),
  CONSTRAINT `fk_usuarios_rol_id` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `perfiles` (
  `id` CHAR(36) NOT NULL,
  `usuario_id` CHAR(36) NOT NULL,
  `nombre` VARCHAR(150) NOT NULL,
  `universidad` VARCHAR(150) NOT NULL,
  `campus` VARCHAR(150) NOT NULL,
  `carrera` VARCHAR(150) NOT NULL,
  `ciclo` SMALLINT NOT NULL,
  `telefono` VARCHAR(30) NOT NULL,
  `foto_url` TEXT,
  `creado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_perfiles_usuario_id` (`usuario_id`),
  CONSTRAINT `fk_perfiles_usuario_id` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `categorias` (
  `id` CHAR(36) NOT NULL,
  `codigo` VARCHAR(50) NOT NULL,
  `nombre` VARCHAR(100) NOT NULL,
  `activa` BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_categorias_codigo` (`codigo`),
  UNIQUE KEY `uq_categorias_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `publicaciones` (
  `id` CHAR(36) NOT NULL,
  `propietario_usuario_id` CHAR(36) NOT NULL,
  `categoria_id` CHAR(36) NOT NULL,
  `titulo` VARCHAR(180) NOT NULL,
  `descripcion` TEXT NOT NULL,
  `condicion_objeto` TEXT NOT NULL,
  `universidad` VARCHAR(150) NOT NULL,
  `campus` VARCHAR(150) NOT NULL,
  `ubicacion` VARCHAR(250) NOT NULL,
  `lugar_intercambio` VARCHAR(250) NOT NULL,
  `latitud` DECIMAL(9,6),
  `longitud` DECIMAL(9,6),
  `tarifa_diaria` DECIMAL(12,2) NOT NULL,
  `garantia_monetaria` DECIMAL(12,2),
  `moneda` CHAR(3) NOT NULL,
  `condiciones_uso` TEXT NOT NULL,
  `condiciones_entrega` TEXT NOT NULL,
  `condiciones_devolucion` TEXT NOT NULL,
  `condiciones_cancelacion` TEXT NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
  `creado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_publicaciones_0` (`propietario_usuario_id`),
  KEY `idx_publicaciones_1` (`estado`, `categoria_id`, `universidad`, `campus`),
  CONSTRAINT `fk_publicaciones_categoria_id` FOREIGN KEY (`categoria_id`) REFERENCES `categorias` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `disponibilidades_publicacion` (
  `id` CHAR(36) NOT NULL,
  `publicacion_id` CHAR(36) NOT NULL,
  `desde` DATETIME(6) NOT NULL,
  `hasta` DATETIME(6) NOT NULL,
  `creado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_disponibilidades_publicacion_0` (`publicacion_id`, `desde`, `hasta`),
  CONSTRAINT `fk_disponibilidades_publicaci_publicacion_id` FOREIGN KEY (`publicacion_id`) REFERENCES `publicaciones` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `imagenes_publicacion` (
  `id` CHAR(36) NOT NULL,
  `publicacion_id` CHAR(36) NOT NULL,
  `cloudinary_public_id` VARCHAR(255) NOT NULL,
  `url` TEXT NOT NULL,
  `orden` INTEGER NOT NULL,
  `creado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_imagenes_publicacion_0` (`publicacion_id`, `orden`),
  KEY `idx_imagenes_publicacion_0` (`publicacion_id`),
  CONSTRAINT `fk_imagenes_publicacion_publicacion_id` FOREIGN KEY (`publicacion_id`) REFERENCES `publicaciones` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `bloqueos_reserva_lectura` (
  `id` CHAR(36) NOT NULL,
  `reserva_id` CHAR(36) NOT NULL,
  `publicacion_id` CHAR(36) NOT NULL,
  `desde` DATETIME(6) NOT NULL,
  `hasta` DATETIME(6) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'VIGENTE',
  `actualizado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bloqueos_reserva_lectura_reserva_id` (`reserva_id`),
  KEY `idx_bloqueos_reserva_lectura_0` (`publicacion_id`, `estado`, `desde`, `hasta`),
  CONSTRAINT `fk_bloqueos_reserva_lectura_publicacion_id` FOREIGN KEY (`publicacion_id`) REFERENCES `publicaciones` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `solicitudes` (
  `id` CHAR(36) NOT NULL,
  `publicacion_id` CHAR(36) NOT NULL,
  `prestatario_usuario_id` CHAR(36) NOT NULL,
  `prestamista_usuario_id` CHAR(36) NOT NULL,
  `desde` DATETIME(6) NOT NULL,
  `hasta` DATETIME(6) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  `tarifa_diaria_aceptada` DECIMAL(12,2) NOT NULL,
  `garantia_monetaria_aceptada` DECIMAL(12,2),
  `moneda_aceptada` CHAR(3) NOT NULL,
  `lugar_intercambio_aceptado` VARCHAR(250) NOT NULL,
  `condiciones_uso_aceptadas` TEXT NOT NULL,
  `condiciones_entrega_aceptadas` TEXT NOT NULL,
  `condiciones_devolucion_aceptadas` TEXT NOT NULL,
  `condiciones_cancelacion_aceptadas` TEXT NOT NULL,
  `condiciones_aceptadas_en` DATETIME(6) NOT NULL,
  `creada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `respondida_en` DATETIME(6),
  `motivo_rechazo` TEXT,
  `cancelada_en` DATETIME(6),
  `actualizada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_solicitudes_0` (`prestatario_usuario_id`, `estado`, `creada_en`),
  KEY `idx_solicitudes_1` (`prestamista_usuario_id`, `estado`, `creada_en`),
  KEY `idx_solicitudes_2` (`publicacion_id`, `desde`, `hasta`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `agendas_objeto` (
  `id` CHAR(36) NOT NULL,
  `publicacion_id` CHAR(36) NOT NULL,
  `version` INTEGER NOT NULL DEFAULT '0',
  `creada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_agendas_objeto_publicacion_id` (`publicacion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `reservas` (
  `id` CHAR(36) NOT NULL,
  `solicitud_id` CHAR(36) NOT NULL,
  `agenda_id` CHAR(36) NOT NULL,
  `desde` DATETIME(6) NOT NULL,
  `hasta` DATETIME(6) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
  `tarifa_diaria_acordada` DECIMAL(12,2) NOT NULL,
  `garantia_monetaria_acordada` DECIMAL(12,2),
  `moneda_acordada` CHAR(3) NOT NULL,
  `lugar_intercambio_acordado` VARCHAR(250) NOT NULL,
  `condiciones_uso_acordadas` TEXT NOT NULL,
  `condiciones_entrega_acordadas` TEXT NOT NULL,
  `condiciones_devolucion_acordadas` TEXT NOT NULL,
  `condiciones_cancelacion_acordadas` TEXT NOT NULL,
  `confirmada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `cancelada_en` DATETIME(6),
  `cancelada_por_usuario_id` CHAR(36),
  `motivo_cancelacion` TEXT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reservas_solicitud_id` (`solicitud_id`),
  KEY `idx_reservas_0` (`agenda_id`, `estado`, `desde`, `hasta`),
  CONSTRAINT `fk_reservas_solicitud_id` FOREIGN KEY (`solicitud_id`) REFERENCES `solicitudes` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_reservas_agenda_id` FOREIGN KEY (`agenda_id`) REFERENCES `agendas_objeto` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `prestamos` (
  `id` CHAR(36) NOT NULL,
  `reserva_id` CHAR(36) NOT NULL,
  `publicacion_id` CHAR(36) NOT NULL,
  `prestamista_usuario_id` CHAR(36) NOT NULL,
  `prestatario_usuario_id` CHAR(36) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'RESERVADO',
  `entrega_programada_en` DATETIME(6) NOT NULL,
  `devolucion_original_en` DATETIME(6) NOT NULL,
  `devolucion_vigente_en` DATETIME(6) NOT NULL,
  `tarifa_diaria_acordada` DECIMAL(12,2) NOT NULL,
  `moneda` CHAR(3) NOT NULL,
  `garantia_requerida` BOOLEAN NOT NULL,
  `pago_tarifa_confirmado_en` DATETIME(6),
  `garantia_constituida_en` DATETIME(6),
  `entrega_registrada_en` DATETIME(6),
  `recepcion_confirmada_en` DATETIME(6),
  `activado_en` DATETIME(6),
  `devolucion_registrada_en` DATETIME(6),
  `devolucion_confirmada_en` DATETIME(6),
  `incidencias_pendientes` INTEGER NOT NULL DEFAULT '0',
  `vencido_en` DATETIME(6),
  `finalizado_en` DATETIME(6),
  `cancelado_en` DATETIME(6),
  `creado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_prestamos_reserva_id` (`reserva_id`),
  KEY `idx_prestamos_0` (`prestatario_usuario_id`, `estado`),
  KEY `idx_prestamos_1` (`prestamista_usuario_id`, `estado`),
  KEY `idx_prestamos_2` (`estado`, `devolucion_vigente_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cambios_fecha_prestamo` (
  `id` CHAR(36) NOT NULL,
  `prestamo_id` CHAR(36) NOT NULL,
  `tipo` VARCHAR(20) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'PENDIENTE',
  `propuesto_por_usuario_id` CHAR(36) NOT NULL,
  `fecha_anterior_en` DATETIME(6) NOT NULL,
  `fecha_propuesta_en` DATETIME(6) NOT NULL,
  `costo_adicional` DECIMAL(12,2),
  `moneda` CHAR(3),
  `propuesto_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `respondido_por_usuario_id` CHAR(36),
  `respondido_en` DATETIME(6),
  `motivo_rechazo` TEXT,
  `transaccion_pago_id` CHAR(36),
  `pago_confirmado_en` DATETIME(6),
  `fecha_resultante_en` DATETIME(6),
  `aplicado_en` DATETIME(6),
  PRIMARY KEY (`id`),
  KEY `idx_cambios_fecha_prestamo_0` (`prestamo_id`, `propuesto_en`),
  CONSTRAINT `fk_cambios_fecha_prestamo_prestamo_id` FOREIGN KEY (`prestamo_id`) REFERENCES `prestamos` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `incidencias` (
  `id` CHAR(36) NOT NULL,
  `prestamo_id` CHAR(36) NOT NULL,
  `reportada_por_usuario_id` CHAR(36) NOT NULL,
  `tipo` VARCHAR(24) NOT NULL,
  `descripcion` TEXT NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  `reportada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `revision_iniciada_en` DATETIME(6),
  `resuelta_en` DATETIME(6),
  `resuelta_por_usuario_id` CHAR(36),
  `justificacion_resolucion` TEXT,
  `decision_garantia` VARCHAR(24),
  `monto_garantia_afectado` DECIMAL(12,2),
  `saldo_garantia_previsto` DECIMAL(12,2),
  `moneda` CHAR(3),
  `actualizada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_incidencias_0` (`estado`, `tipo`, `reportada_en`),
  KEY `idx_incidencias_1` (`prestamo_id`, `estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `evidencias` (
  `id` CHAR(36) NOT NULL,
  `prestamo_id` CHAR(36) NOT NULL,
  `incidencia_id` CHAR(36),
  `registrada_por_usuario_id` CHAR(36) NOT NULL,
  `etapa` VARCHAR(20) NOT NULL,
  `tipo` VARCHAR(20) NOT NULL,
  `url` TEXT,
  `cloudinary_public_id` VARCHAR(255),
  `observacion` TEXT,
  `registrada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `estado_integracion` VARCHAR(24) NOT NULL DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id`),
  KEY `idx_evidencias_0` (`prestamo_id`, `etapa`, `registrada_en`),
  KEY `idx_evidencias_1` (`incidencia_id`),
  CONSTRAINT `fk_evidencias_incidencia_id` FOREIGN KEY (`incidencia_id`) REFERENCES `incidencias` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analisis_evidencias` (
  `id` CHAR(36) NOT NULL,
  `prestamo_id` CHAR(36) NOT NULL,
  `incidencia_id` CHAR(36),
  `evidencia_inicial_id` CHAR(36) NOT NULL,
  `evidencia_final_id` CHAR(36) NOT NULL,
  `solicitado_por_usuario_id` CHAR(36) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  `solicitado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `completado_en` DATETIME(6),
  `resultado_resumen` TEXT,
  `resultado_detalle` JSON,
  `error_descripcion` TEXT,
  PRIMARY KEY (`id`),
  KEY `idx_analisis_evidencias_0` (`prestamo_id`, `solicitado_en`),
  KEY `idx_analisis_evidencias_1` (`incidencia_id`),
  CONSTRAINT `fk_analisis_evidencias_incidencia_id` FOREIGN KEY (`incidencia_id`) REFERENCES `incidencias` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_analisis_evidencias_evidencia_inicial_id` FOREIGN KEY (`evidencia_inicial_id`) REFERENCES `evidencias` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_analisis_evidencias_evidencia_final_id` FOREIGN KEY (`evidencia_final_id`) REFERENCES `evidencias` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `observaciones_incidencia` (
  `id` CHAR(36) NOT NULL,
  `incidencia_id` CHAR(36) NOT NULL,
  `administrador_usuario_id` CHAR(36) NOT NULL,
  `contenido` TEXT NOT NULL,
  `registrada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_observaciones_incidencia_0` (`incidencia_id`, `registrada_en`),
  CONSTRAINT `fk_observaciones_incidencia_incidencia_id` FOREIGN KEY (`incidencia_id`) REFERENCES `incidencias` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `garantias` (
  `id` CHAR(36) NOT NULL,
  `prestamo_id` CHAR(36) NOT NULL,
  `monto_acordado` DECIMAL(12,2) NOT NULL,
  `moneda` CHAR(3) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'PENDIENTE',
  `monto_constituido` DECIMAL(12,2),
  `monto_afectado` DECIMAL(12,2) NOT NULL DEFAULT '0',
  `monto_devuelto` DECIMAL(12,2) NOT NULL DEFAULT '0',
  `constituida_en` DATETIME(6),
  `cerrada_en` DATETIME(6),
  `creada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_garantias_prestamo_id` (`prestamo_id`),
  KEY `idx_garantias_0` (`estado`, `actualizada_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `transacciones_economicas` (
  `id` CHAR(36) NOT NULL,
  `prestamo_id` CHAR(36) NOT NULL,
  `garantia_id` CHAR(36),
  `cambio_fecha_prestamo_id` CHAR(36),
  `incidencia_id` CHAR(36),
  `tipo` VARCHAR(32) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  `monto` DECIMAL(12,2) NOT NULL,
  `moneda` CHAR(3) NOT NULL,
  `comision_proveedor` DECIMAL(12,2),
  `medio_pago_seleccionado` VARCHAR(100),
  `referencia_proveedor` VARCHAR(150),
  `clave_idempotencia` VARCHAR(150) NOT NULL,
  `solicitada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `confirmada_en` DATETIME(6),
  `actualizada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_transacciones_economicas_clave_idempotencia` (`clave_idempotencia`),
  KEY `idx_transacciones_economicas_0` (`prestamo_id`, `solicitada_en`),
  KEY `idx_transacciones_economicas_1` (`garantia_id`, `solicitada_en`),
  KEY `idx_transacciones_economicas_2` (`referencia_proveedor`),
  CONSTRAINT `fk_transacciones_economicas_garantia_id` FOREIGN KEY (`garantia_id`) REFERENCES `garantias` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `calificaciones` (
  `id` CHAR(36) NOT NULL,
  `prestamo_id` CHAR(36) NOT NULL,
  `evaluador_usuario_id` CHAR(36) NOT NULL,
  `evaluado_usuario_id` CHAR(36) NOT NULL,
  `puntaje` SMALLINT NOT NULL,
  `comentario` VARCHAR(1000),
  `registrada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_calificaciones_0` (`prestamo_id`, `evaluador_usuario_id`),
  KEY `idx_calificaciones_0` (`evaluado_usuario_id`, `registrada_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `notificaciones` (
  `id` CHAR(36) NOT NULL,
  `destinatario_usuario_id` CHAR(36) NOT NULL,
  `clave_notificacion` VARCHAR(200) NOT NULL,
  `tipo` VARCHAR(20) NOT NULL,
  `origen_tipo` VARCHAR(20) NOT NULL,
  `origen_id` CHAR(36) NOT NULL,
  `evento_origen_id` VARCHAR(150),
  `titulo` VARCHAR(180) NOT NULL,
  `mensaje` TEXT NOT NULL,
  `estado` VARCHAR(20) NOT NULL,
  `programada_para` DATETIME(6),
  `disponible_en` DATETIME(6),
  `leida_en` DATETIME(6),
  `cancelada_en` DATETIME(6),
  `enviar_correo` BOOLEAN NOT NULL DEFAULT FALSE,
  `estado_correo` VARCHAR(20) NOT NULL DEFAULT 'NO_APLICA',
  `intentos_correo` INTEGER NOT NULL DEFAULT '0',
  `ultimo_intento_correo_en` DATETIME(6),
  `correo_enviado_en` DATETIME(6),
  `ultimo_error_correo` TEXT,
  `creada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_notificaciones_0` (`destinatario_usuario_id`, `clave_notificacion`),
  KEY `idx_notificaciones_0` (`destinatario_usuario_id`, `estado`, `creada_en`),
  KEY `idx_notificaciones_1` (`estado`, `programada_para`),
  KEY `idx_notificaciones_2` (`estado_correo`, `ultimo_intento_correo_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `webhook_events` (
  `id` CHAR(36) NOT NULL PRIMARY KEY,
  `proveedor` VARCHAR(40) NOT NULL,
  `evento_proveedor_id` VARCHAR(150),
  `payload_json` JSON NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
  `recibido_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY `uq_webhook_proveedor_evento` (`proveedor`,`evento_proveedor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS universidades (
  codigo VARCHAR(30) PRIMARY KEY, nombre VARCHAR(150) NOT NULL, sigla VARCHAR(30) NOT NULL, activa BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS dominios_universidad (
  dominio VARCHAR(253) PRIMARY KEY, universidad_codigo VARCHAR(30) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_dominio_universidad FOREIGN KEY (universidad_codigo) REFERENCES universidades(codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
