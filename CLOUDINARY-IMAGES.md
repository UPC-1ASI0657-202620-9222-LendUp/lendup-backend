# Fotos de objetos

Configurar CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET únicamente en el backend. No se requiere un upload preset sin firma. La aplicación arranca sin estas variables; los intentos de subir devuelven 503.

- POST /api/v1/objetos/{id}/imagenes: multipart/form-data con file y uploadId (UUID de reintento). Devuelve id, url HTTPS y orden.
- DELETE /api/v1/objetos/{id}/imagenes/{imageId}: elimina la referencia y el recurso Cloudinary con invalidación de CDN.
- GET /api/v1/objetos y GET /api/v1/objetos/{id} incluyen imagenes.

Solo el propietario con correo verificado puede modificar las fotos. Máximo 6 por objeto; JPG/PNG/WebP, 5 MB cada una. Se validan las firmas de archivo y Cloudinary valida el contenido. La fila de publicación se bloquea durante la operación para mantener el límite ante concurrencia. Los archivos se guardan bajo lendup/objetos/{publicationId}/{imageId}; MySQL conserva la URL, el identificador y el orden. La primera foto es la portada.

El objeto se guarda antes que las fotos. Si falla una foto, el formulario conserva el id y los archivos pendientes; reintentar no vuelve a crear el objeto y uploadId evita duplicados de la misma solicitud. Un fallo de inserción intenta limpiar el archivo subido. Los fallos de limpieza se registran para revisión.

La prueba con la cuenta Cloudinary real se realiza después del despliegue con una cuenta de LendUp: publicar una foto, comprobar tarjeta/detalle, editar y quitarla. Las pruebas automatizadas utilizan un proveedor simulado, sin credenciales ni recursos de producción.

Protocolo de firmas: https://cloudinary.com/documentation/authentication_signatures
Upload/Delete: https://cloudinary.com/documentation/image_upload_api_reference

Comprobación de autodeploy: este cambio de documentación permite verificar que Render despliega los commits nuevos de main.

Segunda comprobación de autodeploy: verificar en Render el despliegue automático de este commit.
