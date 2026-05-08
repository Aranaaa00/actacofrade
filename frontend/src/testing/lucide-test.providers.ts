import { LUCIDE_ICONS, LucideIconProvider, House, FileText, ChevronDown, ChevronLeft, ChevronRight, Clock, Users, Settings, LogOut, Menu, X, Search, Diamond, TriangleAlert, Lock, Calendar, Gem, Check, Minus, UserPlus, Building2, MapPin, Info, Link, History, Pencil, Upload, Trash2, Send, Inbox, Shield, UserCog } from 'lucide-angular';

export const LUCIDE_TEST_PROVIDERS = [
  {
    provide: LUCIDE_ICONS,
    multi: true,
    useValue: new LucideIconProvider({ House, FileText, ChevronDown, ChevronLeft, ChevronRight, Clock, Users, Settings, LogOut, Menu, X, Search, Diamond, TriangleAlert, Lock, Calendar, Gem, Check, Minus, UserPlus, Building2, MapPin, Info, Link, History, Pencil, Upload, Trash2, Send, Inbox, Shield, UserCog }),
  },
];
