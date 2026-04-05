import { Routes } from '@angular/router';

export const routes: Routes = [
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
  }
];
