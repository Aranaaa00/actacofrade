import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

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
    path: '',
    loadComponent: () => import('./layout/main/main').then(m => m.Main),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard)
      },
      {
        path: 'events/new',
        loadComponent: () => import('./pages/event-form/event-form').then(m => m.EventForm)
      },
      {
        path: 'events/:id/edit',
        loadComponent: () => import('./pages/event-form/event-form').then(m => m.EventForm)
      },
      {
        path: 'events/:eventId/decisions/new',
        loadComponent: () => import('./pages/decision-form/decision-form').then(m => m.DecisionForm)
      },
      {
        path: 'events/:eventId/decisions/:decisionId/edit',
        loadComponent: () => import('./pages/decision-form/decision-form').then(m => m.DecisionForm)
      },
      {
        path: 'events/:eventId/incidents/new',
        loadComponent: () => import('./pages/incident-form/incident-form').then(m => m.IncidentForm)
      },
      {
        path: 'events/:eventId/close',
        loadComponent: () => import('./pages/close-event/close-event').then(m => m.CloseEvent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
