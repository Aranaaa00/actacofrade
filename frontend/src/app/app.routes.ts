import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import { ROLES_ADMIN, ROLES_ALL, ROLES_MANAGE, ROLES_SUPER_ADMIN, ROLES_WRITE } from './shared/constants/roles.const';

// suffix appended by the Title service to every page title
const APP_NAME = 'ActaCofrade';

// build a title string for the document head
const pageTitle = (label: string): string => `${label} | ${APP_NAME}`;

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: APP_NAME,
    data: { description: 'Pagina de bienvenida de ActaCofrade.' },
    loadComponent: () => import('./pages/landing/landing').then(m => m.Landing)
  },
  {
    path: 'login',
    title: pageTitle('Iniciar sesión'),
    data: { description: 'Accede a tu cuenta de ActaCofrade.' },
    loadComponent: () => import('./pages/login/login').then(m => m.Login)
  },
  {
    path: 'register',
    title: pageTitle('Registro'),
    data: { description: 'Crea una cuenta para gestionar los actos de tu hermandad.' },
    loadComponent: () => import('./pages/register/register').then(m => m.Register)
  },
  {
    path: 'auth/check-email',
    title: pageTitle('Confirma tu correo'),
    data: { description: 'Te hemos enviado un correo de verificación para activar la cuenta.' },
    loadComponent: () => import('./pages/auth/check-email/check-email').then(m => m.CheckEmail)
  },
  {
    path: 'auth/verify-email',
    title: pageTitle('Verificación de correo'),
    data: { description: 'Confirma tu correo y activa tu cuenta de ActaCofrade.' },
    loadComponent: () => import('./pages/auth/verify-email/verify-email').then(m => m.VerifyEmail)
  },
  {
    path: 'auth/reset-password',
    title: pageTitle('Restablecer contraseña'),
    data: { description: 'Define una nueva contraseña tras la intervención del administrador.' },
    loadComponent: () => import('./pages/auth/reset-password/reset-password').then(m => m.ResetPassword)
  },
  {
    path: 'terminos',
    title: pageTitle('Términos y condiciones'),
    data: { description: 'Condiciones de uso de ActaCofrade.' },
    loadComponent: () => import('./pages/legal/terms/terms').then(m => m.Terms)
  },
  {
    path: 'privacidad',
    title: pageTitle('Política de privacidad'),
    data: { description: 'Cómo tratamos tus datos en ActaCofrade.' },
    loadComponent: () => import('./pages/legal/privacy/privacy').then(m => m.Privacy)
  },
  {
    path: 'cookies',
    title: pageTitle('Política de cookies'),
    data: { description: 'Almacenamiento técnico utilizado en ActaCofrade.' },
    loadComponent: () => import('./pages/legal/cookies/cookies').then(m => m.Cookies)
  },
  {
    path: '',
    loadComponent: () => import('./layout/main/main').then(m => m.Main),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        title: pageTitle('Panel'),
        data: { description: 'Resumen de actos, alertas y acciones pendientes.' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard)
      },
      {
        path: 'events',
        title: pageTitle('Actos'),
        data: { description: 'Listado de actos con filtros por tipo, estado y fecha.' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-list/act-list').then(m => m.ActList)
      },
      {
        path: 'events/new',
        title: pageTitle('Nuevo acto'),
        data: { description: 'Crea un nuevo acto para tu hermandad.' },
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/act-editor/act-editor').then(m => m.ActEditor)
      },
      {
        path: 'events/:id',
        title: pageTitle('Detalle del acto'),
        data: { description: 'Detalle del acto con tareas, decisiones e incidencias.' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-detail/act-detail').then(m => m.ActDetail)
      },
      {
        path: 'events/:id/edit',
        title: pageTitle('Editar acto'),
        data: { description: 'Edita los datos del acto seleccionado.' },
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/act-editor/act-editor').then(m => m.ActEditor)
      },
      {
        path: 'events/:eventId/close',
        title: pageTitle('Cerrar acto'),
        data: { description: 'Cierra el acto y genera el resumen final.' },
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/close-event/close-event').then(m => m.CloseEvent)
      },
      {
        path: 'my-tasks',
        title: pageTitle('Mis tareas'),
        data: { description: 'Tareas asignadas al usuario actual.' },
        canActivate: [roleGuard(ROLES_WRITE)],
        loadComponent: () => import('./pages/my-tasks/my-tasks').then(m => m.MyTasks)
      },
      {
        path: 'history',
        title: pageTitle('Historial'),
        data: { description: 'Historial de actos realizados por la hermandad.' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-history/act-history').then(m => m.ActHistory)
      },
      {
        path: 'users',
        title: pageTitle('Usuarios'),
        data: { description: 'Gestion de usuarios y roles de la hermandad.' },
        canActivate: [roleGuard(ROLES_ADMIN)],
        loadComponent: () => import('./pages/users/users').then(m => m.Users)
      },
      {
        path: 'settings',
        title: pageTitle('Ajustes'),
        data: { description: 'Preferencias de cuenta y datos de la hermandad.' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/settings/settings').then(m => m.Settings)
      },
      {
        path: 'super-admin',
        title: pageTitle('Super administrador'),
        data: { description: 'Gestión de solicitudes de soporte enviadas por las hermandades.' },
        canActivate: [roleGuard(ROLES_SUPER_ADMIN)],
        loadComponent: () => import('./pages/super-admin/super-admin').then(m => m.SuperAdmin)
      },
      {
        path: 'super-admin/usuarios',
        title: pageTitle('Centro de Intervención'),
        data: { description: 'Acciones manuales sobre cuentas: estado, verificación, rol y reseteo de contraseña.' },
        canActivate: [roleGuard(ROLES_SUPER_ADMIN)],
        loadComponent: () => import('./pages/super-admin-users/super-admin-users').then(m => m.SuperAdminUsers)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
