import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/landing/landing').then(m => m.Landing)
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then(m => m.Login)
  },
  {
    path: 'events/new',
    loadComponent: () => import('./pages/event-form/event-form').then(m => m.EventForm),
    canActivate: [authGuard]
  },
  {
    path: 'events/:id/edit',
    loadComponent: () => import('./pages/event-form/event-form').then(m => m.EventForm),
    canActivate: [authGuard]
  },
  {
    path: 'events/:eventId/decisions/new',
    loadComponent: () => import('./pages/decision-form/decision-form').then(m => m.DecisionForm),
    canActivate: [authGuard]
  },
  {
    path: 'events/:eventId/decisions/:decisionId/edit',
    loadComponent: () => import('./pages/decision-form/decision-form').then(m => m.DecisionForm),
    canActivate: [authGuard]
  },
  {
    path: 'events/:eventId/incidents/new',
    loadComponent: () => import('./pages/incident-form/incident-form').then(m => m.IncidentForm),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
