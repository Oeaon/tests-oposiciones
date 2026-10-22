# Tests Oposiciones

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white) ![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)

Tests de la Constitución Española y de las leyes 39/2015 y 40/2015 para preparar oposiciones, con práctica por temas, repaso de fallos y simulacros de examen.

App Android nativa desarrollada en **Kotlin** con **Jetpack Compose** (Material 3).

- 251 preguntas en tres temarios, cada una con el artículo que la justifica:
  - Constitución Española: 136 preguntas de sus 11 títulos.
  - Ley 39/2015, del Procedimiento Administrativo Común: 65 preguntas.
  - Ley 40/2015, de Régimen Jurídico del Sector Público: 50 preguntas.
- Selector de temario; el progreso, los fallos y la mejor nota de simulacro van por temario.
- **Práctica por temas**: 10 preguntas, primero las que aún no has acertado, con la corrección y la explicación al momento.
- **Test rápido** de todo el temario y **repaso de fallos**.
- **Simulacro de examen** (Premium o 24 h con un anuncio): 30 preguntas en 30 minutos, sin corrección hasta el final y restando un tercio por fallo.
- Progreso por tema, racha de días estudiando y mejor nota.
- Funciona sin conexión.

Las preguntas están en castellano, que es el idioma de los exámenes estatales; la interfaz está en castellano, catalán, gallego e inglés.

## Anuncios y Premium

| Formato | Dónde | Límite |
|---|---|---|
| Banner adaptativo | Pantallas de consulta | — |
| Intersticial | Solo en pausas naturales (al terminar un test, antes del resultado) | Uno cada 3 min como mucho |
| App Open | Al volver a la app desde segundo plano, nunca en el primer arranque | Uno cada 5 min como mucho |
| Recompensado | Cuando el usuario lo pide para desbloquear los simulacros de examen durante 24 h | — |

Premium (Google Play Billing) quita todos los anuncios y desbloquea los simulacros de examen para siempre:

- `premium_monthly`: suscripción mensual.
- `premium_lifetime`: producto de compra única.

Créalos en Play Console con esos IDs. Al arrancar se consulta la tienda, así que si la suscripción caduca vuelven los anuncios. Para una app con muchos ingresos conviene añadir verificación de compras en servidor.

Los IDs de AdMob van en `admob.properties` (no se sube a git; plantilla en `admob.properties.example`). Sin ese archivo se usan las unidades de prueba de Google. El consentimiento RGPD se pide con UMP antes de cargar ningún anuncio.

## Idiomas

Castellano (por defecto), catalán, gallego e inglés.

## Preguntas

Están en `app/src/main/assets/` (`constitucion.json`, `ley39.json`, `ley40.json`). Son preguntas originales redactadas a partir del texto publicado en el BOE (las normas son públicas); no se han copiado de bancos de preguntas de academias ni de exámenes. Formato:

```json
{"id": "ce-001", "topic": "ce-preliminar", "question": "…", "options": ["correcta", "falsa", "falsa", "falsa"], "explanation": "Art. 1.1 CE."}
```

La primera opción es siempre la correcta; la app las baraja. `./gradlew :core:test` comprueba que cada pregunta tiene cuatro opciones distintas, explicación y un tema válido. Cada archivo lleva además `id` y `shortTitle` (el texto del selector), y los ids de preguntas y temas llevan el prefijo del temario. Para añadir otro temario (Estatuto Básico del Empleado Público, Ley 19/2013 de Transparencia…) basta con otro archivo con el mismo formato y añadirlo a `QuestionRepository.FILES`.

## Estructura

```
core/   Kotlin puro: lectura del banco, tests, corrección con penalización, progreso y racha (con tests)
app/    Android: Compose, ViewModel y DataStore
  ui/             inicio, test, resultado y ajustes
  monetization/   consentimiento, anuncios y compras de Premium
```

## Compilar

Requisitos: Android Studio reciente, Android SDK 36 y un JDK entre el 17 y el 25 (probado con Java 25). Gradle 9.7, Android Gradle Plugin 9.4 (con Kotlin integrado) y Kotlin 2.3.

```bash
./gradlew :core:test        # tests de la lógica y del banco de preguntas
./gradlew installDebug      # instala en el emulador o el móvil (anuncios de prueba)
./gradlew bundleRelease     # AAB para Google Play
```

### Release

1. Crea el keystore de subida y un `keystore.properties` en la raíz (no se sube a git):
   ```
   storeFile=upload.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
2. Copia `admob.properties.example` como `admob.properties` y pon los IDs reales de AdMob de esta app.
3. Crea en Play Console los productos `premium_monthly` (suscripción) y `premium_lifetime` (compra única).
4. Publica `docs/privacy.html` (por ejemplo con GitHub Pages) y enlázala en la ficha de Play. En la ficha, deja claro que la app no está vinculada a ningún organismo público.
