import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { LUCIDE_ICONS, LucideIconProvider, House, FileText, ChevronDown, ChevronLeft, ChevronRight, Clock, Users, Settings, LogOut, Menu, X, Search, Diamond, TriangleAlert, Lock, Calendar, Gem, Check, Minus, UserPlus, Building2, MapPin, Info, Link, History, Pencil, Upload, Trash2 } from 'lucide-angular';
import { authInterceptor } from './interceptors/auth.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    {
      provide: LUCIDE_ICONS,
      multi: true,
      useValue: new LucideIconProvider({ House, FileText, ChevronDown, ChevronLeft, ChevronRight, Clock, Users, Settings, LogOut, Menu, X, Search, Diamond, TriangleAlert, Lock, Calendar, Gem, Check, Minus, UserPlus, Building2, MapPin, Info, Link, History, Pencil, Upload, Trash2 })
    }
  ]
};
