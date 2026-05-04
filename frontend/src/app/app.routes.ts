import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';
import { ROLES_ADMIN, ROLES_ALL, ROLES_MANAGE, ROLES_WRITE } from './shared/constants/roles.const';

// Each route declares a human-readable title used by BrowserService.
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    data: { title: 'Bienvenida' },
    loadComponent: () => import('./pages/landing/landing').then(m => m.Landing)
  },
  {
    path: 'login',
    data: { title: 'Acceso' },
    loadComponent: () => import('./pages/login/login').then(m => m.Login)
  },
  {
    path: 'register',
    data: { title: 'Crear cuenta' },
    loadComponent: () => import('./pages/register/register').then(m => m.Register)
  },
  {
    path: '',
    loadComponent: () => import('./layout/main/main').then(m => m.Main),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        data: { title: 'Panel principal' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard)
      },
      {
        path: 'events',
        data: { title: 'Actos' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-list/act-list').then(m => m.ActList)
      },
      {
        path: 'events/new',
        data: { title: 'Nuevo acto' },
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/act-editor/act-editor').then(m => m.ActEditor)
      },
      {
        path: 'events/:id',
        data: { title: 'Detalle del acto' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-detail/act-detail').then(m => m.ActDetail)
      },
      {
        path: 'events/:id/edit',
        data: { title: 'Editar acto' },
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/act-editor/act-editor').then(m => m.ActEditor)
      },
      {
        path: 'events/:eventId/close',
        data: { title: 'Cerrar acto' },
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/close-event/close-event').then(m => m.CloseEvent)
      },
      {
        path: 'my-tasks',
        data: { title: 'Mis tareas' },
        canActivate: [roleGuard(ROLES_WRITE)],
        loadComponent: () => import('./pages/my-tasks/my-tasks').then(m => m.MyTasks)
      },
      {
        path: 'history',
        data: { title: 'Historial' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-history/act-history').then(m => m.ActHistory)
      },
      {
        path: 'users',
        data: { title: 'Usuarios' },
        canActivate: [roleGuard(ROLES_ADMIN)],
        loadComponent: () => import('./pages/users/users').then(m => m.Users)
      },
      {
        path: 'settings',
        data: { title: 'Configuración' },
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/settings/settings').then(m => m.Settings)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
