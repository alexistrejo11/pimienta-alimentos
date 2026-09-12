# Entorno de desarrollo Android

El proyecto usa el Gradle Wrapper (`gradlew.bat`) y el plugin de Kotlin de
Gradle. No es necesario instalar `gradle` ni `kotlinc` globalmente. Android
Studio incluye un JDK compatible y Gradle compila Kotlin a través del plugin.

## Configuración recomendada en Windows

1. Instalar Android Studio estable.
2. Abrir este proyecto desde Android Studio.
3. Ir a `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`.
4. En `Gradle JDK`, seleccionar `Embedded JDK (jbr)`.
5. Dejar `Gradle distribution` en `gradle-wrapper.properties` / Wrapper.
6. Sincronizar el proyecto y ejecutar la configuración `app`.

El JDK requerido debe ser el que soporte la versión de Android Gradle Plugin
del catálogo. No se debe fijar una ruta absoluta de Java en el repositorio,
porque cambiaría entre computadoras.

## Terminal PowerShell

Para compilar fuera de Android Studio, usar:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
& "$env:JAVA_HOME\bin\java.exe" -version
.\gradlew.bat :app:assembleDebug
```

También se puede cargar el JDK automáticamente en la sesión actual:

```powershell
. .\scripts\use-android-studio-jdk.ps1
.\gradlew.bat :app:assembleDebug
```

El script también fija `GRADLE_USER_HOME` dentro de tu perfil de usuario para
que la caché y las descargas del Wrapper no intenten escribirse en la raíz del
disco.

La ruta puede variar si Android Studio se instaló en otra carpeta. También se
puede configurar `JAVA_HOME` permanentemente desde las variables de entorno de
Windows. En tu computadora también existe el JDK `C:\Users\Alexis\.jdks\openjdk-26.0.2.1`,
y el script lo intenta detectar automáticamente. El proyecto no necesita
`KOTLIN_HOME`.

JDK 26 es relativamente nuevo; si Gradle o Android Gradle Plugin reporta una
versión no soportada, selecciona `Embedded JDK (jbr)` en Android Studio o usa
un JDK LTS compatible.

## Diagnóstico rápido

```powershell
Get-Command java
java -version
$env:JAVA_HOME
.\gradlew.bat --version
```

Si Android Studio compila pero PowerShell no, el problema es únicamente la
variable `JAVA_HOME` de la terminal. Se debe cerrar y abrir la terminal después
de cambiar variables de entorno.

## Nota sobre el backend

En `SANDBOX` el POS clona la plantilla `pos-training-bootstrap.json` en una base
scratch desechable (`pimienta-pos-training.db`) sin red. El build debug arranca
en playground pinless con botón para reiniciar datos demo. En `PRODUCTION` el
cliente ya habla Device API (enrolamiento,
bootstrap/deltas y eventos). El mapeo histórico en
[../backend/backend-pos-mapping.md](../backend/backend-pos-mapping.md) es
referencia; el contrato canónico está en
[../../integration/01-contrato-implementado.md](../../integration/01-contrato-implementado.md).
