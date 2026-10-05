# Universidades y dominios institucionales

GET /api/v1/universidades es público. El registro resuelve la universidad desde el correo del token Firebase, no desde el valor enviado por el cliente. La coincidencia del dominio es exacta, sin permitir automáticamente subdominios. La verificación Firebase sigue siendo obligatoria para usar la aplicación. Un dominio institucional no demuestra matrícula vigente.

La sede del perfil es texto opcional (hasta 150 caracteres). Ausencia o null se guardan como cadena vacía para conservar compatibilidad con las columnas NOT NULL existentes. La universidad del perfil no es editable mediante PUT.

El catálogo reside en universidades y dominios_universidad. Las semillas INSERT IGNORE no sobrescriben decisiones de los administradores al reiniciar. Para añadir una institución, insertar primero su código/nombre/sigla y luego sus dominios exactos; para retirar un dominio establecer activo=FALSE. No requiere desplegar el frontend. El cliente refresca el catálogo al volver al formulario y tras un minuto.

## Fuentes de los dominios iniciales

Se conservan los dominios del catálogo existente y se añaden los siguientes alias/instituciones confirmados en fuentes oficiales:

- PUCP (pucp.pe y pucp.edu.pe): https://agora.pucp.edu.pe/tutorial/correoweb/target2.php?id=36
- UPN (upn.pe y upn.edu.pe): https://contacto.upn.edu.pe/es_ES/acceso-a-mis-cursos y https://www.upn.edu.pe/noticias/potenciamos-la-formacion-internacional-de-nuestros-docentes-con-la-plataforma-academy-del
- USIL (usil.pe y epg.usil.pe): https://usil.edu.pe/servicios/tramites-academicos/constancia-de-ingreso
- UTP (utp.edu.pe): https://contrasena.utp.edu.pe/recuperacion/olvidemiclave.aspx
- UPC: https://mi-epg.upc.edu.pe/guia-del-estudiante/servicios-digitales/
- ULima: https://www.ulima.edu.pe/en/node/871
- UNI: https://portal.uni.edu.pe/index.php/2015-05-28-20-25-15/obten-tu-correo-uni
