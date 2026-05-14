# Leo

Aplicación Android para la gestión, lectura y reproducción de libros digitales y audiolibros.

## Descripción

Leo es una aplicación Android diseñada para organizar, leer y reproducir libros en distintos formatos, principalmente PDF y audio. La app permite importar archivos desde el dispositivo, extraer portadas y metadatos, guardar automáticamente el progreso de lectura o reproducción y mantener una biblioteca local ordenada de forma sencilla.

La aplicación sigue una arquitectura MVVM y utiliza almacenamiento local con Room, junto con Kotlin Coroutines y Flow para manejar operaciones asíncronas y datos reactivos.

## Características

- Lectura de archivos PDF.
- Reproducción de audiolibros.
- Guardado automático del progreso de lectura y reproducción.
- Gestión de biblioteca local.
- Importación de archivos PDF y audio.
- Extracción automática de portadas y metadatos.
- Organización de libros por estados y colecciones.
- Navegación entre biblioteca, búsqueda, leídos y colecciones.
- Interfaz adaptada a orientación vertical y horizontal.
- Arquitectura MVVM.
- Persistencia local con Room.
- Compatibilidad con Android 8.1+.

## Capturas de pantalla

Puedes añadir aquí imágenes de la aplicación para mostrar:

- Pantalla principal.
- Biblioteca.
- Visor PDF.
- Reproductor de audio.
- Buscador.
- Sección de libros leídos.

## Tecnologías utilizadas

- Kotlin
- Android SDK
- Room Database
- ViewModel
- Kotlin Coroutines
- Kotlin Flow
- RecyclerView
- MediaPlayer
- AndroidPdfViewer
- Material Design 3
- ConstraintLayout
- Figma

## Arquitectura

La aplicación está estructurada siguiendo el patrón MVVM:

- **Model**: entidades, base de datos, DAO y repositorios.
- **View**: actividades, fragments y adaptadores.
- **ViewModel**: capa intermedia entre interfaz y datos.

Además, se emplean:
- **Room** para la persistencia local.
- **Repository** para centralizar el acceso a datos.
- **Flow** para observar cambios en tiempo real.
- **Coroutines** para tareas asíncronas.

## Estructura del proyecto

- `data/`: acceso a datos, base de datos, DAO y repositorios.
- `entities/`: entidades del modelo de datos.
- `ui/activity/`: actividades principales de la aplicación.
- `ui/fragment/`: fragmentos de interfaz.
- `ui/adapter/`: adaptadores de listas.
- `ui/viewmodel/`: ViewModels y factories.
- `util/`: clases auxiliares y utilidades.
- `res/`: recursos XML, layouts, drawables y valores.

## Requisitos

- Android Studio Otter 2 Feature Drop | 2025.2.2.
- JDK integrado de Android Studio.
- SDK mínimo: API 27.
- Target SDK: API 36.

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/Crp35/TFC.git
```

### 2. Abrir el proyecto

Abrir Android Studio y seleccionar `File > Open`, eligiendo la carpeta raíz del proyecto.

### 3. Sincronizar Gradle

Esperar a que Android Studio sincronice las dependencias del proyecto.

### 4. Ejecutar la aplicación

Seleccionar un dispositivo físico o emulador y pulsar `Run`.

## Permisos necesarios

La aplicación puede requerir acceso al almacenamiento o al selector de archivos del sistema para importar documentos y audiolibros. Los permisos exactos dependen de la versión de Android y del origen del archivo.

## Uso de la aplicación

1. Abrir la app.
2. Importar un libro PDF o un audiolibro.
3. Acceder a la biblioteca.
4. Leer o reproducir el contenido.
5. Guardar y recuperar automáticamente el progreso.

## Generación del APK

Para generar el APK desde Android Studio:

1. Abrir el proyecto.
2. Sincronizar Gradle.
3. Seleccionar la variante `release` si se desea distribución.
4. Ir a `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
5. Para distribución externa, usar `Build > Generate Signed Bundle / APK`.

## Estado del proyecto

Proyecto en desarrollo.

## Licencia

Copyright © 2026 Crispín Ondó Mayíe.

Todos los derechos reservados.

## Licencia

Copyright © 2026 Leo (Gestor y lector de libros).Reader.

Todos los derechos reservados. All rights reserved.

No se permite la reproducción, distribución, modificación ni uso comercial de este software sin autorización previa y por escrito del autor.

El código fuente se proporciona únicamente con fines de evaluación académica y portfolio profesional.