# Las dependencias (AdMob, Billing) traen sus propias reglas.

# Room crea las bases de datos (como la de WorkManager, que trae AdMob) por reflexión
# con el constructor vacío. R8 lo elimina y la app se cierra al abrirse.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
