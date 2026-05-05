package com.actacofrade.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 (Swagger) configuration.
 *
 * Exposed endpoints:
 *   - /v3/api-docs           - OpenAPI JSON definition
 *   - /swagger-ui.html       - Interactive UI
 *   - /swagger-ui/index.html - Direct UI entry
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI actaCofradeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ActaCofrade Backend API")
                        .description("""
                                API REST para la gestión de actos cofrades, tareas, incidencias,
                                decisiones, usuarios y auditoría dentro de una hermandad.

                                ## Autenticación

                                La mayoría de los endpoints requieren un token JWT en la cabecera
                                `Authorization: Bearer <token>`. El token se obtiene mediante
                                `POST /api/auth/login` o `POST /api/auth/register`.

                                ## Roles soportados

                                - `ADMINISTRADOR`: gestión total de la hermandad y los usuarios.
                                - `RESPONSABLE`: gestión operativa de actos, tareas y decisiones.
                                - `COLABORADOR`: ejecución de tareas asignadas y reporte de incidencias.
                                - `CONSULTA`: solo lectura.

                                ## Formato de error estándar

                                Todas las respuestas de error siguen el mismo contrato:
                                ```json
                                {
                                  "status": "error",
                                  "message": "Descripción legible",
                                  "data": null,
                                  "errors": []
                                }
                                ```
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ActaCofrade")
                                .email("soporte@actacofrade.local"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://actacofrade.local")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("/").description("Mismo origen (proxy)")
                ))
                .tags(List.of(
                        new Tag().name("Auth").description("Registro e inicio de sesión"),
                        new Tag().name("Events").description("Gestión de actos cofrades"),
                        new Tag().name("Tasks").description("Tareas asociadas a un acto"),
                        new Tag().name("Incidents").description("Incidencias detectadas durante un acto"),
                        new Tag().name("Decisions").description("Decisiones tomadas por la junta"),
                        new Tag().name("Users").description("Gestión de usuarios de la hermandad"),
                        new Tag().name("Me").description("Operaciones del usuario autenticado"),
                        new Tag().name("My Tasks").description("Tareas del usuario autenticado"),
                        new Tag().name("Dashboard").description("Métricas agregadas"),
                        new Tag().name("Audit Log").description("Histórico de cambios"),
                        new Tag().name("Hermandad").description("Datos de la hermandad"),
                        new Tag().name("Roles").description("Catálogo de roles"),
                        new Tag().name("Export").description("Exportación de actos a PDF/CSV")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en /api/auth/login")));
    }
}
