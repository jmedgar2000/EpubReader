Lector EPUB básico (WebView)

COMPILAR EN LÍNEA (sin Android Studio)
1. Crea una cuenta en github.com y un repositorio nuevo (público o privado).
2. Sube TODO el contenido de esta carpeta, incluida la carpeta oculta .github
   (si la web no la sube, crea el archivo a mano: Add file > Create new file,
   escribe  .github/workflows/build.yml  y pega el contenido de ese archivo).
3. Entra en la pestaña "Actions": se compila solo (3 a 6 minutos).
   Si no arranca, elige "Compilar APK" > "Run workflow".
4. Abre la ejecución terminada y descarga "lector-epub-apk" (abajo, en Artifacts).
   Es un .zip que contiene app-debug.apk.
5. Instala el APK en el teléfono (permite "instalar apps desconocidas").

COMPILAR EN ANDROID STUDIO
File > Open > carpeta del proyecto > Build > Build APK(s).
