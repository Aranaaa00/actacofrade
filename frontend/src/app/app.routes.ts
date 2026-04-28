import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

const ROLES_ALL = ['ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR', 'CONSULTA'];
const ROLES_MANAGE = ['ADMINISTRADOR', 'RESPONSABLE'];

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./pages/landing/landing').then(m => m.Landing)
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then(m => m.Login)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register').then(m => m.Register)
  },
  {
    path: '',
    loadComponent: () => import('./layout/main/main').then(m => m.Main),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard)
      },
      {
        path: 'events',
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-list/act-list').then(m => m.ActList)
      },
      {
        path: 'events/new',
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/act-editor/act-editor').then(m => m.ActEditor)
      },
      {
        path: 'events/:id',
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-detail/act-detail').then(m => m.ActDetail)
      },
      {
        path: 'events/:id/edit',
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/act-editor/act-editor').then(m => m.ActEditor)
      },
      {
        path: 'events/:eventId/close',
        canActivate: [roleGuard(ROLES_MANAGE)],
        loadComponent: () => import('./pages/close-event/close-event').then(m => m.CloseEvent)
      },
      {
        path: 'my-tasks',
        canActivate: [roleGuard(['ADMINISTRADOR', 'RESPONSABLE', 'COLABORADOR'])],
        loadComponent: () => import('./pages/my-tasks/my-tasks').then(m => m.MyTasks)
      },
      {
        path: 'history',
        canActivate: [roleGuard(ROLES_ALL)],
        loadComponent: () => import('./pages/act-history/act-history').then(m => m.ActHistory)
      },
      {
        path: 'users',
        canActivate: [roleGuard(['ADMINISTRADOR'])],
        loadComponent: () => import('./pages/users/users').then(m => m.Users)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
