# KalendU

Agenda académica para Android. Organiza clases y tareas, programa recordatorios y comparte el horario semanal como PDF.

## Abrir y probar en Windows

1. Abre la carpeta `Studentcalendar` en Android Studio. Usa el JDK 17 para Gradle e instala Android SDK 35 y Build Tools 35.0.0.
2. Selecciona un teléfono o emulador con servicios de Google Play. El mínimo declarado es Android 6 (API 23). Para reproducir las pruebas automatizadas de interfaz se usa API 35.
3. Ejecuta desde PowerShell en la raíz del proyecto.

   ```powershell
   .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   .\gradlew.bat :app:connectedDebugAndroidTest
   ```

   El APK local de depuración queda en `app/build/outputs/apk/debug/`. Para el inicio de sesión, activa Google como proveedor en Firebase Authentication y registra los SHA-1/SHA-256 de la firma que usas. Consulta las huellas con `.\gradlew.bat :app:signingReport` y descarga un `app/google-services.json` actualizado desde tu proyecto de Firebase si hace falta.

## Datos y recordatorios

- Las clases nuevas tienen un identificador de serie y un documento por ocurrencia. El límite es 26 semanas. Al editar la serie se conservan los eventos pasados y se actualizan las ocurrencias futuras. Los eventos antiguos que carecen de identificador se editan o eliminan individualmente.
- La aplicación registra las alarmas localmente al recibir cambios confirmados por Firestore. Conserva los avisos confirmados de la cuenta activa para restaurarlos al reiniciar, incluso sin conexión. Los cambios de otro dispositivo se aplican cuando este dispositivo recibe el estado del servidor. Se necesitan el permiso de notificaciones y, cuando Android lo exige, el de alarmas exactas.
- Los sonidos importados son locales. En un dispositivo que carezca del archivo, se usa el sonido de alarma del sistema. La exportación incluye clases y tareas y permite elegir un destino mediante el selector de archivos de Android.
- `firestore.rules` es una propuesta de aislamiento por usuario para esta app. Compárala con las reglas desplegadas y pruébala en un proyecto de pruebas antes de aplicarla. Ningún flujo de GitHub Actions publica reglas ni cambia el Firebase existente.

## Integración y distribución

El flujo **Android** se ejecuta en pull requests, pushes a `main` o `master`, etiquetas `v*` y ejecuciones manuales. Compila el APK de depuración, ejecuta pruebas JVM y lint, y corre las pruebas instrumentadas en un emulador API 35. Los informes se adjuntan al run. El APK de CI usa metadatos ficticios y sirve para pruebas de compilación e interfaz. El acceso real con Google requiere Firebase y una firma registrada.

Para distribuir un APK firmado, configura un entorno de GitHub llamado `release` y agrega estos secretos.

| Secreto | Contenido |
| --- | --- |
| `FIREBASE_GOOGLE_SERVICES_JSON` | JSON de la app Android en el Firebase destinado al release. |
| `ANDROID_KEYSTORE_BASE64` | Keystore de distribución codificado en Base64. |
| `ANDROID_STORE_PASSWORD` | Contraseña del almacén. |
| `ANDROID_KEY_ALIAS` | Alias de la clave. |
| `ANDROID_KEY_PASSWORD` | Contraseña de la clave. |

Después de pasar las verificaciones de la rama, sube una etiqueta de versión como `v1.1.0` desde el commit que quieres distribuir. El flujo vuelve a ejecutar las comprobaciones, verifica la firma del APK y publica el APK y su SHA-256 en GitHub Releases. Usa la misma clave que firmó la instalación anterior si necesitas actualizarla en el dispositivo.

## Verificación en un teléfono

| Flujo | Qué comprobar |
| --- | --- |
| Acceso y perfil | Inicia sesión, cambia el nombre, cierra sesión y vuelve con la cuenta correcta. Comprueba el modo claro, oscuro y los cinco idiomas. |
| Clases | Crea dos series con el mismo nombre. Edita una y borra una ocurrencia. Verifica que la segunda serie conserve sus fechas. |
| Tareas | Crea una tarea y cambia su fecha. Comprueba su hora en Inicio, en la lista y en el horario semanal. |
| Alarmas | Concede ambos permisos, guarda una alarma futura y reinicia. Borra otro evento desde el horario y verifica que su alarma no suene. |
| Sincronización | Abre una misma cuenta en dos dispositivos. Cambia un recordatorio, espera la lectura confirmada y comprueba la programación en ambos. |
| PDF | Exporta una semana con una clase y una tarea. Prueba la vista previa, el guardado y compartir. Cambia de semana y comprueba que la vista previa anterior se invalida. |
| Borrar agenda | Usa una cuenta de pruebas. Cancela el selector, prueba otra cuenta y confirma con la cuenta original. Comprueba que desaparezcan únicamente sus eventos. |
