import { TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { Header } from './header';
import { ToastService } from '../../services/toast.service';
import { BrowserService } from '../../shared/services/browser.service';
import { LUCIDE_TEST_PROVIDERS } from '../../../testing/lucide-test.providers';

describe('Header', () => {
  let connection$: BehaviorSubject<boolean>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    connection$ = new BehaviorSubject<boolean>(true);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['warning', 'success']);
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [
        LUCIDE_TEST_PROVIDERS,
        { provide: ToastService, useValue: toast },
        { provide: BrowserService, useValue: { connectionStatus$: connection$ } },
      ],
    }).compileComponents();
  });

  it('creates and subscribes to connection changes', () => {
    const fixture = TestBed.createComponent(Header);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('emits warning on connection lost and success on recovery', () => {
    const fixture = TestBed.createComponent(Header);
    fixture.detectChanges();
    connection$.next(false);
    expect(toast.warning).toHaveBeenCalled();
    connection$.next(true);
    expect(toast.success).toHaveBeenCalled();
  });
});
