# FaunaReborn v1.0.0

Primera release estable de **FaunaReborn** para servidores **Paper/Folia 1.21.x**.

## Added
- Sistema de hostilidad para chicken, cow y pig con comportamiento social.
- Escalado de agresividad por entorno (clima, condiciones del mundo y contexto de juego).
- Panel GUI para control en runtime de módulos.
- Comando `/fauna` con subcomandos orientados a administración y diagnóstico.
- Soporte multilenguaje (`en`, `es`, `pt`) y cambio de idioma en runtime.
- Compatibilidad Paper y Folia con adaptadores de scheduling.

## Changed
- Arquitectura modular/refactors para mejorar mantenibilidad y consistencia entre entidades.
- Mejoras de rendimiento y limpieza de rutas de configuración.
- Mejoras de organización documental y estructura de configuración YAML.

## Fixed
- Ajustes en cooldowns y lógica de agresión.
- Correcciones de concurrencia/estado compartido para entornos Folia.
- Reducción de código duplicado en módulos de hostilidad/settings.

## Compatibility
- Minecraft: **1.21.x**
- Server software: **Paper/Folia**

## Notes
- Esta versión marca el inicio de la línea estable `1.x`.
- Se recomienda respaldar configuraciones antes de actualizar versiones futuras.
